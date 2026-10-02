import '../../interface/interface.dart';

class StoneTechPrinterHandler extends IStoneHandler {
  final Function(String) onFinishedMonitor;
  final Function(String) onMessageMonitor;

  StoneTechPrinterHandler({
    required this.onFinishedMonitor,
    required this.onMessageMonitor,
  });
  @override
  Future<void> onAuthProgress(String message) async {
    // Sem ação na impressão (antes lançava UnimplementedError e quebrava o handler do canal).
  }

  @override
  Future<void> onChanged(String message) async {
    // Sem ação na impressão (antes lançava UnimplementedError e quebrava o handler do canal).
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
  Future<void> onLoading(bool show) async {
    // Sem ação na impressão (antes lançava UnimplementedError e quebrava o handler do canal).
  }

  @override
  Future<void> onMessage(String message) {
    onMessageMonitor(message);
    return Future.value();
  }

  @override
  Future<void> onTransactionSuccess() async {
    // Sem ação na impressão (antes lançava UnimplementedError e quebrava o handler do canal).
  }
}
