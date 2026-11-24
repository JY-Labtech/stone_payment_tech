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

    /**
     * Validade máxima do cache offline sem nova consulta bem-sucedida ao servidor (contrato de
     * licenças do jylabtech_core: no máximo 72h; igual ao TTL do token assinado).
     */
    public static final long CACHE_MAX_AGE_MS = TimeUnit.HOURS.toMillis(72);
    /** Com cache válido, consulta o servidor de novo depois deste intervalo (em uso). */
    public static final long REVALIDATE_INTERVAL_MS = TimeUnit.HOURS.toMillis(4);
    /** Depois de uma falha de rede, espera este tempo antes de tentar o servidor de novo (usa o cache). */
    public static final long RETRY_AFTER_FAILURE_MS = TimeUnit.MINUTES.toMillis(5);
    /** Motivo do core para cliente bloqueado/banido (bloqueio total imediato). */
    public static final String REASON_CUSTOMER_BLOCKED = "customer_blocked";
    public static final String MSG_CUSTOMER_BLOCKED = "Cliente bloqueado. Entre em contato com a JY Labtech.";
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
     * Indica se uma resposta HTTP diferente de 200 é uma recusa definitiva do servidor (licença
     * não encontrada, vencida, cancelada, suspensa, cliente bloqueado, aparelho desativado, limite
     * de aparelhos, requisição inválida). 408, 429, 5xx e outros códigos são tratados como falha
     * temporária (servidor fora do ar), que pode usar o cache offline.
     */
    public static boolean isDefinitiveRejection(int statusCode) {
        return statusCode >= 400 && statusCode < 500 && statusCode != 408 && statusCode != 429;
    }

    /**
     * Indica se é hora de consultar o servidor: sem cache, na primeira validação deste processo
     * (abertura do app) ou quando a última consulta online tem mais de {@link #REVALIDATE_INTERVAL_MS}.
     */
    public static boolean shouldRevalidate(CheckLicenceEntity cache, boolean checkedOnlineInThisProcess, Date now) {
        if (cache == null || !checkedOnlineInThisProcess || now == null) {
            return true;
        }
        Date savedAt = cache.getSavedLocalDatabaseAt();
        return savedAt == null || now.getTime() - savedAt.getTime() >= REVALIDATE_INTERVAL_MS;
    }

    /**
     * O cache offline só vale se a licença for utilizável, tiver sido salva
     * há no máximo 72h (e não no futuro), for da mesma chave e tiver sido
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
        if (age > CACHE_MAX_AGE_MS) {
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
     * Só HTTP 200 com licença utilizável é sucesso. {@link LicenceResult#isRejectedByServer()}
     * indica recusa definitiva (4xx exceto 408/429, ou 200 com licença lida mas inutilizável);
     * corpo 200 ilegível (ex.: página de proxy) não é recusa.
     */
    public static LicenceResult evaluateHttpResponse(int statusCode, String body, Date now) {
        if (statusCode == 200) {
            CheckLicenceEntity entity = CheckLicenceEntity.fromJson(body);
            if (entity == null) {
                // Corpo ilegível (ex.: página de erro de proxy) não é recusa da licença.
                return LicenceResult.error(MSG_INVALID_RESPONSE);
            }
            if (entity.getExpiredAt() == null) {
                return LicenceResult.rejected(MSG_INVALID_RESPONSE);
            }
            if (!isLicenceUsable(entity, now)) {
                return LicenceResult.rejected(MSG_NOT_USABLE);
            }
            return LicenceResult.success(entity);
        }
        LicenceResult result = LicenceResult.error(extractErrorMessage(statusCode, body));
        result.setRejectedByServer(isDefinitiveRejection(statusCode));
        return result;
    }

    static String extractErrorMessage(int statusCode, String body) {
        String fallback = "Erro ao validar licença (HTTP " + statusCode + ")";
        if (body == null || body.trim().isEmpty()) {
            return fallback;
        }
        try {
            JSONObject json = new JSONObject(body);
            if (REASON_CUSTOMER_BLOCKED.equals(json.optString("reason", ""))) {
                return MSG_CUSTOMER_BLOCKED;
            }
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
