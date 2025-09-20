import 'package:flutter/widgets.dart';
import 'package:stone_payment_tech/app/handler/stone_stream_handler.dart';
import 'package:stone_payment_tech/app/model/stone_credentials.dart';
import 'package:stone_payment_tech/printer/helper/stone_printer_helper.dart';
import 'package:stone_payment_tech/stone_smart_flutter.dart';

class StonePrinterController {
  final StoneStreamHandler handler;

  StonePrinterController({required this.handler});

  StoneTech get stone => StoneTech.I;
  late StoneCredentialsModel credentials;

  Future<void> initialize({
    required StoneCredentialsModel stoneCredentials,
  }) async {
    credentials = stoneCredentials;

    await stone.initPayment(
      handler: handler,
    );

    if (credentials.qrCodeAuthorization != null && credentials.qrCodeProviderid != null) {
      await stone.payment.activePinpadWithCredentials(
        appName: credentials.appName,
        stoneCode: credentials.stoneCode,
        qrCodeAuthroization: credentials.qrCodeAuthorization ?? '',
        qrCodeProviderid: credentials.qrCodeProviderid ?? '',
      );
      return;
    }
    await stone.payment.activePinpad(
      appName: credentials.appName,
      stoneCode: credentials.stoneCode,
    );
  }

  Future<void> printerWidget(
    GlobalKey key, {
    required Function(String) onMessage,
    required Function(bool) onPop,
    required StoneCredentialsModel stoneCredentials,
  }) async {
    try {
      credentials = stoneCredentials;
      if (!StoneTech.isPinpadActivated) {
        onMessage('Ativando terminal');
        await initialize(stoneCredentials: credentials);
        onMessage('Ativando terminal\nAguarde...');
        await Future.delayed(const Duration(seconds: 3));
        if (!StoneTech.isPinpadActivated) {
          onPop(false);
          return;
        }
      }
      onMessage('Gerando comprovante...');
      await Future.delayed(const Duration(seconds: 1));
      final printerHelper = StonePrinterLayoutHelper();

      final result = await printerHelper.generateReceipt(key);
      if (result == null) {
        onMessage('Erro ao gerar recibo');
        await Future.delayed(const Duration(seconds: 3));
        onPop(false);
        return;
      }
      onMessage('Aguarde enquanto o comprovante é gerado...');

      onMessage('Enviando para a impressora...');
      stone.payment.printFromBase64(result.base64String);
      await Future.delayed(const Duration(seconds: 3));
      onPop(true);
    } catch (e) {
      onMessage('*Erro ao gerar recibo: $e');
      onPop(false);
    }
  }

  void dispose() {
    handler.dispose();
  }
}
