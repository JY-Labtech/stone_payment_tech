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
  <a href="#requisitos">Requisitos e Configurações</a> •
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

## 🎯 <a name="sobre">Sobre</a>

O `stone_payment_tech` é um plugin Flutter para integração direta com o SDK Stone, permitindo a realização de transações em maquininhas smart Android.

## 📋 <a name="requisitos">Requisitos e Configurações</a>

Para utilizar este plugin corretamente, seu projeto precisa atender aos seguintes requisitos:

### Versões do Gradle e Dependências

| Componente            | Versão Mínima | Recomendada |
| --------------------- | ------------- | ----------- |
| Gradle Wrapper        | 8.12          | 8.12+       |
| Kotlin                | 2.1.21        | 2.1.21+     |
| Android Gradle Plugin | 8.12.1        | 8.12.1+     |
| Stone SDK             | 4.13.0        | 4.13.0      |
| Java (compilação)     | 11            | 11+         |

### Configuração do Android SDK

No arquivo `android/app/build.gradle` do seu projeto:

```gradle
android {
    compileSdk = 35  // Obrigatório: versão 35 ou superior

    defaultConfig {
        minSdk = 23        // Mínimo exigido pelo SDK Stone
        targetSdk = 35     // Recomendado: 35 ou superior
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}
```

### Gradle Wrapper Properties

No arquivo `android/gradle/wrapper/gradle-wrapper.properties`:

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.12-all.zip
```

### Build.gradle do Projeto

No arquivo `android/build.gradle` (nível de projeto):

```gradle
buildscript {
    ext.kotlin_version = '2.1.21'

    dependencies {
        classpath 'com.android.tools.build:gradle:8.12.1'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}
```

### Arquivo local.properties

Na raiz da pasta `android/`, crie ou edite o arquivo `local.properties` e adicione:

```properties
sdk.dir=/caminho/para/seu/Android/sdk
flutter.sdk=/caminho/para/seu/flutter/sdk
licenceKey=<sua_licenca_jylabtech>
licenceInternalKey=aHR0cHM6Ly9wb3MtcGF5bWVudHMtYXBpLTU3NzQ2NDIzNTQwOC5zb3V0aGFtZXJpY2EtZWFzdDEucnVuLmFwcC9wb3MtcGF5bWVudHMvbGljZW5jZS9jaGVjay9pbnZvaWNl
```

> ⚠️ **Importante:**
>
> - O `compileSdk` deve ser **35 ou superior** para evitar conflitos com dependências do AndroidX
> - O `minSdk` deve ser **23 ou superior** (requisito do SDK Stone)
> - Certifique-se de que todas as configurações de `compileSdkVersion` em subprojetos também estejam na versão 35+

### Resumo de Versões Utilizadas

```gradle
// Gradle
gradle-8.12

// Kotlin
kotlin_version = '2.1.21'

// Stone SDK
stone_sdk_version = '4.13.0'

// Android Gradle Plugin
classpath 'com.android.tools.build:gradle:8.12.1'

// SDK Versions
compileSdk = 35
targetSdk = 35
minSdk = 23
```

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
licenceInternalKey=aHR0cHM6Ly9wb3MtcGF5bWVudHMtYXBpLTU3NzQ2NDIzNTQwOC5zb3V0aGFtZXJpY2EtZWFzdDEucnVuLmFwcC9wb3MtcGF5bWVudHMvbGljZW5jZS9jaGVjay9pbnZvaWNl
```

### ✅ Máquinas compatíveis

- Positivo L300
- Positivo L400
- Ingenico APOS A8
- Sunmi P2
- Sunmi A-11
- Gertec GPOS700X
- Tectoy T2
- Tectoy T8

---

## 🚀 <a name="tecnologias">Tecnologias</a>

- [Flutter](https://flutter.dev/)
- SDK Stone `4.13.0`
- Comunicação via Platform Channels

---

## ⚙️ <a name="instalacao">Instalação</a>

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

## 📦 <a name="uso">Uso</a>

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

## 📄 <a name="respostas">Modelo de Resposta</a>

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
