package org.dweb_browser.browser

import org.dweb_browser.browser.data.DataNMM
import org.dweb_browser.browser.http.installDevRenderer
import org.dweb_browser.browser.jsProcess.JsProcessNMM
import org.dweb_browser.browser.mwebview.MultiWebViewNMM
import org.dweb_browser.browser.nativeui.torch.TorchNMM
import org.dweb_browser.browser.scan.SmartScanNMM
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.boot.BootNMM
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.core.std.http.HttpNMM
import org.dweb_browser.core.std.http.MultipartNMM
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.addDebugTags
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.sys.biometrics.BiometricsNMM
import org.dweb_browser.sys.clipboard.ClipboardNMM
import org.dweb_browser.sys.configure.ConfigNMM
import org.dweb_browser.sys.contact.ContactNMM
import org.dweb_browser.sys.device.DeviceNMM
import org.dweb_browser.sys.filechooser.FileChooserNMM
import org.dweb_browser.sys.haptics.HapticsNMM
import org.dweb_browser.sys.keychain.KeychainNMM
import org.dweb_browser.sys.location.LocationNMM
import org.dweb_browser.sys.media.MediaNMM
import org.dweb_browser.sys.mediacapture.MediaCaptureNMM
import org.dweb_browser.sys.motionSensors.MotionSensorsNMM
import org.dweb_browser.sys.notification.NotificationNMM
import org.dweb_browser.sys.permission.PermissionNMM
import org.dweb_browser.sys.share.ShareNMM
import org.dweb_browser.sys.shortcut.ShortcutNMM
import org.dweb_browser.sys.toast.ToastNMM


class AppLauncher(
  debugTags: List<String> = listOf(),
  private val extMM: List<ExtMicroModule<*>> = listOf(),
) {
  constructor(
    debugTag: String?,
    extMM: List<ExtMicroModule<*>> = listOf(),
  ) : this(
    debugTags = when (debugTag) {
      null -> emptyList()
      "*", "true" -> listOf("/.+/")
      else -> listOf(debugTag)
    },
    extMM = extMM,
  )

  class ExtMicroModule<out T : MicroModule>(
    factory: () -> T,
    val boot: Boolean,
    internal val onSetup: (@UnsafeVariance T) -> Unit = {},
  ) {
    internal val instance by lazy { factory() }
  }

  init {
    addDebugTags(debugTags)
  }

  /// 初始化DNS服务
  val dnsNMM = DnsNMM()

  suspend fun <T : MicroModule> T.setup() = this.also {
    dnsNMM.install(this)
  }

  suspend fun launch(): DnsNMM.DnsRuntime {
    val permissionNMM = PermissionNMM().setup()

    /// 安装系统应用
    val jsProcessNMM = JsProcessNMM().setup()
    val multiWebViewNMM = MultiWebViewNMM().setup()
    val httpNMM = HttpNMM().setup()
    httpNMM.installDevRenderer()

    val mediaCaptureNMM = MediaCaptureNMM().setup()
    val contactNMM = ContactNMM().setup()
    val shortcutNMM = ShortcutNMM().setup()
    /// 扫码
    val smartScanNMM = SmartScanNMM().setup()
    ///安装剪切板
    val clipboardNMM = ClipboardNMM().setup()
    ///设备信息
    val deviceNMM = DeviceNMM().setup()
    val configNMM = ConfigNMM().setup()
    ///位置
    val locationNMM = LocationNMM().setup()
    // 标准文件模块
    val fileNMM = FileNMM().setup()
    /// NFC
//  val nfcNMM = NfcNMM().setup()
    /// 通知
    val notificationNMM = NotificationNMM().setup()
    /// 弹窗
    val toastNMM = ToastNMM().setup()
    /// 分享
    val shareNMM = ShareNMM().setup()
    /// 振动效果
    val hapticsNMM = HapticsNMM().setup()
    /// 手电筒
    val torchNMM = TorchNMM().setup()
    /// 生物识别
    val biometricsNMM = BiometricsNMM().setup()
    /// 运动传感器
    val motionSensorsNMM = MotionSensorsNMM().setup()
    /// 媒体操作
    val mediaNMM = MediaNMM().setup()
    /// multipart
    val multipartNMM = MultipartNMM().setup()
    /// file chooser
    val fileChooser = FileChooserNMM().setup()

//    val aboutNMM = AboutNMM().setup()
    /// 安装存储管理
    val storeNMMSetup = SuspendOnce { DataNMM().setup() }
    envSwitch.watch(ENV_SWITCH_KEY.DWEBVIEW_PROFILE) {
      if (IDWebView.isEnableProfile) {
        storeNMMSetup()
      }
    }
    val keyChainNMM = KeychainNMM().setup()
//    val deskNMM = DeskNMM().setup()
    val bootMmidList = mutableListOf(
      deviceNMM.mmid, // 为了直接初始化设备ID
//      shortcutNMM.mmid, // 为了启动时，注入快捷内容
      httpNMM.mmid,// 启动时，初始化 pingPong
    )

    for (mm in extMM) {
      mm.instance.also {
        it.setup()
        mm.onSetup(it)
        if (mm.boot) {
          bootMmidList.add(it.mmid)
        }
      }
    }

    /// 启动程序
    val bootNMM = BootNMM(bootMmidList).setup()

    /// 启动
    val dnsRuntime = dnsNMM.bootstrap()
    dnsRuntime.boot(bootNMM)
    return dnsRuntime
  }
}