import 'package:stone_payment_tech/src/interface/model/stone_transaction_model.dart';

sealed class StoneStatusTech {
  const StoneStatusTech();

  factory StoneStatusTech.message(String message) => StoneStatusTechMessage(message: message);

  factory StoneStatusTech.transaction(StoneTransactionModel transaction) => StoneStatusTechTransaction(transaction: transaction);

  factory StoneStatusTech.activate() => const StoneStatusTechActivate();
  factory StoneStatusTech.canceled(Map<String, dynamic> cancelExtraData) => StoneStatusTechCanceled(cancelExtraData: cancelExtraData);
}

final class StoneStatusTechMessage extends StoneStatusTech {
  final String message;

  const StoneStatusTechMessage({
    required this.message,
  });
}

final class StoneStatusTechTransaction extends StoneStatusTech {
  final StoneTransactionModel transaction;
  bool get isApproved => transaction.isApproved;

  StoneStatusTechTransaction({
    required this.transaction,
  });
}

final class StoneStatusTechQRCodeTransaction extends StoneStatusTech {
  final StoneTransactionModel transaction;
  String get qrCode => transaction.message ?? '';
  bool get isExpired => (transaction.errorMessage ?? '').contains('QRCODE_EXPIRED') || (transaction.message ?? '').contains('QRCODE_EXPIRED');

  StoneStatusTechQRCodeTransaction({
    required this.transaction,
  });
}

final class StoneStatusTechActivate extends StoneStatusTech {
  const StoneStatusTechActivate();
}

final class StoneStatusTechActivateRunning extends StoneStatusTech {
  final String message;
  const StoneStatusTechActivateRunning({required this.message});
}

final class StoneStatusTechActivateError extends StoneStatusTech {
  final String message;
  const StoneStatusTechActivateError({required this.message});
}

final class StonePrinterCustomer extends StoneStatusTech {
  const StonePrinterCustomer();
}

final class StoneStatusTechCanceled extends StoneStatusTech {
  final Map<String, dynamic> cancelExtraData;

  const StoneStatusTechCanceled({required this.cancelExtraData});
}

final class StoneStatusTechPaymentOptions extends StoneStatusTech {
  final List<String> options;
  final String message;

  const StoneStatusTechPaymentOptions({
    required this.options,
    required this.message,
  });
}
