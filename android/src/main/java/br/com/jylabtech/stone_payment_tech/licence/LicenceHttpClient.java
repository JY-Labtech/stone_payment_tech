package br.com.jylabtech.stone_payment_tech.licence;

/**
 * Abstração da chamada HTTP de validação de licença (permite fake em testes JVM).
 */
public interface LicenceHttpClient {

    /**
     * Faz o POST de validação e devolve o status HTTP e o corpo da resposta
     * (corpo de erro quando não for 200; pode ser null).
     * Lança exceção em falhas de rede/timeout.
     */
    LicenceHttpResponse post(String url, String licenceKey) throws Exception;
}
