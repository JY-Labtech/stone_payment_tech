import 'package:check_licence_tech/check_licence_tech.dart';
import 'package:flutter/services.dart';
import 'package:stone_payment_tech/interface/handler/istone_handler.dart';
import 'package:stone_payment_tech/interface/handler/istone_smart_handler.dart';

import 'payments/payment.dart';

class StoneTech {
  static const channelName = "stone_payment_tech";
  final MethodChannel _channel;
  Payment? _payment;

  static StoneTech? _instance;
  static bool isPinpadActivated = false;

  StoneTech(this._channel);

  static StoneTech get I {
    _instance ??= StoneTech(const MethodChannel(channelName));
    return _instance!;
  }

  final _check = CheckLicenceTech.instance();

  Future<String> getSerialNumber() async => await _check.getSerialNumber();

  Future<bool> wipeLicenceLocalDatabase() async => await _check.wipeLicence();

  /// Function to initialize payment and register the notification handler
  Future<Payment?> initPayment({
    required IStoneHandler handler,
    IStoneTechHandler? iStoneTechHandler,
    required String licenceKey,
  }) async {
    if (_payment != null) {
      return _payment!;
    }
    _payment = await Payment.create(
      channel: _channel,
      paymentHandler: handler,
      iStoneTechHandler: iStoneTechHandler,
      licenceKey: licenceKey,
    );
    return _payment;
  }

  /// Get the Payment object. Note: It needs to be initialized.
  Payment get payment {
    if (_payment == null) {
      throw "PAYMENT NEEDS TO BE INITIALIZED! \n TRY: StoneTech._instance.initPayment(handler)";
    }
    return _payment!;
  }
}
