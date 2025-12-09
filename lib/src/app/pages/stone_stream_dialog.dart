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

class StoneStreamDialog extends StatefulWidget {
  final StoneTechPaymentParams stonePaymentParams;
  final void Function(StoneTransactionModel) onConfirmPayment;
  final StoneStreamStyle? style;

  const StoneStreamDialog({
    super.key,
    required this.stonePaymentParams,
    required this.onConfirmPayment,
    this.style,
  });
  @override
  State<StoneStreamDialog> createState() => _StoneStreamDialogState();
}

class _StoneStreamDialogState extends State<StoneStreamDialog> {
  bool reciboImpresso = false;
  bool reciboEmail = false;
  bool reciboWhatsApp = false;
  bool isLoading = false;
  bool isBlockPop = false;
  bool _hasStartedTransaction = false;
  bool _isActivating = false;
  final stoneController = StoneStreamController(handler: StoneStreamHandler());
  late StoneStreamStyle style;
  late Timer _timerTransaction;
  Timer? _timerPop;
  StreamSubscription<StoneStatusTech>? _paymentOptionsSubscription;
  bool _isShowingPaymentOptionsDialog = false;

  String? popMessage;

  void setPopMessage(String? message) {
    popMessage = message;
  }

  @override
  void initState() {
    style = widget.style ?? StoneStreamStyle();
    _timerTransaction = Timer(const Duration(minutes: 10), () {
      _pop();
    });
    if (widget.stonePaymentParams.type == StoneTechPaymentType.pix) {
      isBlockPop = true;
    }
    super.initState();

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
    return PopScope(
      canPop: isBlockPop ? false : true,
      onPopInvokedWithResult: (value, __) {
        if (isBlockPop) {
          setPopMessage('Pagamento via PIX não pode ser cancelado, aguarde.');
          StoneSnackHelper.showSnackException('Pagamento via PIX não pode ser cancelado, aguarde.', Colors.red, context: context);
          return;
        }
        return;
      },
      child: AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        backgroundColor: style.backgroundColor,
        title: _text('Pagamento via máquina'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            const SizedBox(height: 5),
            Icon(style.icon ?? Icons.credit_card, color: style.iconColor, size: 45),
            const SizedBox(height: 10),
            StreamBuilder<StoneStatusTech>(
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
                          const SizedBox(height: 5),
                          Text('Leia o QRCode acima para concluir o pagamento', style: TextStyle(color: style.textColor)),
                          Visibility(
                            visible: popMessage != null,
                            child: Padding(
                              padding: const EdgeInsets.only(top: 5),
                              child: Text(
                                popMessage ?? '',
                                style: TextStyle(color: style.textAlertColor, fontWeight: FontWeight.w700),
                                textAlign: TextAlign.center,
                              ),
                            ),
                          ),
                        ],
                      );
                    },
                  );
                }
                return const SizedBox.shrink();
              },
            ),
            StreamBuilder<StoneStatusTech>(
              stream: stoneController.stream,
              builder: (context, snapshot) {
                final data = snapshot.data;

                if (data case StoneStatusTechActivate()) {
                  _isActivating = false;
                  // Evitar chamar startTransaction múltiplas vezes
                  if (!_hasStartedTransaction && mounted) {
                    _hasStartedTransaction = true;
                    // Usar WidgetsBinding para garantir que não estamos no build
                    WidgetsBinding.instance.addPostFrameCallback((_) {
                      if (mounted) {
                        stoneController.startTransaction(params: widget.stonePaymentParams);
                      }
                    });
                  }
                }

                if (data case StoneStatusTechActivateRunning()) {
                  _isActivating = true;
                  return _text(data.message);
                }

                if (data case StoneStatusTechActivateError()) {
                  _isActivating = false;
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
                    _timerPop = Timer(const Duration(minutes: 2), () {
                      _pop();
                    });

                    return Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        _text('Transação aprovada'),
                        const SizedBox(height: 8),
                        _text('Deseja imprimir via do cliente?'),
                        const SizedBox(height: 15),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                          children: [
                            Padding(
                              padding: const EdgeInsets.symmetric(horizontal: 8),
                              child: TextButton(
                                onPressed: () async {
                                  widget.onConfirmPayment(data.transaction);
                                  _pop();
                                },
                                child: Text('Não', style: TextStyle(color: style.buttonColor)),
                              ),
                            ),
                            ElevatedButton(
                              style: ElevatedButton.styleFrom(backgroundColor: style.buttonColor),
                              onPressed: () async {
                                stoneController.onPrinterCustomer(true);
                                widget.onConfirmPayment(data.transaction);
                                _pop();
                              },
                              child: Padding(
                                padding: const EdgeInsets.symmetric(horizontal: 8),
                                child: Text('Sim', style: TextStyle(color: style.buttonTextColor)),
                              ),
                            ),
                          ],
                        ),
                      ],
                    );
                  }

                  if (!data.transaction.isApproved && (data.transaction.transactionStatus != '' && data.transaction.transactionStatus != null)) {
                    _popWithResult(data.transaction);
                  }

                  if (data.transaction.messageFromAuthorize != null) {
                    return _text(data.transaction.messageFromAuthorize!);
                  } else if (data.transaction.message != null) {
                    return _text(data.transaction.message!);
                  } else if (data.transaction.errorMessage != null) {
                    return _text(data.transaction.errorMessage!.split(":").last);
                  }

                  return _text("Error ao processar transação");
                }

                if (data case StoneStatusTechCanceled()) {
                  return _text("Transação cancelada");
                }
                return const SizedBox.shrink();
              },
            ),
            const SizedBox(height: 16),
            StreamBuilder<StoneStatusTech>(
              stream: stoneController.stream,
              builder: (context, snapshot) {
                final data = snapshot.data;
                // Mostrar botão durante ativação ou quando há mensagem
                final isActivatingStatus = data is StoneStatusTechActivateRunning;
                final isMessageStatus = data is StoneStatusTechMessage;
                final isErrorStatus = data is StoneStatusTechActivateError;
                final shouldShowAbortButton = _isActivating || isActivatingStatus || isMessageStatus || isErrorStatus;

                if (shouldShowAbortButton) {
                  final isDuringActivation = _isActivating || isActivatingStatus;
                  return ElevatedButton(
                    style: ElevatedButton.styleFrom(backgroundColor: style.buttonColor),
                    onPressed: isLoading
                        ? null
                        : () async {
                            // Prevenir múltiplos cliques
                            if (isLoading) return;
                            setState(() => isLoading = true);

                            try {
                              stoneController.abortTransaction();
                              // Aguardar um pouco para o abort ser processado
                              await Future.delayed(const Duration(seconds: 1));
                              if (mounted) {
                                _pop();
                              }
                            } finally {
                              if (mounted) {
                                setState(() => isLoading = false);
                              }
                            }
                          },
                    child: Text(
                      isDuringActivation ? "Cancelar ativação" : "Abortar transação",
                      style: TextStyle(color: style.buttonTextColor),
                    ),
                  );
                }
                return const SizedBox.shrink();
              },
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }

  Future<void> _error() async {
    await Future.delayed(const Duration(seconds: 2));
    _pop();
  }

  Future<void> _popWithResult(StoneTransactionModel? model) async {
    await Future.delayed(const Duration(seconds: 2));
    if (model != null) {
      widget.onConfirmPayment(model);
    }
    _pop(model);
  }

  void _pop([StoneTransactionModel? model]) {
    if (mounted) {
      _cancellAllTimers();
      _disposeHandler();

      Navigator.pop(context, model);
    }
  }

  void _disposeHandler() {
    stoneController.handler?.clear();
    stoneController.handler?.streamDispose();
  }

  Widget _text(String text) {
    return Text(
      text,
      textAlign: TextAlign.center,
      style: TextStyle(color: style.textColor, fontWeight: FontWeight.w500),
    );
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

  void _cancellAllTimers() {
    _timerTransaction.cancel();
    _timerPop?.cancel();
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

class CheckPaymentDialogItemWidget extends StatelessWidget {
  final bool? value;
  final Function(bool?)? onChanged;
  final String title;

  const CheckPaymentDialogItemWidget({super.key, this.value, this.onChanged, required this.title});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Checkbox(value: value, onChanged: onChanged),
        Text(title),
      ],
    );
  }
}
