package br.com.jylabtech.stone_payment_tech.payments;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import stone.application.enums.Action;
import stone.application.enums.ErrorsEnum;
import stone.application.enums.InstalmentTransactionEnum;
import stone.application.enums.TypeOfTransactionEnum;
import stone.application.xml.enums.ResponseCodeEnum;
import stone.utils.keys.StoneKeyType;

/** Mapeamentos puros entre o canal e os enums do SDK Stone. */
public class StoneHelperTest {

    private final StoneHelper helper = new StoneHelper();

    @Test
    public void transactionTypeCodesMatchDispatcher() {
        assertEquals(TypeOfTransactionEnum.CREDIT, helper.getTypeTransaction(1));
        assertEquals(TypeOfTransactionEnum.DEBIT, helper.getTypeTransaction(2));
        assertEquals(TypeOfTransactionEnum.PIX, helper.getTypeTransaction(3));
        assertEquals(TypeOfTransactionEnum.VOUCHER, helper.getTypeTransaction(4));
        assertEquals(TypeOfTransactionEnum.INSTANT_PAYMENT, helper.getTypeTransaction(99));
    }

    @Test
    public void installmentsWithAndWithoutInterest() {
        assertEquals(InstalmentTransactionEnum.ONE_INSTALMENT, helper.getInstallment(1, true));
        assertEquals(InstalmentTransactionEnum.ONE_INSTALMENT, helper.getInstallment(1, false));
        assertEquals(InstalmentTransactionEnum.TWO_INSTALMENT_NO_INTEREST, helper.getInstallment(2, false));
        assertEquals(InstalmentTransactionEnum.TWO_INSTALMENT_WITH_INTEREST, helper.getInstallment(2, true));
        assertEquals(InstalmentTransactionEnum.TWELVE_INSTALMENT_NO_INTEREST, helper.getInstallment(12, false));
        assertEquals(InstalmentTransactionEnum.THIRTEEN_INSTALMENT_WITH_INTEREST, helper.getInstallment(13, true));
        // Fora da faixa suportada pela Stone cai em à vista.
        assertEquals(InstalmentTransactionEnum.ONE_INSTALMENT, helper.getInstallment(0, false));
        assertEquals(InstalmentTransactionEnum.ONE_INSTALMENT, helper.getInstallment(14, true));
    }

    @Test
    public void everyInstallmentFrom2To13HasAnEnum() {
        for (int parc = 2; parc <= 13; parc++) {
            assertTrue(helper.getInstallment(parc, false).name().endsWith("_NO_INTEREST"));
            assertTrue(helper.getInstallment(parc, true).name().endsWith("_WITH_INTEREST"));
        }
    }

    @Test
    public void actionMessagesShownToTheUser() {
        assertEquals("Insira, passe ou aproxime o cartão.", helper.getMessageFromTransactionAction(Action.TRANSACTION_WAITING_CARD));
        assertEquals("Digite a sua senha.", helper.getMessageFromTransactionAction(Action.TRANSACTION_WAITING_PASSWORD));
        assertEquals("Selecione Alimentação ou Refeição.", helper.getMessageFromTransactionAction(Action.TRANSACTION_TYPE_SELECTION));
        assertEquals("Leia o QRCode para prosseguir com o pagamento.", helper.getMessageFromTransactionAction(Action.TRANSACTION_WAITING_QRCODE_SCAN));
        assertEquals("Em processamento. Aguarde.", helper.getMessageFromTransactionAction(null));
        for (Action action : Action.values()) {
            assertNotNull(helper.getMessageFromTransactionAction(action));
        }
    }

    @Test
    public void responseCodeMessages() {
        assertEquals("Aprovado", helper.getMessageFromResponseCodeEnum(ResponseCodeEnum.Approved));
        assertEquals("Recusado", helper.getMessageFromResponseCodeEnum(ResponseCodeEnum.Declined));
        assertEquals("Erro técnico", helper.getMessageFromResponseCodeEnum(ResponseCodeEnum.TechnicalError));
        assertEquals("Erro na operação", helper.getMessageFromResponseCodeEnum(null));
    }

    @Test
    public void errorListUsesFirstErrorAndToleratesNullOrEmpty() {
        assertEquals("", helper.getErrorFromErrorList(null));
        assertEquals("", helper.getErrorFromErrorList(Collections.<ErrorsEnum>emptyList()));
        assertEquals("CARD_BLOCKED: Cartão bloqueado",
                helper.getErrorFromErrorList(Arrays.asList(ErrorsEnum.CARD_BLOCKED, ErrorsEnum.TIME_OUT)));
        assertEquals("QRCODE_EXPIRED: QRCode expirado", helper.getErrorMessageFromErrorEnum(ErrorsEnum.QRCODE_EXPIRED));
        assertEquals("", helper.getErrorMessageFromErrorEnum(null));
        for (ErrorsEnum error : ErrorsEnum.values()) {
            assertTrue(helper.getErrorMessageFromErrorEnum(error).startsWith(error.name() + ": "));
        }
    }

    @Test
    public void optionIndexIsSafe() {
        assertEquals(1, StoneHelper.optionIndex(Arrays.asList("Alimentação", "Refeição"), "Refeição"));
        assertEquals(-1, StoneHelper.optionIndex(Arrays.asList("Alimentação"), "Refeição"));
        assertEquals(-1, StoneHelper.optionIndex(null, "Refeição"));
        assertEquals(-1, StoneHelper.optionIndex(Arrays.asList("Alimentação"), null));
    }

    @Test
    public void qrCodeKeysUseBearerPrefix() {
        Map<StoneKeyType, String> keys = PaymentsPresenter.qrCodeKeys("tok", "prov");
        assertEquals("Bearer tok", keys.get(StoneKeyType.QRCODE_AUTHORIZATION));
        assertEquals("prov", keys.get(StoneKeyType.QRCODE_PROVIDERID));
        assertEquals(2, keys.size());
    }

    @Test
    public void fragmentNeverSendsNullStringsToDart() {
        assertEquals("", PaymentsFragment.nonNull(null));
        assertEquals("ok", PaymentsFragment.nonNull("ok"));
    }
}
