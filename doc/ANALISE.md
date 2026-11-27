# Análise técnica: stone_payment_tech 4.0.0

> 4.0.0: licença, cobrança e gate por operação removidos (projeto open source, MIT). Todas as operações chamam o SDK da Stone direto; não há chamada HTTP à JY Labtech. Esta análise foi feita na 3.1.0 e atualizada para a 4.0.0.

Documento interno (excluído do pacote pelo `.pubignore`). Cobre a superfície Dart, o MethodChannel `stone_payment_tech`, o nativo Java (`android/src/main`), os bugs encontrados, as otimizações e o que ainda depende de hardware.

## 1. Arquitetura e fluxo

```
Dart StonePayment.xxx()  ──invokeMethod──▶  StoneTechFlutterPlugin.onMethodCall
                                               └─ StoneTech.initPayment (glue Android)
                                                    └─ StoneMethodHandler.handle   (puro, testado na JVM)
                                                         1. método desconhecido → notImplemented
                                                         2. conhecido → MethodDispatcher.dispatch (main thread)
                                                              └─ StoneActions (fachada) → PaymentsPresenter → PaymentsUseCase / StonePrinter → SDK Stone
Resultado assíncrono: SDK callback → PaymentsFragment.onXxx → channel.invokeMethod("onXxx", json)
                      → IStoneHelper.callHandler → IStoneHandler (+ IStoneTechHandler) → StoneStreamHandler (stream)
```

- Cada chamada do canal é respondida **exatamente uma vez** (`ReplyOnce`): operações respondem `true` ao serem disparadas; getters respondem o valor; falha ao disparar responde `ERROR`.
- Não há EventChannel: os eventos voltam pelo mesmo MethodChannel (nativo → Dart).
- Valores monetários: o Dart recebe `int` em centavos e envia `String` (`value.toString()`); o nativo repassa a String ao `TransactionObject.setAmount`. Os componentes prontos (`StoneStreamController`) recebem `double` em reais e convertem com `(value * 100).toStringAsFixed(0)` (arredondamento correto, ex.: 10.29 → 1029, coberto por teste).

## 2. Tabela por método (Dart → canal → nativo)

Legenda da resposta: **op** = `true` ao disparar; o resultado chega por eventos (`onFinishedResponse` etc.). Erros: `ERROR` (exceção ao disparar), `notImplemented` (método desconhecido). "Dart: false" = o método Dart captura a exceção e devolve `false`; "Dart: lança" = propaga `PlatformException`.

