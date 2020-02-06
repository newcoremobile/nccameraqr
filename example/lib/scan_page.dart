/*
 * 
 * @Author     : Yongsheng.X
 * @Date       : 2019-09-26 17:10
 * @Description: 扫码页面
 *
 */

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:scanner/scanner.dart';
import 'drop_back.dart';

class ScannerPage extends StatefulWidget {
  @override
  _ScannerPageState createState() => _ScannerPageState();
}

class _ScannerPageState extends State<ScannerPage> {
  ScannerController _controller = ScannerController();
  String _result = '';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Backdrop(
        expanded: _result.length > 0,
        backLayer: ScannerWidget(
          scannerController: _controller,
          onResult: (value) {
            print('scan result = $value');
            setState(() {
              _result = value;
            });
          },
        ),
        frontLayer: Container(
          color: Colors.blue[50],
          child: Center(
            child: Text(
              _result,
              style: TextStyle(fontSize: 20, color: Colors.red),
            ),
          ),
        ),
        frontHead: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Center(
              child: Text(
                '手动报工',
                style: TextStyle(color: Colors.black, fontWeight: FontWeight.w600, fontSize: 16),
              ),
            )
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          setState(() {
            _result = '';
            _controller.beginScan();
          });
        },
        child: Icon(Icons.add),
      ),
    );
  }
}
