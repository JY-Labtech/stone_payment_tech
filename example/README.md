# stone_payment_tech_example

App de exemplo do plugin [`stone_payment_tech`](../README.md): tela de pagamento pronta, dialog de pagamento, impressão via base64 e ativação manual do terminal.

## Configuração

1. Token do PackageCloud da Stone (o SDK é privado). Em `example/android/local.properties`:

   ```properties
   packageCloudReadToken=<SEU_TOKEN_DE_LEITURA_DA_STONE>
   ```

   ou `export PACKAGECLOUD_READ_TOKEN=<SEU_TOKEN_DE_LEITURA_DA_STONE>`.

2. Stone Code e credenciais por `--dart-define` (nada fica no código):

   | `--dart-define` | Obrigatório | Uso |
   | --- | --- | --- |
   | `STONE_CODE` | sim | Stone Code para ativar o terminal. Sem ele o app mostra um aviso na tela. |
   | `APP_NAME` | não | Nome do app na ativação (padrão `Stone Payment Tech`). |
   | `QRCODE_AUTHORIZATION` | só PIX | Token do PIX fornecido pela Stone. |
   | `QRCODE_PROVIDER_ID` | só PIX | Provider id do PIX fornecido pela Stone. |

3. (Opcional) Assinatura do fabricante: crie `example/android/key.properties` com `positivoFile`, `positivoAlias`, `positivoPassword` e `positivoStorePassword`. Sem ele, o debug usa a chave de debug padrão.

## Rodando

Escolha o flavor da maquininha (`sunmi`, `sunmiSeriesP`, `positivoSeriesL`, `gertecGpos700`, `gertecGposSeries7`, `ingenico`, `tectoySeriesT`):

```bash
flutter run --flavor sunmi \
  --dart-define=STONE_CODE=<SEU_STONE_CODE> \
  --dart-define=APP_NAME="Meu App"

flutter build apk --debug --flavor sunmi --dart-define=STONE_CODE=<SEU_STONE_CODE>
```
