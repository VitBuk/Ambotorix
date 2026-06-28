package vitbuk.com.Ambotorix.entities;

import java.util.Objects;
import java.util.Set;

public class Leader {
    // Lowercased words that stay lowercase inside a prettified qualifier (e.g. "Age of Empire").
    private static final Set<String> SMALL_WORDS = Set.of("of", "the", "de", "a", "an", "and", "di", "da");

    String fullName;
    String shortName;
    String description;
    String picPath;

    public Leader(String fullName, String shortName, String description, String picPath) {
        this.fullName = fullName;
        this.shortName = shortName;
        this.description = description;
        this.picPath = picPath;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getShortName() {
        return shortName;
    }

    // Compact, unambiguous label for UI (e.g. buttons), derived from the shortName:
    // "lincoln" -> "Lincoln", "roosevelt_bull_moose" -> "Roosevelt (Bull Moose)".
    public String getDisplayName() {
        if (shortName == null || shortName.isBlank()) {
            return fullName;
        }
        String[] parts = shortName.split("_");
        String name = capitalize(parts[0]);
        if (parts.length == 1) {
            return name;
        }
        StringBuilder qualifier = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            if (i > 1) {
                qualifier.append(" ");
            }
            qualifier.append(SMALL_WORDS.contains(parts[i]) ? parts[i] : capitalize(parts[i]));
        }
        return name + " (" + qualifier + ")";
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPicPath() {
        return picPath;
    }

    public void setPicPath(String picPath) {
        this.picPath = picPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Leader leader = (Leader) o;
        return fullName.equals(leader.fullName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName);
    }
}