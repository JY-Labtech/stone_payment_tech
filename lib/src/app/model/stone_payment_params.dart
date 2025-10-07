import 'package:stone_payment_tech/src/app/model/stone_credentials.dart';
import 'package:stone_payment_tech/src/app/model/stone_options.dart';

enum StonePaymentType { credit, creditParc, debit, voucher, pix }

class StonePaymentParams {
  final double amount;
  final StonePaymentType type;
  final int? installments;
  final StoneCredentialsModel credentials;
  final StoneOptions? options;

  StonePaymentParams({
    required this.amount,
    required this.type,
    required this.credentials,
    this.options,
    this.installments,
  });
}
