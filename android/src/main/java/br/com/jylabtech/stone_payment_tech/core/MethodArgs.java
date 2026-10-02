package br.com.jylabtech.stone_payment_tech.core;

/** Argumentos de uma chamada do MethodChannel (MethodCall.argument). */
public interface MethodArgs {
    <T> T argument(String key);
}
