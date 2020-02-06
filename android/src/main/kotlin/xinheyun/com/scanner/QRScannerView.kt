package xinheyun.com.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import com.google.zxingnc.Result
import com.google.zxingnc.client.android.AmbientLightManager
import com.google.zxingnc.client.android.ViewfinderView
import com.google.zxingnc.client.android.BeepManager
import com.google.zxingnc.client.android.camera.CameraManager
import com.google.zxingnc.client.result.ResultParser
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.io.IOException
import java.lang.RuntimeException

class QRScannerView(private val context: Context, messenger: BinaryMessenger, id: Int?, params: Map<String, Any>?): PlatformView, MethodChannel.MethodCallHandler,  SurfaceHolder.Callback {
    private val TAG = "QRScannerView"

    private var hasSurfaceCreated = false
    private var methodResult:  MethodChannel.Result? = null
    private val decodeThread = HandlerThread("qrdecode_thread")
    private val decodeHandler by lazy {
        QRDecodeHandler(cameraManager, decodeCallback = onDecodeResultCallback, looper = decodeThread.looper)
    }

    private val uiHandler = Handler()

    private val cameraView = LayoutInflater.from(context).inflate(R.layout.nc_capture, null)

    //采集框view
    private val finderView by lazy {
        cameraView.findViewById(R.id.nc_viewfinder_view) as ViewfinderView
    }

    //渲染view
    private val surfaceView by lazy {
        cameraView.findViewById(R.id.nc_preview_view) as SurfaceView
    }
    //蜂鸣提示
    private val beepManager by lazy {
        BeepManager(context)
    }

    //闪光灯
    private val lightManager by lazy {
        AmbientLightManager(context)
    }

    private val cameraManager by lazy {
        CameraManager(context.applicationContext)
    }

    override fun getView(): View = cameraView

    override fun dispose() {
        lightManager.stop()
        beepManager.close()
        stop()
        surfaceView.holder.removeCallback(this)
        decodeThread.interrupt()
        decodeThread.quit()
    }

    init {
        MethodChannel(messenger, "scanner").setMethodCallHandler(this)

        cameraManager.setFrameSize(context.resources.getDimension(R.dimen.camera_rect_width))
        cameraManager.setScanSize(context.resources.getDimension(R.dimen.camera_scan_width))
        finderView.setCameraManager(cameraManager)
        lightManager.start(cameraManager)

        start()
    }

    /**
     * Flutter method call
     */
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        Log.i(TAG, "ScannerView call method: ${call.method}")
        when {
            call.method == "beginScanner" -> {
                start()
                methodResult = result
            }
            call.method == "dispose" -> dispose()
            else -> result.notImplemented()
        }
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {

    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        hasSurfaceCreated = false
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        Log.i(TAG, "onSurfaceCreated: $holder")
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!")
        }
        if(!hasSurfaceCreated){
            hasSurfaceCreated = true
            if(holder != null){
                initCamera(holder)
            }
        }
    }

    fun error(tag: String, message: String){
        methodResult?.error(tag, message, null)
    }

    fun start(){
        if(hasSurfaceCreated){
            initCamera(surfaceView.holder)
        }else{
            surfaceView.holder.addCallback(this)
        }
    }

    fun stop() {
        decodeThread.quit()
        cameraManager.stopPreview()
        cameraManager.closeDriver()
    }

    private fun initCamera(holder: SurfaceHolder){
        if(ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        Log.i(TAG, "Init camera")
        if(!cameraManager.isOpen){
            try{
                cameraManager.openDriver(holder)
                if(!decodeThread.isAlive){
                    decodeThread.start()
                }
                val rect = cameraManager.cameraFramingRect
                cameraManager.setManualFramingRect(rect.width(), rect.height())
                cameraManager.startPreview()
                cameraManager.requestPreviewFrame(decodeHandler, QRDecodeHandler.DECODE)
            }catch (ioe: IOException){
                Log.w(TAG, "IOException  initializing camera", ioe)
            }catch (re: RuntimeException){
                Log.w(TAG, "Unexpected error initializing camera", re)
            }
        }
    }

    /**
     * 解码结果回调
     */
    private val onDecodeResultCallback: DecodeCallback =  { rawResult, bitmapBytes, _ ->
        if(rawResult != null){
            //Decode success
            if(bitmapBytes?.isNotEmpty() == true){
                val barcode = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size, null).copy(Bitmap.Config.ARGB_8888, true)
                handleDecode(rawResult, barcode)
            }else{
                onHandleResult(null)
            }
        }else{
            //Decode failed
            cameraManager.requestPreviewFrame(decodeHandler, QRDecodeHandler.DECODE)
        }
    }

    private fun handleDecode(rawResult: Result, barcode: Bitmap){
        //播放蜂鸣提示
        beepManager.playBeepSoundAndVibrate()
        val parseResult = ResultParser.parseResult(rawResult)
        val content = parseResult.displayResult
        onHandleResult(content)
    }

    private fun onHandleResult(content: String?){
        uiHandler.post {
            methodResult?.success(content ?: "")
        }
    }
}