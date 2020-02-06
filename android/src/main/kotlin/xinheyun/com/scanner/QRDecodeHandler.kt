package xinheyun.com.scanner

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.google.zxingnc.*
import com.google.zxingnc.client.android.DecodeFormatManager
import com.google.zxingnc.client.android.camera.CameraManager
import com.google.zxingnc.common.HybridBinarizer
import java.io.ByteArrayOutputStream
import java.util.*

typealias DecodeCallback = (rawResult: Result?, bitmapBytes: ByteArray?, scaledFactor: Float) -> Unit


class QRDecodeHandler(private val cameraManager: CameraManager, private val decodeCallback: DecodeCallback, pointCallback: Function<ResultPoint>? = null, looper: Looper) : Handler(looper) {
    private val TAG = "QRDecodeHandler"

    companion object {
        const val DECODE = 1
    }

    private val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
    private val multiFormatReader = MultiFormatReader()

    init {
        val decodeFormats = EnumSet.noneOf(BarcodeFormat::class.java)
        decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS)
        decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS)
        decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS)
        decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS)
        decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS)
        decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS)
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats)
        if (pointCallback != null) {
            hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, pointCallback)
        }
        multiFormatReader.setHints(hints)
    }

    override fun handleMessage(msg: Message?) {
        super.handleMessage(msg)
        when (msg?.what) {
            DECODE -> {
                decode(msg.obj as ByteArray, msg.arg1, msg.arg2)
            }
        }
    }

    /**
     * 解码
     */
    private fun decode(data: ByteArray, width: Int, height: Int) {
        val start = System.currentTimeMillis()
        val w = height
        val h = width
        val rotateData = ByteArray(data.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                rotateData[x * height + height - y - 1] = data[x + y * width]
            }
        }
        val source = cameraManager.buildLuminanceSource(rotateData, w, h)
        var rawResult: Result? = null
        if (source != null) {
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            try {
                rawResult = multiFormatReader.decodeWithState(bitmap)
            } catch (e: ReaderException) {
                Log.e(TAG, e.toString())
            } finally {
                multiFormatReader.reset()
            }
        }

        if (rawResult != null) {
            val end = System.currentTimeMillis()
            Log.d(TAG, "Found barcode in ${end - start}ms")
            val pixels = source.renderThumbnail()
            val tw = source.thumbnailWidth
            val th = source.thumbnailHeight
            val bitmap = Bitmap.createBitmap(pixels, 0, tw, tw, th, Bitmap.Config.ARGB_8888)
            val bout = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bout)
            decodeCallback(rawResult, bout.toByteArray(), tw / source.width.toFloat())
        } else {
            decodeCallback(null, null, 0f)
        }

    }

}