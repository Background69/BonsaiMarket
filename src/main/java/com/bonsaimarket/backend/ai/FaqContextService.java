package com.bonsaimarket.backend.ai;

import com.bonsaimarket.backend.ai.dto.AiSourceDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.LocalDate;

@Service
public class FaqContextService {
    public record Entry(long id, String title, String text, List<String> aliases, String url, String sourceCheckedOn) {
        public AiSourceDto source() { return new AiSourceDto("FAQ", id, title, url, sourceCheckedOn); }
    }
    private static final Set<String> STOP = Set.of("cay","bonsai","toi","cho","va","co","la","nhu","nao","khong","gi","nen",
            "bao","nhieu","lan","cach","can","the","de","duoc","khi","voi","trong","cham","soc","sao","lam","thi","hay");
    private static final Set<String> SOURCE_HOSTS = Set.of("www.rhs.org.uk","www.extension.umd.edu","extension.umn.edu");
    private final List<Entry> entries;
    private final String version;

    public FaqContextService() throws IOException {
        this(readResource("ai/bonsai-faq.md"), readResource("ai/knowledge-index.json"));
    }

    FaqContextService(String document, String metadata) {
        var index = new ObjectMapper().readTree(metadata);
        version = index.path("version").asText();
        if (version.isBlank() || !index.path("entries").isArray()) throw new IllegalArgumentException("Invalid knowledge index");
        Map<Long, tools.jackson.databind.JsonNode> records = new HashMap<>();
        for (var record : index.path("entries")) {
            long id = record.path("id").asLong();
            if (id <= 0 || records.put(id, record) != null) throw new IllegalArgumentException("Duplicate knowledge ID");
        }
        List<Entry> parsed = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (String section : document.split("(?m)^## ")) {
            if (!section.matches("(?s)^\\d+\\..*")) continue;
            int end = section.indexOf('\n'), dot = section.indexOf('.');
            if (end < dot) throw new IllegalArgumentException("Invalid knowledge heading");
            long id = Long.parseLong(section.substring(0, dot));
            var record = records.get(id);
            if (!seen.add(id) || record == null) throw new IllegalArgumentException("Missing knowledge metadata");
            String title = section.substring(dot + 1, end).strip(), text = section.substring(end + 1).strip();
            String url = record.path("url").asText(), checked = record.path("sourceCheckedOn").asText();
            var uri = URI.create(url);
            if (!"https".equals(uri.getScheme()) || !SOURCE_HOSTS.contains(uri.getHost()) || uri.getUserInfo() != null
                    || uri.getPort() != -1 || uri.getFragment() != null || uri.getQuery() != null)
                throw new IllegalArgumentException("Knowledge reference must use an approved source");
            if (LocalDate.parse(checked).isAfter(LocalDate.now()) || title.isBlank() || title.length() > 160
                    || text.isBlank() || text.length() > 900 || !record.path("aliases").isArray()
                    || !"SOURCE_CHECKED".equals(record.path("status").asText()))
                throw new IllegalArgumentException("Invalid knowledge content or provenance");
            List<String> aliases = new ArrayList<>();
            for (var alias : record.path("aliases")) {
                if (!alias.isString() || alias.asText().isBlank() || alias.asText().length() > 100 || aliases.size() >= 12)
                    throw new IllegalArgumentException("Invalid knowledge alias");
                aliases.add(alias.asText());
            }
            parsed.add(new Entry(id,title,text,List.copyOf(aliases),url,checked));
        }
        if (parsed.isEmpty() || parsed.size() > 500 || parsed.size() != records.size())
            throw new IllegalArgumentException("Knowledge index and document do not match");
        entries = List.copyOf(parsed);
    }

    private static String readResource(String path) throws IOException {
        try (var input = new ClassPathResource(path).getInputStream()) {
            byte[] bytes = input.readNBytes(524289);
            if (bytes.length > 524288) throw new IOException("Knowledge resource too large");
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public String version() { return version; }

    public List<Entry> retrieve(String question) {
        String normalized = normalize(question);
        Set<String> keywords = tokens(normalized);
        return entries.stream().filter(e -> score(e,normalized,keywords) >= 3)
                .sorted(Comparator.<Entry>comparingDouble(e -> score(e,normalized,keywords)).reversed().thenComparingLong(Entry::id))
                .limit(4).toList();
    }

    private double score(Entry entry, String question, Set<String> keywords) {
        Set<String> title = tokens(normalize(entry.title()));
        Set<String> body = tokens(normalize(entry.text()));
        Set<String> aliases = tokens(normalize(String.join(" ",entry.aliases())));
        double score = 0;
        for (String word : keywords) {
            if (title.contains(word)) score += 4;
            if (aliases.contains(word)) score += 3;
            if (body.contains(word)) score += 0.25;
        }
        for (String alias : entry.aliases()) {
            String phrase = normalize(alias);
            if (phrase.length() >= 4 && (" "+question+" ").contains(" "+phrase+" ")) score += 10;
        }
        return score;
    }

    private static Set<String> tokens(String text) {
        Set<String> words = new HashSet<>(Arrays.asList(text.split("[^a-z0-9]+")));
        words.removeAll(STOP); words.removeIf(w -> w.length() < 2);
        return words;
    }

    public static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
    }
}
