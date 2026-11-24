## Não lançado

- Licença conforme o contrato do jylabtech_core (JYN-234): o cache offline vale por no máximo 72 horas (antes 7 dias) e só é usado quando o servidor não responde (sem internet, timeout, 408/429, 5xx ou resposta ilegível).
- A primeira operação depois de abrir o app consulta o servidor, e a licença é revalidada a cada 4 horas de uso. O atalho sem rede do gate (e a 2ª barreira do caso de uso) só libera pelo cache quando nenhuma consulta é devida.
- Recusa do servidor (cliente bloqueado, licença vencida/cancelada/suspensa, não encontrada, aparelho desativado) apaga o cache e bloqueia na hora; `reason: customer_blocked` exibe "Cliente bloqueado. Entre em contato com a JY Labtech.".
- Após falha de rede, espera 5 minutos antes de tentar o servidor de novo (usa o cache), para não travar cada operação offline no timeout.

## 3.1.0

- **Validação de licença mais rígida.** A URL de validação fica travada no servidor oficial (`https://portal-api.jylabtech.com.br/pos-payments/licence/check`): o `licenceInternalKey` passa a ser opcional e qualquer valor fora desse servidor (inclusive o antigo do Cloud Run, http sem TLS ou outro host) é ignorado; o build não exige mais `licenceInternalKey`
- Só libera o uso com licença válida e ativa: HTTP 200 com licença vencida, inativa (`isInactive`), cancelada (`isCanceled`), sem `expiredAt` ou com JSON inválido agora é erro e não é salvo no cache
- Removido o fallback que inventava validade de 1 ano quando o servidor não enviava `expiredAt` válido
- Gate de licença em todos os métodos do canal: licença inválida devolve `PlatformException(code: 'LICENCE_ERROR')` e o SDK da Stone não é chamado
- Cache offline só vale se for da mesma chave, validado no servidor oficial, salvo há no máximo 7 dias e não "no futuro" (relógio adulterado, tolerância de 5 min); caso contrário é apagado e a licença é revalidada online
- A validação online roda em background (sem bloquear a main thread), com timeouts de 15 s
- Erros HTTP sem corpo JSON exibem `Erro ao validar licença (HTTP <código>)`
- A chave de licença deixou de ser impressa no logcat
- Testes: JVM (JUnit) da validação de licença no nativo e testes Dart do canal com `LICENCE_ERROR`
- Mensagens amigáveis para falhas de rede/DNS na validação de licença
- Fallback offline: usa cache local válido quando o servidor está inacessível
- Timeout de conexão (15s) na requisição de licença
- Validação de licença migrada para o jylabtech_core: novo `licenceInternalKey` (`https://portal-api.jylabtech.com.br/pos-payments/licence/check`, em base64 ou URL pura). Atualizada a constante `licenceCheck`
- Fallback offline respeita o limite de 7 dias desde a última validação online (não renova mais o cache sem consultar o servidor)
- Mensagem de erro de licença usa o `message` do servidor; respostas sem corpo JSON exibem o código HTTP
- **Fix: os métodos do canal agora sempre completam o `Future`.** Antes, em caso de sucesso, a maioria (pagamentos, `activePinpad*`, `reversal`, `abort*`, impressões, consultas, `cancelTransaction`, `setPaymentOption`, `activeDebugLog`) nunca respondia e o `Future` ficava pendente para sempre. Agora cada chamada é respondida exatamente uma vez: `true` assim que a operação é disparada no SDK (o resultado continua chegando pelos callbacks/streams), o valor nos getters, `PlatformException(code: 'ERROR')` se o disparo falhar e `LICENCE_ERROR` com licença inválida. Atenção: código após `await` nesses métodos passa a executar logo após o disparo
- Fix: `paymentPrinterTransactionKey` enviava `transactionKey`, mas o nativo lia `paymentPrinterTransactionKey` (chave nula); o nativo aceita os dois nomes
- Fix: falha ao salvar o cache local da licença (EncryptedSharedPreferences) não bloqueia mais a operação quando o servidor confirmou licença válida e ativa; o erro é logado e a próxima operação revalida online
- Biblioteca principal renomeada para `package:stone_payment_tech/stone_payment_tech.dart` (corrige o aviso do pub sobre o nome do arquivo). `stone_tech_payment.dart` continua exportando a mesma API por compatibilidade
- Testes JVM do despacho do canal (resposta única por chamada) e do save de cache com falha; testes Dart do retorno dos métodos
- README: novo portal de licenças (portal.jylabtech.com.br), plano debug de cortesia, limite por dispositivo, cache offline, tabela de erros e troubleshooting; exemplos de código alinhados à API atual
- Novo `StonePayment.checkLicence()`: valida a licença sem chamar o SDK. O método `checkLicence` do canal existia no nativo, mas o plugin só repassava métodos iniciados por `payment` e ele respondia `notImplemented`
- Fix `IStoneTechHandler`: o tipo do evento era lido do conteúdo da mensagem (e não do nome do método do canal), então o handler nunca recebia eventos; agora recebe todos, já convertidos, e é mantido após `setHandler`
- Fix evento `onChanged` (QRCode do PIX e opções Alimentação/Refeição) não era reconhecido no Dart e lançava "Not Implemented"
- Fix argumentos nulos vindos do nativo (`onMessage`/`onError`…) causavam `TypeError` no handler; o nativo também não envia mais `null`
- Fix NPE no Dart ao receber erro de ativação (`method: active` sem `message`)
- Fix `getAllTransactions`: sempre falhava (lista nula no nativo) e repetia o mesmo item; agora devolve uma entrada por transação
- Fix `printerCurrentTransaction(printCustomerSlip: false)` imprimia a via do cliente mesmo assim (argumento ignorado)
- Fix `customPrinter` com `PrinterType.image`: o Dart envia `image` e o nativo só aceitava `base64`; aceita os dois
- Fix `StonePrinterParams.fromMap/fromJson` (cast para `List<int>` sempre falhava) e parse de modelos mais tolerante (números como double/string, JSON inválido)
- Fix NPEs no nativo: `setPaymentOption` sem opções, `getTransactionByInitiatorTransactionKey` sem terminal ativado (e resposta montada duas vezes), erro no cancelamento por `idFromBase`, abort PIX sem transação, transação aprovada sem registro local, impressora sem lista de erros
- Fix `getPosManufacture` devolvia `null` após a ativação do terminal
- Fix `activeDebugLog` recriava o presenter e perdia a transação em andamento (o abort deixava de funcionar)
- Fix 2ª barreira de licença no caso de uso recusava a operação quando o servidor validou a licença mas o cache local não pôde ser salvo
- Fix `StonePrinterController` acumulava um listener do stream a cada `initialize` (onSuccess repetido); `StoneTechPrinterHandler` lançava `UnimplementedError` em eventos sem ação; `StoneStreamController` deixava erros do canal como exceção assíncrona não tratada
- Exemplo: flavor `gertecGposSeries7` não estava declarado no `example/android/app/build.gradle` (o APK desse modelo não era gerado)
- Otimização: um único `CheckLicenceTech`/`EncryptedSharedPreferences` por engine (antes MasterKey + prefs eram recriados a cada leitura e havia três instâncias), sem releitura do cache durante o despacho; gate de licença extraído para `StoneMethodHandler` (testável) e executor/handler liberados no `onDetachedFromEngine`
- Testes: 101 JVM (antes 62) e 160 Dart (antes 8), cobrindo todos os métodos do canal nos dois lados (argumentos, sucesso, `ERROR` e `LICENCE_ERROR`); análise técnica em `doc/ANALISE.md`

