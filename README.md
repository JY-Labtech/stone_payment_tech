# stone_payment_tech

> Open source Flutter plugin for the Stone Android Smart POS SDK: credit, debit, PIX, voucher, cancellation, reversal and printing on Sunmi, Positivo, Gertec, Ingenico and Tectoy terminals.

[![pub package](https://img.shields.io/pub/v/stone_payment_tech.svg)](https://pub.dev/packages/stone_payment_tech)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Plugin Flutter **não oficial** e **open source** para integrar seu app ao SDK Android da Stone em maquininhas Smart POS. Ele expõe as operações do SDK (ativação do terminal, pagamentos, cancelamento, estorno, impressão e consultas) por um `MethodChannel` e traz telas prontas (`StoneStreamPage`, `StoneStreamDialog`, `StonePrinterPage`) que fazem o fluxo completo.

Não há licença nem cobrança: o plugin chama o SDK da Stone direto. Para usar o SDK você precisa do seu próprio acesso à Stone (token do PackageCloud e Stone Code do estabelecimento).

---

## Sumário

1. [Funcionalidades](#funcionalidades)
2. [Maquininhas compatíveis e requisitos](#maquininhas-compatíveis-e-requisitos)
3. [Instalação](#instalação)
4. [Configuração Android](#configuração-android)
5. [Uso rápido](#uso-rápido)
6. [Referência da API](#referência-da-api)
7. [Exemplos por operação](#exemplos-por-operação)
8. [Tratamento de erros](#tratamento-de-erros)
9. [App de exemplo](#app-de-exemplo)
10. [Testes](#testes)
11. [Migração da 3.x para a 4.0.0](#migração-da-3x-para-a-400)
12. [Contribuindo](#contribuindo)
13. [Licença e créditos](#licença-e-créditos)

---

## Funcionalidades

Todas as operações ficam em `StoneTech.I.payment` (`StonePayment`):

| Operação | Método |
| --- | --- |
| Ativar o terminal (Stone Code) | `activePinpad` |
| Ativar o terminal com credenciais PIX | `activePinpadWithCredentials` |
| Crédito à vista | `creditPayment` |
| Crédito parcelado (com ou sem juros) | `creditPaymentParc` |
| Débito | `debitPayment` |
| PIX (QR Code) | `pixPayment` |
| Voucher (alimentação/refeição) | `voucherPayment` |
| Escolher alimentação/refeição durante o voucher | `setPaymentOption` |
| Abortar a transação em andamento | `abortTransaction` |
| Abortar a transação PIX em andamento | `abortPIXTransaction` |
| Abortar a ativação | `abortActivation` |
| Cancelar uma transação (por `idFromBase`) | `cancelTransaction` |
| Estorno (reversal) de transações pendentes | `reversal` |
| Reimprimir a transação atual (via cliente ou estabelecimento) | `printerCurrentTransaction` |
| Reimprimir por `transactionKey` | `paymentPrinterTransactionKey` |
| Listar todas as transações salvas no terminal | `getAllTransactions` |
| Buscar transação por `initiatorTransactionKey` | `getTransactionByInitiatorTransactionKey` |
| Impressão personalizada (texto e imagem) | `customPrinter` |
| Imprimir imagem em base64 | `printFromBase64` |
| Avançar papel | `printWrapPaper` |
| Número de série do POS | `getPosSerialNumber` |
| Fabricante do POS | `getPosManufacture` |
| Log de debug do SDK | `activeDebugLog` |

E também:

- `StoneStreamPage` e `StoneStreamDialog`: tela e dialog prontos que ativam o terminal, fazem o pagamento, mostram o QR Code do PIX, perguntam alimentação/refeição e devolvem o `StoneTransactionModel`.
- `StonePrinterPage`: transforma um widget em imagem e imprime.
- `StoneStreamController` / `StoneStreamHandler`: o mesmo fluxo como `Stream<StoneStatusTech>`, para montar sua própria UI.
- `StoneCustomPrinterController`: ativa o terminal e imprime um base64.

## Maquininhas compatíveis e requisitos

Maquininhas Smart POS da Stone com o SDK `posandroid` (cada uma é um *flavor* Android):

| Flavor | Modelos |
| --- | --- |
| `positivoSeriesL` | Positivo L300, L400 |
| `ingenico` | Ingenico APOS A8 |
| `sunmi` / `sunmiSeriesP` | Sunmi P2, A-11 |
| `gertecGpos700` / `gertecGposSeries7` | Gertec GPOS700X |
| `tectoySeriesT` | Tectoy T2, T8 |

Requisitos:

| Item | Versão |
| --- | --- |
| Dart | `>= 3.1.5` |
| Flutter | `>= 3.13.0` |
| Android `minSdk` | 23 (exigido pelo SDK da Stone) |
| Android `compileSdk` | 35 |
| Stone SDK | `4.15.0` (`br.com.stone:stone-sdk` e `stone-sdk-posandroid-*`) |
| Android Gradle Plugin | 8.12.1 ou mais novo |
| Kotlin | 2.1.21 ou mais novo |
| JDK de build | 17 |

Você também precisa:

- de um **token de leitura do PackageCloud da Stone** (o SDK é privado; peça à Stone);
- do **Stone Code** do estabelecimento (ativação do terminal);
- para PIX, do `qrCodeAuthorization` e do `qrCodeProviderid` fornecidos pela Stone;
- de um terminal Stone homologado. Alguns fabricantes (Gertec, Positivo) exigem que o APK seja assinado com a chave do fabricante para instalar no terminal; use a chave de desenvolvimento fornecida por eles.

Permissões declaradas pelo plugin (mescladas no Manifest do app automaticamente): `INTERNET`, `ACCESS_NETWORK_STATE`, `BLUETOOTH`, `BLUETOOTH_ADMIN`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`.

## Instalação

```bash
flutter pub add stone_payment_tech
```

Ou no `pubspec.yaml`:

```yaml
dependencies:
  stone_payment_tech: ^4.0.0
```

```dart
import 'package:stone_payment_tech/stone_payment_tech.dart';
```

> O import antigo `package:stone_payment_tech/stone_tech_payment.dart` continua funcionando (exporta a mesma API).

## Configuração Android

### 1. Token do PackageCloud da Stone

O SDK da Stone vem de um repositório Maven privado do PackageCloud. O plugin adiciona esse repositório sozinho, montando a URL com **o seu** token. Informe o token de um destes jeitos (a variável de ambiente tem prioridade):

**a) `android/local.properties` do seu app** (não vai para o git):

```properties
sdk.dir=/caminho/para/Android/sdk
flutter.sdk=/caminho/para/flutter
packageCloudReadToken=<SEU_TOKEN_DE_LEITURA_DA_STONE>
```

**b) Variável de ambiente** (recomendado em CI):

```bash
export PACKAGECLOUD_READ_TOKEN=<SEU_TOKEN_DE_LEITURA_DA_STONE>
```

Opcional, só para quem tem acesso ao repositório interno da Stone (`pos-android-internal`): `packageCloudReadTokenInternal` no `local.properties` ou `PACKAGECLOUD_READ_TOKEN_INTERNAL`.

Sem token o Gradle mostra o aviso `stone_payment_tech: packageCloudReadToken não configurado` e falha ao resolver `br.com.stone:stone-sdk`.

> Nunca commite o token. O `local.properties` já fica fora do git nos projetos Flutter; em CI, guarde-o como *secret*.

### 2. `minSdk`, `compileSdk` e Java

No `android/app/build.gradle`:

```gradle
android {
    compileSdk = 35

    defaultConfig {
        minSdk = 23
        targetSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
```

O `android/gradle.properties` precisa de AndroidX e Jetifier (o SDK da Stone ainda usa bibliotecas antigas) e de memória suficiente:

```properties
android.useAndroidX=true
android.enableJetifier=true
org.gradle.jvmargs=-Xmx4G -XX:MaxMetaspaceSize=2G
```

### 3. Flavors (obrigatório)

O plugin tem um *flavor* por fabricante, e cada um puxa o módulo `stone-sdk-posandroid-<fabricante>` certo. O app precisa declarar **os mesmos flavors** (com a dimensão `model`), mesmo que use só um:

```gradle
android {
    flavorDimensions += "model"

    productFlavors {
        gertecGpos700     { dimension "model"; versionNameSuffix "-gertecGpos700" }
        gertecGposSeries7 { dimension "model"; versionNameSuffix "-gertecGposSeries7" }
        positivoSeriesL   { dimension "model"; versionNameSuffix "-positivoSeriesL" }
        ingenico          { dimension "model"; versionNameSuffix "-ingenico" }
        sunmi             { dimension "model"; versionNameSuffix "-sunmi" }
        sunmiSeriesP      { dimension "model"; versionNameSuffix "-sunmiSeriesP" }
        tectoySeriesT     { dimension "model"; versionNameSuffix "-tectoySeriesT" }
    }
}
```

Rode e gere o APK sempre com o flavor da maquininha:

```bash
flutter run --flavor sunmi
flutter build apk --release --flavor positivoSeriesL
```

### 4. Assinatura

Use o `signingConfig` exigido pelo fabricante do terminal (por exemplo, a chave de desenvolvimento da Gertec ou da Positivo). Guarde senhas e keystores fora do git (`key.properties`, variáveis de ambiente). O módulo do plugin não assina nada.

## Uso rápido

```dart
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:stone_payment_tech/stone_payment_tech.dart';

/// Recebe os eventos do SDK da Stone.
class MeuStoneHandler extends IStoneHandler {
  @override
  Future<void> onMessage(String message) async => debugPrint('mensagem: $message');

  @override
  Future<void> onError(String message) async => debugPrint('erro: $message');

  @override
  Future<void> onFinishedResponse(String message) async {
    final map = jsonDecode(message) as Map<String, dynamic>;
    switch (map['method']) {
      case 'active':
        debugPrint('ativação: ${map['message'] ?? map['errorMessage']}');
        break;
      case 'transaction':
        final transacao = StoneTransactionModel.fromMap(map);
        debugPrint(transacao.isApproved ? 'Aprovada: ${transacao.amount}' : 'Negada: ${transacao.errorMessage}');
        break;
    }
  }

  @override
  Future<void> onChanged(String message) async {}
  @override
  Future<void> onAuthProgress(String message) async {}
  @override
  Future<void> onTransactionSuccess() async {}
  @override
  Future<void> onLoading(bool show) async {}
}

Future<void> pagar() async {
  // 1. Registra o handler (uma vez).
  StoneTech.I.initPayment(handler: MeuStoneHandler());

  // 2. Ativa o terminal com o seu Stone Code (o resultado chega em onFinishedResponse, method "active").
  await StoneTech.I.payment.activePinpad(appName: 'Meu App', stoneCode: '<SEU_STONE_CODE>');

  // 3. Depois de ativado, cobra R$ 12,50 no débito (valores em centavos).
  try {
    await StoneTech.I.payment.debitPayment(1250);
    // true = operação disparada; o resultado chega em onFinishedResponse (method "transaction").
  } on PlatformException catch (e) {
    debugPrint('Falha ao disparar: ${e.code} ${e.message}');
  }
}
```

> O `Future` de cada operação completa com `true` assim que a operação é **disparada** no SDK. A aprovação/negação, o terminal ativado, a impressão concluída etc. chegam pelos callbacks do handler. `true` **não** significa transação aprovada.

Prefere não lidar com o handler? Use a tela pronta:

```dart
final StoneTransactionModel? resultado = await Navigator.push(
  context,
  MaterialPageRoute(
    builder: (_) => StoneStreamPage(
      stonePaymentParams: StoneTechPaymentParams(
        amount: 12.50, // em reais
        type: StoneTechPaymentType.debit,
        credentials: StoneTechCredentialsModel(stoneCode: '<SEU_STONE_CODE>', appName: 'Meu App'),
      ),
    ),
  ),
);
```

## Referência da API

### `StoneTech`

| Membro | Descrição |
| --- | --- |
| `static StoneTech get I` | Instância única, no canal `stone_payment_tech`. |
| `StonePayment initPayment({required IStoneHandler handler, IStoneTechHandler? iStoneTechHandler})` | Cria o `StonePayment` (uma vez) e registra os handlers. Chamadas seguintes devolvem a mesma instância. |
| `StonePayment get payment` | O `StonePayment` criado. Lança erro se `initPayment` não foi chamado. |
| `static bool isPinpadActivated` | `true` depois que o evento `active` com "Terminal ativado" chega. |
| `static const channelName` | `"stone_payment_tech"`. |

### `StonePayment` (`StoneTech.I.payment`)

Valores monetários são `int` em **centavos**. Os parâmetros `printCustomerSlip` (padrão `true`) e `printEstablishmentSlip` (padrão `false`) controlam as vias impressas; `initiatorTransactionKey` (padrão `""`) é a sua chave da transação.

| Método | Retorno | Observação |
| --- | --- | --- |
| `activePinpad({required String appName, required String stoneCode})` | `Future<bool>` | `false` se falhar ao disparar. |
| `activePinpadWithCredentials({required String appName, required String stoneCode, required String qrCodeAuthroization, required String qrCodeProviderid, bool printCustomerSlip = true})` | `Future<bool>` | Ativa já com as chaves do PIX. Repare na grafia `qrCodeAuthroization`. |
| `creditPayment(int value, {printCustomerSlip, printEstablishmentSlip, initiatorTransactionKey})` | `Future<bool>` | Crédito à vista. |
| `creditPaymentParc({required int value, int installment = 1, bool withInterest = false, printCustomerSlip, printEstablishmentSlip, initiatorTransactionKey})` | `Future<bool>` | Crédito parcelado. `withInterest: true` = juros do comprador. |
| `debitPayment(int value, {...})` | `Future<bool>` | Débito. |
| `voucherPayment(int value, {...})` | `Future<bool>` | Voucher. Pode emitir `PaymentOptions` (alimentação/refeição). |
| `pixPayment({required int amount, required String qrCodeAuthroization, required String qrCodeProviderid, ...})` | `Future<bool>` | PIX. O QR Code chega em `onChanged`/`onAuthProgress` (`method: "QRCode"`). |
| `setPaymentOption({required String option})` | `Future<bool>` | Responde ao `PaymentOptions` com a opção escolhida. |
| `abortTransaction()` | `Future<bool>` | Aborta a transação atual. |
| `abortPIXTransaction()` | `Future<bool>` | Aborta o PIX atual. |
| `abortActivation()` | `Future<bool>` | Aborta a ativação em andamento. |
| `cancelTransaction({required int idFromBase})` | `Future<bool>` | Cancela pelo `idFromBase` da transação. |
| `reversal()` | `Future<bool>` | Estorna transações pendentes. `false` se falhar ao disparar. |
| `printerCurrentTransaction({required bool printCustomerSlip})` | `Future<bool>` | Reimprime a última transação (`true` = via do cliente). |
| `paymentPrinterTransactionKey({required String transactionKey})` | `Future<bool>` | Reimprime pela chave. |
| `getAllTransactions()` | `Future<bool>` | A lista chega em `onFinishedResponse` (`method: "AllTransactions"`). |
| `getTransactionByInitiatorTransactionKey({required String initiatorTransactionKey})` | `Future<bool>` | A transação chega em `onFinishedResponse`. |
| `customPrinter(StonePrinterParams params)` | `Future<bool>` | Impressão de linhas de texto/imagem. `false` se falhar. |
| `printFromBase64(String base64)` | `Future<bool>` | Imprime uma imagem. `false` se falhar. |
| `printWrapPaper(int lines)` | `Future<bool>` | Avança o papel. `false` se falhar. |
| `getPosSerialNumber()` | `Future<String?>` | Número de série. |
| `getPosManufacture()` | `Future<String?>` | Fabricante. |
| `activeDebugLog({required bool isDebugLog})` | `Future<void>` | Liga/desliga o log do SDK. |
| `setHandler(IStoneHandler handler)` | `void` | Troca o handler (mantém o `IStoneTechHandler`). |

### Handlers

`IStoneHandler` (obrigatório; recebe o texto/JSON cru do nativo):

| Callback | Quando |
| --- | --- |
| `onMessage(String)` | Mensagens de andamento ("Insira o cartão", "Ativando terminal"...). |
| `onAuthProgress(String)` | Progresso da autorização (JSON com `method`, inclui QR Code do PIX). |
| `onChanged(String)` | QR Code do PIX e opções alimentação/refeição (JSON com `method`). |
| `onFinishedResponse(String)` | Resultado final (JSON, veja abaixo). |
| `onError(String)` | Erro de uma operação (JSON com `errorMessage`). |
| `onTransactionSuccess()` | Transação concluída com sucesso. |
| `onLoading(bool)` | Mostrar/esconder carregamento. |

`IStoneTechHandler` (opcional, em `initPayment`): os mesmos eventos já convertidos (`StoneTechResponse`, `StoneTransactionModel`) mais `writeToFile`, `onAbortedSuccessfully` e `onTransactionInfo`.

### Eventos de `onFinishedResponse`

O campo `method` identifica o evento:

| `method` | Significado |
| --- | --- |
| `active` | Ativação. `message: "Terminal ativado"` em caso de sucesso; `errorMessage` no erro. |
| `transaction` | Resultado do pagamento. |
| `QRCode` | QR Code do PIX (imagem em base64 no `message`). |
| `PaymentOptions` | Pede alimentação/refeição (`options: [...]`). Responda com `setPaymentOption`. |
| `cancel` | Resultado do cancelamento. |
| `abortPix` | PIX abortado. |
| `reversal` | Resultado do estorno. |
| `AllTransactions` | Lista `stoneTransactions` de `getAllTransactions`. |
| `paymentGetTransactionByInitiatorTransactionKey` | Resultado da busca por chave. |
| `printerTransaction`, `customPrinter`, `printFromBase64`, `printWrapPaper` | Resultado da impressão. |

Exemplo de `transaction`:

```json
{
  "method": "transaction",
  "idFromBase": 12,
  "amount": "1250",
  "cardHolderNumber": "************1234",
  "cardBrand": "MASTERCARD",
  "date": "27/11/2026",
  "time": "10:15:00",
  "aid": "A0000000041010",
  "arcq": "",
  "transactionReference": "...",
  "saleAffiliationKey": "...",
  "entryMode": "ICC",
  "typeOfTransactionEnum": "DEBIT",
  "serialNumber": "...",
  "manufacture": "SUNMI",
  "actionCode": "0000",
  "authorizationCode": "123456",
  "transactionStatus": "APPROVED",
  "messageFromAuthorize": "Aprovado",
  "errorMessage": "",
  "result": 0
}
```

### Modelos

- **`StoneTransactionModel`**: `method`, `idFromBase`, `amount`, `cardHolderNumber`, `cardBrand`, `date`, `time`, `aid`, `arcq`, `transactionReference`, `saleAffiliationKey`, `entryMode`, `typeOfTransactionEnum`, `serialNumber`, `manufacture`, `actionCode`, `authorizationCode`, `transactionStatus`, `messageFromAuthorize`, `actionResultMessage`, `message`, `errorMessage`, `result`. `isApproved` indica aprovação. `fromMap`, `fromJson`.
- **`StoneTechResponse`**: `method`, `message`, `errorMessage`, `result`.
- **`StoneTechCredentialsModel`**: `stoneCode`, `appName`, `qrCodeAuthorization?`, `qrCodeProviderid?`. Com as duas chaves de PIX preenchidas, a ativação usa `activePinpadWithCredentials`.
- **`StoneTechPaymentParams`**: `amount` (`double`, em reais), `type` (`StoneTechPaymentType`), `credentials`, `installments?`, `options?` (`StoneTechOptions`).
- **`StonePrinterParams`** / **`StonePrinter`**: `printers: [StonePrinter(title, description, printerType, imageBase64?)]`.
- **`StoneStreamStyle`**: cores e ícone das telas prontas.

### `StoneStatusTech` (stream das telas prontas e do `StoneStreamController`)

`StoneStatusTechMessage(message)`, `StoneStatusTechActivateRunning(message)`, `StoneStatusTechActivate`, `StoneStatusTechActivateError(message)`, `StoneStatusTechTransaction(transaction)`, `StoneStatusTechQRCodeTransaction(transaction)`, `StoneStatusTechPaymentOptions(options, message)`, `StoneStatusTechCanceled(cancelExtraData)`, `StonePrinterCustomer`.

### Enums

- `StoneTechPaymentType`: `credit`, `creditParc`, `debit`, `voucher`, `pix`.
- `PrinterType`: `text`, `image`.
- `PaymentTypeCall`: nomes dos métodos do canal (uso interno/testes).
- `PaymentTypeHandler`: nomes dos eventos do nativo.

## Exemplos por operação

```dart
final payment = StoneTech.I.payment;

// Crédito à vista, R$ 100,00, imprimindo também a via do estabelecimento
await payment.creditPayment(10000, printEstablishmentSlip: true);

// Crédito parcelado em 3x sem juros, com a sua chave da venda
await payment.creditPaymentParc(value: 30000, installment: 3, initiatorTransactionKey: 'pedido-42');

// Crédito parcelado em 6x com juros do comprador
await payment.creditPaymentParc(value: 60000, installment: 6, withInterest: true);

// Débito
await payment.debitPayment(1990);

// Voucher (se vier PaymentOptions, responda a opção escolhida)
await payment.voucherPayment(3500);
await payment.setPaymentOption(option: 'Cartão Refeição');

// PIX (o QR Code chega no handler com method "QRCode")
await payment.pixPayment(
  amount: 2500,
  qrCodeAuthroization: '<QRCODE_AUTHORIZATION>',
  qrCodeProviderid: '<QRCODE_PROVIDER_ID>',
);
await payment.abortPIXTransaction(); // desiste do PIX

// Cancelamento (idFromBase vem no StoneTransactionModel da venda)
await payment.cancelTransaction(idFromBase: 12);

// Estorno de transações pendentes
await payment.reversal();

// Reimpressão
await payment.printerCurrentTransaction(printCustomerSlip: true);
await payment.paymentPrinterTransactionKey(transactionKey: '<transactionKey>');

// Consultas
await payment.getAllTransactions();
await payment.getTransactionByInitiatorTransactionKey(initiatorTransactionKey: 'pedido-42');
final serial = await payment.getPosSerialNumber();
final fabricante = await payment.getPosManufacture();

// Impressão personalizada
await payment.customPrinter(StonePrinterParams(printers: [
  StonePrinter(title: 'Pedido', description: '#42', printerType: PrinterType.text),
  StonePrinter(title: 'Total', description: 'R\$ 25,00', printerType: PrinterType.text),
  StonePrinter(title: '', description: '', printerType: PrinterType.image, imageBase64: '<base64>'),
]));
await payment.printFromBase64('<imagem em base64>');
await payment.printWrapPaper(3);
```

Dialog pronto, com parcelamento:

```dart
await showDialog(
  context: context,
  builder: (_) => StoneStreamDialog(
    stonePaymentParams: StoneTechPaymentParams(
      amount: 150.00,
      type: StoneTechPaymentType.credit,
      installments: 3,
      credentials: StoneTechCredentialsModel(stoneCode: '<SEU_STONE_CODE>', appName: 'Meu App'),
    ),
    onConfirmPayment: (transacao) => debugPrint('Aprovada: ${transacao.authorizationCode}'),
  ),
);
```

Impressão de um widget:

```dart
final bool? impresso = await Navigator.push(
  context,
  MaterialPageRoute(
    builder: (_) => StonePrinterPage(
      params: StonePrinterPageParams(
        credentials: StoneTechCredentialsModel(stoneCode: '<SEU_STONE_CODE>', appName: 'Meu App'),
        child: const Text('Comprovante do pedido #42'),
      ),
    ),
  ),
);
```

Ativar e imprimir um base64 sem tela:

```dart
StoneCustomPrinterController().initializeAndPrintBase64(
  stoneCredentials: StoneTechCredentialsModel(stoneCode: '<SEU_STONE_CODE>', appName: 'Meu App'),
  base64: '<imagem em base64>',
  onMessage: (m) => debugPrint(m),
  onError: (e) => debugPrint(e),
);
```

## Tratamento de erros

| Situação | Como aparece |
| --- | --- |
| Falha ao disparar a operação no SDK (terminal não ativado, argumento faltando, exceção do SDK) | `PlatformException(code: 'ERROR', message: <mensagem do SDK>)`. Em `activePinpad`, `activePinpadWithCredentials`, `reversal`, `customPrinter`, `printFromBase64` e `printWrapPaper` o retorno é `false`. |
| Plugin desanexado da engine | `PlatformException(code: 'ERROR', message: 'Plugin desanexado da engine')`. |
| Método inexistente no nativo (ex.: `checkLicence`, removido na 4.0.0) | `MissingPluginException` (`notImplemented`). |
| `StoneTech.I.payment` antes de `initPayment` | Erro `PAYMENT NEEDS TO BE INITIALIZED!`. |
| Transação negada, ativação recusada, erro do autorizador | Não é exceção: chega em `onFinishedResponse`/`onError` com `result != 0` e `errorMessage`. Nas telas prontas, vira `StoneStatusTechActivateError` ou mensagem. |
| PIX expirado | `errorMessage` contendo `QRCODE_EXPIRED`. |

```dart
try {
  await StoneTech.I.payment.creditPayment(1000);
} on PlatformException catch (e) {
  if (e.code == 'ERROR') {
    // Ex.: terminal não ativado. Chame activePinpad e tente de novo.
  }
}
```

## App de exemplo

O app em [`example/`](example) mostra a tela pronta, o dialog, a impressão base64 e a ativação manual. O Stone Code e as credenciais não ficam no código: passe-os por `--dart-define`.

```bash
cd example
# token do PackageCloud em example/android/local.properties (packageCloudReadToken) ou:
export PACKAGECLOUD_READ_TOKEN=<SEU_TOKEN>

flutter run --flavor sunmi \
  --dart-define=STONE_CODE=<SEU_STONE_CODE> \
  --dart-define=APP_NAME="Meu App" \
  --dart-define=QRCODE_AUTHORIZATION=<TOKEN_PIX> \
  --dart-define=QRCODE_PROVIDER_ID=<PROVIDER_ID_PIX>
```

| `--dart-define` | Obrigatório | Uso |
| --- | --- | --- |
| `STONE_CODE` | sim | Stone Code para ativar o terminal. Sem ele o app mostra um aviso. |
| `APP_NAME` | não | Nome do app na ativação (padrão `Stone Payment Tech`). |
| `QRCODE_AUTHORIZATION` / `QRCODE_PROVIDER_ID` | só para PIX | Credenciais do PIX. |

Para instalar em terminais que exigem a chave do fabricante, crie `example/android/key.properties` com `positivoFile`, `positivoAlias`, `positivoPassword` e `positivoStorePassword`; sem esse arquivo o debug usa a chave de debug padrão.

## Testes

```bash
# Testes Dart (canal mockado): todos os métodos públicos, eventos do nativo, modelos, stream e controller
flutter test

# Testes JVM do nativo: despacho e argumentos de cada método do canal, resposta única,
# mapeamentos do SDK, parser de impressão e JSON enviado ao Dart.
# Requer JDK 17 e o token do PackageCloud configurado (o Gradle baixa o SDK da Stone).
cd example/android
./gradlew :stone_payment_tech:testSunmiDebugUnitTest
# qualquer flavor serve, ex.: testGertecGposSeries7DebugUnitTest
```

## Migração da 3.x para a 4.0.0

A 4.0.0 remove toda a licença/cobrança da JY Labtech. O plugin passa a ser gratuito e open source (MIT).

O que mudou:

- `StonePayment.checkLicence()` foi removido.
- A constante `licenceCheck` foi removida (`CHANNEL_NAME` e `USER_REFERENCE` continuam).
- Não existe mais `PlatformException(code: 'LICENCE_ERROR')` nem o evento `onFinishedResponse` com `method: "licence"` / mensagens "Verificando licença" e "Licença ativada".
- O build não lê nem exige mais `licenceKey`/`licenceInternalKey` (nem `LICENCE_KEY`/`LICENCE_INTERNAL_KEY`).
- Nenhuma chamada de rede à JY Labtech: as operações chamam o SDK da Stone direto.

Antes (3.x):

```properties
# android/local.properties
packageCloudReadToken=<token>
licenceKey=<sua_licenca_jylabtech>
licenceInternalKey=<opcional>
```

```dart
StoneTech.I.initPayment(handler: handler);
final ok = await StoneTech.I.payment.checkLicence();
try {
  await StoneTech.I.payment.debitPayment(1000);
} on PlatformException catch (e) {
  if (e.code == 'LICENCE_ERROR') mostrarErroDeLicenca(e.message);
}

// no handler
if (map['method'] == 'licence') { /* ... */ }
```

Depois (4.0.0):

```properties
# android/local.properties
packageCloudReadToken=<token>
```

```dart
StoneTech.I.initPayment(handler: handler);
try {
  await StoneTech.I.payment.debitPayment(1000);
} on PlatformException catch (e) {
  // só 'ERROR' (falha ao disparar no SDK)
}
```

Apague `licenceKey`/`licenceInternalKey` do `local.properties` e das variáveis de CI, remova as chamadas a `checkLicence()` e o tratamento de `LICENCE_ERROR`/`method == 'licence'`.

## Contribuindo

Issues e pull requests são bem-vindos em [github.com/JY-Labtech/stone_payment_tech](https://github.com/JY-Labtech/stone_payment_tech/issues).

1. Abra uma issue descrevendo o bug ou a melhoria (modelo da maquininha, versão do SDK e logs ajudam).
2. Faça um fork, crie um branch e mantenha `flutter analyze` sem avisos.
3. Rode `flutter test` e os testes nativos antes de abrir o PR.
4. Nunca inclua tokens, Stone Codes reais ou keystores no PR.

## Licença e créditos

Distribuído sob a licença [MIT](LICENSE).

Este plugin não é oficial e não tem vínculo com a Stone. "Stone" é marca de seus respectivos donos.

Desenvolvido e mantido pela **JY Labtech**: [jylabtech.com.br](https://jylabtech.com.br).
