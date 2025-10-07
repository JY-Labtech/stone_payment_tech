import 'package:flutter/material.dart';

class PrinterCustomBody extends StatelessWidget {
  const PrinterCustomBody({
    super.key,
    required this.globalKey,

    required this.child,
  });
  final Key globalKey;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(5.0),
      child: RepaintBoundary(
        key: globalKey,
        child: Container(
          width: 300,
          padding: const EdgeInsets.symmetric(horizontal: 5),
          color: Colors.white,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              child,
              const SizedBox(height: 10),
            ],
          ),
        ),
      ),
    );
  }
}
