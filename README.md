# Stone Payment Tech

<div align="center">
  <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/c/c9/Stone_pagamentos.png/800px-Stone_pagamentos.png" alt="Stone" height="100" />
</div>

<p align="center">
  Plugin Flutter não oficial para integração com o SDK da Stone Smart POS.<br>
  Compatível com maquininhas Android inteligentes.
</p>

<p align="center">
  <a href="#sobre">Sobre</a> •
  <a href="#tecnologias">Tecnologias</a> •
  <a href="#instalacao">Instalação</a> •
  <a href="#flavors">Configuração de Flavors</a> •
  <a href="#uso">Uso</a> •
  <a href="#respostas">Modelo de Resposta</a> •
</p>

---

## 🚨 Aviso

> **Este plugin é _não oficial_** e foi desenvolvido de forma independente para facilitar a integração de pagamentos via SDK da Stone em dispositivos Android Smart POS.

---

## 🎯 Sobre

O `stone_payment_tech` é um plugin Flutter para integração direta com o SDK Stone, permitindo a realização de transações em maquininhas smart Android.

## Gradle

O gradle.wrapper.properties utilizado está na versão 8.9
kotlin_version = '1.9.0'
stone_sdk_version = '4.11.5'
classpath 'com.android.tools.build:gradle:8.7.3'

## 🔐 Licenciamento

