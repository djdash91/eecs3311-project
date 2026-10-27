package ca.yorku.accessfix.wcag;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * The local WCAG 2.2 knowledge base (F05). Loads src/main/resources/wcag/wcag-criteria.json when the
 * application starts; a load failure stops start-up with a clear message (F05, second error case).
 *
 * <p>Note: Spring Boot 4 uses Jackson 3, whose packages start with {@code tools.jackson}. The
 * Anthropic SDK brings Jackson 2 ({@code com.fasterxml.jackson}). Both can be on the classpath.
 * If an AI tool suggests {@code com.fasterxml.jackson.databind.ObjectMapper} here, that is the old one.
 */
@Service
public class WcagKnowledgeBase {

    static final String DATA_FILE = "/wcag/wcag-criteria.json";

    private final List<WcagCriterion> entries;

    public WcagKnowledgeBase() {
        this.entries = load(DATA_FILE);
    }

    /** For tests: a knowledge base with your own entries. */
    public WcagKnowledgeBase(List<WcagCriterion> entries) {
        this.entries = List.copyOf(entries);
    }

    private static List<WcagCriterion> load(String path) {
        JsonMapper mapper = JsonMapper.builder().build();
        try (InputStream in = WcagKnowledgeBase.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("WCAG data file not found on the classpath: " + path);
            }
            return List.copyOf(mapper.readValue(in, new TypeReference<List<WcagCriterion>>() { }));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read WCAG data file " + path, e);
        }
    }

    public List<WcagCriterion> all() {
        return entries;
    }

    public Optional<WcagCriterion> get(String number) {
        return entries.stream().filter(c -> c.number().equals(number)).findFirst();
    }

    /**
     * TODO(F05): match the query's words against each entry's number, title and summary, and rank
     * entries by how many words match (Stage 1, F05). Return an empty list when nothing matches, so
     * the agent doesn't invent a criterion.
     *
     * <p>Decide and test: case sensitivity, punctuation, very short words ("a", "of"), ties in the
     * ranking (keep it deterministic), and a maximum number of results.
     */
    public List<WcagCriterion> search(String query) {
        throw new UnsupportedOperationException("TODO: WcagKnowledgeBase.search");
    }
}
