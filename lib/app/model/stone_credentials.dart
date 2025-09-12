// ignore_for_file: public_member_api_docs, sort_constructors_first
import 'dart:convert';

class StoneCredentialsModel {
  final String stoneCode;
  final String? qrCodeAuthorization;
  final String? qrCodeProviderid;
  final String licenceKey;
  final String appName;

  StoneCredentialsModel({
    required this.stoneCode,
    this.qrCodeAuthorization,
    this.qrCodeProviderid,
    required this.licenceKey,
    required this.appName,
  });

  StoneCredentialsModel copyWith({
    String? stoneCode,
    String? qrCodeAuthorization,
    String? qrCodeProviderid,
    String? licenceKey,
    String? appName,
  }) {
    return StoneCredentialsModel(
      stoneCode: stoneCode ?? this.stoneCode,
      qrCodeAuthorization: qrCodeAuthorization ?? this.qrCodeAuthorization,
      qrCodeProviderid: qrCodeProviderid ?? this.qrCodeProviderid,
      licenceKey: licenceKey ?? this.licenceKey,
      appName: appName ?? this.appName,
    );
  }

  Map<String, dynamic> toMap() {
    return <String, dynamic>{
      'stoneCode': stoneCode,
      'qrCodeAuthorization': qrCodeAuthorization,
      'qrCodeProviderid': qrCodeProviderid,
      'licenceKey': licenceKey,
      'appName': appName,
    };
  }

  factory StoneCredentialsModel.fromMap(Map<String, dynamic> map) {
    return StoneCredentialsModel(
      stoneCode: map['stoneCode'] as String,
      qrCodeAuthorization: map['qrCodeAuthorization'] != null ? map['qrCodeAuthorization'] as String : null,
      qrCodeProviderid: map['qrCodeProviderid'] != null ? map['qrCodeProviderid'] as String : null,
      licenceKey: map['licenceKey'] as String? ?? '',
      appName: map['appName'] as String? ?? '',
    );
  }

  String toJson() => json.encode(toMap());

  factory StoneCredentialsModel.fromJson(String source) => StoneCredentialsModel.fromMap(json.decode(source) as Map<String, dynamic>);

  @override
  String toString() => 'StoneCredentialsModel(stoneCode: $stoneCode, qrCodeAuthorization: $qrCodeAuthorization, qrCodeProviderid: $qrCodeProviderid)';

  @override
  bool operator ==(covariant StoneCredentialsModel other) {
    if (identical(this, other)) return true;

    return other.stoneCode == stoneCode && other.qrCodeAuthorization == qrCodeAuthorization && other.qrCodeProviderid == qrCodeProviderid;
  }

  @override
  int get hashCode => stoneCode.hashCode ^ qrCodeAuthorization.hashCode ^ qrCodeProviderid.hashCode;
}
