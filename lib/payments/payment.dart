import 'dart:developer';

import 'package:check_licence_tech/check_licence_tech.dart';
import 'package:flutter/services.dart';
import 'package:stone_payment_tech/interface/handler/istone_handler.dart';
import 'package:stone_payment_tech/interface/handler/istone_smart_handler.dart';
import 'package:stone_payment_tech/interface/model/stone_printer.dart';
import 'package:stone_payment_tech/interface/model/stone_transaction_model.dart';
import 'package:stone_payment_tech/interface/utils/extensions/payment_type_call_ext.dart';
import 'package:stone_payment_tech/interface/utils/types/payment_type_call.dart';
import 'package:stone_payment_tech/licence/constants/licence_constants.dart';

import '../stone_smart_flutter.dart';
import 'helper/istone_helper.dart';

final class Payment {
  final MethodChannel channel;
  IStoneHandler paymentHandler;
  final String licenceKey;
  // final IStoneTechHandler? iStoneTechHandler;

  // Cache da instância criada
  static Payment? _instance;
  static bool _isLicenceValidated = false;

  Payment._({
    required this.channel,
    required this.paymentHandler,
    required this.licenceKey,
  });

  /// Método factory assíncrono para criação controlada e validação da licença
  static Future<Payment> create({
    required MethodChannel channel,
    required IStoneHandler paymentHandler,
    IStoneTechHandler? iStoneTechHandler,
    required String licenceKey,
  }) async {
    // Se já existe instância e licença validada, retorna ela diretamente
    if (_instance != null && _isLicenceValidated) {
      return _instance!;
    }

    // Cria nova instância
    final payment = Payment._(
      channel: channel,
      paymentHandler: paymentHandler,
      licenceKey: licenceKey,
    );

    _isLicenceValidated = await payment._checkLicence();

    if (!_isLicenceValidated) {
      // Limpa cache pois licença inválida
      _instance = null;
      _isLicenceValidated = false;
      throw Exception('Invalid licence key. Please check your licence key.');
    }

    // Salva instância e estado da licença validada
    _instance = payment;
    _isLicenceValidated = true;

    payment.channel.setMethodCallHandler((e) => IStoneHelper.callHandler(
          call: e,
          iStoneHandler: paymentHandler,
          stoneTechHandler: iStoneTechHandler,
          onTransaction: (model) => _checkPinpadActivated(model),
        ));

    return payment;
  }

  static void _checkPinpadActivated(StoneTransactionModel model) {
    if (model.method?.toLowerCase() == 'active') {
      if (model.message!.toLowerCase().contains('terminal ativado')) {
        StoneTech.isPinpadActivated = true;
        log('Pinpad activated');
        return;
      }
    }
  }

  Future<bool> _checkLicence() async {
    if (licenceKey.isEmpty) {
      return false;
    }
    try {
      final licence = CheckLicenceTech.instance();
      final result = await licence.checkLicence(
        licenceKey,
        url: licenceCheck,
      );
      return result;
    } catch (e) {
      log('Error checking licence: $e');
      return false;
    }
  }

  void _ensureLicence() {
    if (!_isLicenceValidated) {
      throw Exception('Invalid licence key. Please check your licence key.');
    }
  }

  void setHandler(IStoneHandler handler) {
    if (_instance == null) {
      throw Exception('Payment instance not initialized. Call Payment.create() first.');
    }

    _instance?.paymentHandler = handler;
    _instance?.channel.setMethodCallHandler(
      (e) => IStoneHelper.callHandler(
        call: e,
        iStoneHandler: paymentHandler,
        stoneTechHandler: null,
        onTransaction: (model) => _checkPinpadActivated(model),
      ),
    );
  }

  /// Function to reversal transaction
  Future<bool> reversal() async {
    _ensureLicence();
    try {
      await channel.invokeMethod(PaymentTypeCall.reversal.method);
      return true;
    } catch (e) {
      return false;
    }
  }

