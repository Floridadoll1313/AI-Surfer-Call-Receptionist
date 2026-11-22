package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.OceanAbyss
import com.example.ui.theme.OceanCard
import com.example.ui.theme.OceanDeepBlue
import com.example.ui.theme.SandTextPrimary
import com.example.ui.theme.SandTextSecondary
import com.example.ui.theme.SeaGreen
import com.example.ui.theme.SunsetOrange
import com.example.ui.theme.SurfCyan
import com.example.ui.theme.SurfTeal
import com.example.ui.theme.SurfTurquoise
import kotlin.math.sin

@Composable
fun OceanWaveHeader(
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
  val wavePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2 * Math.PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(4000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "phase"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(130.dp)
      .background(
        Brush.verticalGradient(
          colors = listOf(OceanAbyss, OceanDeepBlue, OceanCard)
        )
      )
  ) {
    // Dynamic ocean wave canvas
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(70.dp)
        .align(Alignment.BottomCenter)
    ) {
      val width = size.width
      val height = size.height

      val wavePath = Path()
      wavePath.moveTo(0f, height * 0.5f)

      for (x in 0..width.toInt() step 10) {
        val y = height * 0.5f + (sin((x / width * 3 * Math.PI) + wavePhase).toFloat() * 14f)
        wavePath.lineTo(x.toFloat(), y)
      }
      wavePath.lineTo(width, height)
      wavePath.lineTo(0f, height)
      wavePath.close()

      drawPath(
        path = wavePath,
        brush = Brush.horizontalGradient(
          colors = listOf(
            SurfTeal.copy(alpha = 0.45f),
            SurfCyan.copy(alpha = 0.35f),
            SurfTurquoise.copy(alpha = 0.45f)
          )
        )
      )
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .align(Alignment.TopStart),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = SurfTurquoise.copy(alpha = 0.18f),
        modifier = Modifier.size(46.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Waves,
            contentDescription = "Wave Icon",
            tint = SurfTurquoise,
            modifier = Modifier.size(28.dp)
          )
        }
      }

      androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(12.dp))

      androidx.compose.foundation.layout.Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.5.sp
            ),
            color = SandTextPrimary
          )
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = SurfCyan
        )
      }
    }
  }
}

@Composable
fun UrgencyBadge(urgency: String, modifier: Modifier = Modifier) {
  val (bgColor, textColor, label) = when (urgency) {
    "URGENT_VIP" -> Triple(CoralAlert, Color.White, "⚡ URGENT VIP")
    "HIGH" -> Triple(SunsetOrange, Color.White, "🔥 HIGH")
    "MEDIUM" -> Triple(GoldenSun.copy(alpha = 0.25f), GoldenSun, "🌊 MEDIUM")
    else -> Triple(SeaGreen.copy(alpha = 0.2f), SeaGreen, "🌴 LOW")
  }

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = bgColor,
    modifier = modifier
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

@Composable
fun AudioWaveformIndicator(
  waveHeights: List<Float>,
  modifier: Modifier = Modifier,
  tint: Color = SurfTurquoise
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    waveHeights.forEach { heightRatio ->
      Box(
        modifier = Modifier
          .padding(horizontal = 2.dp)
          .size(width = 4.dp, height = (32 * heightRatio).coerceAtLeast(6f).dp)
          .background(tint, shape = RoundedCornerShape(2.dp))
      )
    }
  }
}
