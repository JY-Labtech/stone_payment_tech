package br.com.jylabtech.stone_payment_tech.core;

public class StoneTransaction {

    public static StoneTransaction of(int idFromBase, String amount, String transactionStatus) {
        StoneTransaction transaction = new StoneTransaction();
        transaction.setIdFromBase(idFromBase);
        transaction.setAmount(amount);
        transaction.setTransactionStatus(transactionStatus);
        return transaction;
    }

    private int idFromBase;
    private String amount;
    private String transactionStatus;
    public int getIdFromBase() {
        return idFromBase;
    }

    public String getAmount() {
        return amount;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public void setIdFromBase(int value) {
        this.idFromBase  = value;
    }

    public void setAmount(String value) {
        this.amount = value;
    }

    public void setTransactionStatus(String value) {
        this.transactionStatus = value;
    }



}