  /// Create external functions from invoke methodChannel
  /// Function to active pinpad with SDK the Stone
  Future<bool> activePinpad({
    required String appName,
    required String stoneCode,
  }) async {
    _ensureLicence();
    try {
      await channel.invokeMethod(PaymentTypeCall.activePinpad.method, {"appName": appName, "stoneCode": stoneCode});
      return true;
    } catch (e) {
      return false;
    }
  }

  /// Create external functions from invoke methodChannel
  /// Function to active pinpad with SDK the Stone with credentials
  Future<bool> activePinpadWithCredentials({
    required String appName,
    required String stoneCode,
    required String qrCodeAuthroization,
    required String qrCodeProviderid,
    bool printCustomerSlip = true,
  }) async {
    _ensureLicence();
    try {
      await channel.invokeMethod(PaymentTypeCall.activePinpadCredentials.method, {"appName": appName, "stoneCode": stoneCode, "qrCodeAuthorization": qrCodeAuthroization, "qrCodeProviderid": qrCodeProviderid, "printCustomerSlip": printCustomerSlip});
      return true;
    } catch (e) {
      return false;
    }
  }

  /// Function to invoke method from credit payment with SDK the Stone
  Future<bool> creditPayment(
    int value, {
    bool printCustomerSlip = true,
    bool printEstablishmentSlip = false,
    String initiatorTransactionKey = "",
  }) async {
    _ensureLicence();
    return await channel.invokeMethod(
      PaymentTypeCall.credit.method,
      {
        "amount": value.toString(),
        "installment": 1,
        "withInterest": false,
        "printCustomerSlip": printCustomerSlip,
        "printEstablishmentSlip": printEstablishmentSlip,
        "initiatorTransactionKey": initiatorTransactionKey,
      },
    );
  }

  /// Function to invoke method from credit installment payment  with SDK the Stone
  Future<bool> creditPaymentParc({
    required int value,
    int installment = 1,
    bool withInterest = false,
    bool printCustomerSlip = true,
    bool printEstablishmentSlip = false,
    String initiatorTransactionKey = "",
  }) async {
    _ensureLicence();
    return await channel.invokeMethod(PaymentTypeCall.creditParc.method, {
      "amount": value.toString(),
      "installment": installment,
      "withInterest": withInterest,
      "printCustomerSlip": printCustomerSlip,
      "printEstablishmentSlip": printEstablishmentSlip,
      "initiatorTransactionKey": initiatorTransactionKey,
    });
  }

  /// Function to invoke method from debit payment with SDK the Stone
  Future<bool> debitPayment(
    int value, {
    bool printCustomerSlip = true,
    bool printEstablishmentSlip = false,
    String initiatorTransactionKey = "",
  }) async {
    _ensureLicence();
    return await channel.invokeMethod(PaymentTypeCall.debit.method, {
      "amount": value.toString(),
      "installment": 1,
      "withInterest": false,
      "printCustomerSlip": printCustomerSlip,
      "printEstablishmentSlip": printEstablishmentSlip,
      "initiatorTransactionKey": initiatorTransactionKey,
    });
  }

  /// Function to invoke method from debit payment with SDK the Stone
  Future<bool> pixPayment({
    required int amount,
    required String qrCodeAuthroization,
    required String qrCodeProviderid,
    bool printCustomerSlip = true,
    bool printEstablishmentSlip = false,
    String initiatorTransactionKey = "",
  }) async {
    _ensureLicence();
    return await channel.invokeMethod(PaymentTypeCall.pix.method, {
      "amount": amount.toString(),
      "installment": 1,
      "withInterest": false,
      "qrCodeAuthorization": qrCodeAuthroization,
      "qrCodeProviderid": qrCodeProviderid,
      "printCustomerSlip": printCustomerSlip,
      "printEstablishmentSlip": printEstablishmentSlip,
      "initiatorTransactionKey": initiatorTransactionKey,
    });
  }

  /// Function to invoke method from voucher payment with SDK the Stone
  Future<bool> voucherPayment(
    int value, {
    bool printCustomerSlip = true,
    bool printEstablishmentSlip = false,
    String initiatorTransactionKey = "",
  }) async {
    _ensureLicence();
    return await channel.invokeMethod(PaymentTypeCall.voucher.method, {
      "amount": value.toString(),
      "installment": 1,
      "withInterest": false,
      "printCustomerSlip": printCustomerSlip,
      "printEstablishmentSlip": printEstablishmentSlip,
      "initiatorTransactionKey": initiatorTransactionKey,
    });
  }