Para adquirir uma licença, cadastre-se no site [Licença JY Labtech](https://licenca.jylabtech.com.br).  
Cada licença é válida por terminal. Após a compra, será gerada uma chave `licenceKey` vinculada ao terminal.

### Inicialização com licença

##### Obervação: A partir da versão 1.0.0 não é mais necessário passar o `licenceKey` como parâmetro.

```dart
await StonePaymentTech.initPayment(
  handler: controller,
);
```

A partir da versão 1.0.0 é necessário adicionar as chaves no seu arquivo `local.properties` na raiz da pasta `android`.

```
licenceKey=<sua_licenca>
licenceInternalKey=***REMOVIDO***
```

### ✅ Máquinas compatíveis

- Positivo L300
- Positivo L400
- Ingenico APOS A8
- Sunmi P2
- Gertec GPOS700X
- Tectoy T2

---

## 🚀 Tecnologias

- [Flutter](https://flutter.dev/)
- SDK Stone `v4.11.5`
- Comunicação via Platform Channels

---

## ⚙️ Instalação

### 1. `pubspec.yaml`

Adicione a dependência:

```yaml
dependencies:
  stone_payment_tech: any
```

### 2. `build.gradle`

No arquivo `android/app/build.gradle`, defina o `minSdkVersion` como 23:

```gradle
defaultConfig {
    ...
    minSdkVersion 23
    ...
}
```

> ⚠️ O SDK da Stone requer `minSdkVersion >= 23`.

---

## 🧩 <a name="flavors">Configuração de Flavors</a>

Para utilizar o plugin corretamente, **você precisa configurar os seguintes `productFlavors` e `signingConfigs` no `android/app/build.gradle`** do seu projeto:

### 👇 Exemplo completo:

```gradle
...

android {
    ....
    signingConfigs {
        release {
            storeFile keyProperties.storeFile
            keyAlias keyProperties.keyAlias
            keyPassword keyProperties.keyPassword
            storePassword keyProperties.storePassword
        }
    }

    flavorDimensions "model"
    productFlavors {
        gertecGPOS700X {
            dimension "model"
            signingConfig signingConfigs.release
        }
        positivoSeriesL {
            dimension "model"
            signingConfig signingConfigs.release
        }
        sunmi {
            dimension "model"
            signingConfig signingConfigs.release
        }
        tectoySeriesT {
            dimension "model"
            signingConfig signingConfigs.release
        }
        ingenico {
            dimension "model"
            signingConfig signingConfigs.release
        }
    }
}
```

> ✅ **Importante:** mesmo que o seu projeto não use todos os modelos, você precisa declarar todos os `flavors` para evitar erro de compilação, pois o plugin depende da estrutura deles.

---

> ⚠️ O SDK da Stone requer `minSdkVersion >= 23`.

---

## 📦 Uso

### Criando um `StoneHandler`

Crie uma classe que estenda `IStoneHandler`. Essa será responsável por monitorar os eventos e respostas da Stone:

```dart
class StoneHandler extends IStoneHandler {
  final Function(StoneTransaction?)? onTransaction;
  final Function(String)? onMessageMonitor;

  StoneHandler({this.onTransaction, this.onMessageMonitor});

  @override
  Future<void> onError(String message) async {}
  @override
  Future<void> onMessage(String message) async {}
  @override
  Future<void> onFinishedResponse(String message) async {}
  @override
  Future<void> onChanged(String message) async {}
  @override
  Future<void> onAuthProgress(String message) async {}
  @override
  Future<void> onTransactionSuccess() async {}
  @override
  Future<void> onLoading(bool show) async {}
}
```

### Iniciando uma transação

Ative primeiro o pinpad com seu StoneCode:

```dart
StoneTech.I.payment.activePinpad(stoneCode: '12345');
```

Após ativar, você pode iniciar uma das transações disponíveis:

```dart
StoneTech.I.payment.creditPayment(12.50);
StoneTech.I.payment.creditPaymentParc(value: 12.50, installment: 2);
StoneTech.I.payment.debitPayment(12.50);
StoneTech.I.payment.voucherPayment(12.50);
StoneTech.I.payment.pixPayment(
  amount: 1250,
  qrCodeAuthorization: '',
  qrCodeProviderid: '',
);
StoneTech.I.payment.cancelTransaction(
  amount: 12.50,
  transactionType: PaymentTypeTransaction.CREDIT,
);
StoneTech.I.payment.abortTransaction();
```

> 🔖 **Obs:** o SDK imprime automaticamente a via do consumidor.

### Implementação de página

Abaixo segue um exemplo usando uma tela pré montada, onde você deve passar os parâmetros solicitados e a tela faz todo o fluxo de transação, e ao terminar lhe devolve um `StoneTransactionModel`.

```dart
 final result = await Navigator.push(
  context,
  MaterialPageRoute(
    builder: (context) => StoneStreamPage(
      stonePaymentParams: StonePaymentParams(
        amount: 15,
        type: StonePaymentType.debit,
        credentials: StoneCredentialsModel(
          stoneCode: 'StoneCode',
          appName: 'nome_do_seu_app',
        ),
      ),
    ),
  ),
);
```

### Implementação usando Dialog

Abaixo segue um exemplo usando um dialog pré montado, onde você deve passar os parâmetros solicitados e o dialog faz todo o fluxo de transação, e ao terminar lhe devolve um `StoneTransactionModel`.

```dart
await showDialog(
  context: context,
  builder: (context) {
    return StoneStreamDialog(
      stonePaymentParams: StonePaymentParams(
        amount: 15,
        type: StonePaymentType.debit,
        credentials: StoneCredentialsModel(
          stoneCode: 'StoneCode',
          appName: 'nome_do_seu_app',
        ),
      ),
      onConfirmPayment: (transaction) {
        print('***Response dialog: $transaction');
      },
    );
  });
```

### Implementação usando Tela para IMPRESSÃO

```dart
 final result = await Navigator.push(
  context,
  MaterialPageRoute(
    builder: (context) => StonePrinterPage(
      credentials: StoneCredentialsModel(
          stoneCode: 'StoneCode',
          appName: 'nome_do_seu_app',
        ),
        child: <Seu widget montando seu layout para impressão>
    ),
  ),
);
```

Ele vai devolver um boolean, informando se deu certo a impressão ou não.

#### Style

Caso queira passar um estilo diferente para o `StoneStreamDialog` ou para `StoneStreamPage`, é só passar o parâmetro `style`, sendo este a classe `StoneStreamStyle`.

---

## 📥 Callbacks implementáveis (`StoneHandler`)

| Método                  | Descrição                               |
| ----------------------- | --------------------------------------- |
| `onAbortedSuccessfully` | Transação abortada com sucesso          |
| `onAuthProgress`        | Status de autenticação (inclui PIN pad) |
| `onError`               | Erro em uma operação                    |
| `onMessage`             | Mensagem bruta vinda da Stone           |
| `onFinishedResponse`    | Resultado final da transação            |
| `onTransactionSuccess`  | Sucesso da transação                    |

---

## 📄 Modelo de resposta

### Exemplo: `onAuthProgress`, `onChanged`, `onError`

```json
{
  "method": "transaction",
  "message": "Transação aprovada",
  "errorMessage": "",
  "result": 0
}
```

### Exemplo: `onFinishedResponse`

```json
{
  "method": "transaction",
  "idFromBase": 0,
  "amount": 1250,
  "cardHolderNumber": "",
  "cardBrand": "",
  "date": "",
  "time": "",
  "aid": "",
  "arcq": "",
  "transactionReference": "",
  "saleAffiliationKey": "",
  "entryMode": "",
  "typeOfTransactionEnum": "",
  "serialNumber": "",
  "manufacture": "",
  "actionCode": "",
  "transactionStatus": "",
  "messageFromAuthorize": "",
  "errorMessage": "",
  "result": 0
}
```

---

## ❗ Observações importantes

- Para transações PIX, forneça `qrCodeAuthorization` e `qrCodeProviderid`.
- Para exibir QRCode (campo `method: QRCode`), o retorno virá em `message` como uma imagem em `Bitmap` convertida para `String`.
- O método `getAllTransactions()` pode ser usado para buscar transações anteriores para fins de estorno.

---

## 💡 Dica

A resposta **mais completa** da SDK Stone virá sempre no método `onFinishedResponse`. Use o campo `method` da resposta para identificar o tipo de evento:

```dart
void onFinishedResponseMonitor(StoneTransaction? stoneTransaction) {
  switch (stoneTransaction?.method) {
    case 'licence':
    //se result vier == 0, licença ativada, do contrário o objeto "errorMessage" irá trazer a descrição do erro.
    case 'active':
      // Terminal ativado
      break;
    case 'transaction':
      // Transação finalizada
      break;
    case 'abort':
    case 'abortPix':
    case 'cancel':
    case 'printer':
    case 'QRCode':
      // Exibir QR Code
      break;
    case 'PaymentOptions':
      // Use _stoneTech.payment.setPaymentOption(option: ...)
      break;
    case 'reversal':
      break;
    default:
  }
}
```

---

<div align="left">
  <table>
  <tr>
    <td align="center"><a href="https://github.com/JY-Labtech"><img src="https://avatars.githubusercontent.com/u/185066406?s=200&v=4" width="100px;" alt=""/><br /><sub><b>JY Labtech</b></sub></a><br /> <a href="https://github.com/JY-Labtech" title="Autor"></a></td>
  </tr>

</table>

</div>

---

<p align="center">
  <a href="#top">🔝 Voltar ao topo</a>
</p>
