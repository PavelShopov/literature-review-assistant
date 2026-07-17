package mk.ukim.finki.literaturereviewassistant.service;

import java.util.Locale;

public final class TaxonomyTextNormalizer {
    private TaxonomyTextNormalizer() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
