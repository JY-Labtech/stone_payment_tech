package br.com.jylabtech.stone_payment_tech.printer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converte o JSON de {@code StonePrinterParams.toJson()} (Dart) nas linhas e
 * imagens enviadas ao PosPrintProvider. Lógica pura (testável na JVM).
 */
public final class PrinterParams {

    public static final class Item {
        public final boolean image;
        /** Texto ({@code key + value}) ou imagem em base64. */
        public final String content;

        Item(boolean image, String content) {
            this.image = image;
            this.content = content;
        }
    }

    private static final Gson GSON = new Gson();

    private PrinterParams() {}

    @SuppressWarnings("unchecked")
    public static List<Item> parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("Parâmetros de impressão vazios");
        }
        Map<String, Object> data = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
        Object printersValue = data != null ? data.get("printers") : null;
        if (!(printersValue instanceof List)) {
            throw new IllegalArgumentException("Parâmetros de impressão sem a lista 'printers'");
        }
        List<Item> items = new ArrayList<>();
        for (Object entry : (List<Object>) printersValue) {
            if (!(entry instanceof Map)) {
                continue;
            }
            Map<String, Object> printer = (Map<String, Object>) entry;
            String type = asString(printer.get("type"));
            if ("text".equals(type)) {
                items.add(new Item(false, asString(printer.get("key")) + asString(printer.get("value"))));
            } else if ("base64".equals(type) || "image".equals(type)) {
                // O Dart (StonePrinter.toMap) envia "image" (PrinterType.name); "base64"
                // é o valor de PrinterTypeExt.type. Antes só "base64" imprimia.
                String imageBase64 = asString(printer.get("fileBase64"));
                if (imageBase64.isEmpty()) {
                    throw new IllegalArgumentException("Imagem sem 'fileBase64'");
                }
                items.add(new Item(true, imageBase64));
            }
        }
        return items;
    }

    private static String asString(Object value) {
        return value == null ? "" : value.toString();
    }
}
