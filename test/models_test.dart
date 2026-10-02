import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:stone_payment_tech/stone_payment_tech.dart';

/// JSON real gerado pelo ActionResult (Gson) numa transação aprovada.
const _approvedTransactionJson = '''
{"idFromBase":12,"amount":"1050","cardHolderNumber":"************1234","cardBrand":"MASTERCARD",
"date":"01/10/2026","time":"14:22","aid":"A0000000041010","arcq":"ABCD","transactionReference":"REF",
"saleAffiliationKey":"KEY","entryMode":"ICC","typeOfTransactionEnum":"CREDIT","serialNumber":"SN1",
"manufacture":"SUNMI","actionCode":"0000","cardBrandId":2,"method":"transaction","transactionStatus":"APPROVED",
"messageFromAuthorize":"Aprovado","authorizationCode":"123456","result":0,"isBuildResponse":1,
"transactionKey":"TK","InitiatorTransactionKey":"ITK","isPrinterRequest":true}
''';

void main() {
  group('StoneTransactionModel', () {
    test('fromMap com payload realista do nativo', () {
      final model = StoneTransactionModel.fromMap(json.decode(_approvedTransactionJson) as Map<String, dynamic>);
      expect(model.idFromBase, 12);
      expect(model.amount, '1050');
      expect(model.cardBrand, 'MASTERCARD');
      expect(model.transactionStatus, 'APPROVED');
      expect(model.authorizationCode, '123456');
      expect(model.result, 0);
      expect(model.isBuildResponse, 1);
      expect(model.isApproved, isTrue);
      expect(model.errorMessage, isNull);
    });

    test('campos ausentes ficam nulos e números como double/string são aceitos', () {
      final model = StoneTransactionModel.fromMap({'method': 'transaction', 'result': 999999.0, 'idFromBase': '7'});
      expect(model.result, 999999);
      expect(model.idFromBase, 7);
      expect(model.amount, isNull);
      expect(model.isApproved, isFalse);
    });

    test('isApproved por mensagem do autorizador', () {
      expect(StoneTransactionModel.fromMap({'messageFromAuthorize': 'APROVADO'}).isApproved, isTrue);
      expect(StoneTransactionModel.fromMap({'transactionStatus': 'DECLINED'}).isApproved, isFalse);
    });

    test('toMap/toJson/fromJson ida e volta', () {
      final model = StoneTransactionModel.fromJson(_approvedTransactionJson);
      final copy = StoneTransactionModel.fromJson(model.toJson());
      expect(copy, model);
      expect(copy.hashCode, model.hashCode);
      expect(model.toMap()['amount'], '1050');
    });

    test('toError', () {
      final model = StoneTransactionModel.toError('falhou');
      expect(model.errorMessage, 'falhou');
      expect(model.result, 999999);
      expect(model.isApproved, isFalse);
    });
  });

  group('StoneTechResponse', () {
    test('fromMap/fromJson com campos nulos', () {
      final response = StoneTechResponse.fromJson('{"method":"cancel","result":0,"message":"Aprovado"}');
      expect(response.method, 'cancel');
      expect(response.result, 0);
      expect(response.message, 'Aprovado');
      expect(response.errorMessage, isNull);
      expect(StoneTechResponse.fromMap({}).result, isNull);
      expect(StoneTechResponse.fromMap({'result': '999999'}).result, 999999);
    });

    test('ida e volta e igualdade', () {
      final response = StoneTechResponse(method: 'm', errorMessage: 'e', message: 'x', result: 1);
      expect(StoneTechResponse.fromJson(response.toJson()), response);
      expect(response.toString(), contains('method: m'));
    });
  });

  group('StonePrinterParams / StonePrinter', () {
    test('toJson tem o formato lido pelo nativo', () {
      final params = StonePrinterParams(printers: [
        StonePrinter(title: 'Total: ', description: 'R\$ 1,00', printerType: PrinterType.text),
        StonePrinter(title: '', description: '', printerType: PrinterType.image, imageBase64: 'aGVsbG8='),
      ]);
      final map = json.decode(params.toJson()) as Map<String, dynamic>;
      expect(map['printers'], [
        {'key': 'Total: ', 'value': 'R\$ 1,00', 'type': 'text', 'fileBase64': null},
        {'key': '', 'value': '', 'type': 'image', 'fileBase64': 'aGVsbG8='},
      ]);
    });

    test('fromJson funciona (antes o cast para List<int> sempre falhava)', () {
      final params = StonePrinterParams(printers: [
        StonePrinter(title: 'A', description: 'B', printerType: PrinterType.text),
        StonePrinter(title: '', description: '', printerType: PrinterType.image, imageBase64: 'aGVsbG8='),
      ]);
      final copy = StonePrinterParams.fromJson(params.toJson());
      expect(copy.printers, params.printers);
    });

    test('fromMap aceita "base64" como imagem e tolera campos nulos', () {
      final printer = StonePrinter.fromMap({'key': null, 'value': null, 'type': 'base64', 'fileBase64': 'x'});
      expect(printer.printerType, PrinterType.image);
      expect(printer.title, '');
      expect(StonePrinter.fromMap({'type': 'desconhecido'}).printerType, PrinterType.text);
      expect(StonePrinterParams.fromMap({}).printers, isEmpty);
    });

    test('copyWith', () {
      final printer = StonePrinter(title: 'A', description: 'B', printerType: PrinterType.text);
      expect(printer.copyWith(title: 'C').title, 'C');
      expect(printer.copyWith().description, 'B');
      expect(StonePrinterParams(printers: [printer]).copyWith().printers, [printer]);
    });
  });

  group('StoneTechCredentialsModel', () {
    test('toMap/fromMap/fromJson', () {
      final credentials = StoneTechCredentialsModel(stoneCode: '***REMOVIDO***', appName: 'Loja', qrCodeAuthorization: 'tok', qrCodeProviderid: 'prov');
      final copy = StoneTechCredentialsModel.fromJson(credentials.toJson());
      expect(copy, credentials);
      expect(copy.appName, 'Loja');
      expect(StoneTechCredentialsModel.fromMap({'stoneCode': '1'}).appName, '');
      expect(credentials.copyWith(stoneCode: '2').stoneCode, '2');
      expect(credentials.toString(), isNot(contains('Loja')));
    });
  });

  group('enums e extensões', () {
    test('PaymentTypeCall.method', () {
      expect(PaymentTypeCall.credit.method, 'paymentCredit');
      expect(PaymentTypeCall.paymentPrinterTransactionKey.method, 'paymentPrinterTransactionKey');
      for (final type in PaymentTypeCall.values) {
        expect(type.method, startsWith('payment'));
      }
    });

    test('PaymentTypeHandler ida e volta pelo nome do canal', () {
      for (final type in PaymentTypeHandler.values) {
        expect(type.method.handler, type);
      }
      expect(() => 'onFoo'.handler, throwsA('Not Implemented'));
    });

    test('PaymentTypeTransaction.type bate com o nativo', () {
      expect(PaymentTypeTransaction.credit.type, 1);
      expect(PaymentTypeTransaction.debit.type, 2);
      expect(PaymentTypeTransaction.pix.type, 3);
      expect(PaymentTypeTransaction.voucher.type, 4);
    });

    test('PaymentTypecredit e PrinterType', () {
      expect(PaymentTypecredit.salesman.value, 2);
      expect(PaymentTypecredit.client.value, 3);
      expect(PrinterType.text.type, 'text');
      expect(PrinterType.image.type, 'base64');
    });
  });

  group('IStoneHelper conversões', () {
    test('convertToStoneTechResponse', () {
      expect(IStoneHelper.convertToStoneTechResponse('{"result":0}').result, 0);
      expect(IStoneHelper.convertToStoneTechResponse('texto').message, 'texto');
      expect(IStoneHelper.convertToStoneTechResponse('{quebrado').message, '{quebrado');
      expect(IStoneHelper.convertToStoneTechResponse('[{"a":1}]').message, '[{"a":1}]');
    });

    test('convertToStoneTransactionModel', () {
      expect(IStoneHelper.convertToStoneTransactionModel(_approvedTransactionJson).isApproved, isTrue);
      expect(IStoneHelper.convertToStoneTransactionModel('falhou').errorMessage, 'falhou');
      expect(IStoneHelper.convertToStoneTransactionModel('{x').errorMessage, '{x');
    });
  });

  group('StoneStatusTech', () {
    test('factories e getters', () {
      expect(StoneStatusTech.message('m'), isA<StoneStatusTechMessage>());
      expect(StoneStatusTech.activate(), isA<StoneStatusTechActivate>());
      expect(StoneStatusTech.canceled({'a': 1}), isA<StoneStatusTechCanceled>());
      final approved = StoneStatusTech.transaction(StoneTransactionModel.fromJson(_approvedTransactionJson));
      expect((approved as StoneStatusTechTransaction).isApproved, isTrue);
      final qr = StoneStatusTechQRCodeTransaction(transaction: StoneTransactionModel.fromMap({'message': 'base64', 'errorMessage': 'QRCODE_EXPIRED: QRCode expirado'}));
      expect(qr.qrCode, 'base64');
      expect(qr.isExpired, isTrue);
    });
  });
}
