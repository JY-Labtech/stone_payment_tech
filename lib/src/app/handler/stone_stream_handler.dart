import 'dart:async';
import 'dart:convert';
import 'dart:developer';

import 'package:stone_payment_tech/src/app/model/stone_status.dart';
import 'package:stone_payment_tech/src/interface/handler/istone_handler.dart';
import 'package:stone_payment_tech/src/interface/model/stone_transaction_model.dart';

class StoneStreamHandler extends IStoneHandler {
  final _controller = StreamController<StoneStatusTech>.broadcast();
  StoneStatusTech? lastStatus;

  StoneStreamHandler();

  Stream<StoneStatusTech> get statusStream => _controller.stream;

  void clear() {
    lastStatus = null;
  }

  void streamDispose() {
    _controller.close();
  }

  void setStatus(String message) {
    if (lastStatus is StoneStatusTechTransaction) {
      return;
    }
    lastStatus = StoneStatusTechMessage(message: message);
    _emit(lastStatus!);
  }

  void setStreamStatus(StoneStatusTech status) {
    if (lastStatus is StoneStatusTechTransaction) {
      return;
    }
    lastStatus = status;
    _emit(lastStatus!);
  }

  void _emit(StoneStatusTech status) {
    if (!_controller.isClosed) {
      _controller.add(status);
    }
  }

  @override
  Future<void> onError(String message) async {
    log('onError_STONE: $message');
    setStoneTransaction(message);
  }

  @override
  Future<void> onMessage(String message) async {
    log('onMessage_STONE: $message');
    setStatus(message);
  }

  @override
  Future<void> onFinishedResponse(String message) async {
    log('onFinishedResponse_STONE: $message');
    setStoneTransaction(message);
  }

  @override
  Future<void> onChanged(String message) async {}

  @override
  Future<void> onAuthProgress(String message) async {
    log('onAuthProgress_STONE: $message');
    setStoneTransaction(message);
  }

  @override
  Future<void> onTransactionSuccess() async {
    log('onTransactionSuccess_STONE');
  }

  @override
  Future<void> onLoading(bool show) async {}

  void setStoneTransaction(String message) {
    try {
      if (message.contains('{')) {
        final map = json.decode(message);

        switch (map['method']) {
          case 'active':
            final message = (map['message'] ?? '').toString().toLowerCase();
            if (message.contains('terminal ativado')) {
              lastStatus = const StoneStatusTechActivate();
            } else {
              // Nos erros o nativo envia errorMessage (message vem nula).
              final error = (map['errorMessage'] ?? map['message'] ?? '').toString();
              lastStatus = StoneStatusTechActivateError(message: error.isEmpty ? 'Erro ao ativar o terminal' : error);
            }
            _emit(lastStatus!);
            return;
          case 'cancel':
            lastStatus = StoneStatusTechCanceled(cancelExtraData: map);
            _emit(lastStatus!);
            return;
          case 'transaction':
            final transaction = StoneTransactionModel.fromMap(map);
            lastStatus = StoneStatusTechTransaction(transaction: transaction);
            _emit(lastStatus!);
            return;
          case 'QRCode':
            final transaction = StoneTransactionModel.fromMap(map);
            lastStatus = StoneStatusTechQRCodeTransaction(transaction: transaction);
            _emit(lastStatus!);
            return;
          case 'printerTransaction':
            lastStatus = const StonePrinterCustomer();
            _emit(lastStatus!);
            return;
          case 'PaymentOptions':
            final optionsList = map['options'] as List<dynamic>?;
            final options = optionsList?.map((e) => e.toString()).toList() ?? <String>[];
            final message = (map['message'] ?? '').toString();
            lastStatus = StoneStatusTechPaymentOptions(options: options, message: message);
            _emit(lastStatus!);
            return;
        }
      }
    } catch (e) {
      log('***Erro setStoneTransaction: $e');
    }
  }

  void dispose() {
    _controller.close();
  }
}
