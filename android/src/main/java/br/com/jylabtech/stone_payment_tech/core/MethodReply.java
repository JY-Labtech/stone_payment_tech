package br.com.jylabtech.stone_payment_tech.core;

/**
 * Resposta de uma chamada do MethodChannel, sem depender das classes do Flutter
 * (permite testar o despacho na JVM).
 */
public interface MethodReply {
    void success(Object value);

    void error(String code, String message, Object details);

    void notImplemented();
}
