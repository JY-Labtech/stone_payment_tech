package br.com.jylabtech.stone_payment_tech.licence;


public class LicenceResult {
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_ERROR = 999999;

    private String method = "licence";
    private String errorMessage;
    // Começa como erro: só vira sucesso quando a licença for comprovadamente válida.
    private int result = RESULT_ERROR;
    private String message;

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
