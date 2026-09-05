package com.adam.downloadhub;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lightweight link normalizer. Removes common tracking parameters without touching media-specific parameters. */
public final class LinkTools {
    private LinkTools() {}

    public static String clean(String raw) {
        if (raw == null) return "";
        String input = raw.trim();
        if (!input.startsWith("http://") && !input.startsWith("https://")) return input;
        try {
            URI u = new URI(input);
            String query = u.getRawQuery();
            if (query == null || query.isEmpty()) return stripFragment(u).toString();
            List<String> kept = new ArrayList<>();
            for (String pair : query.split("&")) {
                if (pair.isEmpty()) continue;
                String key = pair;
                int eq = pair.indexOf('=');
                if (eq >= 0) key = pair.substring(0, eq);
                try { key = URLDecoder.decode(key, StandardCharsets.UTF_8.name()); } catch (Exception ignored) {}
                String k = key.toLowerCase(Locale.ROOT);
                if (k.startsWith("utm_") || k.equals("fbclid") || k.equals("gclid") || k.equals("dclid")
                        || k.equals("igshid") || k.equals("mc_cid") || k.equals("mc_eid")
                        || k.equals("ref_src") || k.equals("ref_url") || k.equals("feature")
                        || k.equals("si") || k.equals("share_id")) continue;
                kept.add(pair);
            }
            String cleanedQuery = kept.isEmpty() ? null : join(kept, "&");
            return new URI(u.getScheme(), u.getRawAuthority(), u.getRawPath(), cleanedQuery, null).toString();
        } catch (Exception e) {
            int hash = input.indexOf('#');
            return hash >= 0 ? input.substring(0, hash) : input;
        }
    }

    private static URI stripFragment(URI u) throws Exception {
        return new URI(u.getScheme(), u.getRawAuthority(), u.getRawPath(), u.getRawQuery(), null);
    }

    private static String join(List<String> xs, String sep) {
        StringBuilder b = new StringBuilder();
        for (String s : xs) { if (b.length() > 0) b.append(sep); b.append(s); }
        return b.toString();
    }
}
