// ignore_for_file: public_member_api_docs, sort_constructors_first
import 'dart:convert';

import '../utils/extensions/printer_type_ext.dart';
import '../utils/types/printer_type.dart';

class StonePrinterParams {
  final List<StonePrinter> printers;
  StonePrinterParams({required this.printers});

  StonePrinterParams copyWith({List<StonePrinter>? printers}) {
    return StonePrinterParams(printers: printers ?? this.printers);
  }

  Map<String, dynamic> toMap() {
    return <String, dynamic>{'printers': printers.map((x) => x.toMap()).toList()};
  }

  factory StonePrinterParams.fromMap(Map<String, dynamic> map) {
    // Antes fazia cast para List<int> e sempre falhava.
    final printers = map['printers'] as List<dynamic>? ?? const [];
    return StonePrinterParams(printers: printers.map<StonePrinter>((x) => StonePrinter.fromMap(Map<String, dynamic>.from(x as Map))).toList());
  }

  String toJson() => json.encode(toMap());

  factory StonePrinterParams.fromJson(String source) => StonePrinterParams.fromMap(json.decode(source) as Map<String, dynamic>);

  @override
  String toString() => 'StonePrinterParams(printers: $printers)';
}

class StonePrinter {
  final String title;
  final String description;
  final PrinterType printerType;
  final String? imageBase64;
  StonePrinter({required this.title, required this.description, required this.printerType, this.imageBase64});

  StonePrinter copyWith({String? title, String? description, PrinterType? printerType, String? imageBase64}) {
    return StonePrinter(title: title ?? this.title, description: description ?? this.description, printerType: printerType ?? this.printerType, imageBase64: imageBase64 ?? this.imageBase64);
  }

  Map<String, dynamic> toMap() {
    return <String, dynamic>{'key': title, 'value': description, 'type': printerType.name, 'fileBase64': imageBase64};
  }

  factory StonePrinter.fromMap(Map<String, dynamic> map) {
    final type = map['type']?.toString();
    return StonePrinter(
      title: map['key']?.toString() ?? '',
      description: map['value']?.toString() ?? '',
      // Aceita o nome do enum ("image") e o valor enviado ao nativo ("base64").
      printerType: PrinterType.values.firstWhere((element) => element.name == type || element.type == type, orElse: () => PrinterType.text),
      imageBase64: map['fileBase64']?.toString(),
    );
  }

  String toJson() => json.encode(toMap());

  factory StonePrinter.fromJson(String source) => StonePrinter.fromMap(json.decode(source) as Map<String, dynamic>);

  @override
  String toString() {
    return 'StonePrinter(title: $title, description: $description, printerType: $printerType, imageBase64: $imageBase64)';
  }

  @override
  bool operator ==(covariant StonePrinter other) {
    if (identical(this, other)) return true;

    return other.title == title && other.description == description && other.printerType == printerType && other.imageBase64 == imageBase64;
  }

  @override
  int get hashCode {
    return title.hashCode ^ description.hashCode ^ printerType.hashCode ^ imageBase64.hashCode;
  }
}
