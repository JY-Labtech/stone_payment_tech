import 'dart:convert';

import 'package:flutter/services.dart';

import '../../interface/interface.dart';

abstract class IStoneHelper {
  static StoneTechResponse convertToStoneTechResponse(String message) {
    if (message.contains('{')) {
      final map = json.decode(message);
      return StoneTechResponse.fromMap(map);
    }
    return StoneTechResponse(message: message);
  }

  static StoneTransactionModel convertToStoneTransactionModel(String message) {
    if (message.contains('{')) {
      final map = json.decode(message);
      return StoneTransactionModel.fromMap(map);
    }
    return StoneTransactionModel.toError(message);
  }

  static Future<dynamic> callHandler({required MethodCall call, required IStoneHandler iStoneHandler, IStoneTechHandler? stoneTechHandler, required Function(StoneTransactionModel) onTransaction}) async {
    switch (call.method.handler) {
      case PaymentTypeHandler.onTransactionSuccess:
        {
          iStoneHandler.onTransactionSuccess();
          _stSmartHandler(message: call.arguments, iStoneTechHandler: stoneTechHandler);
        }
        break;
      case PaymentTypeHandler.onError:
        {
          iStoneHandler.onError(call.arguments);
          _stSmartHandler(message: call.arguments, iStoneTechHandler: stoneTechHandler);
        }
        break;
      case PaymentTypeHandler.onMessage:
        {
          iStoneHandler.onMessage(call.arguments);
          _stSmartHandler(message: call.arguments, iStoneTechHandler: stoneTechHandler);
        }
        break;
      case PaymentTypeHandler.onFinishedResponse:
        {
          iStoneHandler.onFinishedResponse(call.arguments);
          final model = _getTransactionModel(call.arguments);
          if (model != null) {
            onTransaction(model);
          }
          _stSmartHandler(message: call.arguments, iStoneTechHandler: stoneTechHandler);
        }
        break;
      case PaymentTypeHandler.onChanged:
        {
          iStoneHandler.onChanged(call.arguments);
          _stSmartHandler(message: call.arguments, iStoneTechHandler: stoneTechHandler);
        }
        break;
      case PaymentTypeHandler.onLoading:
        {
          iStoneHandler.onLoading(call.arguments);
          _stSmartHandler(message: call.arguments, iStoneTechHandler: stoneTechHandler);
        }
        break;
      case PaymentTypeHandler.onAuthProgress:
        {
          iStoneHandler.onAuthProgress(call.arguments);
          _stSmartHandler(message: call.arguments, iStoneTechHandler: stoneTechHandler);
        }
        break;
      default:
        throw "METHOD NOT IMPLEMENTED";
    }
    return true;
  }

  static void _stSmartHandler({required dynamic message, IStoneTechHandler? iStoneTechHandler}) {
    if (iStoneTechHandler == null) return;
    switch (message.handler) {
      case PaymentTypeHandler.onTransactionSuccess:
        {
          iStoneTechHandler.onTransactionSuccess();
        }
        break;
      case PaymentTypeHandler.onError:
        {
          iStoneTechHandler.onError(IStoneHelper.convertToStoneTechResponse(message));
        }
        break;
      case PaymentTypeHandler.onMessage:
        {
          iStoneTechHandler.onMessage(message);
        }
        break;
      case PaymentTypeHandler.onFinishedResponse:
        {
          iStoneTechHandler.onFinishedResponse(IStoneHelper.convertToStoneTransactionModel(message));
        }
        break;
      case PaymentTypeHandler.onChanged:
        {
          iStoneTechHandler.onChanged(IStoneHelper.convertToStoneTechResponse(message));
        }
        break;
      case PaymentTypeHandler.onLoading:
        {
          iStoneTechHandler.onLoading(message);
        }
        break;
      case PaymentTypeHandler.writeToFile:
        {
          iStoneTechHandler.writeToFile(transactionCode: message['transactionCode'], transactionId: message['transactionId'], response: message['response']);
        }
        break;
      case PaymentTypeHandler.onAbortedSuccessfully:
        {
          iStoneTechHandler.onAbortedSuccessfully();
        }
        break;
      case PaymentTypeHandler.onAuthProgress:
        {
          iStoneTechHandler.onAuthProgress(IStoneHelper.convertToStoneTechResponse(message));
        }
        break;
      case PaymentTypeHandler.onTransactionInfo:
        {
          iStoneTechHandler.onTransactionInfo(message);
        }
        break;
      default:
        throw "METHOD NOT IMPLEMENTED";
    }
  }

  static StoneTransactionModel? _getTransactionModel(dynamic value) {
    try {
      final map = jsonDecode(value) as Map<String, dynamic>;
      final transactionModel = StoneTransactionModel.fromMap(map);
      return transactionModel;
    } catch (e) {
      return null;
    }
  }
}
