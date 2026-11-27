import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:stone_payment_tech/stone_payment_tech.dart';
import 'package:stone_payment_tech/stone_tech_payment.dart' as legacy;

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

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel(StoneTech.channelName);
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  final calls = <String>[];
  late StonePayment payment;

  /// Simula o nativo: operações respondem `true` ao serem disparadas e os
  /// getters respondem o valor (contrato do MethodDispatcher).
  void mockNativeSuccess() {
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call.method);
      switch (call.method) {
        case 'paymentGetSerialNumber':
          return 'SN123';
        case 'paymentGetManufacture':
          return 'SUNMI';
        default:
          return true;
      }
    });
  }

  setUpAll(() {
    payment = StoneTech(channel).initPayment(handler: _FakeHandler());
  });

  setUp(calls.clear);

  tearDown(() => messenger.setMockMethodCallHandler(channel, null));

  test('todos os métodos completam com a resposta de sucesso do nativo', () async {
    mockNativeSuccess();
    const timeout = Duration(seconds: 1);
    expect(await payment.activePinpad(appName: 'app', stoneCode: '123').timeout(timeout), isTrue);
    expect(
      await payment.activePinpadWithCredentials(appName: 'app', stoneCode: '123', qrCodeAuthroization: 'a', qrCodeProviderid: 'b').timeout(timeout),
      isTrue,
    );
    expect(await payment.reversal().timeout(timeout), isTrue);
    expect(await payment.creditPayment(100).timeout(timeout), isTrue);
    expect(await payment.creditPaymentParc(value: 100, installment: 2).timeout(timeout), isTrue);
    expect(await payment.debitPayment(100).timeout(timeout), isTrue);
    expect(await payment.voucherPayment(100).timeout(timeout), isTrue);
    expect(await payment.pixPayment(amount: 100, qrCodeAuthroization: 'a', qrCodeProviderid: 'b').timeout(timeout), isTrue);
    await payment.activeDebugLog(isDebugLog: true).timeout(timeout);
    expect(await payment.getPosSerialNumber().timeout(timeout), 'SN123');
    expect(await payment.getPosManufacture().timeout(timeout), 'SUNMI');
    expect(await payment.abortTransaction().timeout(timeout), isTrue);
    expect(await payment.abortPIXTransaction().timeout(timeout), isTrue);
    expect(await payment.abortActivation().timeout(timeout), isTrue);
    expect(await payment.printerCurrentTransaction(printCustomerSlip: true).timeout(timeout), isTrue);
    expect(await payment.paymentPrinterTransactionKey(transactionKey: 'k').timeout(timeout), isTrue);
    expect(await payment.cancelTransaction(idFromBase: 1).timeout(timeout), isTrue);
    expect(await payment.getAllTransactions().timeout(timeout), isTrue);
    expect(await payment.setPaymentOption(option: 'opt').timeout(timeout), isTrue);
    expect(await payment.getTransactionByInitiatorTransactionKey(initiatorTransactionKey: 'i').timeout(timeout), isTrue);
    expect(await payment.printWrapPaper(2).timeout(timeout), isTrue);
    expect(await payment.printFromBase64('aGVsbG8=').timeout(timeout), isTrue);
    expect(calls, hasLength(22));
  });

  test('erro ao disparar (code ERROR) completa com false ou PlatformException', () async {
    messenger.setMockMethodCallHandler(channel, (call) async {
      throw PlatformException(code: 'ERROR', message: 'SDK não ativado');
    });
    expect(await payment.reversal(), isFalse);
    expect(await payment.activePinpad(appName: 'app', stoneCode: '123'), isFalse);
    await expectLater(
      payment.creditPayment(100),
      throwsA(isA<PlatformException>().having((e) => e.code, 'code', 'ERROR')),
    );
  });

  test('import antigo stone_tech_payment.dart exporta a mesma API', () {
    expect(legacy.StoneTech.channelName, StoneTech.channelName);
    expect(identical(legacy.StoneTech.I, StoneTech.I), isTrue);
    expect(legacy.CHANNEL_NAME, CHANNEL_NAME);
  });
}
