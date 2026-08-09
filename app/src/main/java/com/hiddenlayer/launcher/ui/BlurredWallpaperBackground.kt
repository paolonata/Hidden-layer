package com.hiddenlayer.launcher.ui

import android.app.WallpaperManager
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Frosted-glass panel background: the device's actual wallpaper, blurred, with a dark
 * scrim on top for text contrast — instead of a flat opaque Material surface color.
 *
 * The blur is done ONCE, on a small (64px-wide) downscaled copy of the wallpaper, using a
 * plain CPU box blur — not `Modifier.blur()`. That modifier applies a RenderEffect at
 * *draw* time, scaled to the size of the layer it's attached to (here, the full screen),
 * regardless of the source bitmap's resolution: it re-blurs a screen-sized layer on every
 * single frame, which is expensive enough to visibly stutter scrolling on mid-range
 * hardware. Blurring a tiny bitmap once and just stretching the (already blurred) result to
 * fill the screen costs nothing per frame — a plain bitmap draw — and looks effectively
 * identical since blur removes fine detail anyway. As a bonus this works on API 26+, not
 * just 31+ like Modifier.blur().
 *
 * Reading the live wallpaper via WallpaperManager only works without extra permissions
 * while this app is the active home/launcher (which is the whole point of this project);
 * if it's ever unavailable, this silently falls back to just the scrim rather than crashing.
 */
/**
 * La copia sfocata, tenuta per tutto il processo.
 *
 * `produceState` senza chiavi riparte a ogni ingresso in composizione, quindi lo sfondo
 * veniva rifatto a ogni apertura del cassetto, della Concentrazione e delle impostazioni —
 * e due volte insieme durante la transizione fra due di queste. Ogni giro allocava una
 * bitmap **grande quanto lo schermo** (~10 MB su 1080×2400) solo per ridurla a 64 px:
 * memoria e pressione sul garbage collector proprio nel frame in cui parte l'animazione di
 * apertura. Il risultato non cambia finché non cambia lo sfondo, quindi si calcola una
 * volta sola; un cambio di sfondo si vede al riavvio del launcher.
 */
private var cachedBlur: Bitmap? = null

/**
 * [neutral] sostituisce la foto vera con [NightNeutralBackground]: serve alla modalità rossa,
 * dove il vero sfondo — spesso già rosso o arancione di suo, essendo il cielo notturno — non
 * diventa un pannello pulito ma resta la stessa foto colorata sotto un filtro, illeggibile.
 * Un fondo davvero neutro invece dà un rosso piatto e uniforme una volta filtrato.
 */
@Composable
fun BlurredWallpaperBackground(
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0.28f,
    neutral: Boolean = false
) {
    if (neutral) {
        Box(modifier = modifier.fillMaxSize().background(NightNeutralBackground))
        return
    }

    val context = LocalContext.current
    val blurredBitmap by produceState<Bitmap?>(initialValue = cachedBlur) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.Default) {
            runCatching {
                val wallpaper = WallpaperManager.getInstance(context).drawable?.toBitmap() ?: return@runCatching null
                downscaleAndBoxBlur(wallpaper, targetWidth = 64, passes = 3, radius = 3)
            }.getOrNull()
        }.also { cachedBlur = it }
    }

    Box(modifier = modifier.fillMaxSize()) {
        blurredBitmap?.let { bitmap ->
            Image(
                bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )
    }
}

private fun downscaleAndBoxBlur(source: Bitmap, targetWidth: Int, passes: Int, radius: Int): Bitmap {
    val targetHeight = (targetWidth.toFloat() * source.height / source.width).toInt().coerceAtLeast(1)
    val small = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    val pixels = IntArray(targetWidth * targetHeight)
    small.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

    repeat(passes) {
        boxBlurHorizontal(pixels, targetWidth, targetHeight, radius)
        boxBlurVertical(pixels, targetWidth, targetHeight, radius)
    }

    val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    result.setPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
    return result
}

private fun boxBlurHorizontal(pixels: IntArray, width: Int, height: Int, radius: Int) {
    if (radius < 1) return
    val temp = IntArray(pixels.size)
    for (y in 0 until height) {
        val rowStart = y * width
        for (x in 0 until width) {
            var a = 0
            var r = 0
            var g = 0
            var b = 0
            var count = 0
            for (dx in -radius..radius) {
                val xx = (x + dx).coerceIn(0, width - 1)
                val p = pixels[rowStart + xx]
                a += (p ushr 24) and 0xFF
                r += (p ushr 16) and 0xFF
                g += (p ushr 8) and 0xFF
                b += p and 0xFF
                count++
            }
            temp[rowStart + x] = ((a / count) shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
        }
    }
    System.arraycopy(temp, 0, pixels, 0, pixels.size)
}

private fun boxBlurVertical(pixels: IntArray, width: Int, height: Int, radius: Int) {
    if (radius < 1) return
    val temp = IntArray(pixels.size)
    for (x in 0 until width) {
        for (y in 0 until height) {
            var a = 0
            var r = 0
            var g = 0
            var b = 0
            var count = 0
            for (dy in -radius..radius) {
                val yy = (y + dy).coerceIn(0, height - 1)
                val p = pixels[yy * width + x]
                a += (p ushr 24) and 0xFF
                r += (p ushr 16) and 0xFF
                g += (p ushr 8) and 0xFF
                b += p and 0xFF
                count++
            }
            temp[y * width + x] = ((a / count) shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
        }
    }
    System.arraycopy(temp, 0, pixels, 0, pixels.size)
}
