// ignore_for_file: public_member_api_docs, sort_constructors_first
import 'dart:convert';

class StoneTechResponse {
  final String? method;
  final String? errorMessage;
  final String? message;
  final int? result;

  StoneTechResponse({
    this.method,
    this.errorMessage,
    this.message,
    this.result,
  });

  Map<String, dynamic> toMap() {
    return <String, dynamic>{
      'method': method,
      'errorMessage': errorMessage,
      'message': message,
      'result': result,
    };
  }

  factory StoneTechResponse.fromMap(Map<String, dynamic> map) {
    return StoneTechResponse(
      method: map['method']?.toString(),
      errorMessage: map['errorMessage']?.toString(),
      message: map['message']?.toString(),
      result: _asInt(map['result']),
    );
  }

  String toJson() => json.encode(toMap());

  factory StoneTechResponse.fromJson(String source) => StoneTechResponse.fromMap(json.decode(source) as Map<String, dynamic>);

  @override
  String toString() {
    return 'StoneTechResponse(method: $method, errorMessage: $errorMessage, message: $message, result: $result)';
  }

  @override
  bool operator ==(covariant StoneTechResponse other) {
    if (identical(this, other)) return true;

    return other.method == method && other.errorMessage == errorMessage && other.message == message && other.result == result;
  }

  @override
  int get hashCode {
    return method.hashCode ^ errorMessage.hashCode ^ message.hashCode ^ result.hashCode;
  }
}

int? _asInt(dynamic value) {
  if (value == null) return null;
  if (value is num) return value.toInt();
  return int.tryParse(value.toString());
}
