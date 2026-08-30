package com.devpath.knowledge.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

final class KnowledgeChunker {
    private static final int MAX_CHARS = 1800;

    List<ChunkDraft> chunk(String content) {
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.isBlank() || normalized.length() > 200_000) {
            throw new IllegalArgumentException("Knowledge content size is invalid");
        }
        List<ChunkDraft> chunks = new ArrayList<>();
        String activeHeading = null;
        String currentHeading = null;
        StringBuilder current = new StringBuilder();
        for (String paragraph : normalized.split("\n{2,}")) {
            String clean = paragraph.strip();
            if (clean.isBlank()) continue;
            String candidateHeading = heading(clean);
            if (candidateHeading != null) {
                if (!current.isEmpty()) {
                    add(chunks, currentHeading, current.toString());
                    current.setLength(0);
                }
                activeHeading = candidateHeading;
            }
            if (!current.isEmpty() && current.length() + clean.length() + 2 > MAX_CHARS) {
                add(chunks, currentHeading, current.toString()); current.setLength(0);
            }
            if (clean.length() <= MAX_CHARS) {
                if (current.isEmpty()) currentHeading = activeHeading;
                if (!current.isEmpty()) current.append("\n\n");
                current.append(clean);
            } else {
                if (!current.isEmpty()) { add(chunks, currentHeading, current.toString()); current.setLength(0); }
                for (int start = 0; start < clean.length(); start += MAX_CHARS) {
                    add(chunks, activeHeading, clean.substring(start, Math.min(clean.length(), start + MAX_CHARS)));
                }
            }
        }
        if (!current.isEmpty()) add(chunks, currentHeading, current.toString());
        if (chunks.isEmpty() || chunks.size() > 200) throw new IllegalArgumentException("Knowledge chunk count is invalid");
        return List.copyOf(chunks);
    }

    static String hash(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void add(List<ChunkDraft> chunks, String heading, String content) {
        chunks.add(new ChunkDraft(chunks.size(), heading, content, hash(content), Math.max(1, (content.length() + 3) / 4)));
    }

    private static String heading(String value) {
        if (!value.startsWith("#")) return null;
        String result = value.replaceFirst("^#{1,3}\\s*", "").lines().findFirst().orElse("").trim();
        return result.isBlank() ? null : result.substring(0, Math.min(result.length(), 512));
    }

    record ChunkDraft(int position, String heading, String content, String contentHash, int tokenEstimate) {}
}
