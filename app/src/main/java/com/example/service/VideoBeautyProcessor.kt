package com.example.service

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class VideoBeautyPreset(
    val name: String,
    val description: String,
    val smoothing: Float,       // 0.0 to 1.0
    val toneWarmth: Float,      // -0.2 to +0.3
    val toneRosy: Float,        // 0.0 to +0.2
    val lightingBoost: Float,   // -0.1 to +0.3
    val contrast: Float,        // 0.9 to 1.2
    val edgeThreshold: Float    // edge preservation sensitivity
)

object VideoBeautyProcessor {
    val presets = listOf(
        VideoBeautyPreset(
            name = "Original",
            description = "Natural raw camera feed without post-processing",
            smoothing = 0.0f,
            toneWarmth = 0.0f,
            toneRosy = 0.0f,
            lightingBoost = 0.0f,
            contrast = 1.0f,
            edgeThreshold = 1.0f
        ),
        VideoBeautyPreset(
            name = "Natural",
            description = "Subtle micro-texture smoothing with delicate tone balancing",
            smoothing = 0.35f,
            toneWarmth = 0.04f,
            toneRosy = 0.03f,
            lightingBoost = 0.06f,
            contrast = 1.02f,
            edgeThreshold = 22f
        ),
        VideoBeautyPreset(
            name = "Clear",
            description = "Enhanced clarity with radiant highlights and crisp detail retention",
            smoothing = 0.40f,
            toneWarmth = -0.02f,
            toneRosy = 0.02f,
            lightingBoost = 0.12f,
            contrast = 1.06f,
            edgeThreshold = 24f
        ),
        VideoBeautyPreset(
            name = "Soft",
            description = "Velvet portrait glow with softened contours and diffused lighting",
            smoothing = 0.55f,
            toneWarmth = 0.03f,
            toneRosy = 0.04f,
            lightingBoost = 0.08f,
            contrast = 0.98f,
            edgeThreshold = 18f
        ),
        VideoBeautyPreset(
            name = "Glow",
            description = "Luminous, sunlit radiance with pearlescent tone enhancement",
            smoothing = 0.48f,
            toneWarmth = 0.08f,
            toneRosy = 0.06f,
            lightingBoost = 0.14f,
            contrast = 1.04f,
            edgeThreshold = 20f
        ),
        VideoBeautyPreset(
            name = "Fresh",
            description = "Invigorating blush tint with clean youthful vibrancy",
            smoothing = 0.38f,
            toneWarmth = 0.02f,
            toneRosy = 0.08f,
            lightingBoost = 0.10f,
            contrast = 1.03f,
            edgeThreshold = 22f
        ),
        VideoBeautyPreset(
            name = "Warm",
            description = "Golden-hour ambient glow with rich amber undertones",
            smoothing = 0.42f,
            toneWarmth = 0.14f,
            toneRosy = 0.02f,
            lightingBoost = 0.05f,
            contrast = 1.05f,
            edgeThreshold = 21f
        ),
        VideoBeautyPreset(
            name = "Classic",
            description = "Cinematic tone curve with timeless portrait depth",
            smoothing = 0.30f,
            toneWarmth = 0.06f,
            toneRosy = -0.01f,
            lightingBoost = 0.04f,
            contrast = 1.10f,
            edgeThreshold = 25f
        )
    )

    fun getPresetByName(name: String): VideoBeautyPreset {
        return presets.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: presets[1]
    }

    /**
     * Determines if a given pixel is within human skin chromaticity range.
     * Preserves eyes, eyebrows, lips, teeth, and hair by excluding non-skin loci.
     */
    private fun isSkinPixel(r: Int, g: Int, b: Int): Boolean {
        // Standard normalized RGB and YCbCr skin locus rules
        if (r < 60 || g < 40 || b < 20) return false
        if (r <= g || r <= b) return false
        if (r - g < 15) return false
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        if (maxC - minC < 15) return false
        // Exclude intense red of lips (where R is vastly greater than G and B)
        if (r > 180 && g < 80 && b < 80) return false
        return true
    }

