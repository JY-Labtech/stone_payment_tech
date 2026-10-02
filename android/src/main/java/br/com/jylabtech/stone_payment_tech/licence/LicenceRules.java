package br.com.jylabtech.stone_payment_tech.licence;

import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Regras puras (sem Android) da validação de licença. Toda decisão de
 * "liberar ou não" passa por aqui para ser testável na JVM.
 */
public final class LicenceRules {

    public static final long LOCAL_CACHE_MAX_AGE_MS = TimeUnit.DAYS.toMillis(7);
    public static final long CLOCK_SKEW_TOLERANCE_MS = TimeUnit.MINUTES.toMillis(5);

    public static final String MSG_INVALID_RESPONSE = "Resposta inválida do servidor de licença";
    public static final String MSG_NOT_USABLE = "Licença vencida, suspensa ou cancelada. Verifique sua licença no portal da JY Labtech.";

    private LicenceRules() {}

    /**
     * A licença só pode ser usada se tiver validade definida e futura e não
     * estiver vencida, inativa ou cancelada.
     */
    public static boolean isLicenceUsable(CheckLicenceEntity entity, Date now) {
        if (entity == null || now == null) {
            return false;
        }
        Date expiredAt = entity.getExpiredAt();
        return expiredAt != null
                && expiredAt.after(now)
                && !entity.isExpired()
                && !entity.isInactive()
                && !entity.isCanceled();
    }

    /**
     * O cache offline só vale se a licença for utilizável, tiver sido salva
     * há no máximo 7 dias (e não no futuro), for da mesma chave e tiver sido
     * validada no mesmo servidor oficial.
     */
    public static boolean isCacheValid(CheckLicenceEntity cache, String licenceKey, String licenceInternalKey, Date now) {
        if (!isLicenceUsable(cache, now)) {
            return false;
        }
        Date savedAt = cache.getSavedLocalDatabaseAt();
        if (savedAt == null) {
            return false;
        }
        long age = now.getTime() - savedAt.getTime();
        if (age < -CLOCK_SKEW_TOLERANCE_MS) {
            return false; // salvo "no futuro": relógio adulterado
        }
        if (age > LOCAL_CACHE_MAX_AGE_MS) {
            return false;
        }
        if (licenceKey == null || licenceKey.isEmpty() || !licenceKey.equals(cache.getLicenceEncrypt())) {
            return false;
        }
        String saved = cache.getLicenceInternalKey();
        if (!LicenceConfig.isTrustedLicenceInternalKey(saved)) {
            return false;
        }
        return LicenceConfig.resolveLicenceUrl(saved).equals(LicenceConfig.resolveLicenceUrl(licenceInternalKey));
    }

    /**
     * Decide o resultado de uma resposta HTTP do servidor de licença.
     * Só HTTP 200 com licença utilizável é sucesso.
     */
    public static LicenceResult evaluateHttpResponse(int statusCode, String body, Date now) {
        if (statusCode == 200) {
            CheckLicenceEntity entity = CheckLicenceEntity.fromJson(body);
            if (entity == null || entity.getExpiredAt() == null) {
                return LicenceResult.error(MSG_INVALID_RESPONSE);
            }
            if (!isLicenceUsable(entity, now)) {
                return LicenceResult.error(MSG_NOT_USABLE);
            }
            return LicenceResult.success(entity);
        }
        return LicenceResult.error(extractErrorMessage(statusCode, body));
    }

    static String extractErrorMessage(int statusCode, String body) {
        String fallback = "Erro ao validar licença (HTTP " + statusCode + ")";
        if (body == null || body.trim().isEmpty()) {
            return fallback;
        }
        try {
            JSONObject json = new JSONObject(body);
            if (json.has("message") && !json.isNull("message")) {
                String message = json.optString("message", "").trim();
                if (!message.isEmpty()) {
                    return message;
                }
            }
        } catch (Exception ignored) {
            // corpo não é JSON
        }
        return fallback;
    }

    public static String formatConnectionError(Throwable error) {
        if (error == null) {
            return "Erro de conexão com o servidor de licença";
        }
        if (error instanceof UnknownHostException) {
            return "Sem conexão com a internet. Verifique a rede do terminal e tente novamente.";
        }
        if (error instanceof SocketTimeoutException) {
            return "Tempo esgotado ao conectar no servidor de licença. Verifique sua conexão.";
        }
        if (error instanceof IOException) {
            return "Falha na comunicação com o servidor de licença. Verifique sua conexão.";
        }
        String message = error.getMessage();
        if (message == null || message.isEmpty()) {
            return "Erro ao validar a licença";
        }
        return message;
    }
}
