package com.bonsaimarket.backend.ai;

import org.springframework.web.bind.annotation.*;
import com.bonsaimarket.backend.ai.dto.AiSourceDto;
import java.util.List;

/** Public curated knowledge only. Updates are reviewed files deployed with the application. */
@RestController
@RequestMapping("/api/ai/knowledge")
public class AiKnowledgeController {
    private final FaqContextService knowledge;
    public AiKnowledgeController(FaqContextService knowledge) { this.knowledge = knowledge; }
    public record Passage(long id, String title, String text, AiSourceDto source) {}
    public record Result(String version, String expertReview, List<Passage> passages) {}
    @GetMapping
    public Result search(@RequestParam(defaultValue="") String q) {
        if (q.isBlank() || q.length() > 160) throw new AiException(400,"INVALID_QUERY","Từ khóa cần từ 1 đến 160 ký tự.");
        return new Result(knowledge.version(), "PENDING", knowledge.retrieve(q).stream()
                .map(e -> new Passage(e.id(), e.title(), e.text(), e.source())).toList());
    }
}
