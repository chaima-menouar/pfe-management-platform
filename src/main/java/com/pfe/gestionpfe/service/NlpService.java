package com.pfe.gestionpfe.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NlpService {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "le", "la", "les", "de", "des", "du", "et", "ou",
            "pour", "avec", "sur", "en", "au", "aux", "un", "une",
            "dans", "par", "via", "d", "l"
    ));

    private static final Map<String, List<String>> SYNONYMS = Map.of(
            "ia", List.of("intelligence", "artificielle"),
            "ai", List.of("intelligence", "artificielle"),
            "bigdata", List.of("data", "donnees"),
            "big", List.of("data"),
            "datamining", List.of("data", "donnees"),
            "donnees", List.of("data"),
            "web", List.of("application", "site"),
            "ml", List.of("machine", "learning"),
            "iot", List.of("internet", "objets")
    );

    public String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        text = text.toLowerCase();

        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = text.replaceAll("\\p{M}", "");
        text = text.replaceAll("[^a-z0-9 ]", " ");
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    public Set<String> extractKeywords(String text) {
        String normalized = normalizeText(text);

        if (normalized.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> words = Arrays.stream(normalized.split(" "))
                .filter(word -> word.length() > 1)
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(Collectors.toSet());

        Set<String> enriched = new HashSet<>(words);

        for (String word : words) {
            if (SYNONYMS.containsKey(word)) {
                enriched.addAll(SYNONYMS.get(word));
            }
        }

        return enriched;
    }

    public int calculateSimilarityScore(String themePfe, String specialite) {
        Set<String> themeKeywords = extractKeywords(themePfe);
        Set<String> specialiteKeywords = extractKeywords(specialite);

        int score = 0;

        for (String word : themeKeywords) {
            if (specialiteKeywords.contains(word)) {
                score++;
            }
        }

        return score;
    }
}