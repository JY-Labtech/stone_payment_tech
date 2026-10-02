import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:stone_payment_tech/stone_payment_tech.dart';

class _RecordingHandler extends IStoneHandler {
  final events = <String>[];

  @override
  Future<void> onAuthProgress(String message) async => events.add('authProgress:$message');
  @override
  Future<void> onChanged(String message) async => events.add('changed:$message');
  @override
  Future<void> onError(String message) async => events.add('error:$message');
  @override
  Future<void> onFinishedResponse(String message) async => events.add('finished:$message');
  @override
  Future<void> onLoading(bool show) async => events.add('loading:$show');
  @override
  Future<void> onMessage(String message) async => events.add('message:$message');
  @override
  Future<void> onTransactionSuccess() async => events.add('success');
}

class _RecordingTechHandler extends IStoneTechHandler {
  final events = <Object>[];

  @override
  void onAbortedSuccessfully() => events.add('aborted');
  @override
  void onAuthProgress(StoneTechResponse response) => events.add(response);
  @override
  void onChanged(StoneTechResponse response) => events.add(response);
  @override
  void onError(StoneTechResponse response) => events.add(response);
  @override
  void onFinishedResponse(StoneTransactionModel response) => events.add(response);
  @override
  void onLoading(bool show) => events.add('loading:$show');
  @override
  void onMessage(String message) => events.add('message:$message');
  @override
  void onTransactionInfo(String response) => events.add('info:$response');
  @override
  void onTransactionSuccess() => events.add('success');
  @override
  void writeToFile({String? transactionCode, String? transactionId, String? response}) => events.add('file:$transactionCode/$transactionId/$response');
}

Future<dynamic> _call(String method, [dynamic arguments]) {
  return IStoneHelper.callHandler(
    call: MethodCall(method, arguments),
    iStoneHandler: _handler,
    stoneTechHandler: _tech,
    onTransaction: _transactions.add,
  );
}

