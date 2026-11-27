import 'dart:developer';

import 'package:flutter/material.dart';
import 'package:stone_payment_tech/stone_payment_tech.dart';
import 'package:stone_payment_tech_example/config.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  StoneTechPaymentType? selectedPaymentType;

  void onChanged(StoneTechPaymentType? value) {
    if (!mounted) return;
    setState(() {
      selectedPaymentType = value;
    });
  }

  void printerBase64() {
    StoneCustomPrinterController().initializeAndPrintBase64(
      stoneCredentials: ExampleConfig.credentials,
      onMessage: (message) {
        log('onMessage: $message');
      },
      onError: (error) {
        log('onError: $error');
      },
      base64:
          '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',
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Stone Payment Tech'), centerTitle: true),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(10),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (!ExampleConfig.hasStoneCode) const _MissingStoneCodeBanner(),
            // Card de Seleção de Tipo de Pagamento
            Card(
              child: Padding(
                padding: const EdgeInsets.all(10),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(Icons.payment, color: theme.colorScheme.primary),
                        const SizedBox(width: 8),
                        Text('Tipo de Pagamento', style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                      ],
                    ),
                    const SizedBox(height: 16),
                    DropdownButtonFormField<StoneTechPaymentType>(
                      decoration: const InputDecoration(border: OutlineInputBorder(), contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 12)),
                      hint: const Text('Selecione o tipo de pagamento'),
                      value: selectedPaymentType,
                      items: [
                        DropdownMenuItem(
                          value: StoneTechPaymentType.debit,
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.credit_card, size: 20),
                              const SizedBox(width: 8),
                              Flexible(child: Text(StoneTechPaymentType.debit.name.toUpperCase(), overflow: TextOverflow.ellipsis)),
                            ],
                          ),
                        ),
                        DropdownMenuItem(
                          value: StoneTechPaymentType.credit,
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.credit_card_outlined, size: 20),
                              const SizedBox(width: 8),
                              Flexible(child: Text(StoneTechPaymentType.credit.name.toUpperCase(), overflow: TextOverflow.ellipsis)),
                            ],
                          ),
                        ),
                        DropdownMenuItem(
                          value: StoneTechPaymentType.pix,
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.qr_code, size: 20),
                              const SizedBox(width: 8),
                              Flexible(child: Text(StoneTechPaymentType.pix.name.toUpperCase(), overflow: TextOverflow.ellipsis)),
                            ],
                          ),
                        ),
                        DropdownMenuItem(
                          value: StoneTechPaymentType.voucher,
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.receipt_long, size: 20),
                              const SizedBox(width: 8),
                              Flexible(child: Text(StoneTechPaymentType.voucher.name.toUpperCase(), overflow: TextOverflow.ellipsis)),
                            ],
                          ),
                        ),
                      ],
                      onChanged: onChanged,
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),

            // Card de Ações de Pagamento
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(Icons.shopping_cart, color: theme.colorScheme.primary),
                        const SizedBox(width: 8),
                        Text('Ações de Pagamento', style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                      ],
                    ),
                    const SizedBox(height: 20),
                    _buildActionButton(
                      context: context,
                      icon: Icons.payment,
                      label: 'Tela de Pagamento',
                      description: 'Abrir tela completa de pagamento',
                      onPressed: () async {
                        if (selectedPaymentType == null) {
                          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Selecione o tipo de pagamento')));
                          return;
                        }
                        final result = await Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (context) => StoneStreamPage(
                              stonePaymentParams: StoneTechPaymentParams(
                                amount: 1.50,
                                type: selectedPaymentType ?? StoneTechPaymentType.debit,
                                credentials: ExampleConfig.credentials,
                              ),
                            ),
                          ),
                        );
                        log('**Result_navigation: $result');
                      },
                    ),
                    const SizedBox(height: 12),
                    _buildActionButton(
                      context: context,
                      icon: Icons.message,
                      label: 'Dialog de Pagamento',
                      description: 'Abrir dialog de pagamento',
                      onPressed: () async {
                        if (selectedPaymentType == null) {
                          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Selecione o tipo de pagamento')));
                          return;
                        } //   adb uninstall br.com.stone.posandroid.datacontainer

                        await showDialog(
                          context: context,
                          builder: (context) {
                            return StoneStreamDialog(
                              stonePaymentParams: StoneTechPaymentParams(
                                amount: 15.90,
                                type: selectedPaymentType ?? StoneTechPaymentType.debit,
                                credentials: ExampleConfig.credentials,
                              ),
                              onConfirmPayment: (transaction) {
                                log('***Response dialog: $transaction');
                              },
                            );
                          },
                        );
                      },
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 24),

            // Card de Impressão
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(Icons.print, color: theme.colorScheme.primary),
                        const SizedBox(width: 8),
                        Text('Impressão', style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                      ],
                    ),
                    const SizedBox(height: 16),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton.icon(
                        onPressed: printerBase64,
                        icon: const Icon(Icons.print),
                        label: const Text('Imprimir via Base64'),
                        style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16)),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildActionButton({required BuildContext context, required IconData icon, required String label, required String description, required VoidCallback onPressed}) {
    final theme = Theme.of(context);
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton.icon(
        onPressed: onPressed,
        icon: Icon(icon),
        label: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            Text(description, style: TextStyle(fontSize: 12, color: theme.colorScheme.onSurface.withOpacity(0.7))),
          ],
        ),
        style: ElevatedButton.styleFrom(padding: const EdgeInsets.all(16), alignment: Alignment.centerLeft),
      ),
    );
  }
}

/// Aviso exibido quando o app foi iniciado sem `--dart-define=STONE_CODE`.
class _MissingStoneCodeBanner extends StatelessWidget {
  const _MissingStoneCodeBanner();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      color: theme.colorScheme.errorContainer,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(Icons.warning_amber, color: theme.colorScheme.onErrorContainer),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                'STONE_CODE não configurado. Rode com --dart-define=STONE_CODE=<seu stone code> '
                '(veja o README do example).',
                style: TextStyle(color: theme.colorScheme.onErrorContainer),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
