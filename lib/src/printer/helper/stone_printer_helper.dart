import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/rendering.dart';
import 'package:flutter/widgets.dart';

class StonePrinterLayoutHelper {
  Future<GeneratePrinter?> generateReceipt(GlobalKey globalKey) async {
    try {
      RenderRepaintBoundary boundary = globalKey.currentContext!.findRenderObject() as RenderRepaintBoundary;

      // Get the original image
      ui.Image originalImage = await boundary.toImage(pixelRatio: 3.0);

      // Calculate dimensions maintaining aspect ratio
      double scale = 380.0 / originalImage.width;
      int targetWidth = 380;
      int targetHeight = (originalImage.height * scale).round();

      // Create recorder and canvas
      final recorder = ui.PictureRecorder();
      final canvas = Canvas(recorder);

      // Draw the scaled image
      canvas.drawImageRect(
        originalImage,
        Rect.fromLTWH(0, 0, originalImage.width.toDouble(), originalImage.height.toDouble()),
        Rect.fromLTWH(0, 0, targetWidth.toDouble(), targetHeight.toDouble()),
        Paint(),
      );

      // Convert to image
      final picture = recorder.endRecording();
      final resizedImage = await picture.toImage(targetWidth, targetHeight);

      ByteData? byteData = await resizedImage.toByteData(format: ui.ImageByteFormat.png);

      if (byteData != null) {
        Uint8List pngBytes = byteData.buffer.asUint8List();
        String base64String = base64Encode(pngBytes);
        return GeneratePrinter(
          pngBytes: pngBytes,
          base64String: base64String,
          printerBytes: pngBytes,
          byteBuffer: byteData.buffer,
        );
      }

      throw Exception('Failed to convert receipt to base64');
    } catch (e) {
      return null;
    }
  }
}

class GeneratePrinter {
  final Uint8List pngBytes;
  final String base64String;
  final List<int> printerBytes;
  final ByteBuffer byteBuffer;

  GeneratePrinter({
    required this.pngBytes,
    required this.base64String,
    required this.printerBytes,
    required this.byteBuffer,
  });
}
