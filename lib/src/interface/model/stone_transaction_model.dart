// ignore_for_file: public_member_api_docs, sort_constructors_first
import 'dart:convert';

class StoneTransactionModel {
  final String? actionCode;
  final String? aid;
  final String? amount;
  final String? arcq;
  final String? cardBrand;
  final String? cardHolderNumber;
  final String? date;
  final String? entryMode;
  final int? idFromBase;
  final int? isBuildResponse;
  final String? manufacture;
  final int? result;
  final String? saleAffiliationKey;
  final String? serialNumber;
  final String? time;
  final String? transactionReference;
  final String? typeOfTransactionEnum;
  final String? errorMessage;
  final String? method;
  final String? transactionStatus;
  final String? messageFromAuthorize;
  final String? message;
  final String? authorizationCode;
  final String? actionResultMessage;

  bool get isApproved => messageFromAuthorize?.toLowerCase() == 'aprovado' || transactionStatus?.toUpperCase() == 'APPROVED';

  StoneTransactionModel({
    required this.actionCode,
    required this.aid,
    required this.amount,
    required this.arcq,
    required this.cardBrand,
    required this.cardHolderNumber,
    required this.date,
    required this.entryMode,
    required this.idFromBase,
    required this.isBuildResponse,
    required this.manufacture,
    required this.result,
    required this.saleAffiliationKey,
    required this.serialNumber,
    required this.time,
    required this.transactionReference,
    required this.typeOfTransactionEnum,
    required this.errorMessage,
    required this.method,
    required this.transactionStatus,
    required this.messageFromAuthorize,
    required this.message,
    required this.authorizationCode,
    required this.actionResultMessage,
  });

  Map<String, dynamic> toMap() {
    return <String, dynamic>{
      'actionCode': actionCode,
      'aid': aid,
      'amount': amount,
      'arcq': arcq,
      'cardBrand': cardBrand,
      'cardHolderNumber': cardHolderNumber,
      'date': date,
      'entryMode': entryMode,
      'idFromBase': idFromBase,
      'isBuildResponse': isBuildResponse,
      'manufacture': manufacture,
      'result': result,
      'saleAffiliationKey': saleAffiliationKey,
      'serialNumber': serialNumber,
      'time': time,
      'transactionReference': transactionReference,
      'typeOfTransactionEnum': typeOfTransactionEnum,
      'errorMessage': errorMessage,
      'method': method,
      'transactionStatus': transactionStatus,
      'messageFromAuthorize': messageFromAuthorize,
      'message': message,
      'authorizationCode': authorizationCode,
      'actionResultMessage': actionResultMessage,
    };
  }

  factory StoneTransactionModel.fromMap(Map<String, dynamic> map) {
    return StoneTransactionModel(
      actionCode: map['actionCode']?.toString(),
      aid: map['aid']?.toString(),
      amount: map['amount']?.toString(),
      arcq: map['arcq']?.toString(),
      cardBrand: map['cardBrand']?.toString(),
      cardHolderNumber: map['cardHolderNumber']?.toString(),
      date: map['date']?.toString(),
      entryMode: map['entryMode']?.toString(),
      idFromBase: _asInt(map['idFromBase']),
      isBuildResponse: _asInt(map['isBuildResponse']),
      manufacture: map['manufacture']?.toString(),
      result: _asInt(map['result']),
      saleAffiliationKey: map['saleAffiliationKey']?.toString(),
      serialNumber: map['serialNumber']?.toString(),
      time: map['time']?.toString(),
      transactionReference: map['transactionReference']?.toString(),
      typeOfTransactionEnum: map['typeOfTransactionEnum']?.toString(),
      errorMessage: map['errorMessage']?.toString(),
      method: map['method']?.toString(),
      transactionStatus: map['transactionStatus']?.toString(),
      messageFromAuthorize: map['messageFromAuthorize']?.toString(),
      message: map['message']?.toString(),
      authorizationCode: map['authorizationCode']?.toString(),
      actionResultMessage: map['actionResultMessage']?.toString(),
    );
  }

  String toJson() => json.encode(toMap());

  factory StoneTransactionModel.fromJson(String source) => StoneTransactionModel.fromMap(json.decode(source) as Map<String, dynamic>);

  factory StoneTransactionModel.toError(String message) {
    return StoneTransactionModel(
      actionCode: '',
      aid: '',
      amount: '',
      arcq: '',
      cardBrand: '',
      cardHolderNumber: '',
      date: '',
      entryMode: '',
      idFromBase: 0,
      isBuildResponse: 0,
      manufacture: '',
      result: 999999,
      saleAffiliationKey: '',
      serialNumber: '',
      time: '',
      transactionReference: '',
      typeOfTransactionEnum: '',
      errorMessage: message,
      method: '',
      transactionStatus: '',
      messageFromAuthorize: '',
      message: '',
      authorizationCode: '',
      actionResultMessage: '',
    );
  }

  @override
  bool operator ==(covariant StoneTransactionModel other) {
    if (identical(this, other)) return true;

    return other.actionCode == actionCode && other.aid == aid && other.amount == amount && other.arcq == arcq && other.cardBrand == cardBrand && other.cardHolderNumber == cardHolderNumber && other.date == date && other.entryMode == entryMode && other.idFromBase == idFromBase && other.isBuildResponse == isBuildResponse && other.manufacture == manufacture && other.result == result && other.saleAffiliationKey == saleAffiliationKey && other.serialNumber == serialNumber && other.time == time && other.transactionReference == transactionReference && other.typeOfTransactionEnum == typeOfTransactionEnum && other.errorMessage == errorMessage && other.method == method && other.transactionStatus == transactionStatus && other.messageFromAuthorize == messageFromAuthorize && other.message == message && other.authorizationCode == authorizationCode && other.actionResultMessage == actionResultMessage;
  }

  @override
  int get hashCode {
    return actionCode.hashCode ^ aid.hashCode ^ amount.hashCode ^ arcq.hashCode ^ cardBrand.hashCode ^ cardHolderNumber.hashCode ^ date.hashCode ^ entryMode.hashCode ^ idFromBase.hashCode ^ isBuildResponse.hashCode ^ manufacture.hashCode ^ result.hashCode ^ saleAffiliationKey.hashCode ^ serialNumber.hashCode ^ time.hashCode ^ transactionReference.hashCode ^ typeOfTransactionEnum.hashCode ^ errorMessage.hashCode ^ method.hashCode ^ transactionStatus.hashCode ^ messageFromAuthorize.hashCode ^ message.hashCode ^ authorizationCode.hashCode ^ actionResultMessage.hashCode;
  }
}

int? _asInt(dynamic value) {
  if (value == null) return null;
  if (value is num) return value.toInt();
  return int.tryParse(value.toString());
}