    /**
     * Applies real-time beauty skin processing to a preview bitmap.
     * Uses bilateral skin smoothing + edge preservation + tone enhancement.
     */
    fun processBitmap(
        source: Bitmap,
        preset: VideoBeautyPreset,
        intensity: Float = 1.0f
    ): Bitmap {
        if (preset.name == "Original" || intensity <= 0.01f) {
            return source
        }

        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val outPixels = IntArray(width * height)

        val effSmoothing = preset.smoothing * intensity
        val effWarmth = preset.toneWarmth * intensity
        val effRosy = preset.toneRosy * intensity
        val effLight = preset.lightingBoost * intensity
        val effContrast = 1.0f + (preset.contrast - 1.0f) * intensity
        val edgeThresh = preset.edgeThreshold

        val radius = if (effSmoothing > 0.4f) 2 else 1

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val centerPixel = pixels[index]

                val cR = (centerPixel shr 16) and 0xFF
                val cG = (centerPixel shr 8) and 0xFF
                val cB = centerPixel and 0xFF

                if (!isSkinPixel(cR, cG, cB) || effSmoothing <= 0.05f) {
                    // Non-skin (eyes, hair, background) - apply only gentle global tone adjustments
                    val adjR = applyTone(cR, effWarmth + effLight, effContrast)
                    val adjG = applyTone(cG, effLight, effContrast)
                    val adjB = applyTone(cB, -effWarmth + effLight, effContrast)
                    outPixels[index] = Color.rgb(adjR, adjG, adjB)
                    continue
                }

                // Bilateral edge-preserving filter over local skin neighborhood
                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                var weightSum = 0f

                for (dy in -radius..radius) {
                    val ny = y + dy
                    if (ny < 0 || ny >= height) continue
                    for (dx in -radius..radius) {
                        val nx = x + dx
                        if (nx < 0 || nx >= width) continue

                        val nIndex = ny * width + nx
                        val nPixel = pixels[nIndex]
                        val nR = (nPixel shr 16) and 0xFF
                        val nG = (nPixel shr 8) and 0xFF
                        val nB = nPixel and 0xFF

                        // Color distance
                        val diff = abs(nR - cR) + abs(nG - cG) + abs(nB - cB)
                        if (diff < edgeThresh * 3) {
                            val spatialW = 1.0f / (1 + dx * dx + dy * dy)
                            val rangeW = 1.0f - (diff / (edgeThresh * 3.5f))
                            val w = spatialW * rangeW
                            sumR += nR * w
                            sumG += nG * w
                            sumB += nB * w
                            weightSum += w
                        }
                    }
                }

                val smoothedR = if (weightSum > 0f) sumR / weightSum else cR.toFloat()
                val smoothedG = if (weightSum > 0f) sumG / weightSum else cG.toFloat()
                val smoothedB = if (weightSum > 0f) sumB / weightSum else cB.toFloat()

                // Blend smoothed skin with original based on effSmoothing
                val finalR = cR * (1f - effSmoothing) + smoothedR * effSmoothing
                val finalG = cG * (1f - effSmoothing) + smoothedG * effSmoothing
                val finalB = cB * (1f - effSmoothing) + smoothedB * effSmoothing

                // Apply skin tone warming, rosy lift, and brightness
                val tonedR = applyTone(finalR.toInt(), effWarmth + effRosy + effLight, effContrast)
                val tonedG = applyTone(finalG.toInt(), effLight, effContrast)
                val tonedB = applyTone(finalB.toInt(), -effWarmth + effLight, effContrast)

                outPixels[index] = Color.rgb(tonedR, tonedG, tonedB)
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun applyTone(channel: Int, boost: Float, contrast: Float): Int {
        var v = channel.toFloat()
        // Contrast around mid-gray 128
        v = (v - 128f) * contrast + 128f
        // Boost
        v += boost * 100f
        return max(0, min(255, v.toInt()))
    }
}
