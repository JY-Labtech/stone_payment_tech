import 'package:stone_payment_tech/src/interface/model/stone_transaction_model.dart';

sealed class StoneStatus {
  const StoneStatus();

  factory StoneStatus.message(String message) => StoneStatusMessage(message: message);

  factory StoneStatus.transaction(StoneTransactionModel transaction) => StoneStatusTransaction(transaction: transaction);

  factory StoneStatus.activate() => const StoneStatusActivate();

  factory StoneStatus.canceled(Map<String, dynamic> cancelExtraData) => StoneStatusCanceled(cancelExtraData: cancelExtraData);
}

final class StoneStatusMessage extends StoneStatus {
  final String message;

  const StoneStatusMessage({
    required this.message,
  });
}

final class StoneStatusTransaction extends StoneStatus {
  final StoneTransactionModel transaction;
  bool get isApproved => transaction.isApproved;

  StoneStatusTransaction({
    required this.transaction,
  });
}

final class StoneQRCodeTransaction extends StoneStatus {
  final StoneTransactionModel transaction;
  String get qrCode => transaction.message ?? '';
  bool get isExpired => (transaction.errorMessage ?? '').contains('QRCODE_EXPIRED') || (transaction.message ?? '').contains('QRCODE_EXPIRED');

  StoneQRCodeTransaction({
    required this.transaction,
  });
}

final class StoneStatusActivate extends StoneStatus {
  const StoneStatusActivate();
}

final class StoneStatusActivateRunning extends StoneStatus {
  final String message;
  const StoneStatusActivateRunning({required this.message});
}

final class StoneStatusActivateError extends StoneStatus {
  final String message;
  const StoneStatusActivateError({required this.message});
}

final class StonePrinterCustomer extends StoneStatus {
  const StonePrinterCustomer();
}

final class StoneStatusCanceled extends StoneStatus {
  final Map<String, dynamic> cancelExtraData;

  const StoneStatusCanceled({required this.cancelExtraData});
}

final class StoneStatusPaymentOptions extends StoneStatus {
  final List<String> options;
  final String message;

  const StoneStatusPaymentOptions({
    required this.options,
    required this.message,
  });
}
