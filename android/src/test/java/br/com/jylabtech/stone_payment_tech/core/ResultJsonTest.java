package br.com.jylabtech.stone_payment_tech.core;

import static org.junit.Assert.*;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;

import stone.application.enums.TransactionStatusEnum;
import stone.database.transaction.TransactionObject;

/** Formato do JSON enviado ao Dart (StoneTechResponse / StoneTransactionModel). */
public class ResultJsonTest {

    private final Gson gson = new Gson();

    @Test
    public void basicResultJsonHasDartFields() throws Exception {
        BasicResult result = new BasicResult();
        result.setMethod("PaymentOptions");
        result.setResult(0);
        result.setMessage("Selecione Alimentação ou Refeição.");
        result.setOptions(Arrays.asList("Alimentação", "Refeição"));
        JSONObject json = new JSONObject(gson.toJson(result));
        assertEquals("PaymentOptions", json.getString("method"));
        assertEquals(0, json.getInt("result"));
        assertEquals("Selecione Alimentação ou Refeição.", json.getString("message"));
        JSONArray options = json.getJSONArray("options");
        assertEquals(2, options.length());
        assertEquals("Refeição", options.getString(1));
        assertFalse("nulos são omitidos", json.has("errorMessage"));
    }

    @Test
    public void basicErrorResultJson() throws Exception {
        BasicResult result = new BasicResult();
        result.setMethod("active");
        result.setResult(999999);
        result.setErrorMessage("Erro ao ativar terminal");
        JSONObject json = new JSONObject(gson.toJson(result));
        assertEquals(999999, json.getInt("result"));
        assertEquals("Erro ao ativar terminal", json.getString("errorMessage"));
    }

    @Test
    public void actionResultSettersIgnoreNullOrEmpty() throws Exception {
        ActionResult result = new ActionResult();
        result.setMethod("transaction");
        result.setTransactionStatus("APPROVED");
        result.setTransactionStatus("");
        result.setAmount(null);
        result.setErrorMessage(null);
        JSONObject json = new JSONObject(gson.toJson(result));
        assertEquals("transaction", json.getString("method"));
        assertEquals("APPROVED", json.getString("transactionStatus"));
        assertFalse(json.has("amount"));
        assertFalse(json.has("errorMessage"));
    }

    @Test
    public void allTransactionsBuildsOneItemPerTransaction() throws Exception {
        TransactionObject first = new TransactionObject();
        first.setAmount("1000");
        first.setTransactionStatus(TransactionStatusEnum.APPROVED);
        TransactionObject second = new TransactionObject();
        second.setAmount("250");
        second.setTransactionStatus(TransactionStatusEnum.CANCELLED);

        ActionResult result = new ActionResult();
        result.setMethod("AllTransactions");
        result.buildAllTransactions(Arrays.asList(first, null, second));

        assertEquals(2, result.getStoneTransactions().size());
        assertEquals("1000", result.getStoneTransactions().get(0).getAmount());
        assertEquals("APPROVED", result.getStoneTransactions().get(0).getTransactionStatus());
        assertEquals("250", result.getStoneTransactions().get(1).getAmount());
        assertEquals("CANCELLED", result.getStoneTransactions().get(1).getTransactionStatus());

        JSONObject json = new JSONObject(gson.toJson(result));
        assertEquals(2, json.getJSONArray("stoneTransactions").length());
    }

    @Test
    public void allTransactionsToleratesNullAndEmpty() {
        ActionResult result = new ActionResult();
        result.buildAllTransactions(null);
        assertTrue(result.getStoneTransactions().isEmpty());
        result.buildAllTransactions(Arrays.<TransactionObject>asList());
        assertTrue(result.getStoneTransactions().isEmpty());
    }

    @Test
    public void transactionResponseIsOnlyBuiltWhenApproved() {
        ActionResult result = new ActionResult();
        result.buildResponseStoneTransaction(null, true);
        result.buildResponseStoneTransaction(new TransactionObject(), false);
        assertEquals(0, result.getBuildResponse());
        assertNull(result.getAmount());
    }

    @Test
    public void stoneTransactionFactory() {
        StoneTransaction transaction = StoneTransaction.of(7, "990", "APPROVED");
        assertEquals(7, transaction.getIdFromBase());
        assertEquals("990", transaction.getAmount());
        assertEquals("APPROVED", transaction.getTransactionStatus());
    }
}
