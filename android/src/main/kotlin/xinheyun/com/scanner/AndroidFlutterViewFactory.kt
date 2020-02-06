package xinheyun.com.scanner

import android.content.Context
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class AndroidFlutterViewFactory(private val messenger: BinaryMessenger): PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    private var scannerView: QRScannerView? = null

    override fun create(context: Context, id: Int, args: Any?): PlatformView {
        val params = args?.let { args as Map<String, Any> }
        scannerView = QRScannerView(context, messenger, id, params)
        return scannerView!!
    }

    fun error(tag: String, message: String){
        scannerView?.error(tag, message)
    }

    fun start(){
        scannerView?.start()
    }
}