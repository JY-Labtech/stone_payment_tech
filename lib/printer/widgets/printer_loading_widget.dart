import 'package:flutter/material.dart';

class PrinterLoadingWidget extends StatelessWidget {
  const PrinterLoadingWidget({
    super.key,
    required this.animationController,
    this.message,
  });
  final AnimationController animationController;
  final String? message;

  @override
  Widget build(BuildContext context) {
    return Align(
      key: UniqueKey(),
      alignment: Alignment.center,
      child: Container(
        color: Colors.white,
        width: double.infinity,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            AnimatedBuilder(
              animation: animationController,
              builder: (context, child) {
                return Transform.scale(
                  scale: 1.0 + (0.1 * animationController.value.abs()),
                  child: const Icon(
                    Icons.print_rounded,
                    size: 48,
                    color: Colors.black,
                  ),
                );
              },
            ),
            const SizedBox(height: 24),
            Text(
              message ?? "Imprimindo...",
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              "Aguarde um momento...",
              style: TextStyle(
                fontSize: 14,
                color: Colors.black54,
              ),
            ),
            const SizedBox(height: 16),
            const SizedBox(
              width: 200,
              child: LinearProgressIndicator(
                valueColor: AlwaysStoppedAnimation<Color>(
                  Colors.black,
                ),
                backgroundColor: Colors.black12,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
