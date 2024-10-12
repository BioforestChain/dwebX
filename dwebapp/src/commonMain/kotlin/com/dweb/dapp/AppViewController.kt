package com.dweb.dapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.SetSystemBarsColor
import org.dweb_browser.helper.platform.bindPureViewController
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.helper.platform.unbindPureViewController
import org.dweb_browser.sys.window.core.constant.LocalWindowMM
import org.dweb_browser.sys.window.render.SceneRender


class AppViewController(val viewController: IPureViewController) {
  private val resumeStateFlow = MutableStateFlow(false) // 增加字段，为了恢复 taskbarFloatView

  @Composable
  fun Render(
    runtime: NativeMicroModule.NativeRuntime,
    activityController: ActivityController,
  ) {
    val pureViewController = LocalPureViewController.current
    DisposableEffect(pureViewController) {
      runtime.bindPureViewController(pureViewController, true)
      onDispose {
        runtime.unbindPureViewController()
      }
    }
    val isDark = isSystemInDarkTheme()

    DwebBrowserAppTheme(isDark) {
      LocalCompositionChain.current.Provider(LocalWindowMM provides runtime) {
        /// 自适应状态栏
        // TODO 这里的颜色应该是自动适应的，特别是窗口最大化的情况下，遮盖了顶部 status-bar 的时候，需要根据 status-bar 来改变颜色
        SetSystemBarsColor(Color.Transparent, if (isDark) Color.White else Color.Black)

        /// 渲染桌面
//        desktopController.Render()

        /// 应用窗口渲染
//        val windowsManager by produceState<DesktopWindowsManager?>(null) {
//          value = desktopController.getDesktopWindowsManager()
//        }
//        windowsManager?.SceneRender()

        /// 顶级弹窗渲染
//        alertController.Render()

        /// 渲染实时活动
//        activityController.Render()
      }
    }
  }

  init {
    viewController.onCreate { params ->
      val sessionId = params.getString("deskSessionId")


//      val runtime = deskController.deskNMM
//      viewController.onDestroy {
//        runtime.shutdown()
//      }
//      val desktopController = deskController.getDesktopController()
//      viewController.addContent {
//        Render(
//          runtime = runtime,
//          desktopController = desktopController,
//          activityController = deskController.activityController
//        )
//      }
    }

    viewController.onResume {
      resumeStateFlow.value = true
    }

    viewController.onPause {
      resumeStateFlow.value = false
    }
  }
}
