import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:stone_payment_tech/stone_payment_tech.dart';

class _FakeHandler extends IStoneHandler {
  @override
  Future<void> onAuthProgress(String message) async {}
  @override
  Future<void> onChanged(String message) async {}
  @override
  Future<void> onError(String message) async {}
  @override
  Future<void> onFinishedResponse(String message) async {}
  @override
  Future<void> onLoading(bool show) async {}
  @override
  Future<void> onMessage(String message) async {}
  @override
  Future<void> onTransactionSuccess() async {}
}

/// Como o método trata um erro do nativo.
enum _OnError { returnsFalse, throws }

class _Case {
  final String name;
  final Future<dynamic> Function(StonePayment p) invoke;
  final String method;
  final Object? arguments;
  final Object? nativeValue;
  final Object? expected;
  final _OnError onError;

  const _Case(
    this.name,
    this.invoke,
    this.method,
    this.arguments, {
    this.nativeValue = true,
    this.expected = true,
    this.onError = _OnError.throws,
  });
}

final _paymentDefaults = {
  'installment': 1,
  'withInterest': false,
  'printCustomerSlip': true,
  'printEstablishmentSlip': false,
  'initiatorTransactionKey': '',
};

final _cases = <_Case>[
  _Case('checkLicence', (p) => p.checkLicence(), 'checkLicence', null),
  _Case('reversal', (p) => p.reversal(), 'paymentReversal', null, onError: _OnError.returnsFalse),
  _Case(
    'activePinpad',
    (p) => p.activePinpad(appName: 'Loja', stoneCode: '***REMOVIDO***'),
    'paymentActivePinpad',
    {'appName': 'Loja', 'stoneCode': '***REMOVIDO***'},
    onError: _OnError.returnsFalse,
  ),
  _Case(
    'activePinpadWithCredentials',
    (p) => p.activePinpadWithCredentials(appName: 'Loja', stoneCode: '***REMOVIDO***', qrCodeAuthroization: 'tok', qrCodeProviderid: 'prov'),
    'paymentActivePinpadCredentials',
    {'appName': 'Loja', 'stoneCode': '***REMOVIDO***', 'qrCodeAuthorization': 'tok', 'qrCodeProviderid': 'prov', 'printCustomerSlip': true},
    onError: _OnError.returnsFalse,
  ),
  _Case(
    'creditPayment (valor em centavos como String)',
    (p) => p.creditPayment(1050),
    'paymentCredit',
    {'amount': '1050', ..._paymentDefaults},
  ),
  _Case(
    'creditPayment com opções',
    (p) => p.creditPayment(1, printCustomerSlip: false, printEstablishmentSlip: true, initiatorTransactionKey: 'ITK'),
    'paymentCredit',
    {'amount': '1', 'installment': 1, 'withInterest': false, 'printCustomerSlip': false, 'printEstablishmentSlip': true, 'initiatorTransactionKey': 'ITK'},
  ),
  _Case(
    'creditPaymentParc defaults',
    (p) => p.creditPaymentParc(value: 9999),
    'paymentCreditParc',
    {'amount': '9999', ..._paymentDefaults},
  ),
  _Case(
    'creditPaymentParc com juros',
    (p) => p.creditPaymentParc(value: 30000, installment: 6, withInterest: true, printCustomerSlip: false, printEstablishmentSlip: true, initiatorTransactionKey: 'K'),
    'paymentCreditParc',
    {'amount': '30000', 'installment': 6, 'withInterest': true, 'printCustomerSlip': false, 'printEstablishmentSlip': true, 'initiatorTransactionKey': 'K'},
  ),
  _Case('debitPayment', (p) => p.debitPayment(250), 'paymentDebit', {'amount': '250', ..._paymentDefaults}),
  _Case('voucherPayment', (p) => p.voucherPayment(3200), 'paymentVoucher', {'amount': '3200', ..._paymentDefaults}),
  _Case(
    'pixPayment',
    (p) => p.pixPayment(amount: 500, qrCodeAuthroization: 'tok', qrCodeProviderid: 'prov'),
    'paymentPix',
    {'amount': '500', 'installment': 1, 'withInterest': false, 'qrCodeAuthorization': 'tok', 'qrCodeProviderid': 'prov', 'printCustomerSlip': true, 'printEstablishmentSlip': false, 'initiatorTransactionKey': ''},
  ),
  _Case('activeDebugLog', (p) => p.activeDebugLog(isDebugLog: true), 'paymentActiveDebugLog', {'isDebugLog': true}, expected: null),
  _Case('getPosSerialNumber', (p) => p.getPosSerialNumber(), 'paymentGetSerialNumber', null, nativeValue: 'SN123', expected: 'SN123'),
  _Case('getPosManufacture', (p) => p.getPosManufacture(), 'paymentGetManufacture', null, nativeValue: 'SUNMI', expected: 'SUNMI'),
  _Case('abortTransaction', (p) => p.abortTransaction(), 'paymentAbort', null),
  _Case('abortPIXTransaction', (p) => p.abortPIXTransaction(), 'paymentAbortPix', null),
  _Case('abortActivation', (p) => p.abortActivation(), 'paymentAbortActivation', null),
  _Case('printerCurrentTransaction', (p) => p.printerCurrentTransaction(printCustomerSlip: false), 'paymentPrinterTransaction', {'printCustomerSlip': false}),
  _Case(
    'paymentPrinterTransactionKey',
    (p) => p.paymentPrinterTransactionKey(transactionKey: 'TK'),
    'paymentPrinterTransactionKey',
    {'transactionKey': 'TK'},
  ),
  _Case('cancelTransaction', (p) => p.cancelTransaction(idFromBase: 42), 'paymentCancelTransaction', {'idFromBase': 42}),
  _Case('getAllTransactions', (p) => p.getAllTransactions(), 'paymentGetAllTransactions', null),
  _Case('setPaymentOption', (p) => p.setPaymentOption(option: 'Refeição'), 'paymentOptions', {'option': 'Refeição'}),
  _Case(
    'getTransactionByInitiatorTransactionKey',
    (p) => p.getTransactionByInitiatorTransactionKey(initiatorTransactionKey: 'ITK'),
    'paymentGetTransactionByInitiatorTransactionKey',
    {'InitiatorTransactionKey': 'ITK'},
  ),
  _Case(
    'customPrinter (JSON do StonePrinterParams)',
    (p) => p.customPrinter(StonePrinterParams(printers: [StonePrinter(title: 'Total: ', description: 'R\$ 10,50', printerType: PrinterType.text)])),
    'paymentCustomPrinter',
    {'printerParams': '{"printers":[{"key":"Total: ","value":"R\$ 10,50","type":"text","fileBase64":null}]}'},
    onError: _OnError.returnsFalse,
  ),
  _Case('printFromBase64', (p) => p.printFromBase64('aGVsbG8='), 'paymentPrintBase64', {'printerParams': 'aGVsbG8='}, onError: _OnError.returnsFalse),
  _Case('printWrapPaper', (p) => p.printWrapPaper(3), 'paymentPrintWrapPaper', {'lines': 3}, onError: _OnError.returnsFalse),
];

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel(StoneTech.channelName);
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  final calls = <MethodCall>[];
  late StonePayment payment;

  void mockNative(Object? Function(MethodCall call) reply) {
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      return reply(call);
    });
  }

  setUpAll(() {
    payment = StoneTech(channel).initPayment(handler: _FakeHandler());
  });

  setUp(calls.clear);
  tearDown(() => messenger.setMockMethodCallHandler(channel, null));

  test('todos os métodos do canal Dart estão cobertos', () {
    final methods = _cases.map((c) => c.method).toSet();
    final expected = PaymentTypeCall.values
        .where((t) => ![PaymentTypeCall.lastTransaction, PaymentTypeCall.refund, PaymentTypeCall.pinpadAuthenticated].contains(t))
        .map((t) => t.method)
        .toSet()
      ..add('checkLicence');
    expect(methods, expected);
  });

  for (final c in _cases) {
    group(c.name, () {
      test('(a) envia o método e os argumentos exatos', () async {
        mockNative((_) => c.nativeValue);
        await c.invoke(payment).timeout(const Duration(seconds: 1));
        expect(calls, hasLength(1));
        expect(calls.single.method, c.method);
        expect(calls.single.arguments, c.arguments);
      });

      test('(b) devolve a resposta de sucesso do nativo', () async {
        mockNative((_) => c.nativeValue);
        final result = await c.invoke(payment).timeout(const Duration(seconds: 1));
        expect(result, c.expected);
      });

      for (final code in ['ERROR', 'LICENCE_ERROR']) {
        test('(c) $code não vira sucesso', () async {
          mockNative((_) => throw PlatformException(code: code, message: 'falhou'));
          if (c.onError == _OnError.returnsFalse) {
            expect(await c.invoke(payment), isFalse);
          } else {
            await expectLater(c.invoke(payment), throwsA(isA<PlatformException>().having((e) => e.code, 'code', code)));
          }
        });
      }
    });
  }

  test('método sem implementação nativa (MissingPluginException) não vira sucesso', () async {
    await expectLater(payment.creditPayment(100), throwsA(isA<MissingPluginException>()));
    expect(await payment.reversal(), isFalse);
  });

  test('checkLicence com resposta nula devolve false', () async {
    mockNative((_) => null);
    expect(await payment.checkLicence(), isFalse);
  });

  group('StoneTech', () {
    test('initPayment devolve sempre a mesma instância', () {
      final tech = StoneTech(channel);
      final first = tech.initPayment(handler: _FakeHandler());
      expect(identical(tech.initPayment(handler: _FakeHandler()), first), isTrue);
      expect(identical(tech.payment, first), isTrue);
    });

    test('StoneTech.I é singleton com o canal oficial', () {
      expect(identical(StoneTech.I, StoneTech.I), isTrue);
      expect(StoneTech.channelName, 'stone_payment_tech');
      expect(CHANNEL_NAME, StoneTech.channelName);
    });
  });
}
