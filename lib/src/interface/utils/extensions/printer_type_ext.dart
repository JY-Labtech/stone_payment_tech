import 'package:stone_payment_tech/src/interface/utils/types/printer_type.dart';

extension PrinterTypeExt on PrinterType {
  String get type {
    const Map<PrinterType, String> printerTypeMap = {
      PrinterType.text: "text",
      PrinterType.image: "base64",
    };
    return printerTypeMap[this] ?? "text";
  }
}
