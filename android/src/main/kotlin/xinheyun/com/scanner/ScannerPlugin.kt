package xinheyun.com.scanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.PluginRegistry

class ScannerPlugin(private val activity: Activity, private val viewFactory: AndroidFlutterViewFactory): PluginRegistry.RequestPermissionsResultListener {
  private val REQUEST_CODE_GRANT_PERMISSIONS = 5001

  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val viewFactory = AndroidFlutterViewFactory(registrar.messenger())
      registrar.platformViewRegistry().registerViewFactory("com.newcoretech.flutter.plugin/scanner",  viewFactory)

      ScannerPlugin(registrar.activity(), viewFactory).checkPermission()
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?): Boolean {
    if (requestCode == REQUEST_CODE_GRANT_PERMISSIONS && permissions?.size == 3) {
      if (grantResults!![0] == PackageManager.PERMISSION_GRANTED) {
        viewFactory.start()
      } else {

        AlertDialog.Builder(activity)
                .setTitle("提示")
                .setMessage("App需要一些权限，请前往设置里授予该权限")
                .setPositiveButton("设置") { _, _ ->
                  val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                  i.data = Uri.fromParts("package", activity.packageName, null)
                  activity.startActivityForResult(i, REQUEST_CODE_GRANT_PERMISSIONS)
                }.show()
        return false
      }

      return true
    }
    viewFactory.error("PERMISSION_DENIED", "为了App正常运行，请授予存储和摄像头使用权限")
    return false
  }

  fun checkPermission(): Boolean {
    return if (ContextCompat.checkSelfPermission(this.activity,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this.activity,
              arrayOf(Manifest.permission.CAMERA),
              REQUEST_CODE_GRANT_PERMISSIONS)
      false
    } else {
      true
    }
  }

}
