import '../model/stone_response.dart';
import '../model/stone_transaction_model.dart';

abstract class IStoneTechHandler {
  void onTransactionSuccess();

  void onError(StoneTechResponse response);

  void onMessage(String message);

  void onChanged(StoneTechResponse response);

  void onFinishedResponse(StoneTransactionModel response);

  void onLoading(bool show);

  void writeToFile({String? transactionCode, String? transactionId, String? response});

  void onAbortedSuccessfully();

  void onAuthProgress(StoneTechResponse response);

  void onTransactionInfo(String response);
}
