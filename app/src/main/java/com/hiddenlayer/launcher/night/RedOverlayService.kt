package com.hiddenlayer.launcher.night

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Paint
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.hiddenlayer.launcher.MainActivity
import com.hiddenlayer.launcher.R
import com.hiddenlayer.launcher.data.NightModeRepository

/**
 * Il velo rosso su **tutto** lo schermo, non solo sul launcher.
 *
 * È una finestra di sistema (`TYPE_APPLICATION_OVERLAY`) che sta sopra qualunque app. Vive in
 * un servizio in foreground perché deve sopravvivere all'uscita dal launcher: una sessione di
 * astrofotografia dura ore e passi il tempo dentro le app di mappe stellari e di controllo
 * della camera, non qui.
 *
 * **Quello che questo overlay non può fare, e non è un difetto di implementazione.** La
 * composizione fra finestre la fa SurfaceFlinger con alpha blending normale: da un'app non si
 * può chiedere un *multiply* contro il contenuto delle finestre sottostanti. Quindi il blu
 * viene molto smorzato ma non azzerato, e un po' di contrasto si perde. Dentro il launcher il
 * rosso è invece esatto, perché lì disegniamo noi e possiamo usare davvero il multiply (vedi
 * `redFilter` in `RedFilter.kt`).
 *
 * Altri limiti dichiarati: non copre la schermata di blocco né alcune finestre di sistema, e
 * da Android 12 le app che dichiarano `HIDE_OVERLAY_WINDOWS` (di solito quelle bancarie) lo
 * fanno sparire di proposito finché sono in primo piano.
 */
class RedOverlayService : Service() {

    private var overlay: View? = null
    private lateinit var repository: NightModeRepository

    override fun onCreate() {
        super.onCreate()
        repository = NightModeRepository(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForeground va chiamato **sempre e per primo**, anche quando questo avvio serve
        // solo a spegnere: arrivando da startForegroundService il sistema pretende la
        // promozione entro pochi secondi, e se manca uccide il processo con
        // ForegroundServiceDidNotStartInTimeException.
        startForeground(NOTIFICATION_ID, buildNotification())

        if (intent?.action == ACTION_STOP) {
            repository.setEnabled(false)
            removeOverlay()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Il permesso può essere stato revocato mentre il servizio non girava: senza questo
        // controllo addView solleverebbe e il processo cadrebbe.
        if (!Settings.canDrawOverlays(this)) {
            repository.setEnabled(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        showOrUpdateOverlay()
        // START_STICKY: se MIUI lo uccide comunque, al riavvio del servizio il velo torna.
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private fun showOrUpdateOverlay() {
        val (red, dim) = overlayLevels(repository.getRedIntensity(), repository.getDimLevel())

        val existing = overlay
        if (existing is RedOverlayView) {
            existing.setLevels(red, dim)
            return
        }

        val view = RedOverlayView(this).apply { setLevels(red, dim) }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // NOT_TOUCHABLE è ciò che rende il velo attraversabile: i tocchi arrivano all'app
            // sotto come se non ci fosse. NOT_FOCUSABLE evita che rubi la tastiera.
            // LAYOUT_NO_LIMITS lo fa arrivare sotto la barra di stato e quella di navigazione,
            // altrimenti resterebbero due strisce bianche in cima e in fondo.
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Senza questo il notch resta un rettangolo non coperto.
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        // Se il sistema rifiuta comunque (ROM che revoca il permesso di fatto, come sa fare
        // MIUI), si spegne in modo pulito invece di far cadere il processo.
        runCatching { windowManager.addView(view, params) }
            .onSuccess { overlay = view }
            .onFailure {
                repository.setEnabled(false)
                stopSelf()
            }
    }

    private fun removeOverlay() {
        val view = overlay ?: return
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        runCatching { windowManager.removeView(view) }
        overlay = null
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Modalità rossa",
                // IMPORTANCE_LOW: nessun suono e nessun avviso a comparsa. Una notifica che
                // salta fuori illuminata a metà di una posa sarebbe esattamente il contrario
                // di quello che serve.
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            manager.createNotificationChannel(channel)
        }

        val openLauncher = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, RedOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Modalità rossa attiva")
            .setContentText("Lo schermo è filtrato per la visione notturna.")
            // Vettoriale monocromatico, non l'icona adattiva: il sistema tinge di bianco le
            // icone piccole, e un'adattiva lì dentro rende male.
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openLauncher)
            .setOngoing(true)
            // Lo spegnimento deve essere raggiungibile da qualunque app: al buio non si torna
            // alla home per cercare un interruttore.
            .addAction(Notification.Action.Builder(null, "Spegni", stop).build())
            .build()
    }

    /**
     * Due strati sovrapposti: il rosso toglie il blu, il nero toglie luce. Sono due cose
     * diverse e vanno dosate separatamente — vedi il commento in `NightModeRepository`.
     */
    private class RedOverlayView(context: Context) : View(context) {
        private val paint = Paint()
        private var redAlpha = 0f
        private var dimAlpha = 0f

        fun setLevels(red: Float, dim: Float) {
            redAlpha = red
            dimAlpha = dim
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            paint.color = Color.argb((redAlpha * 255).toInt(), 255, 0, 0)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.color = Color.argb((dimAlpha * 255).toInt(), 0, 0, 0)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }

    companion object {
        /**
         * Traduce le due manopole nei due strati del velo, tenendole legate.
         *
         * Fuori dal launcher non si filtra: si **sovrappone**. Un velo rosso su un fondo nero
         * aggiunge luce invece di toglierla, quindi più rosso si vuole più bisogna scurire,
         * altrimenti il nero smette di essere nero e lo schermo diventa un rettangolo rosso
         * acceso — che è esattamente il bug segnalato con il cursore al massimo.
         *
         * Per questo l'attenuazione ha un **minimo che cresce con il rosso**: è calcolato
         * perché il velo non emetta più di [NightModeRepository.OVERLAY_GLOW_CAP] là dove
         * sotto c'è nero. Non è una scelta di gusto, è l'unico modo di avere insieme un velo
         * saturo e un fondo scuro con l'alpha blending.
         */
        fun overlayLevels(redIntensity: Float, dimLevel: Float): Pair<Float, Float> {
            val redAlpha = redIntensity.coerceIn(0f, 1f) * NightModeRepository.OVERLAY_RED_CEILING
            val cap = NightModeRepository.OVERLAY_GLOW_CAP
            val glowFloor = if (redAlpha > cap) 1f - cap / redAlpha else 0f
            val dimAlpha = maxOf(dimLevel.coerceIn(0f, 1f), glowFloor)
                .coerceAtMost(NightModeRepository.MAX_DIM)
            return redAlpha to dimAlpha
        }

        private const val CHANNEL_ID = "red_overlay"
        private const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "com.hiddenlayer.launcher.STOP_RED_OVERLAY"

        fun start(context: Context) {
            val intent = Intent(context, RedOverlayService::class.java)
            context.startForegroundService(intent)
        }

        /** Spegnimento dal launcher: `stopService` basta e non ha il vincolo dei pochi
         * secondi per promuoversi in foreground. L'azione sulla notifica passa invece da
         * ACTION_STOP, perché lì il servizio è già acceso. */
        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, RedOverlayService::class.java)) }
        }
    }
}
