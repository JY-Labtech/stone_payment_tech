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
  <a href="#licenca">Licenciamento</a> •
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
| Stone SDK             | 4.15.0        | 4.15.0      |
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
```

> As chaves de licença são lidas em tempo de build (`local.properties` da pasta `android/` do seu app ou variáveis de ambiente `LICENCE_KEY` / `LICENCE_INTERNAL_KEY`). O `licenceInternalKey` é opcional. Veja a seção [Licenciamento](#licenca).

> ⚠️ **Importante:**
>
> - O `compileSdk` deve ser **35 ou superior** para evitar conflitos com dependências do AndroidX
> - O `minSdk` deve ser **23 ou superior** (requisito do SDK Stone)
> - Certifique-se de que todas as configurações de `compileSdkVersion` em subprojetos também estejam na versão 35+

### Resumo de Versões Utilizadas

```gradle
// Gradle
gradle-8.14.3

// Kotlin
kotlin_version = '2.1.21'

// Stone SDK
stone_sdk_version = '4.15.0'

// Android Gradle Plugin
classpath 'com.android.tools.build:gradle:8.13.0'

// SDK Versions
compileSdk = 35
targetSdk = 35
minSdk = 23
```

## 🔐 <a name="licenca">Licenciamento</a>

O plugin exige uma licença da JY Labtech válida e ativa. Ela é conferida antes de **toda** chamada ao nativo (pagamentos, cancelamento, estorno, impressões, ativação do pinpad, consultas etc.). Sem licença válida, nenhuma operação chega ao SDK da Stone.

### Onde adquirir

Cadastre-se ou entre no portal **[portal.jylabtech.com.br](https://portal.jylabtech.com.br)** (mesmo e-mail e senha do portal anterior). Lá você adquire e gerencia as licenças, acompanha os aparelhos vinculados e pode desativá-los.

- O produto é único, **"Licença plugin de pagamento"**, e vale para qualquer plugin de pagamento da JY Labtech.
- **Plano `debug` (cortesia):** concedido automaticamente no cadastro, válido por 365 dias e para até 2 aparelhos. Ideal para desenvolvimento e homologação.
- **Plano `mensal`:** cobrado por dispositivo. Cada aparelho que valida a licença ocupa uma vaga; ao atingir o limite, novos aparelhos são recusados até que você libere vagas (desativando aparelhos no portal) ou amplie a licença.
- **As chaves de licença existentes continuam valendo**, com os mesmos valores. Chaves novas no formato `JYL-...` também funcionam.

### Configurando as chaves

A partir da versão 1.0.0 a licença não é mais passada por parâmetro em Dart: ela é embutida no build Android. Adicione no `local.properties` da pasta `android/` do seu app:

```properties
licenceKey=<sua_licenca>
```

Ou, em CI, use a variável de ambiente `LICENCE_KEY` (ela tem prioridade sobre o `local.properties`). Sem a chave o build falha com `LicenceKey não encontrado!!`.

- `licenceKey`: a chave da licença obtida no portal.
- `licenceInternalKey` (**opcional**, `local.properties` ou `LICENCE_INTERNAL_KEY`): desde a 3.1.0 o plugin **sempre** valida no servidor oficial da JY Labtech (`https://licence-server.invalid/pos-payments/licence/check`). Se informado, só é aceito quando aponta para esse servidor (base64 ou URL em texto puro, `https`, incluindo as rotas `/licence/check/invoice` e `/licence/check/subscription`). Qualquer outro valor é ignorado e o servidor oficial é usado, **inclusive o valor antigo** (`***REMOVIDO***...`). Pode remover a linha do seu `local.properties`.

Depois é só inicializar o plugin normalmente; a licença é validada automaticamente a cada chamada ao nativo:

```dart
StoneTech.I.initPayment(handler: meuHandler);
```

O resultado da validação chega no handler com `method: "licence"` (`result == 0` para licença ativa; caso contrário `errorMessage` traz o motivo).

**Licença inválida, vencida, suspensa ou cancelada bloqueia a operação:** a chamada nativa falha com `PlatformException(code: 'LICENCE_ERROR', message: <motivo>)` e o SDK da Stone não é chamado. Os métodos que retornam `bool` com tratamento interno (`activePinpad`, `activePinpadWithCredentials`, `reversal`, `customPrinter`, `printFromBase64`, `printWrapPaper`) devolvem `false`; os demais (pagamentos, cancelamento, impressões de comprovante, consultas, abort) propagam a `PlatformException`. No `StoneStreamController`, o erro aparece como `StoneStatusTechActivateError`.