  /// Function to invoke method from refund payment with SDK the Stone
  Future<void> activeDebugLog({required bool isDebugLog}) async {
    _ensureLicence();
    await channel.invokeMethod(PaymentTypeCall.activeDebugLog.method, {"isDebugLog": isDebugLog});
  }

  //Get Pos serial number
  Future<String?> getPosSerialNumber() async {
    _ensureLicence();
    return await channel.invokeMethod(PaymentTypeCall.getPosSerialNumber.method);
  }

  //Get Pos manufacture
  Future<String?> getPosManufacture() async {
    _ensureLicence();
    return await channel.invokeMethod(PaymentTypeCall.getPosManufacture.method);
  }

  /// Function to invoke method from abort current transaction with SDK the Stone
  Future<bool> abortTransaction() async {
    _ensureLicence();
    return await channel.invokeMethod(PaymentTypeCall.abort.method);
  }

  /// Function to invoke method from abort current PIX transaction with SDK the Stone
  Future<bool> abortPIXTransaction() async {
    _ensureLicence();
    return await channel.invokeMethod(PaymentTypeCall.abortPix.method);
  }

  /// Function to invoke method from printer current transaction with SDK the Stone
  Future<bool> printerCurrentTransaction({required bool printCustomerSlip}) async {
    _ensureLicence();
    return await channel.invokeMethod(PaymentTypeCall.printTransaction.method, {"printCustomerSlip": printCustomerSlip});
  }

  /// Function to invoke method from printer transaction by key
  Future<bool> paymentPrinterTransactionKey({required String transactionKey}) async {
    _ensureLicence();
    return await channel.invokeMethod(PaymentTypeCall.paymentPrinterTransactionKey.method, {
      "transactionKey": transactionKey,
    });
  }

  /// Function to invoke method from cancel transaction with SDK the Stone
  Future<bool> cancelTransaction({required int idFromBase}) async {
    _ensureLicence();
    return await channel.invokeMethod(PaymentTypeCall.cancelTransaction.method, {"idFromBase": idFromBase});
  }

  /// Function to get all transactions
  Future<bool> getAllTransactions() async {
    _ensureLicence();
    return await channel.invokeMethod(PaymentTypeCall.getAllTransactions.method);
  }

  /// Function to set a payment option
  Future<bool> setPaymentOption({required String option}) async {
    _ensureLicence();
    return await channel.invokeMethod(PaymentTypeCall.paymentOption.method, {"option": option});
  }

  /// Function to get a transaction by initiator key
  Future<bool> getTransactionByInitiatorTransactionKey({required String initiatorTransactionKey}) async {
    _ensureLicence();
    return await channel.invokeMethod(
      PaymentTypeCall.getTransactionByInitiatorTransactionKey.method,
      {
        "InitiatorTransactionKey": initiatorTransactionKey,
      },
    );
  }

  /// Function to send custom printer parameters
  Future<bool> customPrinter(StonePrinterParams stonePrinterParams) async {
    _ensureLicence();
    try {
      await channel.invokeMethod(PaymentTypeCall.customPrinter.method, {"printerParams": stonePrinterParams.toJson()});
      return true;
    } catch (e) {
      return false;
    }
  }

  /// Function to print from base64 image
  Future<bool> printFromBase64(String base64) async {
    _ensureLicence();
    try {
      await channel.invokeMethod(PaymentTypeCall.printFromBase64.method, {"printerParams": base64});
      return true;
    } catch (e) {
      return false;
    }
  }

  /// Function to print wrap paper
  Future<bool> printWrapPaper(int lines) async {
    _ensureLicence();
    try {
      await channel.invokeMethod(PaymentTypeCall.printWrapPaper.method, {"lines": lines});
      return true;
    } catch (e) {
      return false;
    }
  }
}
