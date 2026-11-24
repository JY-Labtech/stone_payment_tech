package br.com.jylabtech.stone_payment_tech.licence;


public class LicenceResult {
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_ERROR = 999999;

    private String method = "licence";
    private String errorMessage;
    // Começa como erro: só vira sucesso quando a licença for comprovadamente válida.
    private int result = RESULT_ERROR;
    private String message;
    // true quando o servidor respondeu e recusou a licença de forma definitiva (4xx exceto 408/429,
    // ou 200 com licença inutilizável). Nesse caso o cache é apagado e o uso é bloqueado na hora;
    // falhas de rede, timeout, 408/429/5xx e corpo ilegível ficam false e podem usar o cache (até 72h).
    private boolean rejectedByServer;

    public static LicenceResult success(CheckLicenceEntity entity) {
        LicenceResult r = new LicenceResult();
        r.setResult(RESULT_SUCCESS);
        r.setLicenceEntity(entity);
        return r;
    }

    public static LicenceResult error(String errorMessage) {
        LicenceResult r = new LicenceResult();
        r.setResult(RESULT_ERROR);
        r.setErrorMessage(errorMessage);
        return r;
    }

    /** Recusa definitiva do servidor (ver {@link #isRejectedByServer()}). */
    public static LicenceResult rejected(String errorMessage) {
        LicenceResult r = error(errorMessage);
        r.setRejectedByServer(true);
        return r;
    }

    public boolean isRejectedByServer() {
        return rejectedByServer;
    }

    public void setRejectedByServer(boolean value) {
        this.rejectedByServer = value;
    }

    public boolean isSuccess() {
        return result == RESULT_SUCCESS;
    }

    private CheckLicenceEntity licenceEntity;

    public void setLicenceEntity(CheckLicenceEntity entity) {
        this.licenceEntity = entity;
    }

    public CheckLicenceEntity getLicenceEntity() {
        return this.licenceEntity;
    }

    public void setMethod(String value) {
        this.method = value;
    }

    public void setResult(int value) {
        this.result = value;
    }

    public void setErrorMessage(String value){ this.errorMessage = value;}

    public void setMessage(String value){this.message = value;}

    public String getMessage(){return this.message;}

    public String getMethod(){return this.method;}
    public String getErrorMessage(){return this.errorMessage;}
    public int getResult(){return this.result;}
}
