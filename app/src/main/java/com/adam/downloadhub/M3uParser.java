package com.adam.downloadhub;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class M3uParser {
    private static final Pattern ATTR = Pattern.compile("([A-Za-z0-9_-]+)=\\\"([^\\\"]*)\\\"");
    private static final Pattern EPISODE = Pattern.compile(".*(?:^|[^a-z0-9])s\\d{1,2}[ ._\\-]?e\\d{1,3}(?:[^a-z0-9]|$).*");

    private M3uParser() {}

    public interface EntryConsumer {
        void accept(Entry entry) throws Exception;
    }

    /**
     * Memory-safe parser for very large M3U/M3U8 files.
     * Only one line and one entry are held in memory at a time.
     */
    public static long parseStream(InputStream input, EntryConsumer consumer) throws Exception {
        if (input == null) throw new IllegalArgumentException("مصدر M3U غير صالح");
        if (consumer == null) throw new IllegalArgumentException("معالج M3U غير صالح");

        long count = 0;
        String currentInfo = null;
        String currentName = null;
        String currentGroup = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8), 64 * 1024)) {
            String raw;
            while ((raw = reader.readLine()) != null) {
                String line = raw.trim();
                if (line.startsWith("\uFEFF")) line = line.substring(1).trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("#EXTINF")) {
                    currentInfo = line;
                    currentGroup = attr(line, "group-title");
                    int comma = line.indexOf(',');
                    currentName = comma >= 0 && comma + 1 < line.length()
                            ? line.substring(comma + 1).trim()
                            : attr(line, "tvg-name");
                    if (currentName == null || currentName.isEmpty()) currentName = "بدون اسم";
                    continue;
                }

                if (line.startsWith("#")) continue;

                if (currentInfo != null) {
                    consumer.accept(new Entry(
                            currentName,
                            currentGroup == null ? "" : currentGroup,
                            line,
                            currentInfo));
                    count++;
                    currentInfo = null;
                    currentName = null;
                    currentGroup = null;
                }
            }
        }
        return count;
    }

    // Kept for compatibility with smaller in-memory inputs.
    public static List<Entry> parse(String content) {
        List<Entry> out = new ArrayList<>();
        if (content == null) return out;

        String currentInfo = null;
        String currentName = null;
        String currentGroup = null;

        String[] lines = content.replace("\r", "").split("\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("#EXTINF")) {
                currentInfo = line;
                currentGroup = attr(line, "group-title");
                int comma = line.indexOf(',');
                currentName = comma >= 0 && comma + 1 < line.length()
                        ? line.substring(comma + 1).trim()
                        : attr(line, "tvg-name");
                if (currentName == null || currentName.isEmpty()) currentName = "بدون اسم";
                continue;
            }

            if (line.startsWith("#")) continue;
            if (currentInfo != null) {
                out.add(new Entry(currentName, currentGroup == null ? "" : currentGroup, line, currentInfo));
                currentInfo = null;
                currentName = null;
                currentGroup = null;
            }
        }
        return out;
    }

    public static Result classify(List<Entry> entries) {
        Result r = new Result();
        for (Entry e : entries) {
            Type type = classifyOne(e);
            if (type == Type.SERIES) r.series.add(e);
            else if (type == Type.MOVIE) r.movies.add(e);
            else r.channels.add(e);
        }
        return r;
    }

    public static Type classifyOne(Entry e) {
        String s = (e.name + " " + e.group + " " + e.url).toLowerCase(Locale.ROOT);

        if (containsAny(s,
                "/series/", " series ", "series/", "tv series", "tv shows", "season", "episode",
                "مسلسل", "مسلسلات", "موسم", "حلقة") || EPISODE.matcher(s).matches()) {
            return Type.SERIES;
        }

        if (containsAny(s,
                "/movie/", " movies ", "movie/", " movie ", " films ", "film/", " cinema ",
                "أفلام", "افلام", "فيلم")) {
            return Type.MOVIE;
        }

        if (containsAny(s, "/live/", " live ", "قنوات", "قناة") || s.matches(".*\\.ts(\\?.*)?$")) {
            return Type.CHANNEL;
        }

        if (s.matches(".*\\.(mp4|mkv|avi|mov|m4v|webm)(\\?.*)?$")) {
            return Type.MOVIE;
        }

        return Type.CHANNEL;
    }

    private static boolean containsAny(String s, String... tokens) {
        for (String t : tokens) if (s.contains(t)) return true;
        return false;
    }

    private static String attr(String line, String name) {
        Matcher m = ATTR.matcher(line);
        while (m.find()) {
            if (name.equalsIgnoreCase(m.group(1))) return m.group(2).trim();
        }
        return "";
    }

    public enum Type { CHANNEL, MOVIE, SERIES }

    public static final class Entry {
        public final String name;
        public final String group;
        public final String url;
        public final String extInf;

        public Entry(String name, String group, String url, String extInf) {
            this.name = name == null ? "" : name;
            this.group = group == null ? "" : group;
            this.url = url == null ? "" : url;
            this.extInf = extInf == null ? "" : extInf;
        }
    }

    public static final class Result {
        public final List<Entry> channels = new ArrayList<>();
        public final List<Entry> movies = new ArrayList<>();
        public final List<Entry> series = new ArrayList<>();
    }
}
