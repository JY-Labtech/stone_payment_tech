import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:stone_payment_tech/src/app/handler/stone_printer_handler.dart';
import 'package:stone_payment_tech/stone_payment_tech.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('StoneStreamHandler', () {
    late StoneStreamHandler handler;
    late List<StoneStatusTech> statuses;
    late StreamSubscription<StoneStatusTech> sub;

    setUp(() {
      handler = StoneStreamHandler();
      statuses = [];
      sub = handler.statusStream.listen(statuses.add);
    });

    tearDown(() async {
      await sub.cancel();
      handler.dispose();
    });

    Future<void> flush() => Future<void>.delayed(Duration.zero);

    test('mensagem simples vira StoneStatusTechMessage', () async {
      await handler.onMessage('Insira o cartão');
      await flush();
      expect(statuses.single, isA<StoneStatusTechMessage>().having((s) => s.message, 'message', 'Insira o cartão'));
    });

    test('licença válida e inválida', () async {
      await handler.onFinishedResponse(jsonEncode({'method': 'licence', 'result': 0, 'message': 'Licença ativada com sucesso'}));
      await handler.onFinishedResponse(jsonEncode({'method': 'licence', 'result': 999999}));
      await flush();
      expect(statuses[0], isA<StoneStatusTechActivateRunning>());
      expect(statuses[1], isA<StoneStatusTechActivateError>().having((s) => s.message, 'message', 'Erro ao validar a licença'));
    });

    test('ativação: sucesso e erro com errorMessage do nativo', () async {
      await handler.onFinishedResponse(jsonEncode({'method': 'active', 'result': 0, 'message': 'Terminal ativado'}));
      await handler.onFinishedResponse(jsonEncode({'method': 'active', 'result': 999999, 'errorMessage': 'Ativação cancelada pelo usuário'}));
      await handler.onFinishedResponse(jsonEncode({'method': 'active', 'result': 999999}));
      await flush();
      expect(statuses[0], isA<StoneStatusTechActivate>());
      expect(statuses[1], isA<StoneStatusTechActivateError>().having((s) => s.message, 'message', 'Ativação cancelada pelo usuário'));
      expect(statuses[2], isA<StoneStatusTechActivateError>().having((s) => s.message, 'message', 'Erro ao ativar o terminal'));
    });

    test('transação, QRCode, opções, cancelamento e impressão', () async {
      await handler.onAuthProgress(jsonEncode({'method': 'QRCode', 'message': 'base64png'}));
      await handler.onAuthProgress(jsonEncode({'method': 'PaymentOptions', 'options': ['Alimentação', 'Refeição'], 'message': 'Selecione'}));
      await handler.onFinishedResponse(jsonEncode({'method': 'printerTransaction', 'message': 'Deseja imprimir sua via?'}));
      await handler.onFinishedResponse(jsonEncode({'method': 'cancel', 'result': 0, 'message': 'Aprovado'}));
      await handler.onFinishedResponse(jsonEncode({'method': 'transaction', 'transactionStatus': 'APPROVED', 'amount': '100'}));
      await flush();
      expect(statuses[0], isA<StoneStatusTechQRCodeTransaction>().having((s) => s.qrCode, 'qrCode', 'base64png'));
      expect(statuses[1], isA<StoneStatusTechPaymentOptions>().having((s) => s.options, 'options', ['Alimentação', 'Refeição']));
      expect(statuses[2], isA<StonePrinterCustomer>());
      expect(statuses[3], isA<StoneStatusTechCanceled>().having((s) => s.cancelExtraData['message'], 'message', 'Aprovado'));
      expect(statuses[4], isA<StoneStatusTechTransaction>().having((s) => s.isApproved, 'isApproved', isTrue));
    });

    test('depois da transação, mensagens não sobrescrevem o status até clear()', () async {
      await handler.onFinishedResponse(jsonEncode({'method': 'transaction', 'transactionStatus': 'APPROVED'}));
      await handler.onMessage('Transação concluída');
      await flush();
      expect(statuses, hasLength(1));
      handler.clear();
      await handler.onMessage('Nova');
      await flush();
      expect(statuses.last, isA<StoneStatusTechMessage>());
    });

    test('JSON inválido ou método desconhecido é ignorado', () async {
      await handler.onError('{quebrado');
      await handler.onError(jsonEncode({'method': 'outro'}));
      await handler.onChanged('x');
      await handler.onLoading(true);
      await handler.onTransactionSuccess();
      await flush();
      expect(statuses, isEmpty);
    });

    test('emitir após dispose não lança', () async {
      handler.dispose();
      await handler.onMessage('tarde');
    });
  });

  group('StoneTechPrinterHandler', () {
    test('eventos sem ação não lançam mais UnimplementedError', () async {
      final finished = <String>[];
      final messages = <String>[];
      final handler = StoneTechPrinterHandler(onFinishedMonitor: finished.add, onMessageMonitor: messages.add);
      await handler.onTransactionSuccess();
      await handler.onAuthProgress('x');
      await handler.onChanged('x');
      await handler.onLoading(true);
      await handler.onMessage('m');
      await handler.onError('e');
      await handler.onFinishedResponse('f');
      expect(messages, ['m', 'e']);
      expect(finished, ['f']);
    });
  });

  group('StoneStreamController', () {
    const channel = MethodChannel(StoneTech.channelName);
    final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
    final calls = <MethodCall>[];

    setUpAll(() => StoneTech.I.initPayment(handler: StoneStreamHandler()));

    tearDown(() {
      messenger.setMockMethodCallHandler(channel, null);
      calls.clear();
      StoneTech.isPinpadActivated = false;
    });

    final credentials = StoneTechCredentialsModel(stoneCode: '206192723', appName: 'Loja');

    test('sem terminal ativado, startTransaction ativa o pinpad', () async {
      messenger.setMockMethodCallHandler(channel, (call) async {
        calls.add(call);
        return true;
      });
      final controller = StoneStreamController(handler: StoneStreamHandler());
      await controller.startTransaction(params: StoneTechPaymentParams(amount: 10.5, type: StoneTechPaymentType.debit, credentials: credentials));
      expect(calls.single.method, 'paymentActivePinpad');
      expect(calls.single.arguments, {'appName': 'Loja', 'stoneCode': '206192723'});
      controller.dispose();
    });

    test('com terminal ativado, converte reais em centavos e chama o pagamento', () async {
      messenger.setMockMethodCallHandler(channel, (call) async {
        calls.add(call);
        return true;
      });
      StoneTech.isPinpadActivated = true;
      final controller = StoneStreamController(handler: StoneStreamHandler());
      await controller.startTransaction(params: StoneTechPaymentParams(amount: 10.29, type: StoneTechPaymentType.credit, installments: 3, credentials: credentials));
      await Future<void>.delayed(Duration.zero);
      expect(calls.single.method, 'paymentCreditParc');
      expect((calls.single.arguments as Map)['amount'], '1029');
      expect((calls.single.arguments as Map)['installment'], 3);
      controller.dispose();
    });

    test('erro do canal no pagamento não vira exceção não tratada', () async {
      messenger.setMockMethodCallHandler(channel, (call) async {
        calls.add(call);
        throw PlatformException(code: 'ERROR', message: 'Usuário não encontrado');
      });
      StoneTech.isPinpadActivated = true;
      final handler = StoneStreamHandler();
      final statuses = <StoneStatusTech>[];
      final sub = handler.statusStream.listen(statuses.add);
      final controller = StoneStreamController(handler: handler);
      await controller.startTransaction(params: StoneTechPaymentParams(amount: 1, type: StoneTechPaymentType.debit, credentials: credentials));
      await Future<void>.delayed(const Duration(milliseconds: 10));
      expect(calls.single.method, 'paymentDebit');
      expect(statuses.last, isA<StoneStatusTechMessage>().having((s) => s.message, 'message', 'Usuário não encontrado'));
      await sub.cancel();
      controller.dispose();
    });

    test('LICENCE_ERROR no pagamento é absorvido (o status vem pelo evento de licença)', () async {
      messenger.setMockMethodCallHandler(channel, (call) async {
        throw PlatformException(code: 'LICENCE_ERROR', message: 'Licença vencida');
      });
      StoneTech.isPinpadActivated = true;
      final controller = StoneStreamController(handler: StoneStreamHandler());
      await controller.startTransaction(params: StoneTechPaymentParams(amount: 1, type: StoneTechPaymentType.pix, credentials: credentials));
      await Future<void>.delayed(const Duration(milliseconds: 10));
      controller.abortTransaction();
      await Future<void>.delayed(const Duration(milliseconds: 10));
      controller.dispose();
    });

    test('abort escolhe o método conforme o fluxo', () async {
      messenger.setMockMethodCallHandler(channel, (call) async {
        calls.add(call);
        return true;
      });
      final controller = StoneStreamController(handler: StoneStreamHandler());
      controller.abortTransaction();
      controller.currentMethod = StoneTechPaymentType.pix;
      controller.abortTransaction();
      controller.cancelTransaction(idFromBase: 9);
      controller.onPrinterCustomer(false);
      await controller.setPaymentOption('Refeição');
      await Future<void>.delayed(Duration.zero);
      expect(calls.map((c) => c.method), ['paymentAbort', 'paymentAbortPix', 'paymentCancelTransaction', 'paymentPrinterTransaction', 'paymentOptions']);
      expect(calls[3].arguments, {'printCustomerSlip': false});
      controller.dispose();
    });
  });
}
