/*
 * 
 * @Author     : Yongsheng.X
 * @Date       : 2019-09-26 17:04
 * @Description: 摄像头扫码功能
 *
 */

import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class ScannerController {
  VoidCallback beginScan;
}

class ScannerWidget extends StatefulWidget {
  final void Function(String result) onResult;
  final ScannerController scannerController;

  ScannerWidget({this.onResult, this.scannerController});

  @override
  _ScannerWidgetState createState() => _ScannerWidgetState();
}

class _ScannerWidgetState extends State<ScannerWidget> {
  static const MethodChannel _channel = const MethodChannel('scanner');

  @override
  void initState() {
    if (widget.scannerController != null) {
      widget.scannerController.beginScan = _beginScanner;
    }
    super.initState();
  }

  @override
  void didUpdateWidget(ScannerWidget oldWidget) {
    if (widget.scannerController != null) {
      widget.scannerController.beginScan = _beginScanner;
    }
    super.didUpdateWidget(oldWidget);
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox.fromSize(
      size: MediaQuery.of(context).size,
      child: Platform.isIOS
          ? UiKitView(
              viewType: 'com.newcoretech.flutter.plugin/scanner',
              onPlatformViewCreated: (_) {
                _beginScanner();
              },
            )
          : AndroidView(
              viewType: 'com.newcoretech.flutter.plugin/scanner',
              onPlatformViewCreated: (_) {
                _beginScanner();
              },
            ),
    );
  }

  ///开始扫码
  void _beginScanner() async {
    final String result = await _channel.invokeMethod('beginScanner');
    //print('result = $result');
    if (widget.onResult != null) {
      widget.onResult(result);
    }
  }

  @override
  void dispose() {
    //销毁原生页面
    _channel.invokeMethod('dispose');
    super.dispose();
  }
}
