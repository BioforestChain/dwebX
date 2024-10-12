package info.bagen.dwebbrowser

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.os.Bundle
import android.transition.Fade
import android.view.Window
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.core.view.WindowCompat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import org.dweb_browser.helper.addStartActivityOptions
import org.dweb_browser.helper.compose.LocalCommonUrl
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.helper.removeStartActivityOptions
import org.dweb_browser.pure.image.compose.CoilAsyncImage

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
  private var mKeepOnAtomicBool by mutableStateOf(true)


  @OptIn(ExperimentalCoroutinesApi::class)
  @SuppressLint("ObjectAnimatorBinding", "CoroutineCreationDuringComposition")
  override fun onCreate(savedInstanceState: Bundle?) {
    WindowCompat.setDecorFitsSystemWindows(window, false) // 全屏
    /// 启动应用
    App.startMicroModuleProcess() // 启动MicroModule
    super.onCreate(savedInstanceState)
    with(window) {
      requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
      allowEnterTransitionOverlap = true
      allowReturnTransitionOverlap = true

      exitTransition = Fade()
    }
    addStartActivityOptions(this) {
      ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
    }

    setContent {
      DwebBrowserAppTheme {
        SplashMainView(
          Modifier,
          startAnimation = !mKeepOnAtomicBool,
        )
      }
    }
  }

  override fun onStop() {
    super.onStop()
    finish()
  }

  override fun onDestroy() {
    super.onDestroy()
    removeStartActivityOptions(this)
  }
}

@Composable
fun dpAni(targetValue: Dp, label: String, onFinished: () -> Unit = {}): Dp {
  return animateDpAsState(targetValue,
    animationSpec = tween(800, easing = EaseInOutQuart),
    label = label,
    finishedListener = { onFinished() }).value
}

@Composable
fun SplashMainView(modifier: Modifier, startAnimation: Boolean) {
  BoxWithConstraints(
    modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center,
  ) {
    var aniStart by remember { mutableStateOf(false) }
    val logoHeight = 288.dp//maxHeight * 0.566f
    var logoTop by remember { mutableStateOf(0.dp) }
    val bannerTop = logoHeight / 2 + logoTop

    if (startAnimation) {
      LaunchedEffect(null) {
        delay(10)
        val boxSize = min(maxWidth, maxHeight) * 1.618f
        logoTop = (boxSize * 0.217f) - ((maxHeight - logoHeight) / 2)
        delay(500)
        aniStart = true
      }
    }

    val logoOffsetY = dpAni(logoTop, "logoPaddingTop")
    CoilAsyncImage(
      model = "https://dweb.xn--fiqs8s/bird.gif",
      modifier = Modifier
        .requiredSize(288.dp)
        .offset {
          IntOffset(0, (logoOffsetY.value * density).toInt())
        },
      contentDescription = null,
      contentScale = ContentScale.Fit,
      onState = {
        it.painter
      },
    )
    val bannerOffsetY = dpAni(bannerTop, "bannerPaddingTop")
    Box(
      Modifier.offset {
        IntOffset(0, (bannerOffsetY.value * density).toInt())
      },
      contentAlignment = Alignment.TopCenter,
    ) {
      var brushStartX by remember { mutableFloatStateOf(0.5f) }
      var brushEndX by remember { mutableFloatStateOf(0.5f) }
      var brushColor by remember { mutableStateOf(Color.Transparent) }
      val toColor = MaterialTheme.colorScheme.primary
      if (aniStart) {
        brushStartX = 0f
        brushEndX = 1f
        brushColor = toColor
      }
      val animationSpec =
        remember { tween<Float>(durationMillis = 2000, easing = FastOutSlowInEasing) }
      val startX by animateFloatAsState(
        brushStartX, label = "startX", animationSpec = animationSpec
      )
      val endX by animateFloatAsState(brushEndX, label = "endX", animationSpec = animationSpec)
      val color by animateColorAsState(
        brushColor,
        label = "color",
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
      )
      val brush = Brush.horizontalGradient(
        colorStops = arrayOf(
          0f to Color.Transparent,
          startX to color,
          0.5f to toColor,
          endX to color,
          1f to Color.Transparent,
        ),
      )
      Text(
        " Plugins test ",
        style = MaterialTheme.typography.headlineLarge.merge(TextStyle(brush = brush)),
      )
    }
  }
}