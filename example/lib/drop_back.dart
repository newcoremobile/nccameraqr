/*
 * 
 * @Author     : Yongsheng.X
 * @Date       : 2019-09-24 13:29
 * @Description: 双层样式
 *
 */

import 'dart:math' as math;

import 'package:flutter/material.dart';

const double kPanelTitleHeight = 48;

///手势区域
class BackdropPanel extends StatelessWidget {
  const BackdropPanel({
    Key key,
    this.onTap,
    this.onVerticalDragUpdate,
    this.onVerticalDragEnd,
    this.title,
    this.child,
  }) : super(key: key);

  final VoidCallback onTap;
  final GestureDragUpdateCallback onVerticalDragUpdate;
  final GestureDragEndCallback onVerticalDragEnd;
  final Widget title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Material(
      elevation: 2.0,
      borderRadius: const BorderRadius.only(
        topLeft: Radius.circular(16.0),
        topRight: Radius.circular(16.0),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          GestureDetector(
            behavior: HitTestBehavior.opaque,
            onVerticalDragUpdate: onVerticalDragUpdate,
            onVerticalDragEnd: onVerticalDragEnd,
            onTap: onTap,
            child: Container(
              height: kPanelTitleHeight - 1,
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
                boxShadow: [
                  BoxShadow(
                    blurRadius: 5,
                    offset: Offset(0, -3),
                    color: Color.fromRGBO(0, 0, 0, 0.2),
                  )
                ],
              ),
              padding: const EdgeInsetsDirectional.only(start: 8.0),
              alignment: AlignmentDirectional.centerStart,
              child: Stack(
                children: <Widget>[
                  Column(
                    children: <Widget>[
                      Padding(
                        padding: const EdgeInsets.only(top: 4),
                        child: Center(
                          child: Container(
                            width: 40,
                            height: 4,
                            decoration: BoxDecoration(
                                color: Colors.grey[400], borderRadius: BorderRadius.circular(2)),
                          ),
                        ),
                      ),
                      Expanded(child: Container())
                    ],
                  ),
                  title
                ],
              ),
            ),
          ),
          const Divider(height: 1.0),
          Expanded(child: child),
        ],
      ),
    );
  }
}

///处理显示效果，拖拽等
class Backdrop extends StatefulWidget {
  final Widget backLayer;
  final Widget frontLayer;
  final Widget frontHead;
  final bool expanded;

  Backdrop({this.backLayer, this.frontLayer, this.frontHead, this.expanded = false});

  @override
  _BackdropState createState() => _BackdropState();
}

class _BackdropState extends State<Backdrop> with SingleTickerProviderStateMixin {
  AnimationController _controller;

  double _kPanelTopPaddingHeight = 56;
  double _backdropHeight = 0;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 200),
      value: 0.0,
      vsync: this,
    );
  }

  @override
  void didUpdateWidget(Backdrop oldWidget) {
    if (widget.expanded) {
      _controller.forward();
    } else {
      _controller.reverse();
    }
    super.didUpdateWidget(oldWidget);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  ///点击手势区域
  void _toggleBackdropPanelVisibility() {
    if (_controller.isAnimating) {
      return;
    }
    if (_controller.isCompleted) {
      _controller.reverse();
    } else {
      _controller.forward();
    }
  }

  void _handleDragUpdate(DragUpdateDetails details) {
    if (_controller.isAnimating) return;

    _controller.value -= details.primaryDelta / (_backdropHeight ?? details.primaryDelta);
  }

  void _handleDragEnd(DragEndDetails details) {
    if (_controller.isAnimating || _controller.status == AnimationStatus.completed) return;

    final double flingVelocity = details.velocity.pixelsPerSecond.dy / _backdropHeight;
    if (flingVelocity < 0.0)
      _controller.fling(velocity: math.max(2.0, -flingVelocity));
    else if (flingVelocity > 0.0)
      _controller.fling(velocity: math.min(-2.0, -flingVelocity));
    else
      _controller.fling(velocity: _controller.value < 0.5 ? -2.0 : 2.0);
  }

  Widget _buildStack(BuildContext context, BoxConstraints constraints) {
    final ThemeData theme = Theme.of(context);

    //获取设备尺寸
    final Size panelSize = constraints.biggest;
    _kPanelTopPaddingHeight = panelSize.height / 2;

    //数据+图标区域+空白间隔，折叠是距离顶部的高度
    var endPositionedTopSpace = panelSize.height - 60;
    _backdropHeight = endPositionedTopSpace;
    Widget frontHead = widget.frontHead;

    final Animation<RelativeRect> panelAnimation = _controller.drive(
      RelativeRectTween(
        //收起的时候的位置
        begin: RelativeRect.fromLTRB(
          0.0,
          endPositionedTopSpace,
          0.0,
          0.0,
        ),
        //展开的时候的位置
        end: RelativeRect.fromLTRB(
          0.0,
          _kPanelTopPaddingHeight,
          0.0,
          0.0,
        ),
      ),
    );

    //print('backdrop page builder..........2');
    var frontWidget = PositionedTransition(
      rect: panelAnimation,
      child: BackdropPanel(
        onTap: _toggleBackdropPanelVisibility,
        onVerticalDragUpdate: _handleDragUpdate,
        onVerticalDragEnd: _handleDragEnd,
        title: frontHead,
        child: widget.frontLayer,
      ),
    );

    return Container(
      color: theme.primaryColor,
      child: Stack(
        children: <Widget>[
          widget.backLayer,
          frontWidget,
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: LayoutBuilder(
        builder: _buildStack,
      ),
    );
  }
}
