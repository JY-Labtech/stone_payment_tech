import 'dart:convert';

import 'package:stone_payment_tech/src/app/handler/stone_printer_handler.dart';
import 'package:stone_payment_tech/src/app/model/stone_credentials.dart';
import 'package:stone_payment_tech/src/stone_tech_payment.dart';

import '../../interface/interface.dart';

class StoneCustomPrinterController {
  late StonePrinterHandler handler;

  StoneTech get stone => StoneTech.I;
  late StoneCredentialsModel credentials;

  void onFinished(
    String value, {
    required Function onSuccess,
    required Function(String) onError,
  }) {
    try {
      if (value.contains('{')) {
        final map = json.decode(value);
        final transaction = StoneTransactionModel.fromMap(map);
        if (transaction.method == 'active') {
          final message = (map['message'] ?? '').toString().toLowerCase();
          if (message.contains('terminal ativado')) {
            StoneTech.isPinpadActivated = true;
            onSuccess();
          } else {
            StoneTech.isPinpadActivated = false;
            onError(message);
          }
        }
      }
    } catch (e) {
      onError(e.toString());
    }
  }

  void onMessage(String value) {
    // print('onMessage: $value');
  }

  Future<void> initialize({
    required StoneCredentialsModel stoneCredentials,
    required Function(String) onMessage,
    required Function() onSuccess,
    required Function(String) onError,
  }) async {
    credentials = stoneCredentials;
    handler = StonePrinterHandler(
      onFinishedMonitor: (e) => onFinished(e, onSuccess: onSuccess, onError: onError),
      onMessageMonitor: onMessage,
    );

    stone.initPayment(handler: handler);
    stone.payment.setHandler(handler);

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
      onSuccess();
    }
  }

  Future<void> initializeAndPrintBase64({
    required StoneCredentialsModel stoneCredentials,
    required Function(String) onMessage,
    required Function(String) onError,
    required String base64,
  }) async {
    credentials = stoneCredentials;
    handler = StonePrinterHandler(
      onFinishedMonitor: (e) => onFinished(e, onSuccess: () {
        stone.payment.printFromBase64(base64);
      }, onError: onError),
      onMessageMonitor: onMessage,
    );

    stone.initPayment(handler: handler);
    stone.payment.setHandler(handler);

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
  }
}
