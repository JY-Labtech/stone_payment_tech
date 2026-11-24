package br.com.jylabtech.stone_payment_tech.licence;

import android.content.Context;

import java.util.Date;

/**
 * Fluxo de validação de licença conforme o contrato de licenças do jylabtech_core:
 *
 * <ul>
 *   <li>A primeira validação de cada processo (abertura do app) sempre consulta o servidor.</li>
 *   <li>Depois disso, com cache válido, usa o cache e consulta o servidor de novo a cada
 *       {@link LicenceRules#REVALIDATE_INTERVAL_MS} (4h).</li>
 *   <li>Recusa definitiva do servidor (cliente bloqueado, licença vencida/cancelada/suspensa, não
 *       encontrada, aparelho desativado...) apaga o cache e bloqueia na hora.</li>
 *   <li>Falha de rede, timeout, 408/429/5xx ou corpo ilegível usa o cache válido (salvo há no
 *       máximo 72h); sem cache, bloqueia. Após uma falha, espera
 *       {@link LicenceRules#RETRY_AFTER_FAILURE_MS} para tentar de novo, para não travar cada
 *       operação offline no timeout.</li>
 * </ul>
 *
 * Não faz chamadas de UI e não troca de thread: a chamada online é bloqueante e deve rodar fora
 * da main thread.
 */
public class CheckLicenceTech {

    private final LicenceStorage storage;
    private final LicenceHttpClient httpClient;
    private final LicenceClock clock;

    // Estado deste processo (não persistido): reiniciar o app força nova consulta ao servidor.
    // volatile: hasValidCachedLicence lê sem lock (é chamado na main thread pelo gate).
    private volatile boolean checkedOnlineInThisProcess;
    private volatile Date lastOnlineFailureAt;

    public CheckLicenceTech(Context context) {
        this(new EncryptedLicenceStorage(context), new LicenceProvider(context), LicenceClock.SYSTEM);
    }

    public CheckLicenceTech(LicenceStorage storage, LicenceHttpClient httpClient, LicenceClock clock) {
        this.storage = storage;
        this.httpClient = httpClient;
        this.clock = clock;
    }

    /**
     * Confere apenas o cache local (sem rede): true só quando {@link #validateLicence} também
     * liberaria sem consultar o servidor agora (cache válido e já validado online neste processo
     * há menos de 4h, ou servidor fora do ar há menos de 5 min). Cache inválido é apagado.
     * Não usa lock: pode ser chamado na main thread como atalho antes de {@link #validateLicence}.
     */
    public boolean hasValidCachedLicence(String licenceKey, String licenceInternalKey) {
        try {
            CheckLicenceEntity cached = validCachedLicence(licenceKey, licenceInternalKey);
            if (cached == null) {
                return false;
            }
            Date now = clock.now();
            return !LicenceRules.shouldRevalidate(cached, checkedOnlineInThisProcess, now)
                    || recentlyFailedOnline(now);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Valida a licença (servidor ou cache, conforme as regras acima).
     * Só devolve sucesso com licença válida e ativa. Bloqueante (rede).
     */
    public synchronized LicenceResult validateLicence(String licenceKey, String licenceInternalKey) {
        try {
            if (licenceKey == null || licenceKey.trim().isEmpty()) {
                return LicenceResult.error("Chave de licença não configurada");
            }

            CheckLicenceEntity cached = validCachedLicence(licenceKey, licenceInternalKey);
            Date now = clock.now();
            if (cached != null && !LicenceRules.shouldRevalidate(cached, checkedOnlineInThisProcess, now)) {
                return LicenceResult.success(cached);
            }
            if (cached != null && recentlyFailedOnline(now)) {
                return LicenceResult.success(cached);
            }

            String apiUrl = LicenceConfig.resolveLicenceUrl(licenceInternalKey);
            LicenceHttpResponse response;
            try {
                response = httpClient.post(apiUrl, licenceKey);
            } catch (Exception e) {
                return onlineFailure(cached, now, LicenceRules.formatConnectionError(e));
            }
            if (response == null) {
                return onlineFailure(cached, now, "Sem resposta do servidor de licença");
            }

            now = clock.now();
            LicenceResult result = LicenceRules.evaluateHttpResponse(response.getStatusCode(), response.getBody(), now);
            if (result.isSuccess()) {
                CheckLicenceEntity entity = result.getLicenceEntity();
                entity.setLicenceInternalKey(apiUrl);
                entity.setSavedLocalDatabaseAt(now);
                // O servidor confirmou licença válida e ativa: a operação é liberada
                // mesmo que o cache não possa ser salvo (a próxima operação revalida online).
                saveCacheQuietly(entity);
                checkedOnlineInThisProcess = true;
                lastOnlineFailureAt = null;
                return result;
            }

            if (result.isRejectedByServer()) {
                // Recusa definitiva: o cache não pode mais liberar o uso.
                deleteCacheQuietly();
                checkedOnlineInThisProcess = true;
                lastOnlineFailureAt = null;
                return result;
            }
            // 408/429/5xx/corpo ilegível: servidor indisponível.
            return onlineFailure(cached, now, result.getErrorMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return LicenceResult.error(LicenceRules.formatConnectionError(e));
        }
    }

    /** Cache válido por todas as regras de {@link LicenceRules#isCacheValid}; inválido é apagado. */
    private CheckLicenceEntity validCachedLicence(String licenceKey, String licenceInternalKey) {
        CheckLicenceEntity cache = storage.load();
        if (cache == null) {
            return null;
        }
        if (LicenceRules.isCacheValid(cache, licenceKey, licenceInternalKey, clock.now())) {
            return cache;
        }
        deleteCacheQuietly();
        return null;
    }

    private boolean recentlyFailedOnline(Date now) {
        Date failedAt = lastOnlineFailureAt;
        if (failedAt == null || now == null) {
            return false;
        }
        long elapsed = now.getTime() - failedAt.getTime();
        return elapsed >= 0 && elapsed < LicenceRules.RETRY_AFTER_FAILURE_MS;
    }

    /** Servidor inacessível: usa o cache válido (até 72h) ou bloqueia se não houver. */
    private LicenceResult onlineFailure(CheckLicenceEntity cached, Date now, String message) {
        lastOnlineFailureAt = now;
        if (cached != null) {
            return LicenceResult.success(cached);
        }
        return LicenceResult.error(message);
    }

    private void deleteCacheQuietly() {
        try {
            storage.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveCacheQuietly(CheckLicenceEntity entity) {
        try {
            if (!storage.save(entity)) {
                System.err.println("CheckLicenceTech: não foi possível salvar o cache da licença; a próxima operação revalida online");
            }
        } catch (Exception e) {
            System.err.println("CheckLicenceTech: erro ao salvar o cache da licença; a próxima operação revalida online");
            e.printStackTrace();
        }
    }
}
