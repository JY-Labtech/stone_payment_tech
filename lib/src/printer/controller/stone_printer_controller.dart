import 'package:flutter/widgets.dart';
import 'package:stone_payment_tech/src/app/handler/stone_stream_handler.dart';
import 'package:stone_payment_tech/src/app/model/stone_credentials.dart';
import 'package:stone_payment_tech/src/printer/helper/stone_printer_helper.dart';
import 'package:stone_payment_tech/src/stone_tech_payment.dart';

import '../../app/model/stone_status.dart';

class StonePrinterController {
  final StoneStreamHandler handler;

  StonePrinterController({required this.handler});

  StoneTech get stone => StoneTech.I;
  late StoneCredentialsModel credentials;

  Future<void> initialize({
    required StoneCredentialsModel stoneCredentials,
    required Function(String) onMessage,
    required Function(bool) onPop,
    required Function() onSuccess,
  }) async {
    credentials = stoneCredentials;

    stone.initPayment(handler: handler);

    if (!StoneTech.isPinpadActivated) {
      if (credentials.qrCodeAuthorization != null && credentials.qrCodeProviderid != null) {
        await stone.payment.activePinpadWithCredentials(
          appName: credentials.appName,
          stoneCode: credentials.stoneCode,
          qrCodeAuthroization: credentials.qrCodeAuthorization ?? '',
          qrCodeProviderid: credentials.qrCodeProviderid ?? '',
        );
      } else {
        await stone.payment.activePinpad(
          appName: credentials.appName,
          stoneCode: credentials.stoneCode,
        );
      }
    } else {
      stone.payment.setHandler(handler);
    }

    _checkStream(onMessage: onMessage, onPop: onPop, onSuccess: onSuccess);
  }

  void _checkStream({
    required Function(String) onMessage,
    required Function(bool) onPop,
    required Function() onSuccess,
  }) {
    handler.statusStream.listen((data) {
      if (data is StoneStatusActivate) {
        // stoneController.startTransaction(params: widget.stonePaymentParams);
        // Terminal ativado - Prosseguir com impressão
        StoneTech.isPinpadActivated = true;
        onSuccess();
      }

      if (data is StoneStatusActivateError) {
        //Erro ao ativar terminal
        onMessage(data.message);
        onPop(false);
      }
      if (data is StoneStatusActivateRunning) {
        onMessage(data.message);
      }

      if (data is StoneStatusMessage) {
        onMessage(data.message);
      }
    });
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
        await initialize(
          stoneCredentials: credentials,
          onMessage: onMessage,
          onPop: onPop,
          onSuccess: () {
            printerWidget(
              key,
              onMessage: onMessage,
              onPop: onPop,
              stoneCredentials: stoneCredentials,
            );
          },
        );
        onMessage('Ativando terminal\nAguarde...');
        return;
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
