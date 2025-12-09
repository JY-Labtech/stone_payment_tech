import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:stone_payment_tech/src/app/handler/stone_stream_handler.dart';
import 'package:stone_payment_tech/src/app/helpers/stone_snack_helper.dart';
import 'package:stone_payment_tech/src/app/model/stone_payment_params.dart';
import 'package:stone_payment_tech/src/app/model/stone_status.dart';
import 'package:stone_payment_tech/src/app/style/stone_stream_style.dart';
import 'package:stone_payment_tech/src/interface/model/stone_transaction_model.dart';

import '../controller/stone_stream_controller.dart';

class StoneStreamPage extends StatefulWidget {
  final StoneTechPaymentParams stonePaymentParams;
  final StoneStreamStyle? style;

  const StoneStreamPage({super.key, required this.stonePaymentParams, this.style});

  @override
  State<StoneStreamPage> createState() => _StoneStreamPageState();
}

class _StoneStreamPageState extends State<StoneStreamPage> {
  bool isBlockPop = false;
  final stoneController = StoneStreamController(handler: StoneStreamHandler());
  late StoneStreamStyle style;
  String? popMessage;
  late Timer _timerTransaction;
  Timer? _timerPop;
  StreamSubscription<StoneStatusTech>? _paymentOptionsSubscription;
  bool _isShowingPaymentOptionsDialog = false;

  void setPopMessage(String? message) {
    popMessage = message;
  }

  @override
  void initState() {
    super.initState();
    style = widget.style ?? StoneStreamStyle();
    _timerTransaction = Timer(const Duration(minutes: 10), () {
      stoneController.abortTransaction();
      _popWithResult(null);
    });
    if (widget.stonePaymentParams.type == StoneTechPaymentType.pix) {
      isBlockPop = true;
    }

    // Listener para detectar PaymentOptions e mostrar dialog
    _paymentOptionsSubscription = stoneController.stream.listen((status) {
      if (status is StoneStatusTechPaymentOptions && mounted && !_isShowingPaymentOptionsDialog) {
        _showPaymentOptionsDialog(status.options, status.message);
      }
    });

    WidgetsBinding.instance.addPostFrameCallback((_) {
      stoneController.startTransaction(params: widget.stonePaymentParams);
    });
  }

  @override
  void dispose() {
    stoneController.handler?.clear();
    stoneController.handler?.streamDispose();
    _timerTransaction.cancel();
    _timerPop?.cancel();
    _paymentOptionsSubscription?.cancel();
    super.dispose();
  }

