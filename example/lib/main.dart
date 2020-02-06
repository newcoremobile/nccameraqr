import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:scanner_example/scan_page.dart';

void main() => runApp(MaterialApp(
      home: MyApp(),
    ));

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Plugin example app'),
      ),
      body: Center(
        child: CupertinoButton(
            child: Text("开启扫码"),
            onPressed: () {
              Navigator.of(context).push(MaterialPageRoute(builder: (context) {
                return ScannerPage();
              }));
            }),
      ),
    );
  }
}
