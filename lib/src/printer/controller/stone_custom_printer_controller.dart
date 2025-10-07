import 'package:stone_payment_tech/src/app/handler/stone_stream_handler.dart';
import 'package:stone_payment_tech/src/app/model/stone_credentials.dart';
import 'package:stone_payment_tech/src/app/model/stone_status.dart';
import 'package:stone_payment_tech/src/stone_tech_payment.dart';

class StoneCustomPrinterController {
  final StoneStreamHandler handler = StoneStreamHandler();

  StoneTech get stone => StoneTech.I;
  late StoneCredentialsModel credentials;

  Future<void> initialize({
    required StoneCredentialsModel stoneCredentials,
    required Function(String) onMessage,
    required Function() onSuccess,
    required Function(String) onError,
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

    _checkStream(onMessage: onMessage, onSuccess: onSuccess, onError: onError);
  }

  Future<void> initializeAndPrintBase64({
    required StoneCredentialsModel stoneCredentials,
    required Function(String) onMessage,
    required Function(String) onError,
    required String base64,
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
    }

    _checkStream(
      onMessage: onMessage,
      onError: onError,
      onSuccess: () async {
        await stone.payment.printFromBase64(base64);
      },
    );
  }

  void _checkStream({
    required Function(String) onMessage,
    required Function() onSuccess,
    required Function(String) onError,
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
        onError(data.message);
      }
      if (data is StoneStatusActivateRunning) {
        onMessage(data.message);
      }

      if (data is StoneStatusMessage) {
        onMessage(data.message);
      }
    });
  }
}