| Método Dart | Método do canal | Handler nativo (StoneActions → caso de uso → SDK) | Resposta sucesso / erro | Testado? (Dart / JVM) | Problemas encontrados (status) |
|---|---|---|---|---|---|
| `reversal()` | `paymentReversal` | `reversal` → `onReversalTransaction` → `ReversalProvider` | op / Dart: false | sim / sim | — |
| `activePinpad(appName, stoneCode)` | `paymentActivePinpad` | `activate` → `initializeAndActivatePinpad` → `StoneStart.init` + `ActiveApplicationProvider.activate` (timeout 60 s) | op / Dart: false | sim / sim | — |
| `activePinpadWithCredentials(...)` | `paymentActivePinpadCredentials` | `activateWithCredentials` → `qrCodeKeys` ("Bearer " + token) → `StoneStart.init(keys)` | op / Dart: false | sim / sim | Envia `printCustomerSlip`, que o nativo ignora (inofensivo, mantido por compatibilidade) |
| `creditPayment(int centavos, ...)` | `paymentCredit` | `doTransaction(type=1)` → `initTransaction` → `PosTransactionProvider` | op / Dart: lança | sim / sim | — |
| `creditPaymentParc(value, installment, withInterest, ...)` | `paymentCreditParc` | `doTransaction(type=1)` com `InstalmentTransactionEnum` (2–13, com/sem juros) | op / Dart: lança | sim / sim | Parcelas fora de 1–13 viram à vista (comportamento do helper, documentado em teste) |
| `debitPayment(int, ...)` | `paymentDebit` | `doTransaction(type=2)` | op / Dart: lança | sim / sim | — |
| `voucherPayment(int, ...)` | `paymentVoucher` | `doTransaction(type=4)` | op / Dart: lança | sim / sim | — |
| `pixPayment(amount, qrCodeAuthroization, qrCodeProviderid, ...)` | `paymentPix` | `doTransaction(type=3, keys QRCode)` | op / Dart: lança | sim / sim | Typo `qrCodeAuthroization` na API pública (mantido; mudar seria quebrante) |
| `activeDebugLog(isDebugLog)` | `paymentActiveDebugLog` | `activeDebugLog` → `setDebugLog` | op (Future<void>) / Dart: lança | sim / sim | Recriava o presenter e perdia a transação em andamento sem liberar o anterior (corrigido) |
| `getPosSerialNumber()` | `paymentGetSerialNumber` | `Stone.getPosAndroidDevice().getPosAndroidSerialNumber()` (fallback `Build.getSerial`) | String / Dart: lança | sim / sim | — |
| `getPosManufacture()` | `paymentGetManufacture` | `Stone.getPosAndroidDevice().getPosAndroidManufacturer()` | String ou null / Dart: lança | sim / sim | Devolvia `null` após a ativação (flag `isInitialized` só era ligada no débito sem chaves) (corrigido) |
| `abortTransaction()` | `paymentAbort` | `abortCurrentPosTransaction` → `abortPayment` | op / Dart: lança | sim / sim | — |
| `abortPIXTransaction()` | `paymentAbortPix` | `abortPIXTransaction` → `cancelTransactionPIX` (`CancellationProvider`) + `abortPayment` | op / Dart: lança | sim / sim | `CancellationProvider(null)` sem transação corrente (agora erro explícito "Nenhuma transação PIX em andamento") |
| `abortActivation()` | `paymentAbortActivation` | `abortActivation` (limpa estado e timeout; o SDK não tem abort de ativação) | op / Dart: lança | sim / sim | — |
| `printerCurrentTransaction(printCustomerSlip)` | `paymentPrinterTransaction` | `printerCurrentTransaction` → `PosPrintReceiptProvider(CLIENT)` | op / Dart: lança | sim / sim | `printCustomerSlip` era ignorado: imprimia mesmo com `false` (corrigido) |
| `paymentPrinterTransactionKey(transactionKey)` | `paymentPrinterTransactionKey` | busca por initiatorTransactionKey no `TransactionDAO` → impressão CLIENT | op / Dart: lança | sim / sim | Chave enviada (`transactionKey`) ≠ lida (corrigido na etapa anterior; aceita os dois nomes) |
| `cancelTransaction(idFromBase)` | `paymentCancelTransaction` | `cancelTransaction` → `TransactionDAO.findTransactionWithId` → `CancellationProvider` | op / Dart: lança | sim / sim | NPE no `onError` (usava `currentTransactionObject`, nulo no cancelamento por id) (corrigido) |
| `getAllTransactions()` | `paymentGetAllTransactions` | `getAllTransactions` → `getAllTransactionsOrderByIdDesc` → `ActionResult.buildAllTransactions` | op / Dart: lança | sim / sim | Lista nula → NPE em toda chamada (e o catch repetia o NPE); mesmo objeto em todos os itens (corrigido) |
| `setPaymentOption(option)` | `paymentOptions` | `setPaymentOption` → `setTransactionTypeSelected(index)` | op / Dart: lança | sim / sim | NPE sem lista de opções; índice -1 enviado ao SDK se a opção não existir (corrigido) |
| `getTransactionByInitiatorTransactionKey(key)` | `paymentGetTransactionByInitiatorTransactionKey` | `TransactionDAO.findTransactionWithInitiatorTransactionKey` → `onFinishedResponse` | op / Dart: lança | sim / sim | NPE com `userModel` nulo (terminal não ativado nesta sessão) e resposta montada duas vezes (corrigido) |
| `customPrinter(StonePrinterParams)` | `paymentCustomPrinter` | `PrinterParams.parse` → `PosPrintProvider.addLine/addBase64Image` | op / Dart: false | sim / sim | Dart envia `type: "image"`, nativo só imprimia `"base64"` (imagens nunca saíam) (corrigido) |
| `printFromBase64(base64)` | `paymentPrintBase64` | `PosPrintProvider.addBase64Image` | op / Dart: false | sim / sim | — |
| `printWrapPaper(lines)` | `paymentPrintWrapPaper` | `PosPrintProvider.addLine("\n")` × lines | op / Dart: false | sim / sim | — |
| `setHandler(handler)` | — | — | — | sim / — | Descartava o `IStoneTechHandler` informado no `create` (corrigido) |
| `StoneTech.I`, `initPayment`, `payment` | — | — | — | sim / — | `StonePayment` é singleton estático: o primeiro `channel`/handler vence (por projeto; use `setHandler`) |
| — (enum sem método) | `paymentLastTransaction`, `paymentRefund`, `paymentIsAuthenticated` | não existem no nativo | — | notImplemented | — / sim | Valores mortos de `PaymentTypeCall`; mantidos (remover seria quebrante) |


