package ca.yorku.accessfix.wcag;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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

    /** Enough for the agent and the search box; more would mostly be weak, one-word matches. */
    static final int MAX_RESULTS = 5;

    /** Common words that would match almost every entry, such as "the" in "what is the page title". */
    private static final Set<String> IGNORED_WORDS = Set.of("a", "an", "and", "are", "about", "as", "at", "be",
            "by", "can", "do", "does", "for", "how", "in", "is", "it", "its", "of", "on", "or", "so", "that", "the",
            "this", "to", "what", "when", "which", "with");

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
     * Matches the query's words against each entry's number, title and summary, and ranks entries by
     * how many different query words they contain (F05). Returns at most MAX_RESULTS entries, and an
     * empty list when nothing matches, so the agent doesn't invent a criterion.
     *
     * <p>Words are compared whole, ignoring case, punctuation, a plural "s" and IGNORED_WORDS, so
     * "Form labels?" matches "label" and "form". Entries with the same number of matches keep the
     * data file's order, so the same query always gives the same result.
     */
    public List<WcagCriterion> search(String query) {
        Set<String> queryWords = words(query == null ? "" : query);
        if (queryWords.isEmpty()) {
            return List.of();
        }
        record Match(WcagCriterion criterion, long matchedWords) {
        }
        return entries.stream()
                .map(c -> new Match(c, countMatches(queryWords, words(c.number() + " " + c.title() + " " + c.summary()))))
                .filter(match -> match.matchedWords() > 0)
                .sorted(Comparator.comparingLong(Match::matchedWords).reversed())
                .limit(MAX_RESULTS)
                .map(Match::criterion)
                .toList();
    }

    private static long countMatches(Set<String> queryWords, Set<String> entryWords) {
        return queryWords.stream().filter(entryWords::contains).count();
    }

    /**
     * The text's words for matching: lower case, split at anything that isn't a letter, digit or dot
     * (so "1.4.3" stays one word), without dots at either end, ignoring single letters and
     * IGNORED_WORDS, and with a plural "s" removed from words longer than three letters.
     */
    static Set<String> words(String text) {
        Set<String> words = new LinkedHashSet<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}.]+")) {
            String word = token.replaceAll("^\\.+|\\.+$", "");
            if (word.length() < 2 || IGNORED_WORDS.contains(word)) {
                continue;
            }
            words.add(word.length() > 3 && word.endsWith("s") ? word.substring(0, word.length() - 1) : word);
        }
        return words;
    }
}
