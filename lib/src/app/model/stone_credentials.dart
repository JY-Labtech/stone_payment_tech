// ignore_for_file: public_member_api_docs, sort_constructors_first
import 'dart:convert';

class StoneTechCredentialsModel {
  final String stoneCode;
  final String? qrCodeAuthorization;
  final String? qrCodeProviderid;
  final String appName;

  StoneTechCredentialsModel({
    required this.stoneCode,
    this.qrCodeAuthorization,
    this.qrCodeProviderid,
    required this.appName,
  });

  StoneTechCredentialsModel copyWith({
    String? stoneCode,
    String? qrCodeAuthorization,
    String? qrCodeProviderid,
    String? appName,
  }) {
    return StoneTechCredentialsModel(
      stoneCode: stoneCode ?? this.stoneCode,
      qrCodeAuthorization: qrCodeAuthorization ?? this.qrCodeAuthorization,
      qrCodeProviderid: qrCodeProviderid ?? this.qrCodeProviderid,
      appName: appName ?? this.appName,
    );
  }

  Map<String, dynamic> toMap() {
    return <String, dynamic>{
      'stoneCode': stoneCode,
      'qrCodeAuthorization': qrCodeAuthorization,
      'qrCodeProviderid': qrCodeProviderid,
      'appName': appName,
    };
  }

  factory StoneTechCredentialsModel.fromMap(Map<String, dynamic> map) {
    return StoneTechCredentialsModel(
      stoneCode: map['stoneCode'] as String,
      qrCodeAuthorization: map['qrCodeAuthorization'] != null ? map['qrCodeAuthorization'] as String : null,
      qrCodeProviderid: map['qrCodeProviderid'] != null ? map['qrCodeProviderid'] as String : null,
      appName: map['appName'] as String? ?? '',
    );
  }

  String toJson() => json.encode(toMap());

  factory StoneTechCredentialsModel.fromJson(String source) => StoneTechCredentialsModel.fromMap(json.decode(source) as Map<String, dynamic>);

  @override
  String toString() => 'StoneTechCredentialsModel(stoneCode: $stoneCode, qrCodeAuthorization: $qrCodeAuthorization, qrCodeProviderid: $qrCodeProviderid)';

  @override
  bool operator ==(covariant StoneTechCredentialsModel other) {
    if (identical(this, other)) return true;

    return other.stoneCode == stoneCode && other.qrCodeAuthorization == qrCodeAuthorization && other.qrCodeProviderid == qrCodeProviderid;
  }

  @override
  int get hashCode => stoneCode.hashCode ^ qrCodeAuthorization.hashCode ^ qrCodeProviderid.hashCode;
}