### Eventos nativo → Dart

| Evento (`invokeMethod`) | Argumento | `IStoneHandler` | `IStoneTechHandler` | `StoneStreamHandler` | Problemas |
|---|---|---|---|---|---|
| `onTransactionSuccess` | `true` | `onTransactionSuccess()` | `onTransactionSuccess()` | log | — |
| `onError` | JSON `BasicResult` ou texto | `onError(String)` | `onError(StoneTechResponse)` | status por `method` | `IStoneTechHandler` nunca recebia (tipo lido do conteúdo); `null` gerava TypeError (corrigidos) |
| `onMessage` | texto | `onMessage(String)` | `onMessage(String)` | `StoneStatusTechMessage` | `null` (ex.: `getMessageFromAuthorize()`) gerava TypeError (corrigido) |
| `onChanged` | JSON (`QRCode`, `PaymentOptions`) | `onChanged(String)` | `onChanged(StoneTechResponse)` | ignorado (usa `onAuthProgress`) | Não estava no mapa `String.handler`: lançava "Not Implemented" (corrigido) |
| `onFinishedResponse` | JSON `ActionResult`/`BasicResult` | `onFinishedResponse(String)` + `_checkPinpadActivated` | `onFinishedResponse(StoneTransactionModel)` | `active`/`transaction`/`cancel`/`printerTransaction`/`PaymentOptions` | NPE em `active` sem `message`; erro de ativação mostrava texto genérico em vez de `errorMessage` (corrigidos) |
| `onAuthProgress` | JSON ou texto | `onAuthProgress(String)` | `onAuthProgress(StoneTechResponse)` | idem `onFinishedResponse` | — |
| `onLoading` | bool | `onLoading(bool)` | `onLoading(bool)` | ignorado | Não é emitido pelo nativo atual |
| `onAbortedSuccessfully`, `writeToFile`, `onTransactionInfo` | — | — | métodos próprios | — | Não emitidos pelo nativo atual (contrato herdado) |

## 3. Bugs encontrados e corrigidos (esta etapa)

Nativo:
1. (removido na 4.0.0: dizia respeito à licença)
2. `getAllTransactions` sempre falhava (NPE na lista nula de `ActionResult`, repetida no `catch`) e reaproveitava o mesmo `StoneTransaction`.
3. `printerCurrentTransaction` ignorava `printCustomerSlip` (o "Não" do diálogo de via do cliente imprimia mesmo assim).
4. `customPrinter` não imprimia imagens (`image` × `base64`).
5. NPEs: `setPaymentOption` (lista nula / índice -1), `getTransactionByInitiatorTransactionKey` (`userModel` nulo), `onError` do cancelamento por id, `CancellationProvider(null)` no abort PIX, `buildResponseStoneTransaction(null)` quando a transação aprovada não está no banco local, `getListOfErrors()` nulo na impressora, `Bitmap` nulo no QRCode, `Action`/`ErrorsEnum` nulos.
6. `getPosManufacture` devolvia `null` após ativação.
7. `activeDebugLog` recriava o presenter (perdia `posTransactionProvider`/`userModel`, abort deixava de funcionar, timeout do anterior ficava pendurado).
8. (removido na 4.0.0: dizia respeito à licença)
9. `null` enviado ao Dart em `onMessage`/`onError`/… (`PaymentsFragment.nonNull`).
10. `onDetachedFromEngine` sem proteção contra nulos; chamadas após o detach agora respondem `ERROR` em vez de NPE.

Dart:
11. `IStoneTechHandler` nunca recebia eventos (`_stSmartHandler` fazia `switch` no conteúdo da mensagem).
12. `onChanged` não reconhecido (`StringPaymentHandlerExt` sem a chave).
13. Argumento nulo → `TypeError`; `onLoading(null)`.
14. NPE em `_checkPinpadActivated` (`message!`).
15. `setHandler` descartava o `IStoneTechHandler`.
16. `StonePrinterParams.fromMap` com cast `List<int>` (sempre falhava); `StonePrinter.fromMap` não aceitava `base64`.
17. Parse frágil: `as int` em `result`/`idFromBase`, `json.decode` sem `try` em `convertToStone*`.
18. `StoneTechPrinterHandler` lançava `UnimplementedError` em 4 callbacks.
19. `StonePrinterController` somava um listener por `initialize` (onSuccess repetido, vazamento).
20. `StoneStreamController` disparava pagamentos/abort/impressão sem tratar o `Future` → `PlatformException` virava erro assíncrono não tratado.
21. Erro de ativação no stream mostrava texto genérico em vez do `errorMessage` do nativo.

