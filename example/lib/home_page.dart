import 'package:flutter/material.dart';
import 'package:stone_payment_tech/app/model/stone_credentials.dart';
import 'package:stone_payment_tech/app/model/stone_payment_params.dart';
import 'package:stone_payment_tech/app/pages/stone_stream_dialog.dart';
import 'package:stone_payment_tech/app/pages/stone_stream_page.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final String licenceKey = "sua-licenca-aqui";

  StonePaymentType? selectedPaymentType;

  void onChanged(StonePaymentType? value) {
    if (!mounted) return;
    setState(() {
      selectedPaymentType = value;
    });
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.sizeOf(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Stone Payment Tech'),
      ),
      body: Container(
        height: size.height,
        width: size.width,
        padding: const EdgeInsets.all(10),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            DropdownButton<StonePaymentType>(
              hint: const Text('Selecione o tipo de pagamento'),
              value: selectedPaymentType,
              items: [
                DropdownMenuItem(
                  value: StonePaymentType.debit,
                  child: Text(StonePaymentType.debit.name),
                ),
                DropdownMenuItem(
                  value: StonePaymentType.credit,
                  child: Text(StonePaymentType.credit.name),
                ),
                DropdownMenuItem(
                  value: StonePaymentType.pix,
                  child: Text(StonePaymentType.pix.name),
                ),
                DropdownMenuItem(
                  value: StonePaymentType.voucher,
                  child: Text(StonePaymentType.voucher.name),
                ),
              ],
              onChanged: onChanged,
            ),
            const SizedBox(height: 30),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.deepOrange,
              ),
              onPressed: () async {
                if (selectedPaymentType == null) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Selecione o tipo de pagamento'),
                    ),
                  );
                  return;
                }
                final result = await Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => StoneStreamPage(
                      stonePaymentParams: StonePaymentParams(
                        amount: 15,
                        type: selectedPaymentType ?? StonePaymentType.debit,
                        credentials: StoneCredentialsModel(
                          stoneCode: '206192723',
                          licenceKey: licenceKey,
                          appName: 'Stone Payment Tech',
                        ),
                      ),
                    ),
                  ),
                );

                print('**Result_navigation: $result');
              },
              child: const Text(
                'Tela de pagamento',
                style: TextStyle(color: Colors.white),
              ),
            ),
            const SizedBox(height: 20),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.deepOrange,
              ),
              onPressed: () async {
                if (selectedPaymentType == null) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Selecione o tipo de pagamento'),
                    ),
                  );
                  return;
                }
                await showDialog(
                    context: context,
                    builder: (context) {
                      return StoneStreamDialog(
                        stonePaymentParams: StonePaymentParams(
                          amount: 15,
                          type: selectedPaymentType ?? StonePaymentType.debit,
                          credentials: StoneCredentialsModel(
                            stoneCode: '206192723',
                            licenceKey: licenceKey,
                            qrCodeAuthorization: '',
                            qrCodeProviderid: '',
                            appName: 'Stone Payment Tech',
                          ),
                        ),
                        onConfirmPayment: (transaction) {
                          print('***Response dialog: $transaction');
                        },
                      );
                    });
              },
              child: const Text(
                'Dialog de pagamento',
                style: TextStyle(color: Colors.white),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
