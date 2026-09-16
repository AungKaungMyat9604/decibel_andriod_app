package com.mariesta.menzies.washui.effects

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class SplashVariant {
    Blob, Splash, Wash, Ring, Drip, Burst, Petal, Cloud, Puddle, Streak,
    Arc, Splatter, Feather, Wave, Ripple, Starburst, Comet, Smudge, Blot, Spray,
    Fan, Crescent, Teardrop, Cluster, Swirl, Blotchy, Streaky, Speckle, Halo, Notch,
    Shard, Bloom, Funnel, Tail, Crown, Island, Delta, Hook, Tendril, Crater,
}

data class WatercolorSplashConfig(
    val seed: Int,
    val variant: SplashVariant,
    val colors: List<Color>,
    val opacity: Float = 0.75f,
    val blurHint: Float = 18f,
    val spread: Float = 1f,
    val rotation: Float = 0f,
    val size: Float = 1f,
)

@Composable
fun rememberWatercolorSplash(
    seed: Int = 42,
    variant: SplashVariant = SplashVariant.Blob,
    colors: List<Color>,
): WatercolorSplashConfig = remember(seed, variant, colors) {
    WatercolorSplashConfig(seed = seed, variant = variant, colors = colors)
}

@Composable
fun WatercolorSplash(
    config: WatercolorSplashConfig,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val random = Random(config.seed)
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension * 0.22f * config.size * config.spread
        translate(center.x, center.y) {
            scale(config.size) {
                config.colors.forEachIndexed { index, color ->
                    val path = splashPath(config.variant, random, index)
                    drawPath(
                        path,
                        color = color.copy(alpha = config.opacity * (0.55f + index * 0.15f)),
                    )
                }
            }
        }
    }
}

private fun splashPath(variant: SplashVariant, random: Random, layer: Int): Path {
    val path = Path()
    val points = 10 + layer * 2
    val radius = 40f + layer * 12f + random.nextFloat() * 20f
    when (variant) {
        SplashVariant.Ring -> {
            path.addOval(androidx.compose.ui.geometry.Rect(-radius, -radius, radius, radius))
        }
        SplashVariant.Drip -> {
            path.moveTo(0f, -radius)
            path.quadraticTo(radius, 0f, 0f, radius * 1.4f)
            path.quadraticTo(-radius, 0f, 0f, -radius)
        }
        else -> {
            for (i in 0 until points) {
                val angle = (i / points.toFloat()) * 2f * PI.toFloat()
                val wobble = radius + random.nextFloat() * 18f
                val x = cos(angle) * wobble
                val y = sin(angle) * wobble
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
        }
    }
    return path
}