## 3.0.1

- Fix validação de licença: confia na resposta HTTP 200 do servidor e persiste cache local corretamente
- Fix licenças unlock/gratuitas que falhavam após validação no servidor por `isExpired`/`expiredAt` desatualizados no banco
- Fix parsing de datas ISO 8601 (`Z` e milissegundos) na entidade de licença
- Fix comparação de chave local (`licenceEncrypt` ou `licenceKey`)
- Fix fall-through do handler de stream no evento `licence`
- Fix estado de ativação preso quando a validação de licença falha antes do terminal ativar
- Evita `RuntimeException` ao limpar cache local de licença

## 3.0.0

- Update Stone SDK version 4.15.0

## 2.2.1

- Add novos getters para a classe StoneQRCodeTransaction
  ⚠️ Breaking Changes:
- Alterar classe Payment => StonePayment
- Alterar casse StoneStatus => StoneStatusTech
- Alterar classe StoneResponse => StoneTechResponse
- Alterar classe StoneCredentialsModel => StoneTechCredentialsModel
- Alterar classe StonePrinterHandler => StoneTechPrinterHandler
- Alterar classe StoneOptions => StoneTechOptions
- Alterar classe StonePaymentType => StoneTechPaymentType
- Alterar classe StonePaymentParams => StoneTechPaymentParams

## 2.1.1

- Add suporte automático para PaymentOptions
- Implementado fluxo automático de seleção de tipo de cartão (alimentação/refeição) em StoneStreamPage e StoneStreamDialog
- Quando recebe method = "PaymentOptions", o plugin automaticamente exibe um dialog com as opções disponíveis
- Após seleção do usuário, a opção é enviada automaticamente via setPaymentOption
- Add novos getters para a classe StoneQRCodeTransaction

## 2.1.0

- Add suporte automático para PaymentOptions
- Implementado fluxo automático de seleção de tipo de cartão (alimentação/refeição) em StoneStreamPage e StoneStreamDialog
- Quando recebe method = "PaymentOptions", o plugin automaticamente exibe um dialog com as opções disponíveis
- Após seleção do usuário, a opção é enviada automaticamente via setPaymentOption

## 2.0.3

- Fix licence validator

## 2.0.2+1

- Add Flavor sunmiSeriesP and gertecGposSeries7
- Fix android manifest

## 2.0.2

- Add Flavor sunmiSeriesP and gertecGposSeries7

## 2.0.1

- Add Flavor sunmiSeriesP and gertecGposSeries7

## 2.0.0

- Update Stone SDK to 4.13.0
- Gradle 8.12+
- Kotlin 2.1.21

## 1.1.2

- Fix class StoneCustomPrinterController

## 1.1.1

- Create class StoneCustomPrinterController

## 1.1.0

- Update Stone SDK to 4.12.0

## 🔥 1.0.0+1 – Breaking Change

⚠️ Attention: In this version, there is an important change in the way payments are initialized.

🛠️ What changed:

Breaking Change: The initPayment method no longer requires the licenceKey to be passed as a parameter.

It is now mandatory to define the following fields in local.properties:

licenceKey → main licence key

licenceInternalKey → internal licence key

This means that any code that previously passed licenceKey directly to initPayment will need to be updated.

## 0.0.7

- Fix check payment approved

## 0.0.6

- Add StonePrinterPage

## 0.0.5

- Fix page and dialog base

## 0.0.4

- Add StoneStreamDialog
- Add StoneStreamPage

## 0.0.3

- Fix method check licence

## 0.0.2

- Add function wipe licence local database

## 0.0.1

- Initial release