### Identificação do aparelho e cache offline

- O aparelho é identificado pelo `ANDROID_ID` (identificador principal) e, quando disponível, pelo número de série.
- Só uma resposta HTTP 200 com licença **válida e ativa** (`expiredAt` no futuro, sem `isExpired`, `isInactive` ou `isCanceled`) libera o uso. Qualquer outra resposta é erro; timeout, falha de rede, 408/429 e 5xx só liberam pelo cache offline (abaixo).
- **Revalidação**: a primeira operação depois de abrir o app sempre consulta o servidor; depois, a licença é consultada de novo a cada **4 horas** de uso.
- **Bloqueio imediato**: se o servidor recusar a licença (cliente bloqueado no portal, licença vencida, cancelada ou suspensa, não encontrada, aparelho desativado ou limite de aparelhos), o cache é apagado e as operações passam a falhar com `LICENCE_ERROR` na hora.
- **Cache offline**: após uma validação online bem-sucedida, a licença fica guardada **criptografada** no aparelho. Ela só é usada quando o servidor não responde (sem internet, timeout, 408/429 ou erro 5xx) e por **no máximo 72 horas** desde a última validação online, desde que a licença continue dentro da validade (`expiredAt`). Depois de uma falha de rede o plugin espera 5 minutos antes de tentar o servidor de novo. O cache é descartado se a chave mudar, se tiver sido validado em outro servidor ou se o relógio do aparelho estiver adiantado/atrasado em relação à data salva. Depois disso o aparelho precisa de internet para revalidar.
- A validação online roda em background (timeout de 15 s), sem travar a interface.
- Se o servidor confirmar a licença válida e ativa mas o aparelho não conseguir salvar o cache (falha do EncryptedSharedPreferences/Keystore), a operação é liberada mesmo assim; o erro é registrado no log e a próxima operação revalida online.

### Erros de licença

| HTTP | Situação                                              | O que fazer                                                                 |
| ---- | ----------------------------------------------------- | --------------------------------------------------------------------------- |
| 404  | Licença não encontrada                                | Confira o `licenceKey` (sem espaços) no portal.                             |
| 412  | Limite de aparelhos atingido ou aparelho desativado   | Libere/reative aparelhos no portal ou amplie a licença.                     |
| 428  | Licença vencida, suspensa ou cancelada                | Regularize o pagamento ou renove a licença no portal.                       |
| 403  | Requisição sem token de licença                       | `licenceKey` vazio no build; confira o `local.properties`/variáveis de CI.   |
| 400  | Aparelho sem identificador estável                    | O aparelho não informou `ANDROID_ID`; verifique o ambiente do terminal.     |
| 429  | Muitas tentativas (rate limit)                        | Aguarde alguns instantes e tente novamente.                                 |

A mensagem exibida (`errorMessage`) é a retornada pelo servidor. Falhas de rede geram mensagens como "Sem conexão com a internet..." ou "Tempo esgotado ao conectar no servidor de licença...".

### Troubleshooting

- **`LICENCE_ERROR`**: a validação falhou; veja a `message` da exceção (ou o `errorMessage` do evento `licence`) e a tabela acima.
- **Funciona no debug mas não no aparelho novo**: verifique o limite de aparelhos da licença (o plano `debug` permite 2).
- **Trocou a chave e nada mudou**: as chaves são embutidas no build; rode `flutter clean` e gere o app novamente.
- **Sem internet**: o aparelho só funciona offline se tiver validado online nas últimas 72 horas e a licença não tiver vencido.

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
- SDK Stone `4.15.0`
- Comunicação via Platform Channels

---

## ⚙️ <a name="instalacao">Instalação</a>

### 1. `pubspec.yaml`

Adicione a dependência:

```yaml
dependencies:
  stone_payment_tech: ^3.1.0
```

E importe a biblioteca principal:

```dart
import 'package:stone_payment_tech/stone_payment_tech.dart';
```

