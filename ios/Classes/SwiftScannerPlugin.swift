import Flutter
import UIKit
import AVFoundation

public class SwiftScannerPlugin: NSObject, FlutterPlugin {
    
    static var factory: NativeViewFactory?
    
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "scanner", binaryMessenger: registrar.messenger())
    let instance = SwiftScannerPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
    factory = NativeViewFactory();
    registrar.register(factory!, withId: "com.newcoretech.flutter.plugin/scanner")
    debugPrint("register ----- call")
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    if(call.method == "beginScanner") {
        debugPrint("begin scanner ---- call")
        SwiftScannerPlugin.factory?.scanView?.beginScan()
        SwiftScannerPlugin.factory?.result = result
    } else if(call.method == "dispose") {
        debugPrint("dispose ---- call")
        SwiftScannerPlugin.factory?.scanView?.stop()
        SwiftScannerPlugin.factory?.scanView = nil
        result(nil)
    }
  }
}


class NativeViewFactory: NSObject, FlutterPlatformViewFactory {
    
    var scanView:NativeScanView?
    public var result: FlutterResult? {
        didSet{
            if let view = scanView {
                view.result = self.result
            }
        }
    }
    
    func create(withFrame frame: CGRect, viewIdentifier viewId: Int64, arguments args: Any?) -> FlutterPlatformView {
        debugPrint("create ---- call")
        scanView = NativeScanView()
        scanView?.result = result
        return scanView!
    }
}


class NativeScanView: NSObject, FlutterPlatformView, AVCaptureMetadataOutputObjectsDelegate {
    
    func beginScan() {
        guard !session.isRunning else {
            return;
        }
        session.startRunning()
        scanBorderView.startAnimation()
    }
    
    func stop(){
        debugPrint("stop --- call")
        if(session.isRunning){
            debugPrint("stop running --- call")
            session.stopRunning()
            self.scanBorderView.stopAnimation()
        }
    }
    
    public var result: FlutterResult?
    
    private let session = AVCaptureSession();
    private let scanBorderView = ScanBorderView();
    
    /// 屏幕 Bounds
    private var screenBounds: CGRect = UIScreen.main.bounds
    
    /// 屏幕宽度
    private var screenWidth: CGFloat = UIScreen.main.bounds.width
    
    /// 屏幕高度
    private var screenHeight: CGFloat = UIScreen.main.bounds.height
    
    private var frame: CGRect = UIScreen.main.bounds;
    
    func view() -> UIView {
        debugPrint("flutter platform view ------- call")
        
        let view = UIView();
        
        view.frame = frame
        view.addSubview(scanBorderView)
        scanBorderView.frame = frame;
        setupScanDevice(view: view)
        
        
        
        return view;
    }
    
    private func setupScanDevice(view: UIView) {
        guard let device = AVCaptureDevice.default(for: .video) else {
            return
        }
        guard let input = try? AVCaptureDeviceInput(device: device) else {
            UIAlertView(title: nil, message: "请在\"设置\"中打开\"新核云\"的相机权限", delegate: nil, cancelButtonTitle: "OK")
            return
        }
        let output = AVCaptureMetadataOutput()
        output.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
        if screenHeight < 500 {
            session.sessionPreset = .vga640x480
        } else {
            session.sessionPreset = .high
        }
        session.addInput(input)
        session.addOutput(output)
        output.metadataObjectTypes = [.qr, .ean13, .ean8, .code128, .code39,
                                      .code93, .code39Mod43, .pdf417, .aztec, .upce,
                                      .interleaved2of5, .itf14, .dataMatrix]
        
        /// 计算中间可探测区域
        var scanRect = CGRect(
            x: (UIScreen.main.bounds.size.width - 257) / 2,
            y: 215,
            width: 257,
            height: 257
        )
        // 计算 rect of interest, 注意 x, y 交换位置
        
        scanRect = CGRect(
            x: scanRect.origin.y / screenHeight,
            y: (screenWidth - (scanRect.width + scanRect.origin.x)) / screenWidth,
            width: scanRect.height / screenHeight,
            height: scanRect.width / screenWidth
        )
        // 计算可探测区域
        output.rectOfInterest = scanRect
        /// 输出预览 Layer
        let previewLayer = AVCaptureVideoPreviewLayer(session: session)
        previewLayer.videoGravity = AVLayerVideoGravity.resizeAspectFill
        previewLayer.frame = screenBounds
        view.layer.insertSublayer(previewLayer, at: 0)
        // 显示并开启探测动画
        scanBorderView.isHidden = false
//        scanBorderView.startAnimation()
//        session.startRunning()
    }
    
    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        /// 扫描数据
        guard let data = metadataObjects.first as? AVMetadataMachineReadableCodeObject else {
            return
        }
        /// 扫描数据的字符串值
        guard let strValue = data.stringValue else {
            return
        }
        session.stopRunning()
        scanBorderView.stopAnimation()
        debugPrint("strValue = \(strValue)")
        if let r = result {
            debugPrint("result ----- call")
            r(strValue);
        }
    }
    
}


