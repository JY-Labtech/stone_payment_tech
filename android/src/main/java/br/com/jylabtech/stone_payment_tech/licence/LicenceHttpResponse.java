package br.com.jylabtech.stone_payment_tech.licence;

public class LicenceHttpResponse {
    private final int statusCode;
    private final String body;

    public LicenceHttpResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() { return statusCode; }

    public String getBody() { return body; }
}
