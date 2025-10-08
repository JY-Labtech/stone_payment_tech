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
