import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:stone_payment_tech/stone_payment_tech.dart';

class _FakeHandler extends IStoneHandler {
  final List<String> finished = [];

  @override
  Future<void> onAuthProgress(String message) async {}
  @override
  Future<void> onChanged(String message) async {}
  @override
  Future<void> onError(String message) async {}
  @override
  Future<void> onFinishedResponse(String message) async => finished.add(message);
  @override
  Future<void> onLoading(bool show) async {}
  @override
  Future<void> onMessage(String message) async {}
  @override
  Future<void> onTransactionSuccess() async {}
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel(StoneTech.channelName);
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  final calls = <MethodCall>[];
  late StonePayment payment;

  void mockLicenceError() {
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      throw PlatformException(code: 'LICENCE_ERROR', message: 'Licença vencida');
    });
  }

  setUpAll(() {
    payment = StoneTech(channel).initPayment(handler: _FakeHandler());
  });

  setUp(calls.clear);

  tearDown(() => messenger.setMockMethodCallHandler(channel, null));

  test('licenceCheck aponta para o servidor oficial (jylabtech_core)', () {
    final url = utf8.decode(base64.decode(licenceCheck));
    expect(url, 'https://portal-api.jylabtech.com.br/pos-payments/licence/check');
    final uri = Uri.parse(url);
    expect(uri.scheme, 'https');
    expect(uri.host, 'portal-api.jylabtech.com.br');
    expect(uri.path, startsWith('/pos-payments/'));
  });

  test('payment antes de initPayment lança erro', () {
    expect(() => StoneTech(channel).payment, throwsA(isA<String>()));
  });

  group('LICENCE_ERROR do nativo nunca vira sucesso', () {
    test('métodos que retornam bool devolvem false', () async {
      mockLicenceError();
      expect(await payment.activePinpad(appName: 'app', stoneCode: '123'), isFalse);
      expect(
        await payment.activePinpadWithCredentials(appName: 'app', stoneCode: '123', qrCodeAuthroization: 'a', qrCodeProviderid: 'b'),
        isFalse,
      );
      expect(await payment.reversal(), isFalse);
      expect(await payment.printWrapPaper(2), isFalse);
      expect(await payment.printFromBase64('aGVsbG8='), isFalse);
      expect(calls.map((c) => c.method), [
        'paymentActivePinpad',
        'paymentActivePinpadCredentials',
        'paymentReversal',
        'paymentPrintWrapPaper',
        'paymentPrintBase64',
      ]);
    });

    test('pagamentos, cancelamento e impressões propagam PlatformException', () async {
      mockLicenceError();
      final licenceError = throwsA(isA<PlatformException>().having((e) => e.code, 'code', 'LICENCE_ERROR'));
      await expectLater(payment.creditPayment(100), licenceError);
      await expectLater(payment.creditPaymentParc(value: 100, installment: 2), licenceError);
      await expectLater(payment.debitPayment(100), licenceError);
      await expectLater(payment.voucherPayment(100), licenceError);
      await expectLater(payment.pixPayment(amount: 100, qrCodeAuthroization: 'a', qrCodeProviderid: 'b'), licenceError);
      await expectLater(payment.cancelTransaction(idFromBase: 1), licenceError);
      await expectLater(payment.printerCurrentTransaction(printCustomerSlip: true), licenceError);
      await expectLater(payment.paymentPrinterTransactionKey(transactionKey: 'k'), licenceError);
      await expectLater(payment.getAllTransactions(), licenceError);
      await expectLater(payment.abortTransaction(), licenceError);
    });
  });

  test('evento de licença com erro vira StoneStatusTechActivateError no stream', () async {
    final handler = StoneStreamHandler();
    final next = handler.statusStream.first;
    await handler.onFinishedResponse(jsonEncode({'method': 'licence', 'result': 999999, 'errorMessage': 'Licença vencida'}));
    final status = await next;
    expect(status, isA<StoneStatusTechActivateError>());
    expect((status as StoneStatusTechActivateError).message, 'Licença vencida');
    handler.streamDispose();
  });
}
