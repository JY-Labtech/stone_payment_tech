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
