package br.com.jylabtech.stone_payment_tech.printer;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.List;

/** JSON gerado por StonePrinterParams.toJson() no Dart. */
public class PrinterParamsTest {

    @Test
    public void textLinesConcatenateKeyAndValue() {
        List<PrinterParams.Item> items = PrinterParams.parse(
                "{\"printers\":[{\"key\":\"Total: \",\"value\":\"R$ 10,50\",\"type\":\"text\",\"fileBase64\":null}]}");
        assertEquals(1, items.size());
        assertFalse(items.get(0).image);
        assertEquals("Total: R$ 10,50", items.get(0).content);
    }

    @Test
    public void imageAcceptsDartNameAndLegacyBase64Type() {
        List<PrinterParams.Item> items = PrinterParams.parse(
                "{\"printers\":[{\"key\":\"\",\"value\":\"\",\"type\":\"image\",\"fileBase64\":\"aGVsbG8=\"},"
                        + "{\"key\":\"\",\"value\":\"\",\"type\":\"base64\",\"fileBase64\":\"d29ybGQ=\"}]}");
        assertEquals(2, items.size());
        assertTrue(items.get(0).image);
        assertEquals("aGVsbG8=", items.get(0).content);
        assertTrue(items.get(1).image);
        assertEquals("d29ybGQ=", items.get(1).content);
    }

    @Test
    public void nullValuesBecomeEmptyAndUnknownTypesAreIgnored() {
        List<PrinterParams.Item> items = PrinterParams.parse(
                "{\"printers\":[{\"key\":\"A\",\"value\":null,\"type\":\"text\"},{\"type\":\"qrcode\"},\"x\"]}");
        assertEquals(1, items.size());
        assertEquals("A", items.get(0).content);
    }

    @Test
    public void emptyPrinterListIsValid() {
        assertTrue(PrinterParams.parse("{\"printers\":[]}").isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingPrintersFails() {
        PrinterParams.parse("{\"other\":1}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void imageWithoutBase64Fails() {
        PrinterParams.parse("{\"printers\":[{\"type\":\"image\",\"fileBase64\":null}]}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullJsonFails() {
        PrinterParams.parse(null);
    }

    @Test(expected = RuntimeException.class)
    public void invalidJsonFails() {
        PrinterParams.parse("{printers:");
    }
}
