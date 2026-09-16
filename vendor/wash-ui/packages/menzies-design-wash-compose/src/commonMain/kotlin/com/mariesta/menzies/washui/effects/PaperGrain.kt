package com.mariesta.menzies.washui.effects

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.mariesta.menzies.washui.theme.WashTheme
import kotlin.math.max
import kotlin.random.Random

/**
 * Optional paper-fiber overlay. Off by default: dense per-draw grain was a major
 * Android jank/ANR source on large panels. Enable only for decorative showcases.
 */
fun Modifier.paperGrain(
    enabled: Boolean = false,
    opacity: Float = 0.08f,
): Modifier = composed {
    if (!enabled) return@composed this
    val grain = WashTheme.colors.pigment_grain
    coarseGrainOverlay(grainColor = grain, opacity = opacity, seed = 42, stepPx = 14f)
}

/**
 * Soft pigment radial washes over [base_100], matching web `page-wash`.
 * Cached brushes rebuild only when size or pigment colors change.
 *
 * [grain] defaults to false for smooth full-screen scrolling. Prefer sparse
 * grain only on decorative previews; do not enable on every panel.
 */
fun Modifier.pageWash(
    grain: Boolean = false,
): Modifier = composed {
    val colors = WashTheme.colors
    val base = colors.base_100
    val washA = colors.wash_a
    val washB = colors.wash_b
    val washC = colors.wash_c
    val grainColor = colors.pigment_grain

    val wash = drawWithCache {
        val w = size.width
        val h = size.height
        val maxDim = max(w, h)

        val bloomA = Brush.radialGradient(
            colors = listOf(washA.copy(alpha = 0.78f), Color.Transparent),
            center = Offset(w * 0.06f, h * -0.05f),
            radius = maxDim * 0.55f,
        )
        val bloomB = Brush.radialGradient(
            colors = listOf(washB.copy(alpha = 0.68f), Color.Transparent),
            center = Offset(w * 0.96f, h * 0.04f),
            radius = maxDim * 0.48f,
        )
        val bloomC = Brush.radialGradient(
            colors = listOf(washC.copy(alpha = 0.55f), Color.Transparent),
            center = Offset(w * 0.72f, h * 1.0f),
            radius = maxDim * 0.42f,
        )
        val bloomCenter = Brush.radialGradient(
            colors = listOf(washA.copy(alpha = 0.28f), Color.Transparent),
            center = Offset(w * 0.40f, h * 0.45f),
            radius = maxDim * 0.35f,
        )

        onDrawBehind {
            drawRect(base)
            drawRect(brush = bloomA)
            drawRect(brush = bloomB)
            drawRect(brush = bloomC)
            drawRect(brush = bloomCenter)
        }
    }

    if (!grain) {
        return@composed wash
    }
    wash.then(
        coarseGrainOverlay(
            grainColor = grainColor,
            opacity = 0.05f,
            seed = 19,
            stepPx = 20f,
        ),
    )
}

/**
 * Panel fill. Background only by default (smooth scrolling / cold start).
 * Pass [grain] = true for a coarse cached fiber overlay (demo decorative use).
 */
fun Modifier.washPanel(
    grain: Boolean = false,
): Modifier = composed {
    val colors = WashTheme.colors
    val bg = colors.wash_panel_bg
    if (!grain) {
        return@composed drawWithCache {
            onDrawBehind {
                drawRect(bg)
            }
        }
    }
    val grainColor = colors.pigment_grain
    drawWithCache {
        onDrawBehind {
            drawRect(bg)
        }
    }.then(
        coarseGrainOverlay(
            grainColor = grainColor,
            opacity = 0.06f,
            seed = 7,
            stepPx = 16f,
        ),
    )
}

/**
 * Sparse grain points. [drawWithCache] rebuilds the point list only when size changes,
 * not on every frame (unlike the previous while-loop drawWithContent path).
 */
private fun Modifier.coarseGrainOverlay(
    grainColor: Color,
    opacity: Float,
    seed: Int,
    stepPx: Float,
): Modifier = drawWithCache {
    val random = Random(seed)
    val points = ArrayList<Pair<Offset, Float>>(64)
    var y = 0f
    while (y < size.height) {
        var x = 0f
        while (x < size.width) {
            if (random.nextFloat() > 0.78f) {
                points.add(Offset(x, y) to (0.5f + random.nextFloat()))
            }
            x += stepPx
        }
        y += stepPx
    }
    onDrawWithContent {
        drawContent()
        for (i in points.indices) {
            val (center, alphaScale) = points[i]
            drawCircle(
                color = grainColor.copy(alpha = opacity * alphaScale),
                radius = 0.9f,
                center = center,
                blendMode = BlendMode.Multiply,
            )
        }
    }
}
