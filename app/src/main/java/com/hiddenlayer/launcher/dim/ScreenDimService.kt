package com.hiddenlayer.launcher.dim

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.hiddenlayer.launcher.MainActivity
import com.hiddenlayer.launcher.R
import com.hiddenlayer.launcher.data.DimRepository

/**
 * Il velo nero che porta lo schermo **sotto la luminosità minima di sistema**.
 *
 * È una finestra di sistema (`TYPE_APPLICATION_OVERLAY`) sopra qualunque app, tenuta in piedi
 * da un servizio in foreground perché deve restare anche quando esci dal launcher: serve
 * mentre leggi o guardi qualcosa, non mentre guardi la home.
 *
 * Attenuare è l'unica cosa che un overlay sa fare **bene**: sovrapporre del nero riduce la
 * luce emessa in modo esatto, senza gli effetti collaterali che ha invece un velo colorato
 * (che aggiunge luce e appiattisce il contrasto). Per questo qui non c'è nessuna tinta.
 *
 * Limiti dichiarati: non copre la schermata di blocco né alcune finestre di sistema, e da
 * Android 12 le app che dichiarano `HIDE_OVERLAY_WINDOWS` (tipicamente quelle bancarie) lo
 * fanno sparire di proposito mentre sono in primo piano.
 */
class ScreenDimService : Service() {

    private var overlay: View? = null
    private lateinit var repository: DimRepository

    override fun onCreate() {
        super.onCreate()
        repository = DimRepository(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForeground va chiamato **sempre e per primo**, anche sul ramo che spegne:
        // arrivando da startForegroundService il sistema pretende la promozione entro pochi
        // secondi, e se manca uccide il processo con
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
        val alpha = repository.getLevel()
        val veil = Color.argb((alpha * 255).toInt(), 0, 0, 0)

        val existing = overlay
        if (existing != null) {
            existing.setBackgroundColor(veil)
            return
        }

        val view = View(this).apply { setBackgroundColor(veil) }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // NOT_TOUCHABLE è ciò che rende il velo attraversabile: i tocchi arrivano all'app
            // sotto come se non ci fosse. NOT_FOCUSABLE evita che rubi la tastiera.
            // LAYOUT_NO_LIMITS lo fa arrivare sotto la barra di stato e quella di navigazione,
            // altrimenti resterebbero due strisce a piena luminosità in cima e in fondo.
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // LAYOUT_NO_LIMITS da solo non basta: senza dichiarare anche la modalità per il
            // ritaglio del display il sistema tiene la finestra **sotto** il notch, e in cima
            // resta una striscia non attenuata. Trappola già pagata con la schermata di blocco
            // della Concentrazione.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    } else {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
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
                "Luminosità extra",
                // IMPORTANCE_LOW: nessun suono e nessun avviso a comparsa. Una notifica che
                // salta fuori illuminata mentre stai attenuando lo schermo sarebbe il
                // contrario di quello che serve.
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
            Intent(this, ScreenDimService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Luminosità extra attiva")
            .setContentText("Lo schermo è più scuro del minimo di sistema.")
            // Vettoriale monocromatico, non l'icona adattiva: il sistema tinge di bianco le
            // icone piccole, e un'adattiva lì dentro rende male.
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openLauncher)
            .setOngoing(true)
            // Lo spegnimento deve essere raggiungibile da qualunque app: al buio, con lo
            // schermo attenuato, non si torna alla home a cercare un interruttore.
            .addAction(Notification.Action.Builder(null, "Spegni", stop).build())
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "screen_dim"
        private const val NOTIFICATION_ID = 43
        const val ACTION_STOP = "com.hiddenlayer.launcher.STOP_SCREEN_DIM"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ScreenDimService::class.java))
        }

        /** Spegnimento dal launcher: `stopService` basta e non ha il vincolo dei pochi secondi
         * per promuoversi in foreground. L'azione sulla notifica passa invece da ACTION_STOP,
         * perché lì il servizio è già acceso. */
        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, ScreenDimService::class.java)) }
        }
    }
}
