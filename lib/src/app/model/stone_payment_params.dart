import 'package:stone_payment_tech/src/app/model/stone_credentials.dart';
import 'package:stone_payment_tech/src/app/model/stone_options.dart';

enum StoneTechPaymentType { credit, creditParc, debit, voucher, pix }

class StoneTechPaymentParams {
  final double amount;
  final StoneTechPaymentType type;
  final int? installments;
  final StoneTechCredentialsModel credentials;
  final StoneTechOptions? options;

  StoneTechPaymentParams({
    required this.amount,
    required this.type,
    required this.credentials,
    this.options,
    this.installments,
  });
}
