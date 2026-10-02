package br.com.jylabtech.stone_payment_tech.licence;

import android.content.Context;

import java.util.Date;

/**
 * Fluxo de validação de licença: cache local válido (sem rede) ou validação
 * online no servidor oficial da JY Labtech. Não faz chamadas de UI e não
 * troca de thread: a chamada online é bloqueante e deve rodar fora da main thread.
 */
public class CheckLicenceTech {

    private final LicenceStorage storage;
    private final LicenceHttpClient httpClient;
    private final LicenceClock clock;

    public CheckLicenceTech(Context context) {
        this(new EncryptedLicenceStorage(context), new LicenceProvider(context), LicenceClock.SYSTEM);
    }

    public CheckLicenceTech(LicenceStorage storage, LicenceHttpClient httpClient, LicenceClock clock) {
        this.storage = storage;
        this.httpClient = httpClient;
        this.clock = clock;
    }

    /**
     * Confere apenas o cache local (sem rede). Se o cache existir e não for
     * válido, ele é apagado.
     */
    public boolean hasValidCachedLicence(String licenceKey, String licenceInternalKey) {
        try {
            CheckLicenceEntity cache = storage.load();
            if (cache == null) {
                return false;
            }
            if (LicenceRules.isCacheValid(cache, licenceKey, licenceInternalKey, clock.now())) {
                return true;
            }
            storage.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Valida a licença: usa o cache válido; senão consulta o servidor oficial.
     * Só devolve sucesso com licença válida e ativa. Bloqueante (rede).
     */
    public LicenceResult validateLicence(String licenceKey, String licenceInternalKey) {
        try {
            if (licenceKey == null || licenceKey.trim().isEmpty()) {
                return LicenceResult.error("Chave de licença não configurada");
            }

            if (hasValidCachedLicence(licenceKey, licenceInternalKey)) {
                return LicenceResult.success(storage.load());
            }

            String apiUrl = LicenceConfig.resolveLicenceUrl(licenceInternalKey);
            LicenceHttpResponse response;
            try {
                response = httpClient.post(apiUrl, licenceKey);
            } catch (Exception e) {
                return LicenceResult.error(LicenceRules.formatConnectionError(e));
            }
            if (response == null) {
                return LicenceResult.error("Sem resposta do servidor de licença");
            }

            Date now = clock.now();
            LicenceResult result = LicenceRules.evaluateHttpResponse(response.getStatusCode(), response.getBody(), now);
            if (!result.isSuccess()) {
                return result;
            }

            CheckLicenceEntity entity = result.getLicenceEntity();
            entity.setLicenceInternalKey(apiUrl);
            entity.setSavedLocalDatabaseAt(now);
            // O servidor confirmou licença válida e ativa: a operação é liberada
            // mesmo que o cache não possa ser salvo (a próxima operação revalida online).
            saveCacheQuietly(entity);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return LicenceResult.error(LicenceRules.formatConnectionError(e));
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
