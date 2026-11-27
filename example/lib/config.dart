import 'package:stone_payment_tech/stone_payment_tech.dart';

/// Configuração do app de exemplo lida de `--dart-define` (nada fica no código).
///
/// ```sh
/// flutter run --flavor sunmi \
///   --dart-define=STONE_CODE=<seu stone code> \
///   --dart-define=APP_NAME="Meu App" \
///   --dart-define=QRCODE_AUTHORIZATION=<token PIX> \
///   --dart-define=QRCODE_PROVIDER_ID=<provider id PIX>
/// ```
abstract final class ExampleConfig {
  /// Stone Code do estabelecimento (obrigatório para ativar o terminal).
  static const stoneCode = String.fromEnvironment('STONE_CODE');

  /// Nome do app enviado na ativação.
  static const appName = String.fromEnvironment('APP_NAME', defaultValue: 'Stone Payment Tech');

  /// Credenciais do PIX (opcionais; só para pagamentos PIX).
  static const qrCodeAuthorization = String.fromEnvironment('QRCODE_AUTHORIZATION');
  static const qrCodeProviderid = String.fromEnvironment('QRCODE_PROVIDER_ID');

  static bool get hasStoneCode => stoneCode.isNotEmpty;

  static StoneTechCredentialsModel get credentials => StoneTechCredentialsModel(
        stoneCode: stoneCode,
        appName: appName,
        qrCodeAuthorization: qrCodeAuthorization.isEmpty ? null : qrCodeAuthorization,
        qrCodeProviderid: qrCodeProviderid.isEmpty ? null : qrCodeProviderid,
      );
}