Exemplo:
22. Flavor `gertecGposSeries7` ausente no `example/android/app/build.gradle` (o plugin declara 7 flavors e o app só 6), por isso o `flutter build apk --debug` não gerava esse APK. Adicionado o flavor e `app/src/gertecGposSeries7/AndroidManifest.xml` (igual ao do GPOS700, mesmo SDK `stone-sdk-posandroid-gertec`).

## 4. Otimizações (todas cobertas por testes antes/depois)

| Otimização | Por quê |
|---|---|
| `activeDebugLog` só troca a flag | Não recria presenter/caso de uso/printer/fragment |
| `payment.dispose()` no detach | Libera o presenter/caso de uso da engine desanexada |
| `PrinterParams` (parser puro com Gson estático) | Remove `new Gson()` por impressão e permite testar o formato do Dart |
| `PaymentsPresenter.qrCodeKeys` | Remove duas cópias do mesmo `HashMap` com inicializador anônimo (classe interna por chamada) |
| Código morto removido | imports duplicados e campos sem uso do presenter (`gson/hasAborted/countPassword/isDebugLog`) |



## 5. Testes

| Suite | Antes (3.0) | 3.1.0 | 4.0.0 (sem licença) |
|---|---|---|---|
| JVM (`testSunmiDebugUnitTest`) | 62 | 101 | 45 |
| Dart (`flutter test`) | 8 | 160 | 125 |

Na 4.0.0 saíram os testes de licença (JVM: `LicenceRules`/`LicenceConfig`/`CheckLicenceTech`/`LicenceHttpResult`; Dart: `licence_test.dart` e os casos `LICENCE_ERROR`/`checkLicence`); os testes de cada método foram mantidos.

JVM: `StoneMethodHandlerTest` (despacho direto ao SDK para os 23 métodos, resposta única, erro do SDK, `notImplemented`, inclusive `checkLicence` removido), `MethodDispatcherTest` (argumentos exatos de cada método → fachada, tipos de pagamento, argumentos obrigatórios), `StoneHelperTest` (tipos, parcelas 1–13, mensagens, erros do SDK, opções, chaves QRCode), `PrinterParamsTest`, `ResultJsonTest` (JSON enviado ao Dart, `buildAllTransactions`).

Dart: `payment_methods_test.dart` (cada método público: (a) método + argumentos exatos, inclusive centavos e defaults; (b) retorno de sucesso; (c) `ERROR` sem sucesso indevido), `handler_events_test.dart` (eventos nativo → `IStoneHandler`/`IStoneTechHandler`, `setHandler`, ativação), `models_test.dart` (parsers com payload realista, campos nulos, enums/extensões), `stream_test.dart` (`StoneStreamHandler`, `StoneTechPrinterHandler`, `StoneStreamController`) e `method_result_test.dart`.

## 6. Pendências (decisão do dono / hardware)

- **Fluxos do SDK com hardware**: `PosTransactionProvider`, `ActiveApplicationProvider`, `CancellationProvider`, `ReversalProvider` e impressão só rodam num POS Stone. Os testes cobrem até a fachada `StoneActions` e as partes puras; o comportamento dos callbacks do SDK precisa de teste manual por fabricante (incluindo o GPOS Series 7, agora com APK gerado).
- **`initiatorTransactionKey` vazio**: quando o app não envia chave, a busca no `TransactionDAO` após a aprovação depende de o SDK preencher a chave; agora há fallback para a própria transação, mas vale confirmar no aparelho que os dados do recibo vêm completos.
- **Erros que viram `false`**: `activePinpad*`, `reversal`, `customPrinter`, `printFromBase64`, `printWrapPaper` engolem a `PlatformException` e devolvem `false`; os demais propagam. Mudar isso é quebrante; sugerimos padronizar numa próxima major.
- **API com typos/legado**: `qrCodeAuthroization`, `PaymentTypecredit`, valores mortos em `PaymentTypeCall` (`lastTransaction`, `refund`, `pinpadAuthenticated`). Podem ser `@Deprecated` numa próxima versão.
- **`StonePayment` singleton estático**: o primeiro `channel`/handler vence; `IStoneHandler` é trocado via `setHandler`. Comportamento mantido.
- **Ativação**: o SDK não oferece abort real de `ActiveApplicationProvider`; `abortActivation` só limpa o estado e avisa o Dart (o diálogo do SDK pode continuar até terminar).
- **Valores em `double`** nos componentes prontos (`StoneTechPaymentParams.amount`): a conversão para centavos está correta, mas uma API com `int` centavos seria mais segura (aditiva, se desejado).