class ScanBorderView: UIView {
    // MARK: - Subviews
    /// 边框
    let borderImageView = UIImageView(image: UIImage(named: "sw_scan_border"))
    let lineImageView = UIImageView(image: UIImage(named: "sw_scan_line"))
    let topView = UIView()
    let leadingView = UIView()
    let bottomView = UIView()
    let trailingView = UIView()
    let tipLabel = UILabel()
    
    // MARK: - Variables
    /// 扫描线移动的定时器
    var timer: Timer?
    /// 扫描线当前位置 Y 轴坐标
    var currentY = CGFloat(215)
    
    
    // MARK: - View
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        borderImageView.frame = CGRect(
            x: (bounds.size.width - 257) / 2,
            y: 215,
            width: 257,
            height: 257
        )
        tipLabel.frame = CGRect(x: (bounds.size.width - 180) / 2, y: 215 + 257 + 10, width: 180, height: 20)
        topView.frame = CGRect(x: 0, y: 0, width: UIScreen.main.bounds.size.width, height: 215 + 2)
        leadingView.frame = CGRect(x: 0, y: 215 + 2, width: borderImageView.frame.minX  + 2, height: borderImageView.frame.height - 4)
        bottomView.frame = CGRect(x: 0, y: borderImageView.frame.maxY - 2, width: UIScreen.main.bounds.size.width, height: UIScreen.main.bounds.size.height - borderImageView.frame.maxY + 2)
        trailingView.frame = CGRect(x: borderImageView.frame.maxX - 2, y: borderImageView.frame.minY + 2, width: UIScreen.main.bounds.size.width - borderImageView.frame.maxX - 2, height: borderImageView.frame.height - 4)
        lineImageView.frame = CGRect(
            x: (bounds.size.width - 257) / 2,
            y: 215,
            width: 257,
            height: 20
        )
    }
    
    // MARK: - Setup
    func setupUI() {
        addSubview(borderImageView)
        addSubview(lineImageView)
        addSubview(topView)
        addSubview(leadingView)
        addSubview(bottomView)
        addSubview(trailingView)
        addSubview(tipLabel)
        tipLabel.text = "对准二维码到框内即可扫描"
        tipLabel.textColor = .white
        tipLabel.font = UIFont.systemFont(ofSize: 14)
        tipLabel.textAlignment  = .center
        topView.backgroundColor = UIColor(white: 0, alpha: 0.3)
        leadingView.backgroundColor = UIColor(white: 0, alpha: 0.3)
        bottomView.backgroundColor = UIColor(white: 0, alpha: 0.3)
        trailingView.backgroundColor = UIColor(white: 0, alpha: 0.3)
    }
    
    @objc func lineAnimation() {
        currentY += 1
        if currentY > 215 + 257 - 20 {
            currentY = 215
        }
        var frame = lineImageView.frame
        frame.origin.y = currentY
        UIView.animate(withDuration: 0.01) {
            self.lineImageView.frame = frame
        }
    }
    
    func startAnimation() {
        timer = .scheduledTimer(
            timeInterval: 0.01,
            target: self,
            selector: #selector(lineAnimation),
            userInfo: nil, repeats: true
        )
        RunLoop.main.add(timer!, forMode: .common)
        timer?.fire()
    }
    
    func stopAnimation() {
        guard let tm = timer else {
            return
        }
        if tm.isValid {
            tm.invalidate()
            timer = nil
        }
    }
    
    deinit {
        debugPrint(("ScanBorderView - deinit"))
    }
}

