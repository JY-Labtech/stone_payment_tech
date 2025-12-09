import 'package:stone_payment_tech/src/app/handler/stone_stream_handler.dart';
import 'package:stone_payment_tech/src/app/model/stone_credentials.dart';
import 'package:stone_payment_tech/src/app/model/stone_payment_params.dart';
import 'package:stone_payment_tech/src/app/model/stone_status.dart';
import 'package:stone_payment_tech/src/interface/model/stone_printer.dart';
import 'package:stone_payment_tech/src/stone_tech_payment.dart';

class StoneStreamController {
  final StoneStreamHandler? handler;

  StoneTech get stone => StoneTech.I;
  late StoneTechCredentialsModel credentials;
  late StoneTechPaymentParams paymentParams;

  StoneTechPaymentType? currentMethod;
  bool isFinishPayment = false;
  bool _isActivating = false;

  Stream<StoneStatusTech> get stream => handler?.statusStream ?? const Stream.empty();

  bool get isActivating => _isActivating;

  StoneStreamController({
    required this.handler,
  });

  Future<void> initialize({
    required StoneTechCredentialsModel stoneCredentials,
    required StoneTechPaymentParams stonePaymentParams,
  }) async {
    if (handler == null) {
      return;
    }
    credentials = stoneCredentials;
    paymentParams = stonePaymentParams;
    stone.initPayment(handler: handler!);
    isFinishPayment = false;

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

  int _getSaleValueStone(double value) {
    int saleValue = 0;
    final valueFixed = (value * 100).toStringAsFixed(0);
    final valueInt = int.parse(valueFixed);
    saleValue = valueInt;
    return saleValue;
  }

  Future<void> startTransaction({
    required StoneTechPaymentParams params,
  }) async {
    isFinishPayment = false;
    handler?.clear();
    if (handler == null) {
      return;
    }
    credentials = params.credentials;
    if (!StoneTech.isPinpadActivated) {
      _isActivating = true;
      handler!.setStreamStatus(const StoneStatusTechActivateRunning(message: 'Ativando Terminal...'));
      try {
        await initialize(stoneCredentials: params.credentials, stonePaymentParams: params);
        // Não resetar _isActivating aqui, será resetado quando receber StoneStatusActivate ou StoneStatusActivateError
      } catch (e) {
        _isActivating = false;
        rethrow;
      }
      return;
    } else {
      _isActivating = false;
      stone.payment.setHandler(handler!);
    }
    final amountInt = _getSaleValueStone(params.amount);
    currentMethod = params.type;
    switch (currentMethod) {
      case StoneTechPaymentType.credit:
        {
          if (params.installments != null) {
            stone.payment.creditPaymentParc(
              value: amountInt,
              installment: params.installments!,
              printEstablishmentSlip: true,
              printCustomerSlip: false,
            );
          } else {
            stone.payment.creditPayment(
              amountInt,
              printEstablishmentSlip: true,
              printCustomerSlip: false,
            );
          }
        }
        break;
      case StoneTechPaymentType.debit:
        {
          stone.payment.debitPayment(
            amountInt,
            printEstablishmentSlip: true,
            printCustomerSlip: false,
          );
        }
        break;
      case StoneTechPaymentType.pix:
        {
          stone.payment.pixPayment(
            amount: amountInt,
            qrCodeAuthroization: credentials.qrCodeAuthorization ?? '',
            qrCodeProviderid: credentials.qrCodeProviderid ?? '',
            printEstablishmentSlip: true,
            printCustomerSlip: false,
          );
        }
        break;
      case StoneTechPaymentType.voucher:
        {
          stone.payment.voucherPayment(
            amountInt,
            printEstablishmentSlip: true,
            printCustomerSlip: false,
          );
        }
        break;
      default:
        throw Exception('Method not supported');
    }
  }

  void abortTransaction() {
    if (_isActivating) {
      _isActivating = false;
      handler?.setStreamStatus(const StoneStatusTechActivateError(message: 'Ativação cancelada pelo usuário'));
      stone.payment.abortActivation();
    } else if (currentMethod == StoneTechPaymentType.pix) {
      stone.payment.abortPIXTransaction();
    } else {
      stone.payment.abortTransaction();
    }
  }

  void printer(StonePrinterParams params) {
    stone.payment.customPrinter(params);
  }

  void printerFromBase64(String base64) {
    stone.payment.printFromBase64(base64);
  }

  void cancelTransaction({required int idFromBase}) {
    handler?.clear();
    stone.payment.cancelTransaction(idFromBase: idFromBase);
  }

  void printCurrentTransaction() {
    stone.payment.printerCurrentTransaction(printCustomerSlip: true);
  }

  void dispose() {
    if (handler == null) {
      return;
    }
    handler!.dispose();
  }

  void onPrinterCustomer(bool value) async {
    stone.payment.printerCurrentTransaction(printCustomerSlip: value);
  }

  Future<void> setPaymentOption(String option) async {
    await stone.payment.setPaymentOption(option: option);
  }
}
