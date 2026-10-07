package com.example.ui.filters

import androidx.compose.ui.graphics.ColorMatrix
import java.io.Serializable

enum class FilterPresetType(val lutMode: Int) {
    BEAUTY(0),
    NATURAL_GLAM(1),
    VINTAGE_FILM(2),
    CYBERPUNK_NEON(3),
    SUNSET_GLOW(4),
    TOKYO_CHIC(5),
    CINEMA_DARK(6),
    MONOCHROME_SOFT(7),
    ROSY_ROMANCE(8)
}

data class BeautySettings(
    var skinSmoothness: Float = 60f,  // 0 to 100 slider (Bilateral blur)
    var skinWhitening: Float = 40f,   // 0 to 100 slider (Brightness / Tone)
    var lipTint: Float = 50f,         // 0 to 100 slider (Pink lip overlay)
    var eyeBright: Float = 45f        // 0 to 100 slider (Eye sharpening)
) : Serializable

data class PhotoFilter(
    val id: String,
    val name: String,
    val type: FilterPresetType,
    val subtitle: String = "",
    val rawMatrixValues: FloatArray = identityMatrixValues
) : Serializable {

    val colorMatrix: ColorMatrix
        get() = ColorMatrix(rawMatrixValues)

    fun getInterpolatedMatrix(intensityPercent: Float): ColorMatrix {
        val fraction = (intensityPercent / 100f).coerceIn(0f, 1f)
        val result = FloatArray(20)
        for (i in 0 until 20) {
            result[i] = identityMatrixValues[i] + fraction * (rawMatrixValues[i] - identityMatrixValues[i])
        }
        return ColorMatrix(result)
    }

    companion object {
        val identityMatrixValues = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )

        /**
         * Real Beauty Camera Filter + 8 Extra LUT-based Filters
         * Replaces all old filters completely
         */
        fun getAllFilters(): List<PhotoFilter> = listOf(
            PhotoFilter(
                id = "f_beauty",
                name = "Beauty Glow",
                type = FilterPresetType.BEAUTY,
                subtitle = "Smooth Skin, Whitening, Lip Tint & Eye Bright",
                rawMatrixValues = identityMatrixValues
            ),
            PhotoFilter(
                id = "f_natural_glam",
                name = "Natural Glam",
                type = FilterPresetType.NATURAL_GLAM,
                subtitle = "Warm radiant skin & soft contrast",
                rawMatrixValues = floatArrayOf(
                    1.12f, 0.05f, 0.02f, 0f, 12f,
                    0.03f, 1.08f, 0.02f, 0f, 10f,
                    0.02f, 0.04f, 0.96f, 0f, 14f,
                    0f, 0f, 0f, 1f, 0f
                )
            ),
            PhotoFilter(
                id = "f_vintage_film",
                name = "Vintage Film",
                type = FilterPresetType.VINTAGE_FILM,
                subtitle = "70s classic warm analog film tone",
                rawMatrixValues = floatArrayOf(
                    0.90f, 0.15f, 0.08f, 0f, 15f,
                    0.05f, 0.85f, 0.10f, 0f, 10f,
                    0.15f, 0.08f, 0.70f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                )
            ),
            PhotoFilter(
                id = "f_cyberpunk_neon",
                name = "Cyber Neon",
                type = FilterPresetType.CYBERPUNK_NEON,
                subtitle = "Vibrant electric magenta & cyan",
                rawMatrixValues = floatArrayOf(
                    1.35f, 0.00f, 0.35f, 0f, 20f,
                    0.00f, 1.10f, 0.50f, 0f, 0f,
                    0.15f, 0.35f, 1.55f, 0f, 30f,
                    0f, 0f, 0f, 1f, 0f
                )
            ),
            PhotoFilter(
                id = "f_sunset_glow",
                name = "Sunset Glow",
                type = FilterPresetType.SUNSET_GLOW,
                subtitle = "Golden hour warmth & amber light",
                rawMatrixValues = floatArrayOf(
                    1.28f, 0.12f, 0.00f, 0f, 25f,
                    0.08f, 1.10f, 0.02f, 0f, 12f,
                    0.00f, 0.05f, 0.75f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f
                )
            ),
            PhotoFilter(
                id = "f_tokyo_chic",
                name = "Tokyo Chic",
                type = FilterPresetType.TOKYO_CHIC,
                subtitle = "Cool urban pastel & crisp clarity",
                rawMatrixValues = floatArrayOf(
                    0.95f, 0.05f, 0.15f, 0f, 5f,
                    0.02f, 1.05f, 0.10f, 0f, 8f,
                    0.08f, 0.12f, 1.30f, 0f, 18f,
                    0f, 0f, 0f, 1f, 0f
                )
            ),
            PhotoFilter(
                id = "f_cinema_dark",
                name = "Cinema Dark",
                type = FilterPresetType.CINEMA_DARK,
                subtitle = "Moody cinematic shadows & rich depth",
                rawMatrixValues = floatArrayOf(
                    1.18f, 0.00f, 0.00f, 0f, -12f,
                    0.00f, 1.12f, 0.00f, 0f, -12f,
                    0.00f, 0.00f, 1.22f, 0f, -12f,
                    0f, 0f, 0f, 1f, 0f
                )
            ),
            PhotoFilter(
                id = "f_monochrome_soft",
                name = "Monochrome Soft",
                type = FilterPresetType.MONOCHROME_SOFT,
                subtitle = "Smooth high-contrast portrait B&W",
                rawMatrixValues = floatArrayOf(
                    0.33f, 0.59f, 0.11f, 0f, 8f,
                    0.33f, 0.59f, 0.11f, 0f, 8f,
                    0.33f, 0.59f, 0.11f, 0f, 8f,
                    0f, 0f, 0f, 1f, 0f
                )
            ),
            PhotoFilter(
                id = "f_rosy_romance",
                name = "Rosy Romance",
                type = FilterPresetType.ROSY_ROMANCE,
                subtitle = "Blushing pink cheeks & soft focus glow",
                rawMatrixValues = floatArrayOf(
                    1.25f, 0.10f, 0.15f, 0f, 20f,
                    0.05f, 1.02f, 0.08f, 0f, 10f,
                    0.08f, 0.08f, 1.10f, 0f, 15f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
}
