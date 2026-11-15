package com.example.wallet;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.wallet.domain.Wallet;
import com.example.wallet.repository.LedgerEntryRepository;
import com.example.wallet.repository.WalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:wallet;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.jwt.secret=bG9jYWwtZGV2ZWxvcG1lbnQtc2VjcmV0LWtleS1wbGVhc2UtY2hhbmdl"
})
@AutoConfigureMockMvc
class TransferApiIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private WalletRepository wallets;
    @Autowired private LedgerEntryRepository ledger;

    @Test
    void transferCommitsBothBalancesAndLedgerPairAndRetryDoesNotDuplicate() throws Exception {
        Account sender = register("sender@example.com");
        Account recipient = register("recipient@example.com");
        Wallet source = wallets.findById(sender.walletId()).orElseThrow();
        source.credit(new BigDecimal("100.00"));
        wallets.save(source);

        String request = """
                {"sourceWalletId":"%s","destinationWalletId":"%s","amount":25.00,"currency":"USD"}
                """.formatted(sender.walletId(), recipient.walletId());
        MvcResult firstResponse = mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + sender.token())
                        .header("Idempotency-Key", "integration-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andReturn();
        UUID transferId = UUID.fromString(json.readTree(firstResponse.getResponse().getContentAsString())
                .get("transferId").asText());

        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + sender.token())
                        .header("Idempotency-Key", "integration-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transferId", is(transferId.toString())));

        assertBalance(sender.walletId(), "75.00");
        assertBalance(recipient.walletId(), "25.00");
        org.junit.jupiter.api.Assertions.assertEquals(2, ledger.countByTransferId(transferId));
    }

    @Test
    void insufficientFundsReturnsClientErrorWithoutPersistingTransferOrLedger() throws Exception {
        Account sender = register("poor@example.com");
        Account recipient = register("payee@example.com");
        String request = """
                {"sourceWalletId":"%s","destinationWalletId":"%s","amount":1.00,"currency":"USD"}
                """.formatted(sender.walletId(), recipient.walletId());

        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + sender.token())
                        .header("Idempotency-Key", "insufficient-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", is("Insufficient wallet balance")));

        assertBalance(sender.walletId(), "0.00");
        org.junit.jupiter.api.Assertions.assertEquals(0, ledger.countByWalletId(sender.walletId()));
        org.junit.jupiter.api.Assertions.assertEquals(0, ledger.countByWalletId(recipient.walletId()));
    }

    @Test
    void unknownDestinationWalletIsRejectedWithoutLedgerWrites() throws Exception {
        Account sender = register("invalid-wallet-sender@example.com");
        UUID missingWalletId = UUID.randomUUID();

        mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + sender.token())
                        .header("Idempotency-Key", "invalid-wallet-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceWalletId":"%s","destinationWalletId":"%s","amount":5.00,"currency":"USD"}
                                """.formatted(sender.walletId(), missingWalletId)))
                .andExpect(status().isNotFound());

        org.junit.jupiter.api.Assertions.assertEquals(0, ledger.countByWalletId(sender.walletId()));
    }

    @Test
    void simultaneousTransfersCannotOverspendWallet() throws Exception {
        Account sender = register("concurrent-sender@example.com");
        Account recipientOne = register("concurrent-one@example.com");
        Account recipientTwo = register("concurrent-two@example.com");
        Wallet source = wallets.findById(sender.walletId()).orElseThrow();
        source.credit(new BigDecimal("100.00"));
        wallets.save(source);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> first = executor.submit(() -> {
                start.await();
                return transferStatus(sender, recipientOne, "concurrent-key-one", "80.00");
            });
            Future<Integer> second = executor.submit(() -> {
                start.await();
                return transferStatus(sender, recipientTwo, "concurrent-key-two", "80.00");
            });
            start.countDown();
            var statuses = java.util.List.of(first.get(), second.get()).stream().sorted().toList();

            org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(201, 422), statuses);
            assertBalance(sender.walletId(), "20.00");
            long entryCount = ledger.countByWalletId(sender.walletId())
                    + ledger.countByWalletId(recipientOne.walletId())
                    + ledger.countByWalletId(recipientTwo.walletId());
            org.junit.jupiter.api.Assertions.assertEquals(2, entryCount);
        } finally {
            executor.shutdownNow();
        }
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

    private int transferStatus(Account sender, Account recipient, String key, String amount) throws Exception {
        String request = """
                {"sourceWalletId":"%s","destinationWalletId":"%s","amount":%s,"currency":"USD"}
                """.formatted(sender.walletId(), recipient.walletId(), amount);
        return mvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + sender.token())
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andReturn().getResponse().getStatus();
    }

    private record Account(String token, UUID walletId) {}
}