> O import antigo `package:stone_payment_tech/stone_tech_payment.dart` continua funcionando (exporta a mesma API), mas prefira o novo.

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
        gertecGpos700 {
            dimension "model"
            versionNameSuffix "-gertecGpos700"
        }
        gertecGposSeries7 {
            dimension "model"
            versionNameSuffix "-gertecGposSeries7"
        }
        positivoSeriesL {
            dimension "model"
            versionNameSuffix "-positivoSeriesL"
        }
        ingenico {
            dimension "model"
            versionNameSuffix "-ingenico"
        }
        sunmi {
            dimension "model"
            versionNameSuffix "-sunmi"
        }
        sunmiSeriesP {
            dimension "model"
            versionNameSuffix "-sunmiSeriesP"
        }
        tectoySeriesT {
            dimension "model"
            versionNameSuffix "-tectoySeriesT"
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
  final Function(StoneTransactionModel?)? onTransaction;
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

Inicialize o plugin com o seu handler:

```dart
StoneTech.I.initPayment(handler: StoneHandler());
```

Ative primeiro o pinpad com seu StoneCode:

```dart
StoneTech.I.payment.activePinpad(appName: 'nome_do_seu_app', stoneCode: '12345');
```

Após ativar, você pode iniciar uma das transações disponíveis (valores em **centavos**):

```dart
StoneTech.I.payment.creditPayment(1250);
StoneTech.I.payment.creditPaymentParc(value: 1250, installment: 2);
StoneTech.I.payment.debitPayment(1250);
StoneTech.I.payment.voucherPayment(1250);
StoneTech.I.payment.pixPayment(
  amount: 1250,
  qrCodeAuthroization: '',
  qrCodeProviderid: '',
);
StoneTech.I.payment.cancelTransaction(idFromBase: 1);
StoneTech.I.payment.abortTransaction();
```

> 🔖 **Obs:** o SDK imprime automaticamente a via do consumidor.

### Retorno dos métodos

Todos os métodos do `StoneTech.I.payment` completam o `Future` (desde a 3.1.0; antes, em caso de sucesso, a maioria ficava pendente para sempre):

- **Pagamentos, ativação, estorno, cancelamento, abort, impressões e consultas** completam com `true` assim que a operação é **disparada** no SDK da Stone. O resultado da operação (aprovada/negada, terminal ativado, impressão concluída etc.) continua chegando pelos callbacks do `IStoneHandler` / `StoneStreamHandler`, como antes. Portanto, `true` **não** significa transação aprovada.
- `getPosSerialNumber()` e `getPosManufacture()` completam com o valor.
- Licença inválida: `PlatformException(code: 'LICENCE_ERROR')`. Falha ao disparar a operação: `PlatformException(code: 'ERROR')`. Os métodos que já tratavam exceção (`activePinpad`, `activePinpadWithCredentials`, `reversal`, `customPrinter`, `printFromBase64`, `printWrapPaper`) devolvem `false`.

Se o seu app fazia `await` em um desses métodos sem esperar retorno (ou usava `then`/`timeout` como contorno), revise o fluxo: o código após o `await` agora roda logo após o disparo.

Para só conferir a licença (sem chamar o SDK da Stone), use `await StoneTech.I.payment.checkLicence()`: `true` com licença válida; `PlatformException(code: 'LICENCE_ERROR')` caso contrário.

Comportamentos corrigidos na 3.1.0 que podem aparecer no app:

- `printerCurrentTransaction(printCustomerSlip: false)` não imprime mais a via do cliente (antes o argumento era ignorado e a via saía mesmo quando o usuário recusava).
- `getAllTransactions()` volta a responder em `onFinishedResponse` (`method: AllTransactions`, lista `stoneTransactions`); antes sempre falhava.
- Imagens em `customPrinter` (`PrinterType.image`) passam a ser impressas.
- O `IStoneTechHandler` informado em `initPayment` passa a receber os eventos (antes nunca recebia) e é mantido após `setHandler`.
- O evento `onChanged` (QRCode do PIX e opções Alimentação/Refeição) chega ao `IStoneHandler.onChanged`.

### Implementação de página

Abaixo segue um exemplo usando uma tela pré montada, onde você deve passar os parâmetros solicitados e a tela faz todo o fluxo de transação, e ao terminar lhe devolve um `StoneTransactionModel`.

```dart
 final result = await Navigator.push(
  context,
  MaterialPageRoute(
    builder: (context) => StoneStreamPage(
      stonePaymentParams: StoneTechPaymentParams(
        amount: 15,
        type: StoneTechPaymentType.debit,
        credentials: StoneTechCredentialsModel(
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
      stonePaymentParams: StoneTechPaymentParams(
        amount: 15,
        type: StoneTechPaymentType.debit,
        credentials: StoneTechCredentialsModel(
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
      params: StonePrinterPageParams(
        credentials: StoneTechCredentialsModel(
          stoneCode: 'StoneCode',
          appName: 'nome_do_seu_app',
        ),
        child: <Seu widget montando seu layout para impressão>,
      ),
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
| `onChanged`             | QRCode do PIX e opções Alimentação/Refeição (JSON com `method`) |
| `onLoading`             | Indicador de carregamento               |

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

### Exemplo: `PaymentOptions`

Quando uma transação requer seleção do tipo de cartão (alimentação/refeição), o seguinte objeto é retornado:

```json
{
  "method": "PaymentOptions",
  "options": ["Cartão Alimentação", "Cartão Refeição"],
  "message": "Selecione o tipo de cartão"
}
```

**Comportamento automático:**

- Ao usar `StoneStreamPage` ou `StoneStreamDialog`, o plugin automaticamente:
  1. Detecta quando `method = "PaymentOptions"` é recebido
  2. Exibe um dialog com as opções disponíveis
  3. Aguarda a seleção do usuário
  4. Envia a opção selecionada via `setPaymentOption`
  5. Continua o fluxo da transação normalmente

**Implementação manual:**
Se você estiver usando um handler customizado, você pode tratar o `PaymentOptions` manualmente:

```dart
@override
Future<void> onFinishedResponse(String message) async {
  final map = json.decode(message);
  if (map['method'] == 'PaymentOptions') {
    final options = List<String>.from(map['options'] ?? []);
    final messageText = map['message'] ?? '';

    // Exibir dialog customizado e aguardar seleção
    final selectedOption = await showDialog<String>(...);

    if (selectedOption != null) {
      await StoneTech.I.payment.setPaymentOption(option: selectedOption);
    }
  }
}
```

---

## 🧪 Testes

```bash
# Testes Dart (canal mockado): todos os métodos públicos (método/argumentos enviados,
# sucesso, ERROR e LICENCE_ERROR), eventos do nativo, modelos, stream e controller
flutter test

# Testes JVM do nativo: gate de licença por método, despacho/argumentos de cada método
# do canal, resposta única, mapeamentos do SDK, parser de impressão e JSON enviado ao Dart.
# Requer o example configurado (local.properties com licenceKey). Use JDK 17.
cd example/android
./gradlew :stone_payment_tech:testSunmiDebugUnitTest
# (qualquer flavor serve, ex.: testGertecGposSeries7DebugUnitTest)
```

A análise técnica completa (tabela por método, bugs e otimizações) fica em `doc/ANALISE.md` no repositório (não vai para o pub.dev).

## ❗ Observações importantes

- Para transações PIX, forneça `qrCodeAuthorization` e `qrCodeProviderid`.
- Para exibir QRCode (campo `method: QRCode`), o retorno virá em `message` como uma imagem em `Bitmap` convertida para `String`.
- O método `getAllTransactions()` pode ser usado para buscar transações anteriores para fins de estorno.
- Para transações que requerem seleção de tipo de cartão (alimentação/refeição), o campo `method: PaymentOptions` será retornado com uma lista de opções no campo `options`. O plugin automaticamente exibe um dialog para o usuário selecionar a opção desejada.

---

## 💡 Dica

A resposta **mais completa** da SDK Stone virá sempre no método `onFinishedResponse`. Use o campo `method` da resposta para identificar o tipo de evento:

```dart
void onFinishedResponseMonitor(StoneTransactionModel? stoneTransaction) {
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
      // Quando o método é PaymentOptions, o objeto retornado contém:
      // - method: "PaymentOptions"
      // - options: List<String> com as opções disponíveis (ex: ["Cartão Alimentação", "Cartão Refeição"])
      // - message: String com mensagem informativa
      //
      // O plugin automaticamente exibe um dialog para o usuário selecionar uma opção.
      // Após a seleção, a opção é enviada automaticamente via setPaymentOption.
      //
      // Se estiver usando StoneStreamPage ou StoneStreamDialog, o fluxo é automático.
      // Para implementação manual, use:
      // await StoneTech.I.payment.setPaymentOption(option: opcaoSelecionada);
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
