import 'package:flutter/material.dart';
import 'package:stone_payment_tech/app/handler/stone_stream_handler.dart';
import 'package:stone_payment_tech/app/model/stone_credentials.dart';
import 'package:stone_payment_tech/printer/controller/stone_printer_controller.dart';
import 'package:stone_payment_tech/printer/widgets/printer_custom_body.dart';
import 'package:stone_payment_tech/printer/widgets/printer_loading_widget.dart';

class StonePrinterPageParams {
  final StoneCredentialsModel credentials;
  final Widget child;

  StonePrinterPageParams({
    required this.credentials,
    required this.child,
  });
}

class StonePrinterPage extends StatefulWidget {
  const StonePrinterPage({
    super.key,
    required this.params,
  });
  final StonePrinterPageParams params;

  @override
  State<StonePrinterPage> createState() => _StonePrinterPageState();
}

class _StonePrinterPageState extends State<StonePrinterPage> with SingleTickerProviderStateMixin, AutomaticKeepAliveClientMixin {
  final GlobalKey globalKeyPrinter = GlobalKey();
  late AnimationController _animationController;
  late StonePrinterController controller;

  String message = '';

  @override
  void initState() {
    controller = StonePrinterController(handler: StoneStreamHandler());
    _animationController = AnimationController(
      duration: const Duration(seconds: 2),
      vsync: this,
    )..repeat();

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      controller.printerWidget(
        globalKeyPrinter,
        stoneCredentials: widget.params.credentials,
        onMessage: setMessage,
        onPop: (e) {
          if (mounted) {
            Navigator.of(context).pop(e);
          }
        },
      );
    });
    super.initState();
  }

  void setMessage(String value) {
    if (mounted) {
      setState(() {
        message = value;
      });
    }
  }

  @override
  void dispose() {
    _animationController.dispose();
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final size = MediaQuery.sizeOf(context);
    return Material(
      child: Container(
        height: size.height,
        width: size.width,
        color: Colors.white,
        child: Stack(
          children: [
            SingleChildScrollView(
              child: Column(
                children: [PrinterCustomBody(globalKey: globalKeyPrinter, child: widget.params.child)],
              ),
            ),
            PrinterLoadingWidget(
              animationController: _animationController,
              message: message,
            )
          ],
        ),
      ),
    );
  }

  @override
  bool get wantKeepAlive => true;
}