  void setBlockPop(bool block) {
    if (mounted) {
      setState(() {
        isBlockPop = block;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.sizeOf(context);
    return PopScope(
      canPop: !isBlockPop,
      onPopInvokedWithResult: (value, __) {
        if (isBlockPop) {
          setPopMessage('Pagamento via PIX não pode ser cancelado, aguarde.');
          StoneSnackHelper.showSnackException('Pagamento via PIX não pode ser cancelado, aguarde.', Colors.red, context: context);
          return;
        }
        return;
      },
      child: Scaffold(
        backgroundColor: style.backgroundColor,
        appBar: AppBar(
          title: const Text("Pagamento via máquina"),
          backgroundColor: style.backgroundColor,
          iconTheme: IconThemeData(color: style.iconColor),
          leading: Container(),
        ),
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              const SizedBox(height: 10),
              Icon(style.icon ?? Icons.credit_card, color: style.iconColor, size: size.height * 0.2),
              const SizedBox(height: 16),
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    children: [
                      /// QRCode
                      Center(
                        child: StreamBuilder<StoneStatusTech>(
                          stream: stoneController.stream,
                          builder: (context, snapshot) {
                            final data = snapshot.data;
                            if (data case StoneStatusTechQRCodeTransaction()) {
                              return FutureBuilder(
                                future: setQrCode(data.transaction.message!),
                                builder: (context, snapshot) {
                                  if (snapshot.connectionState == ConnectionState.waiting) {
                                    return const CircularProgressIndicator();
                                  }
                                  if (snapshot.data == null) {
                                    return _text("Erro ao processar QR Code");
                                  }
                                  return Column(
                                    children: [
                                      Image.memory(snapshot.data!, fit: BoxFit.contain, height: 200, width: 200),
                                      const SizedBox(height: 8),
                                      Text(
                                        'Leia o QRCode acima para concluir o pagamento',
                                        style: TextStyle(color: style.textColor),
                                        textAlign: TextAlign.center,
                                      ),
                                      if (popMessage != null) ...[
                                        const SizedBox(height: 5),
                                        Text(
                                          popMessage ?? '',
                                          style: TextStyle(color: style.textAlertColor, fontWeight: FontWeight.w700),
                                          textAlign: TextAlign.center,
                                        ),
                                      ],
                                    ],
                                  );
                                },
                              );
                            }
                            return const SizedBox.shrink();
                          },
                        ),
                      ),

                      /// Mensagens e status
                      Center(
                        child: StreamBuilder<StoneStatusTech>(
                          stream: stoneController.stream,
                          builder: (context, snapshot) {
                            final data = snapshot.data;

                            if (data case StoneStatusTechActivate()) {
                              stoneController.startTransaction(params: widget.stonePaymentParams);
                            }

                            if (data case StoneStatusTechActivateRunning()) {
                              return _text(data.message);
                            }

                            if (data case StoneStatusTechActivateError()) {
                              _error();
                              return _text(data.message);
                            }

                            if (data case StoneStatusTechMessage()) {
                              return _text(data.message);
                            }

                            if (data case StoneStatusTechTransaction()) {
                              if (data.transaction.errorMessage?.contains('QRCODE_EXPIRED') == true) {
                                setBlockPop(false);
                              }
                              if (data.transaction.isApproved && !stoneController.isFinishPayment) {
                                stoneController.isFinishPayment = true;
                                _timerPop = Timer(const Duration(minutes: 3), () {
                                  _popWithResult(data.transaction);
                                });

                                return Column(
                                  children: [
                                    _text('Transação aprovada'),
                                    const SizedBox(height: 12),
                                    _text('Deseja imprimir via do cliente?', isLoading: false),
                                    const SizedBox(height: 5),
                                    Row(
                                      spacing: 5,
                                      children: [
                                        Expanded(
                                          child: ElevatedButton(
                                            style: ElevatedButton.styleFrom(backgroundColor: style.buttonColor),
                                            onPressed: () async {
                                              _cancellAllTimers();
                                              stoneController.onPrinterCustomer(true);
                                              _popWithResult(data.transaction);
                                            },
                                            child: Padding(
                                              padding: const EdgeInsets.symmetric(horizontal: 8),
                                              child: Text('Sim', style: TextStyle(color: style.buttonTextColor)),
                                            ),
                                          ),
                                        ),
                                        Expanded(
                                          child: ElevatedButton(
                                            style: ElevatedButton.styleFrom(backgroundColor: Colors.grey),
                                            onPressed: () async {
                                              _cancellAllTimers();
                                              _popWithResult(data.transaction);
                                            },
                                            child: Padding(
                                              padding: const EdgeInsets.symmetric(horizontal: 8),
                                              child: Text('Não', style: TextStyle(color: style.buttonTextColor)),
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                  ],
                                );
                              }

                              if (!data.transaction.isApproved && (data.transaction.transactionStatus != '' && data.transaction.transactionStatus != null)) {
                                _error(data.transaction);
                              }

                              if (data.transaction.messageFromAuthorize != null) {
                                return _text(data.transaction.messageFromAuthorize!);
                              } else if (data.transaction.message != null) {
                                return _text(data.transaction.message!);
                              } else if (data.transaction.errorMessage != null) {
                                return _text(data.transaction.errorMessage!.split(":").last);
                              }

                              return _text("Erro ao processar transação", isLoading: false);
                            }

                            if (data case StoneStatusTechCanceled()) {
                              return _text("Transação cancelada", isLoading: false);
                            }
                            return const SizedBox.shrink();
                          },
                        ),
                      ),

                      const SizedBox(height: 16),

                      /// Botão de abortar
                      Center(
                        child: StreamBuilder<StoneStatusTech>(
                          stream: stoneController.stream,
                          builder: (context, snapshot) {
                            final data = snapshot.data;
                            if (data case StoneStatusTechMessage()) {
                              return ElevatedButton(
                                style: ElevatedButton.styleFrom(backgroundColor: style.buttonColor),
                                child: Text("Abortar transação", style: TextStyle(color: style.buttonTextColor)),
                                onPressed: () async {
                                  stoneController.abortTransaction();
                                  await Future.delayed(const Duration(seconds: 1));
                                  _popWithResult(null);
                                },
                              );
                            }
                            return const SizedBox.shrink();
                          },
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _error([StoneTransactionModel? transaction]) async {
    await Future.delayed(const Duration(seconds: 2));
    _popWithResult(transaction);
  }

  void _popWithResult(StoneTransactionModel? transaction) async {
    _cancellAllTimers();
    _disposeHandler();
    if (mounted) {
      Navigator.pop<StoneTransactionModel?>(context, transaction);
    }
  }

  void _disposeHandler() {
    stoneController.handler?.clear();
    stoneController.handler?.streamDispose();
  }

  Widget _text(
    String text, {
    bool isLoading = true,
  }) {
    return isLoading
        ? Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            mainAxisAlignment: MainAxisAlignment.center,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                text,
                textAlign: TextAlign.center,
                style: TextStyle(color: style.textColor, fontWeight: FontWeight.w700, fontSize: 18),
              ),
              const SizedBox(height: 40),
              CircularProgressIndicator(valueColor: AlwaysStoppedAnimation<Color>(style.buttonColor)),
              const SizedBox(height: 20),
              Text(
                'Stone\nAguarde...',
                textAlign: TextAlign.center,
                style: TextStyle(color: style.textColor, fontSize: 14),
              ),
            ],
          )
        : Text(
            text,
            textAlign: TextAlign.center,
            style: TextStyle(color: style.textColor, fontWeight: FontWeight.w700, fontSize: 18),
          );
  }

  _cancellAllTimers() {
    _timerPop?.cancel();
    _timerTransaction.cancel();
  }

  Future<Uint8List?> setQrCode(String bitmapString) async {
    try {
      final edit = bitmapString.replaceAll(RegExp(r'\s+'), '').trim();
      Uint8List bytes = const Base64Decoder().convert(edit.split(',').last);
      return bytes;
    } catch (_) {
      return null;
    }
  }

  Future<void> _showPaymentOptionsDialog(List<String> options, String message) async {
    if (!mounted || _isShowingPaymentOptionsDialog) return;

    _isShowingPaymentOptionsDialog = true;

    final selectedOption = await showDialog<String>(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(
            'Selecione o tipo de cartão',
            style: TextStyle(color: style.textColor),
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (message.isNotEmpty) ...[
                  Text(
                    message,
                    style: TextStyle(color: style.textColor),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 16),
                ],
                ...options.map((option) {
                  return Padding(
                    padding: const EdgeInsets.symmetric(vertical: 4),
                    child: ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: style.buttonColor,
                        minimumSize: const Size(double.infinity, 48),
                      ),
                      onPressed: () {
                        Navigator.of(context).pop(option);
                      },
                      child: Text(
                        option,
                        style: TextStyle(color: style.buttonTextColor),
                      ),
                    ),
                  );
                }),
              ],
            ),
          ),
        );
      },
    );

    _isShowingPaymentOptionsDialog = false;

    if (selectedOption != null && mounted) {
      await stoneController.setPaymentOption(selectedOption);
    }
  }
}
