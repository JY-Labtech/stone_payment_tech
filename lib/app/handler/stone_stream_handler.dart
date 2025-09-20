import 'dart:async';
import 'dart:convert';
import 'dart:developer';

import 'package:stone_payment_tech/app/model/stone_status.dart';
import 'package:stone_payment_tech/interface/handler/istone_handler.dart';
import 'package:stone_payment_tech/interface/model/stone_transaction_model.dart';

class StoneStreamHandler extends IStoneHandler {
  final _controller = StreamController<StoneStatus>.broadcast();
  StoneStatus? lastStatus;

  StoneStreamHandler();

  Stream<StoneStatus> get statusStream => _controller.stream;

  void clear() {
    lastStatus = null;
  }

  void streamDispose() {
    _controller.close();
  }

  void setStatus(String message) {
    if (lastStatus is StoneStatusTransaction) {
      return;
    }
    lastStatus = StoneStatusMessage(message: message);
    _emit(lastStatus!);
  }

  void setStreamStatus(StoneStatus status) {
    if (lastStatus is StoneStatusTransaction) {
      return;
    }
    lastStatus = status;
    _emit(lastStatus!);
  }

  void _emit(StoneStatus status) {
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
          case 'licence':
            final errorMessage = (map['errorMessage'] ?? 'Licença não ativada').toString();
            final resultLicence = int.tryParse((map['result'] ?? '').toString()) ?? 999999;
            if (resultLicence != 0) {
              lastStatus = StoneStatusActivateError(message: errorMessage);
              _emit(lastStatus!);
            }

          case 'active':
            final message = (map['message'] ?? '').toString().toLowerCase();
            if (message.contains('terminal ativado')) {
              lastStatus = const StoneStatusActivate();
            } else {
              lastStatus = StoneStatusActivateError(message: map['message'] ?? 'Erro ao ativar o terminal');
            }
            _emit(lastStatus!);
            return;
          case 'cancel':
            lastStatus = StoneStatusCanceled(cancelExtraData: map);
            _emit(lastStatus!);
            return;
          case 'transaction':
            final transaction = StoneTransactionModel.fromMap(map);
            lastStatus = StoneStatusTransaction(transaction: transaction);
            _emit(lastStatus!);
            return;
          case 'QRCode':
            final transaction = StoneTransactionModel.fromMap(map);
            lastStatus = StoneQRCodeTransaction(transaction: transaction);
            _emit(lastStatus!);
            return;
          case 'printerTransaction':
            lastStatus = const StonePrinterCustomer();
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
