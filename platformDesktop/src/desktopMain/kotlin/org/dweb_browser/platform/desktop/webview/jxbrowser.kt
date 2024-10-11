package org.dweb_browser.platform.desktop.webview

import com.teamdev.jxbrowser.VersionInfo
import com.teamdev.jxbrowser.engine.Engine
import com.teamdev.jxbrowser.engine.EngineOptions
import com.teamdev.jxbrowser.engine.ProprietaryFeature
import com.teamdev.jxbrowser.engine.RenderingMode.HARDWARE_ACCELERATED
import com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN
import com.teamdev.jxbrowser.engine.event.EngineClosed
import kotlinx.atomicfu.locks.SynchronizedObject
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import java.nio.file.Path as NioPath


private const val KEY = "jxbrowser.license.key"
private const val LICENSE = ""

@RequiresOptIn(
  level = RequiresOptIn.Level.ERROR,
  message = "This API is low-level in jxBrowserEngine and should be used with caution."
)
public annotation class LowLevelWebEngineAPI

object jxBrowserEngine {
  val licenseKey = (System.getenv(KEY) ?: System.getProperty(KEY) ?: LICENSE)
  private val engineLock = SynchronizedObject()
  private val engineMaps = ConcurrentHashMap<String, Engine>()

  val allEngines get() = engineMaps.toMap()

  val sandboxChromiumDir = Paths.get(System.getProperty("user.home"))
    .resolve("jxbrowser/chromium/7.39.0")

  /**
   * 将输入流中的数据复制到输出流
   *
   * @param inputStream 要读取的输入流
   * @param outputStream 要写入的输出流
   * @throws IOException 如果读写过程中发生错误
   */

  init {
//    System.setProperty("jxbrowser.chromium.verification.off", "true")
    if (false) {
      if (!sandboxChromiumDir.isDirectory()) {
        val sandboxChromiumZip =
          Paths.get(System.getProperty("java.io.tmpdir")).resolve("jxbrowser/7.39.0.zip").toFile()
        val myChromiumZip = jxBrowserEngine.javaClass.getResourceAsStream("/jxbrowser-7.39.0.zip")!!
        myChromiumZip.pipeTo(sandboxChromiumZip.outputStream())
        val tmpChromiumDir =
          sandboxChromiumDir.parent.resolve(sandboxChromiumDir.name + ".tmp").toFile()
        easyUnZip(sandboxChromiumZip, tmpChromiumDir)
        tmpChromiumDir.renameTo(sandboxChromiumDir.toFile())
      }
    }

    /// 监听进程死亡，那么就关闭所有的 engine
    Runtime.getRuntime().addShutdownHook(Thread {
      synchronized(engineLock) {
        engineMaps.values.forEach { it.close() }
        engineMaps.clear()
      }
    })
  }

  private fun getOrCreateByDir(dir: NioPath, createEngine: () -> Engine) =
    synchronized(engineLock) {
      val key = dir.toAbsolutePath().toString()
      engineMaps.getOrPut(key) {
        createEngine().also {
          it.on(EngineClosed::class.java) {
            synchronized(engineLock) {
              engineMaps.remove(key)
            }
          }
        }
      }
    }

  @LowLevelWebEngineAPI
  fun hardwareAccelerated(
    dataDir: NioPath,
    optionsBuilder: (EngineOptions.Builder.() -> Unit)? = null,
  ): Engine = getOrCreateByDir(dataDir) {
    warpCreateEngine {
      Engine.newInstance(EngineOptions.newBuilder(HARDWARE_ACCELERATED).run {
//      chromiumDir(sandboxChromiumDir)
        optionsBuilder?.invoke(this)
        // 设置用户数据目录，这样WebApp退出再重新打开时能够读取之前的数据
        userDataDir(dataDir)
        // 加入音视频解码器的支持
        enableProprietaryFeature(ProprietaryFeature.HEVC)
        enableProprietaryFeature(ProprietaryFeature.WIDEVINE)
        enableProprietaryFeature(ProprietaryFeature.AAC)
        enableProprietaryFeature(ProprietaryFeature.H_264)
        this.licenseKey(licenseKey)
        build()
      })
    }
  }

  /**处理Chromium创建失败*/
  private fun warpCreateEngine(createEngine: () -> Engine) =
    runCatching {
      createEngine()
    }.getOrElse {
      it.message?.let { error ->
        val prefix = "Failed to extract Chromium binaries into "
        if (error.startsWith(prefix)) {
          Paths.get(error.substring(prefix.length)).toFile().deleteRecursively()
        }
      }
      createEngine()
    }

  @LowLevelWebEngineAPI
  fun offScreen(
    dataDir: NioPath,
    optionsBuilder: (EngineOptions.Builder.() -> Unit)? = null,
  ): Engine = getOrCreateByDir(dataDir) {
    warpCreateEngine {
      Engine.newInstance(EngineOptions.newBuilder(OFF_SCREEN).run {
//      chromiumDir(sandboxChromiumDir)
        optionsBuilder?.invoke(this)
        // 设置用户数据目录，这样WebApp退出再重新打开时能够读取之前的数据
        userDataDir(dataDir)
        this.licenseKey(licenseKey)
        build()
      })
    }
  }

  val chromiumVersion by lazy { VersionInfo.chromiumVersion() }


  init {
    println("QWQ chromiumVersion=$chromiumVersion")
  }
}
