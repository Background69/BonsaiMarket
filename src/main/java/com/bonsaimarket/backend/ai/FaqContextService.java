package com.bonsaimarket.backend.ai;

import com.bonsaimarket.backend.ai.dto.AiSourceDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

@Service
public class FaqContextService {
    public record Entry(long id, String title, String text) {
        public AiSourceDto source() { return new AiSourceDto("FAQ", id, title); }
    }
    private final List<Entry> entries;

    public FaqContextService() throws IOException {
        try (var input = new ClassPathResource("ai/bonsai-faq.md").getInputStream()) {
            String document = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            List<Entry> parsed = new ArrayList<>();
            for (String section : document.split("(?m)^## ")) {
                if (!section.matches("(?s)^\\d+\\..*")) continue;
                int end = section.indexOf('\n');
                String heading = section.substring(0, end).strip();
                int dot = heading.indexOf('.');
                parsed.add(new Entry(Long.parseLong(heading.substring(0, dot)), heading.substring(dot + 1).strip(),
                        section.substring(end + 1).strip()));
            }
            entries = List.copyOf(parsed);
        }
    }

    public List<Entry> retrieve(String question) {
        String normalized = normalize(question);
        Set<String> keywords = new HashSet<>(Arrays.asList(normalized.split("\\W+")));
        keywords.removeAll(Set.of("cay", "bonsai", "toi", "cho", "va", "co", "la", "nhu", "nao", "khong", "gi", "nen", "bao", "nhieu", "lan"));
        return entries.stream().filter(entry -> score(entry, keywords) > 0)
                .sorted(Comparator.<Entry>comparingInt(entry -> score(entry, keywords)).reversed())
                .limit(4).toList();
    }

    private int score(Entry entry, Set<String> keywords) {
        Set<String> words = new HashSet<>(Arrays.asList(normalize(entry.title()).split("\\W+")));
        return (int) keywords.stream().filter(words::contains).count();
    }

    public static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
    }
}
