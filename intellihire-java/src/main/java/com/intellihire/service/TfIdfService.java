package com.intellihire.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Pure-Java TF-IDF + cosine similarity implementation.
 * Identical algorithm to the Python version — no external ML libraries.
 */
@Service
public class TfIdfService {

    /** Stop words to ignore during tokenisation */
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a","an","the","and","or","but","in","on","at","to","for","of","with",
        "by","from","is","are","was","were","be","been","being","have","has",
        "had","do","does","did","will","would","could","should","may","might",
        "i","we","you","he","she","it","they","this","that","these","those"
    ));

    /** Tokenise a document into lowercase word tokens, removing stop words. */
    public List<String> tokenise(String text) {
        List<String> tokens = new ArrayList<>();
        for (String token : text.toLowerCase().split("[^a-z0-9+#.]+")) {
            if (token.length() > 1 && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    /** Compute term frequency map for a document. */
    public Map<String, Double> termFrequency(List<String> tokens) {
        Map<String, Integer> counts = new HashMap<>();
        for (String t : tokens) counts.merge(t, 1, Integer::sum);
        Map<String, Double> tf = new HashMap<>();
        double total = tokens.isEmpty() ? 1.0 : tokens.size();
        counts.forEach((k, v) -> tf.put(k, v / total));
        return tf;
    }

    /** Compute IDF across a list of token-lists (one per document). */
    public Map<String, Double> inverseDocumentFrequency(List<List<String>> corpus) {
        int N = corpus.size();
        Map<String, Integer> docFreq = new HashMap<>();
        for (List<String> doc : corpus) {
            for (String term : new HashSet<>(doc)) {
                docFreq.merge(term, 1, Integer::sum);
            }
        }
        Map<String, Double> idf = new HashMap<>();
        docFreq.forEach((term, df) ->
            idf.put(term, Math.log((double) N / (1.0 + df)))
        );
        return idf;
    }

    /** Compute TF-IDF vector for a document given a pre-computed IDF map. */
    public Map<String, Double> tfidfVector(Map<String, Double> tf, Map<String, Double> idf) {
        Map<String, Double> vec = new HashMap<>();
        tf.forEach((term, tfVal) ->
            vec.put(term, tfVal * idf.getOrDefault(term, 0.0))
        );
        return vec;
    }

    /** Cosine similarity between two TF-IDF vectors. */
    public double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0.0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            dot += e.getValue() * b.getOrDefault(e.getKey(), 0.0);
        }
        double normA = Math.sqrt(a.values().stream().mapToDouble(v -> v * v).sum());
        double normB = Math.sqrt(b.values().stream().mapToDouble(v -> v * v).sum());
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (normA * normB);
    }

    /**
     * Compute cosine similarity between two raw text strings.
     * Builds a 2-document corpus so IDF is meaningful.
     */
    public double similarity(String textA, String textB) {
        List<String> tokensA = tokenise(textA);
        List<String> tokensB = tokenise(textB);
        List<List<String>> corpus = Arrays.asList(tokensA, tokensB);
        Map<String, Double> idf = inverseDocumentFrequency(corpus);
        Map<String, Double> vecA = tfidfVector(termFrequency(tokensA), idf);
        Map<String, Double> vecB = tfidfVector(termFrequency(tokensB), idf);
        return cosineSimilarity(vecA, vecB);
    }
}
