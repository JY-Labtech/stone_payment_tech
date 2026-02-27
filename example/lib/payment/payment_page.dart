import 'package:flutter/material.dart';
import 'package:stone_payment_tech/stone_tech_payment.dart';

import 'payment_controller.dart';

class PaymentPage extends StatefulWidget {
  const PaymentPage({super.key});

  @override
  State<PaymentPage> createState() => _PaymentPageState();
}

class _PaymentPageState extends State<PaymentPage> {
  final PaymentController controller = PaymentController();

  double? saleValue;
  TextEditingController moneyController = TextEditingController();

  @override
  void initState() {
    StoneTech.I.initPayment(handler: controller);
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          // Card de Valor
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.attach_money, color: theme.colorScheme.primary),
                      const SizedBox(width: 8),
                      Text(
                        "Valor da Transação",
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    onChanged: (value) => setState(() {
                      controller.setSaleValue(moneyController.text.isNotEmpty ? double.parse(moneyController.text) : 0.0);
                    }),
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    decoration: InputDecoration(
                      hintText: "0,00",
                      prefixText: "R\$ ",
                      prefixStyle: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: theme.colorScheme.primary,
                      ),
                    ),
                    style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                    controller: moneyController,
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
          
          // Card de Métodos de Pagamento
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.payment, color: theme.colorScheme.primary),
                      const SizedBox(width: 8),
                      Text(
                        "Métodos de Pagamento",
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),
                  Wrap(
                    spacing: 12,
                    runSpacing: 12,
                    children: <Widget>[
                      _buildPaymentButton(
                        context: context,
                        label: "Débito",
                        icon: Icons.credit_card,
                        onPressed: () {
                          FocusScope.of(context).unfocus();
                          setState(() {
                            controller.clickPayment = true;
                          });
                          StoneTech.I.payment.debitPayment(6);
                        },
                      ),
                      _buildPaymentButton(
                        context: context,
                        label: "Crédito",
                        icon: Icons.credit_card_outlined,
                        enabled: controller.enable,
                        onPressed: controller.enable
                            ? () {
                                FocusScope.of(context).unfocus();
                                setState(() {
                                  controller.clickPayment = true;
                                });
                                StoneTech.I.payment.printWrapPaper(6);
                              }
                            : null,
                      ),
                      _buildPaymentButton(
                        context: context,
                        label: "Crédito Parc. 2x",
                        icon: Icons.credit_card,
                        enabled: controller.enable,
                        onPressed: controller.enable
                            ? () {
                                FocusScope.of(context).unfocus();
                                setState(() {
                                  controller.clickPayment = true;
                                });
                                StoneTech.I.payment.creditPaymentParc(value: controller.saleValue, installment: 2, withInterest: false);
                              }
                            : null,
                      ),
                      _buildPaymentButton(
                        context: context,
                        label: "Voucher",
                        icon: Icons.receipt_long,
                        enabled: controller.enable,
                        onPressed: controller.enable
                            ? () {
                                FocusScope.of(context).unfocus();
                                setState(() {
                                  controller.clickPayment = true;
                                });
                                StoneTech.I.payment.voucherPayment(controller.saleValue);
                              }
                            : null,
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
          
          // Card de Configuração
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.settings, color: theme.colorScheme.primary),
                      const SizedBox(width: 8),
                      Text(
                        "Configurações",
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      onPressed: () async {
                        FocusScope.of(context).unfocus();
                        setState(() {
                          controller.clickPayment = true;
                        });
                        StoneTech.I.payment.activePinpad(appName: 'AppDemo', stoneCode: '206192723');
                      },
                      icon: const Icon(Icons.nfc),
                      label: const Text("Ativar PINPAD"),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
          
          // Card de Ações
          Card(
            color: theme.colorScheme.errorContainer,
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.warning, color: theme.colorScheme.onErrorContainer),
                      const SizedBox(width: 8),
                      Text(
                        "Ações de Emergência",
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.bold,
                          color: theme.colorScheme.onErrorContainer,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: controller.clickPayment
                          ? () {
                              controller.setSaleValue(0.0);
                              StoneTech.I.payment.abortTransaction();
                            }
                          : null,
                      icon: const Icon(Icons.cancel),
                      label: const Text("Cancelar Operação"),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: theme.colorScheme.onErrorContainer,
                        side: BorderSide(color: theme.colorScheme.error),
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: () {
                        StoneTech.I.payment.abortTransaction();
                      },
                      icon: const Icon(Icons.stop),
                      label: const Text("Abortar Transação"),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: theme.colorScheme.onErrorContainer,
                        side: BorderSide(color: theme.colorScheme.error),
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: () {
                        StoneTech.I.payment.cancelTransaction(
                          idFromBase: 0,
                        );
                      },
                      icon: const Icon(Icons.delete_outline),
                      label: const Text("Cancelar Transação"),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: theme.colorScheme.onErrorContainer,
                        side: BorderSide(color: theme.colorScheme.error),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPaymentButton({
    required BuildContext context,
    required String label,
    required IconData icon,
    required VoidCallback? onPressed,
    bool enabled = true,
  }) {
    return SizedBox(
      width: (MediaQuery.of(context).size.width - 80) / 2,
      child: ElevatedButton.icon(
        onPressed: enabled ? onPressed : null,
        icon: Icon(icon, size: 20),
        label: Text(label),
        style: ElevatedButton.styleFrom(
          padding: const EdgeInsets.symmetric(vertical: 16),
        ),
      ),
    );
  }
}