late _RecordingHandler _handler;
_RecordingTechHandler? _tech;
late List<StoneTransactionModel> _transactions;

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    _handler = _RecordingHandler();
    _tech = _RecordingTechHandler();
    _transactions = [];
  });

  group('IStoneHelper.callHandler (eventos do nativo)', () {
    test('cada evento chega ao IStoneHandler com o argumento', () async {
      await _call('onTransactionSuccess', true);
      await _call('onError', 'falhou');
      await _call('onMessage', 'Insira o cartão');
      await _call('onChanged', '{"method":"QRCode","message":"base64"}');
      await _call('onLoading', true);
      await _call('onAuthProgress', 'Transação cancelada');
      await _call('onFinishedResponse', '{"method":"transaction","result":0}');
      expect(_handler.events, [
        'success',
        'error:falhou',
        'message:Insira o cartão',
        'changed:{"method":"QRCode","message":"base64"}',
        'loading:true',
        'authProgress:Transação cancelada',
        'finished:{"method":"transaction","result":0}',
      ]);
    });

    test('onChanged é reconhecido (antes lançava "Not Implemented")', () async {
      expect(await _call('onChanged', '{"method":"PaymentOptions","options":["Alimentação","Refeição"]}'), isTrue);
    });

    test('argumento nulo vira string vazia (antes TypeError)', () async {
      await _call('onMessage', null);
      await _call('onError', null);
      await _call('onLoading', null);
      expect(_handler.events, ['message:', 'error:', 'loading:false']);
    });

    test('onFinishedResponse com JSON chama onTransaction com o modelo', () async {
      await _call('onFinishedResponse', jsonEncode({'method': 'active', 'result': 0, 'message': 'Terminal ativado'}));
      expect(_transactions.single.method, 'active');
      expect(_transactions.single.result, 0);
    });

    test('onFinishedResponse com texto ou JSON inválido não chama onTransaction', () async {
      await _call('onFinishedResponse', 'texto');
      await _call('onFinishedResponse', '{inválido');
      expect(_transactions, isEmpty);
      expect(_handler.events, hasLength(2));
    });

    test('método desconhecido lança MissingPluginException', () async {
      await expectLater(_call('onFoo', 'x'), throwsA(isA<MissingPluginException>()));
    });

    test('eventos só do IStoneTechHandler sem ele lançam MissingPluginException', () async {
      _tech = null;
      await expectLater(_call('onAbortedSuccessfully', true), throwsA(isA<MissingPluginException>()));
    });
  });

  group('IStoneTechHandler recebe os eventos convertidos', () {
    test('converte JSON em StoneTechResponse/StoneTransactionModel', () async {
      await _call('onTransactionSuccess', true);
      await _call('onError', '{"method":"active","result":999999,"errorMessage":"Erro"}');
      await _call('onMessage', 'Aguarde');
      await _call('onChanged', '{"method":"QRCode","message":"abc"}');
      await _call('onLoading', false);
      await _call('onAuthProgress', 'Transação cancelada');
      await _call('onFinishedResponse', '{"method":"transaction","amount":"1050","transactionStatus":"APPROVED"}');
      await _call('onAbortedSuccessfully', true);
      await _call('onTransactionInfo', 'info');
      await _call('writeToFile', {'transactionCode': 'C', 'transactionId': 'I', 'response': 'R'});

      final events = _tech!.events;
      expect(events[0], 'success');
      expect(events[1], isA<StoneTechResponse>().having((r) => r.errorMessage, 'errorMessage', 'Erro').having((r) => r.result, 'result', 999999));
      expect(events[2], 'message:Aguarde');
      expect(events[3], isA<StoneTechResponse>().having((r) => r.method, 'method', 'QRCode'));
      expect(events[4], 'loading:false');
      expect(events[5], isA<StoneTechResponse>().having((r) => r.message, 'message', 'Transação cancelada'));
      expect(events[6], isA<StoneTransactionModel>().having((m) => m.isApproved, 'isApproved', isTrue).having((m) => m.amount, 'amount', '1050'));
      expect(events[7], 'aborted');
      expect(events[8], 'info:info');
      expect(events[9], 'file:C/I/R');
    });

    test('onError com texto vira StoneTechResponse(message)', () async {
      await _call('onError', 'Transação não encontrada');
      expect(_tech!.events.single, isA<StoneTechResponse>().having((r) => r.message, 'message', 'Transação não encontrada'));
    });

    test('onFinishedResponse com texto vira StoneTransactionModel.toError', () async {
      await _call('onFinishedResponse', 'falhou');
      expect(_tech!.events.single, isA<StoneTransactionModel>().having((m) => m.errorMessage, 'errorMessage', 'falhou').having((m) => m.result, 'result', 999999));
    });
  });

  group('StonePayment via canal real (mock do nativo)', () {
    const channel = MethodChannel(StoneTech.channelName);
    final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

    Future<void> sendFromNative(String method, dynamic arguments) async {
      final data = const StandardMethodCodec().encodeMethodCall(MethodCall(method, arguments));
      await messenger.handlePlatformMessage(StoneTech.channelName, data, (_) {});
    }

    test('ativação marca isPinpadActivated e setHandler troca o handler mantendo o IStoneTechHandler', () async {
      final first = _RecordingHandler();
      final tech = _RecordingTechHandler();
      final payment = StonePayment.create(channel: channel, paymentHandler: first, iStoneTechHandler: tech);

      StoneTech.isPinpadActivated = false;
      // Erro de ativação: message nula (antes NPE no _checkPinpadActivated).
      await sendFromNative('onFinishedResponse', jsonEncode({'method': 'active', 'result': 999999, 'errorMessage': 'Erro ao ativar'}));
      expect(StoneTech.isPinpadActivated, isFalse);
      await sendFromNative('onFinishedResponse', jsonEncode({'method': 'active', 'result': 0, 'message': 'Terminal ativado'}));
      expect(StoneTech.isPinpadActivated, isTrue);
      expect(first.events, hasLength(2));

      final second = _RecordingHandler();
      payment.setHandler(second);
      await sendFromNative('onMessage', 'depois do setHandler');
      expect(second.events, ['message:depois do setHandler']);
      expect(first.events, hasLength(2));
      expect(tech.events.last, 'message:depois do setHandler');
      StoneTech.isPinpadActivated = false;
      messenger.setMockMethodCallHandler(channel, null);
    });
  });
}
