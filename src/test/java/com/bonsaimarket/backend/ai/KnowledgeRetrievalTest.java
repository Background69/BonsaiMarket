package com.bonsaimarket.backend.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeRetrievalTest {
    private String resource(String name) throws Exception {
        try(var input=getClass().getResourceAsStream("/ai/"+name)) { return new String(input.readAllBytes(),StandardCharsets.UTF_8); }
    }

    @Test void aliasesAccentsStableOrderingAndNoEvidenceAreHandled() throws Exception {
        var faq = new FaqContextService();
        assertEquals(faq.retrieve("thối rễ"),faq.retrieve("THOI RE"));
        assertTrue(faq.retrieve("rễ đen mềm").stream().anyMatch(e -> e.id()==18));
        assertEquals(faq.retrieve("cách tưới bonsai"),faq.retrieve("cách tưới bonsai"));
        assertTrue(faq.retrieve("quantum football xyzzy").isEmpty());
        assertTrue(faq.retrieve("và cho tôi như nào").isEmpty());
        assertTrue(faq.retrieve("tưới nước bón phân thay chậu lá vàng cắt tỉa").size()<=4);
    }

    @ParameterizedTest @ValueSource(strings={"http://www.rhs.org.uk/x","https://www.rhs.org.uk.evil.test/x",
            "https://private@www.rhs.org.uk/x","https://www.rhs.org.uk:444/x","https://127.0.0.1/x","javascript:alert(1)"})
    void untrustedReferenceCannotEnterKnowledge(String url) throws Exception {
        String metadata = resource("knowledge-index.json").replace("https://www.rhs.org.uk/plants/types/trees/bonsai",url);
        assertThrows(IllegalArgumentException.class,()->new FaqContextService(resource("bonsai-faq.md"),metadata));
    }

    @Test void incompleteDuplicateDraftAndOversizedContentFailClosed() throws Exception {
        String document=resource("bonsai-faq.md"), metadata=resource("knowledge-index.json");
        for (String broken : List.of(metadata.replace("\"id\": 1,","\"id\": 2,"),
                metadata.replace("SOURCE_CHECKED","DRAFT"),metadata.replace("2026-10-18","2999-01-01")))
            assertThrows(IllegalArgumentException.class,()->new FaqContextService(document,broken));
        assertThrows(IllegalArgumentException.class,()->new FaqContextService(document+"\n## 99. No metadata\nNo source",metadata));
        assertThrows(IllegalArgumentException.class,()->new FaqContextService(document+"x".repeat(1000),metadata));
    }
}
