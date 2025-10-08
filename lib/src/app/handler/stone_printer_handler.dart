import '../../interface/interface.dart';

class StonePrinterHandler extends IStoneHandler {
  final Function(String) onFinishedMonitor;
  final Function(String) onMessageMonitor;

  StonePrinterHandler({
    required this.onFinishedMonitor,
    required this.onMessageMonitor,
  });
  @override
  Future<void> onAuthProgress(String message) {
    // TODO: implement onAuthProgress
    throw UnimplementedError();
  }

  @override
  Future<void> onChanged(String message) {
    // TODO: implement onChanged
    throw UnimplementedError();
  }

  @override
  Future<void> onError(String message) {
    onMessageMonitor(message);
    return Future.value();
  }

  @override
  Future<void> onFinishedResponse(String message) {
    onFinishedMonitor(message);
    return Future.value();
  }

  @override
  Future<void> onLoading(bool show) {
    // TODO: implement onLoading
    throw UnimplementedError();
  }

  @override
  Future<void> onMessage(String message) {
    onMessageMonitor(message);
    return Future.value();
  }

  @override
  Future<void> onTransactionSuccess() {
    // TODO: implement onTransactionSuccess
    throw UnimplementedError();
  }
}
