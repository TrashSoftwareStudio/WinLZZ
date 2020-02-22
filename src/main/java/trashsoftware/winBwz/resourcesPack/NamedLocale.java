package trashsoftware.winBwz.resourcesPack;

import java.util.Locale;

public class NamedLocale {
    private Locale locale;
    private String description;

    public NamedLocale(String language, String country, String description) {
        this.locale = new Locale(language, country);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getConfigValue() {
        return locale.getLanguage() + "_" + locale.getCountry();
    }

    @Override
    public String toString() {
        return description;
    }
}
