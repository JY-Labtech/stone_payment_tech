import '../model/stone_response.dart';
import '../model/stone_transaction_model.dart';

abstract class IStoneTechHandler {
  void onTransactionSuccess();

  void onError(StoneResponse response);

  void onMessage(String message);

  void onChanged(StoneResponse response);

  void onFinishedResponse(StoneTransactionModel response);

  void onLoading(bool show);

  void writeToFile({String? transactionCode, String? transactionId, String? response});

  void onAbortedSuccessfully();

  void onAuthProgress(StoneResponse response);

  void onTransactionInfo(String response);
}
