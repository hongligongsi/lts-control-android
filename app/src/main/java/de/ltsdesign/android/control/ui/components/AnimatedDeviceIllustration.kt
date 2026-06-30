package de.ltsdesign.android.control.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.ltsdesign.android.control.ui.theme.BrandBlue
import de.ltsdesign.android.control.ui.theme.BrandBlueLight
import kotlin.math.cos
import kotlin.math.sin

/**
 * 设备示意图 - 跟 logo 同款(黑圆 + 两个蓝弧)
 *
 * 当 [spinning] = true 时,蓝色弧形缓慢旋转,表示设备在运行
 */
@Composable
fun AnimatedDeviceIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    spinning: Boolean = false,
    progress: Float = 0f,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fan")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    val finalRotation = if (spinning) rotation else 0f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2, h / 2)
            val outerR = (minOf(w, h) / 2) * 0.95f
            val innerR = outerR * 0.18f

            // 外环浅色细线
            drawCircle(
                color = BrandBlueLight.copy(alpha = 0.25f),
                radius = outerR,
                center = center,
                style = Stroke(width = 2f),
            )

            // 内部灰色环(料盘外壳阴影)
            drawCircle(
                color = Color(0xFFEEEEEE),
                radius = outerR * 0.85f,
                center = center,
            )

            rotate(degrees = finalRotation, pivot = center) {
                // 上方蓝色弧(冷却风扇)
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(BrandBlue, BrandBlueLight, BrandBlue),
                        center = center,
                    ),
                    startAngle = 200f,
                    sweepAngle = 100f,
                    useCenter = true,
                    topLeft = Offset(center.x - outerR * 0.75f, center.y - outerR * 0.75f),
                    size = Size(outerR * 1.5f, outerR * 1.5f),
                )

                // 下方蓝色弧(排气风扇,180° 旋转)
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(BrandBlueLight, BrandBlue, BrandBlueLight),
                        center = center,
                    ),
                    startAngle = 20f,
                    sweepAngle = 100f,
                    useCenter = true,
                    topLeft = Offset(center.x - outerR * 0.75f, center.y - outerR * 0.75f),
                    size = Size(outerR * 1.5f, outerR * 1.5f),
                )
            }

            // 中心黑圆(主轴/料盘)
            drawCircle(
                color = Color(0xFF0A0A0A),
                radius = innerR,
                center = center,
            )

            // 中心小高光
            drawCircle(
                color = Color(0xFF1A1A1A),
                radius = innerR * 0.5f,
                center = Offset(center.x - innerR * 0.15f, center.y - innerR * 0.15f),
            )

            // 进度环(围绕设备图,spinning 时显示)
            if (progress > 0f && spinning) {
                drawArc(
                    color = BrandBlue,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(center.x - outerR, center.y - outerR),
                    size = Size(outerR * 2, outerR * 2),
                    style = Stroke(width = 6f),
                )
            }

            // 散热孔(8 个小白点,外环上)
            for (i in 0 until 8) {
                val angle = (i * 45f + finalRotation * 0.2f) * (Math.PI / 180.0)
                val r = outerR * 0.95f
                val px = center.x + r * cos(angle).toFloat()
                val py = center.y + r * sin(angle).toFloat()
                drawCircle(
                    color = Color(0xFFBDBDBD),
                    radius = outerR * 0.04f,
                    center = Offset(px, py),
                )
            }
        }
    }
}
