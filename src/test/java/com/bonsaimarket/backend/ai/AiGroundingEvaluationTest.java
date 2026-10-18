package com.bonsaimarket.backend.ai;

import com.bonsaimarket.backend.product.ProductRepository;
import com.bonsaimarket.backend.store.StoreRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Offline contract/retrieval benchmark. The provider is deliberately a double, never an LLM quality score. */
class AiGroundingEvaluationTest {
    @Test void evaluateFrozenFiftyQuestions() throws Exception {
        var mapper = new ObjectMapper();
        var faq = new FaqContextService();
        var products = mock(ProductRepository.class);
        var stores = mock(com.bonsaimarket.backend.catalog.StoreDiscovery.class);
        when(stores.search(anyMap())).thenReturn(new com.bonsaimarket.backend.catalog.StoreDiscovery.StorePage(List.of(),0,10,0,0));
        when(stores.findPublicById(anyLong())).thenReturn(new com.bonsaimarket.backend.catalog.StoreDiscovery.StorePage(List.of(),0,1,0,0));
        var catalog = new CatalogContextService(products, stores);
        var provider = mock(AiProviderClient.class);
        var service = new AiChatService(provider, catalog, faq, mapper);
        JsonNode cases;
        try (var input = getClass().getResourceAsStream("/ai/grounding-evaluation.json")) { cases = mapper.readTree(input); }
        assertEquals(50, cases.size());
        List<Map<String,Object>> rows = new ArrayList<>();
        int hits = 0, careCount = 0, routes = 0, safety = 0, attacks = 0, references = 0;
        for (var c : cases) {
            String question = c.path("question").asText();
            boolean route = catalog.relevant(question) == c.path("catalog").asBoolean();
            if (route) routes++;
            var knowledge = faq.retrieve(question);
            boolean hit = !c.has("knowledgeId") || knowledge.stream().anyMatch(e -> e.id() == c.path("knowledgeId").asLong());
            boolean cited = !c.has("knowledgeId") || knowledge.stream().filter(e -> e.id() == c.path("knowledgeId").asLong())
                    .anyMatch(e -> mapper.valueToTree(e.source()).path("url").asText().startsWith("https://"));
            if (c.has("knowledgeId")) { careCount++; if (hit) hits++; if (cited) references++; }
            var search = mapper.valueToTree(CatalogContextService.analyze(question));
            boolean filters = true;
            for (String field : List.of("name", "minPrice", "maxPrice", "productId", "storeId")) {
                if (c.has(field)) filters &= c.path(field).asText().equals(search.path(field).asText());
            }
            boolean safe = true;
            if (c.path("attack").asBoolean()) {
                attacks++;
                // Simulates a compromised model. Structured IDs may be forged; raw text must never escape.
                when(provider.answer(anyString(), anyString(), anyBoolean())).thenAnswer(inv -> (boolean)inv.getArgument(2)
                        ? "{\"productIds\":[999999],\"storeIds\":[999999],\"faqIds\":[999999]}"
                        : "UNTRUSTED_OUTPUT giá 1 đồng <script>attack</script>");
                try { safe = !service.chat(question).answer().contains("UNTRUSTED_OUTPUT"); }
                catch (AiException e) { safe = e.status() >= 400 && !e.getMessage().contains("UNTRUSTED_OUTPUT"); }
                if (safe) safety++;
            }
            rows.add(Map.of("id",c.path("id").asInt(),"question",question,"routeCorrect",route,
                    "retrievedIds",knowledge.stream().map(FaqContextService.Entry::id).toList(),
                    "expectedSourceHit",hit,"externalReference",cited,"filtersCorrect",filters,"attackContained",safe));
        }
        String phase = System.getProperty("ai.eval.phase", "after");
        var result = Map.of("phase",phase,"kind","OFFLINE_RETRIEVAL_AND_CONTRACT_ONLY_MOCK_PROVIDER",
                "total",50,"routingCorrect",routes,"careQuestions",careCount,"careHitAt4",hits,
                "careWithExternalReference",references,"attacks",attacks,"attacksContained",safety,"cases",rows);
        Path output = Path.of("artifacts/ai-grounding",phase+".json");
        Files.createDirectories(output.getParent());
        Files.writeString(output,mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
        System.out.println("AI offline evaluation: " + phase + ", route=" + routes + "/50, hit@4=" + hits + "/"+careCount
                + ", referenced="+references+"/"+careCount+", adversarial="+safety+"/"+attacks);
        if (!phase.equals("before")) {
            assertEquals(50,routes,"Intent routing regression; see per-case artifact");
            assertEquals(careCount,hits,"Knowledge retrieval misses");
            assertEquals(careCount,references,"Missing source metadata");
            assertEquals(attacks,safety,"Untrusted prose escaped");
            assertTrue(rows.stream().allMatch(r -> Boolean.TRUE.equals(r.get("filtersCorrect"))),"Catalog constraint mismatch");
        }
    }
}
