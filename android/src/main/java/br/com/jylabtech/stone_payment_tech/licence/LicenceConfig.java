package br.com.jylabtech.stone_payment_tech.licence;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Configuração da validação de licença. A URL de validação é travada no
 * servidor oficial da JY Labtech (jylabtech_core): valores de
 * {@code licenceInternalKey} que apontem para outro servidor são ignorados.
 *
 * Classe pura (sem dependências Android) para ser testável na JVM.
 */
public final class LicenceConfig {

    public static final String LICENCE_HOST = "portal-api.jylabtech.com.br";
    public static final String LICENCE_PATH_PREFIX = "/pos-payments/licence/check";
    public static final String LICENCE_CHECK_URL = "https://" + LICENCE_HOST + LICENCE_PATH_PREFIX;

    private LicenceConfig() {}

    /**
     * Resolve a URL efetiva de validação a partir do {@code licenceInternalKey}
     * (URL pura ou base64). Só aceita https no host oficial com path de
     * validação de licença; em qualquer outro caso devolve {@link #LICENCE_CHECK_URL}.
     */
    public static String resolveLicenceUrl(String licenceInternalKey) {
        String candidate = decodeCandidate(licenceInternalKey);
        if (candidate != null && isOfficialUrl(candidate)) {
            return candidate;
        }
        return LICENCE_CHECK_URL;
    }

    /**
     * Indica se o valor informado (URL pura ou base64) aponta para o servidor
     * oficial. Usado para não reaproveitar caches validados em outro servidor.
     */
    public static boolean isTrustedLicenceInternalKey(String licenceInternalKey) {
        String candidate = decodeCandidate(licenceInternalKey);
        return candidate != null && isOfficialUrl(candidate);
    }

    private static String decodeCandidate(String licenceInternalKey) {
        if (licenceInternalKey == null) {
            return null;
        }
        String value = licenceInternalKey.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.toLowerCase(Locale.US).contains("http")) {
            return value;
        }
        byte[] decoded = decodeBase64(value);
        if (decoded == null) {
            return null;
        }
        String text = new String(decoded, StandardCharsets.UTF_8).trim();
        return text.isEmpty() ? null : text;
    }

    private static boolean isOfficialUrl(String value) {
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
            if (uri.getRawUserInfo() != null) {
                return false;
            }
            String host = uri.getHost();
            if (host == null || !LICENCE_HOST.equals(host.toLowerCase(Locale.US))) {
                return false;
            }
            int port = uri.getPort();
            if (port != -1 && port != 443) {
                return false;
            }
            if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
                return false;
            }
            String path = uri.getPath();
            if (path == null || !path.startsWith(LICENCE_PATH_PREFIX)) {
                return false;
            }
            String rest = path.substring(LICENCE_PATH_PREFIX.length());
            return rest.isEmpty() || rest.startsWith("/");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Decodificador base64 (padrão e URL-safe) sem dependências Android,
     * compatível com minSdk 22 (java.util.Base64 exige API 26).
     * Retorna null se o valor não for base64 válido.
     */
    static byte[] decodeBase64(String input) {
        if (input == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        int count = 0;
        boolean padding = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\n' || c == '\r' || c == ' ' || c == '\t') {
                continue;
            }
            if (c == '=') {
                padding = true;
                continue;
            }
            if (padding) {
                return null;
            }
            int v;
            if (c >= 'A' && c <= 'Z') v = c - 'A';
            else if (c >= 'a' && c <= 'z') v = c - 'a' + 26;
            else if (c >= '0' && c <= '9') v = c - '0' + 52;
            else if (c == '+' || c == '-') v = 62;
            else if (c == '/' || c == '_') v = 63;
            else return null;
            buffer = (buffer << 6) | v;
            bits += 6;
            count++;
            if (bits >= 8) {
                bits -= 8;
                out.write((buffer >> bits) & 0xFF);
            }
        }
        if (count == 0 || count % 4 == 1) {
            return null;
        }
        return out.toByteArray();
    }
}
