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

class StoneStreamPage extends StatefulWidget {
  final StonePaymentParams stonePaymentParams;
  final StoneCredentialsModel credentials;
  final StoneStreamStyle? style;

  const StoneStreamPage({
    super.key,
    required this.stonePaymentParams,
    required this.credentials,
    this.style,
  });

  @override
  State<StoneStreamPage> createState() => _StoneStreamPageState();
}

class _StoneStreamPageState extends State<StoneStreamPage> {
  bool isBlockPop = false;
  final stoneController = StoneStreamController(handler: StoneStreamHandler());
  late StoneStreamStyle style;
  String? popMessage;

  void setPopMessage(String? message) {
    popMessage = message;
  }

  @override
  void initState() {
    super.initState();
    style = widget.style ?? StoneStreamStyle();
    if (widget.stonePaymentParams.type == StonePaymentType.pix) {
      isBlockPop = true;
    }

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
      canPop: !isBlockPop,
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
      child: Scaffold(
        backgroundColor: style.backgroundColor,
        appBar: AppBar(
          title: const Text("Pagamento via máquina"),
          backgroundColor: style.backgroundColor,
          iconTheme: IconThemeData(color: style.iconColor),
        ),
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            children: [
              const SizedBox(height: 10),
              Icon(
                style.icon ?? Icons.credit_card,
                color: style.iconColor,
                size: 45,
              ),
              const SizedBox(height: 16),
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    children: [
                      /// QRCode
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
                                        style: TextStyle(
                                          color: style.textAlertColor,
                                          fontWeight: FontWeight.w700,
                                        ),
                                        textAlign: TextAlign.center,
                                      ),
                                    ]
                                  ],
                                );
                              },
                            );
                          }
                          return const SizedBox.shrink();
                        },
                      ),

                      /// Mensagens e status
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

                              return Column(
                                children: [
                                  _text('Transação aprovada'),
                                  const SizedBox(height: 12),
                                  _text('Deseja imprimir via do cliente?'),
                                  const SizedBox(height: 12),
                                  ElevatedButton(
                                    style: ElevatedButton.styleFrom(
                                      backgroundColor: style.buttonColor,
                                    ),
                                    onPressed: () async {
                                      stoneController.onPrinterCustomer(true);
                                      _popWithResult(data.transaction);
                                    },
                                    child: Text('Sim', style: TextStyle(color: style.buttonTextColor)),
                                  ),
                                  const SizedBox(height: 8),
                                  TextButton(
                                    onPressed: () async {
                                      stoneController.onPrinterCustomer(false);
                                      _popWithResult(data.transaction);
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

                            return _text("Erro ao processar transação");
                          }

                          if (data case StoneStatusCanceled()) {
                            return _text("Transação cancelada");
                          }
                          return const SizedBox.shrink();
                        },
                      ),

                      const SizedBox(height: 16),

                      /// Botão de abortar
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
                                if (mounted) Navigator.pop(context);
                              },
                            );
                          }
                          return const SizedBox.shrink();
                        },
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

  Future<void> _error() async {
    await Future.delayed(const Duration(seconds: 2));
    if (mounted) Navigator.pop(context);
  }

  void _popWithResult(StoneTransactionModel transaction) {
    if (mounted) {
      Navigator.pop<StoneTransactionModel?>(context, transaction);
    }
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
