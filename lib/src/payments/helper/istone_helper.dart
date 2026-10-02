import 'dart:convert';

import 'package:flutter/services.dart';

import '../../interface/interface.dart';

abstract class IStoneHelper {
  static StoneTechResponse convertToStoneTechResponse(String message) {
    final map = _decodeMap(message);
    if (map != null) {
      return StoneTechResponse.fromMap(map);
    }
    return StoneTechResponse(message: message);
  }

  static StoneTransactionModel convertToStoneTransactionModel(String message) {
    final map = _decodeMap(message);
    if (map != null) {
      return StoneTransactionModel.fromMap(map);
    }
    return StoneTransactionModel.toError(message);
  }

  /// JSON do nativo ou `null` se a mensagem for texto simples/JSON inválido.
  static Map<String, dynamic>? _decodeMap(String message) {
    if (!message.contains('{')) return null;
    try {
      final decoded = json.decode(message);
      return decoded is Map<String, dynamic> ? decoded : null;
    } catch (_) {
      return null;
    }
  }

  static String _asString(dynamic value) => value?.toString() ?? '';

  static Future<dynamic> callHandler({required MethodCall call, required IStoneHandler iStoneHandler, IStoneTechHandler? stoneTechHandler, required Function(StoneTransactionModel) onTransaction}) async {
    final PaymentTypeHandler type;
    try {
      type = call.method.handler;
    } catch (_) {
      throw MissingPluginException('Método não implementado: ${call.method}');
    }
    final arguments = call.arguments;
    switch (type) {
      case PaymentTypeHandler.onTransactionSuccess:
        await iStoneHandler.onTransactionSuccess();
        break;
      case PaymentTypeHandler.onError:
        await iStoneHandler.onError(_asString(arguments));
        break;
      case PaymentTypeHandler.onMessage:
        await iStoneHandler.onMessage(_asString(arguments));
        break;
      case PaymentTypeHandler.onFinishedResponse:
        await iStoneHandler.onFinishedResponse(_asString(arguments));
        final model = _getTransactionModel(arguments);
        if (model != null) {
          onTransaction(model);
        }
        break;
      case PaymentTypeHandler.onChanged:
        await iStoneHandler.onChanged(_asString(arguments));
        break;
      case PaymentTypeHandler.onLoading:
        await iStoneHandler.onLoading(arguments == true);
        break;
      case PaymentTypeHandler.onAuthProgress:
        await iStoneHandler.onAuthProgress(_asString(arguments));
        break;
      default:
        // writeToFile, onAbortedSuccessfully, onTransactionInfo: só o IStoneTechHandler trata.
        if (stoneTechHandler == null) {
          throw MissingPluginException('Método não implementado: ${call.method}');
        }
    }
    _stSmartHandler(type: type, message: arguments, iStoneTechHandler: stoneTechHandler);
    return true;
  }

  /// Repassa o evento ao [IStoneTechHandler] já convertido em modelo.
  ///
  /// Antes o tipo do evento era lido do conteúdo da mensagem (e não do nome do
  /// método do canal), então o IStoneTechHandler nunca recebia os eventos.
  static void _stSmartHandler({required PaymentTypeHandler type, required dynamic message, IStoneTechHandler? iStoneTechHandler}) {
    if (iStoneTechHandler == null) return;
    switch (type) {
      case PaymentTypeHandler.onTransactionSuccess:
        iStoneTechHandler.onTransactionSuccess();
        break;
      case PaymentTypeHandler.onError:
        iStoneTechHandler.onError(IStoneHelper.convertToStoneTechResponse(_asString(message)));
        break;
      case PaymentTypeHandler.onMessage:
        iStoneTechHandler.onMessage(_asString(message));
        break;
      case PaymentTypeHandler.onFinishedResponse:
        iStoneTechHandler.onFinishedResponse(IStoneHelper.convertToStoneTransactionModel(_asString(message)));
        break;
      case PaymentTypeHandler.onChanged:
        iStoneTechHandler.onChanged(IStoneHelper.convertToStoneTechResponse(_asString(message)));
        break;
      case PaymentTypeHandler.onLoading:
        iStoneTechHandler.onLoading(message == true);
        break;
      case PaymentTypeHandler.writeToFile:
        final map = message is Map ? message : const {};
        iStoneTechHandler.writeToFile(
          transactionCode: map['transactionCode']?.toString(),
          transactionId: map['transactionId']?.toString(),
          response: map['response']?.toString(),
        );
        break;
      case PaymentTypeHandler.onAbortedSuccessfully:
        iStoneTechHandler.onAbortedSuccessfully();
        break;
      case PaymentTypeHandler.onAuthProgress:
        iStoneTechHandler.onAuthProgress(IStoneHelper.convertToStoneTechResponse(_asString(message)));
        break;
      case PaymentTypeHandler.onTransactionInfo:
        iStoneTechHandler.onTransactionInfo(_asString(message));
        break;
      case PaymentTypeHandler.disposeDialog:
      case PaymentTypeHandler.activeDialog:
        break;
    }
  }

  static StoneTransactionModel? _getTransactionModel(dynamic value) {
    if (value is! String) return null;
    try {
      final map = _decodeMap(value);
      return map == null ? null : StoneTransactionModel.fromMap(map);
    } catch (e) {
      return null;
    }
  }
}
