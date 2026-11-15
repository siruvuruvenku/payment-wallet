package com.example.wallet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet.domain.OutboxEvent;
import com.example.wallet.domain.Wallet;
import com.example.wallet.repository.LedgerEntryRepository;
import com.example.wallet.repository.OutboxEventRepository;
import com.example.wallet.repository.WalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:rollback-wallet;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.jwt.secret=bG9jYWwtZGV2ZWxvcG1lbnQtc2VjcmV0LWtleS1wbGVhc2UtY2hhbmdl"
})
@AutoConfigureMockMvc
class TransferRollbackIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private WalletRepository wallets;
    @Autowired private LedgerEntryRepository ledger;
    @MockBean private OutboxEventRepository outbox;

    @Test
    void failureAfterBalanceAndLedgerWritesRollsBackTheEntireTransfer() throws Exception {
        Account sender = register("rollback-sender@example.com");
        Account recipient = register("rollback-recipient@example.com");
        Wallet source = wallets.findById(sender.walletId()).orElseThrow();
        source.credit(new BigDecimal("40.00"));
        wallets.save(source);
        when(outbox.save(any(OutboxEvent.class))).thenThrow(new IllegalStateException("simulated outbox storage failure"));

        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + sender.token())
                        .header("Idempotency-Key", "rollback-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceWalletId":"%s","destinationWalletId":"%s","amount":15.00,"currency":"USD"}
                                """.formatted(sender.walletId(), recipient.walletId())))
                .andExpect(status().isInternalServerError());

        assertBalance(sender.walletId(), "40.00");
        assertBalance(recipient.walletId(), "0.00");
        org.junit.jupiter.api.Assertions.assertEquals(0, ledger.countByWalletId(sender.walletId()));
        org.junit.jupiter.api.Assertions.assertEquals(0, ledger.countByWalletId(recipient.walletId()));
    }

    private Account register(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"SecurePassword123!"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = json.readTree(result.getResponse().getContentAsString());
        return new Account(response.get("accessToken").asText(), UUID.fromString(response.get("walletId").asText()));
    }

    private void assertBalance(UUID walletId, String expected) {
        org.junit.jupiter.api.Assertions.assertEquals(
                0, wallets.findById(walletId).orElseThrow().getBalance().compareTo(new BigDecimal(expected)));
    }

    private record Account(String token, UUID walletId) {}
}
