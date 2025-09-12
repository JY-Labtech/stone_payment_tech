import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:stone_payment_tech/app/handler/stone_stream_handler.dart';
import 'package:stone_payment_tech/app/helpers/stone_snack_helper.dart';
import 'package:stone_payment_tech/app/model/stone_credentials.dart';
import 'package:stone_payment_tech/app/model/stone_payment_params.dart';
import 'package:stone_payment_tech/app/model/stone_status.dart';
import 'package:stone_payment_tech/app/style/stone_stream_style.dart';
import 'package:stone_payment_tech/interface/model/stone_transaction_model.dart';

import '../controller/stone_stream_controller.dart';

class StoneStreamDialog extends StatefulWidget {
  final StonePaymentParams stonePaymentParams;
  final void Function(StoneTransactionModel) onConfirmPayment;
  final StoneCredentialsModel credentials;
  final StoneStreamStyle? style;

  const StoneStreamDialog({
    super.key,
    required this.stonePaymentParams,
    required this.onConfirmPayment,
    required this.credentials,
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
  final stoneController = StoneStreamController(handler: StoneStreamHandler());
  late StoneStreamStyle style;

  String? popMessage;

  void setPopMessage(String? message) {
    popMessage = message;
  }

  @override
  void initState() {
    style = widget.style ?? StoneStreamStyle();
    if (widget.stonePaymentParams.type == StonePaymentType.pix) {
      isBlockPop = true;
    }
    super.initState();

    WidgetsBinding.instance.addPostFrameCallback((_) {
      stoneController.startTransaction(
        params: widget.stonePaymentParams,
        credentials: widget.credentials,
      );
    });
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
          StoneSnackHelper.showSnackException(
            'Pagamento via PIX não pode ser cancelado, aguarde.',
            Colors.red,
            context: context,
          );
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
            Icon(
              style.icon ?? Icons.credit_card,
              color: style.iconColor,
              size: 45,
            ),
            const SizedBox(height: 10),
            StreamBuilder<StoneStatus>(
              stream: stoneController.stream,
              builder: (context, snapshot) {
                final data = snapshot.data;
                if (data case StoneQRCodeTransaction()) {
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
                          Image.memory(
                            snapshot.data!,
                            fit: BoxFit.contain,
                            height: 200,
                            width: 200,
                          ),
                          const SizedBox(height: 5),
                          Text(
                            'Leia o QRCode acima para concluir o pagamento',
                            style: TextStyle(
                              color: style.textColor,
                            ),
                          ),
                          Visibility(
                            visible: popMessage != null,
                            child: Padding(
                              padding: const EdgeInsets.only(top: 5),
                              child: Text(
                                popMessage ?? '',
                                style: TextStyle(
                                  color: style.textAlertColor,
                                  fontWeight: FontWeight.w700,
                                ),
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
            StreamBuilder<StoneStatus>(
              stream: stoneController.stream,
              builder: (context, snapshot) {
                final data = snapshot.data;

                if (data case StoneStatusActivate()) {
                  stoneController.startTransaction(params: widget.stonePaymentParams, credentials: widget.credentials);
                }

                if (data case StoneStatusActivateRunning()) {
                  return _text(data.message);
                }

                if (data case StoneStatusActivateError()) {
                  _error();
                  return _text(data.message);
                }

                if (data case StoneStatusMessage()) {
                  return _text(data.message);
                }
                if (data case StoneStatusTransaction()) {
                  if (data.transaction.errorMessage?.contains('QRCODE_EXPIRED') == true) {
                    setBlockPop(false);
                  }
                  if (data.transaction.isAproved && !stoneController.isFinishPayment) {
                    stoneController.isFinishPayment = true;
                    // widget.onConfirmPayment();

                    return Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        _text('Transação aprovada'),
                        const SizedBox(height: 8),
                        _text('Deseja imprimir via do cliente?'),
                        const SizedBox(height: 8),
                        ElevatedButton(
                          style: ElevatedButton.styleFrom(
                            backgroundColor: style.buttonColor,
                          ),
                          onPressed: () async {
                            stoneController.onPrinterCustomer(true);
                            widget.onConfirmPayment(data.transaction);
                          },
                          child: Text('Sim', style: TextStyle(color: style.buttonTextColor)),
                        ),
                        const SizedBox(height: 8),
                        TextButton(
                          onPressed: () async {
                            stoneController.onPrinterCustomer(false);
                            widget.onConfirmPayment(data.transaction);
                          },
                          child: Text(
                            'Não',
                            style: TextStyle(color: style.buttonColor),
                          ),
                        ),
                      ],
                    );
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

                if (data case StoneStatusCanceled()) {
                  return _text("Transação cancelada");
                }
                return const SizedBox.shrink();
              },
            ),
            const SizedBox(height: 16),
            StreamBuilder<StoneStatus>(
              stream: stoneController.stream,
              builder: (context, snapshot) {
                final data = snapshot.data;
                if (data case StoneStatusMessage()) {
                  return ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: style.buttonColor,
                    ),
                    child: Text("Abortar transação", style: TextStyle(color: style.buttonTextColor)),
                    onPressed: () async {
                      stoneController.abortTransaction();
                      await Future.delayed(const Duration(seconds: 1));
                      if (mounted) {
                        Navigator.pop(context);
                      }
                    },
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
    if (mounted) Navigator.pop(context);
  }

  Widget _text(String text) {
    return Text(
      text,
      textAlign: TextAlign.center,
      style: TextStyle(
        color: style.textColor,
        fontWeight: FontWeight.w500,
      ),
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
}

class CheckPaymentDialogItemWidget extends StatelessWidget {
  final bool? value;
  final Function(bool?)? onChanged;
  final String title;

  const CheckPaymentDialogItemWidget({
    super.key,
    this.value,
    this.onChanged,
    required this.title,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Checkbox(
          value: value,
          onChanged: onChanged,
        ),
        Text(title)
      ],
    );
  }
}
