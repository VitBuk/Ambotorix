package vitbuk.com.Ambotorix.entities;

import java.util.List;

public enum CivMap {
    ARCHIPELAGO("Archipelago"),
    CONTINENTS("Continents"),
    CONTINENTS_AND_ISLANDS("Continents and Islands"),
    EARTH("Earth"),
    EARTH_HUGE("Earth Huge"),
    FOUR_LEAF_CLOVER("4-Leaf Clover"),
    FRACTAL("Fractal"),
    HIGHLANDS("Highlands"),
    INLAND_SEA("Inland Sea"),
    ISLAND_PLATES("Island Plates"),
    LAKES("Lakes"),
    MEDITERRANEAN_LARGE("Mediterranean Large"),
    MIRROR("Mirror"),
    PANGEA("Pangea"),
    PRIMORDIAL("Primordial"),
    RICH_HIGHLANDS("Rich Highlands"),
    SEVEN_SEAS("Seven Seas"),
    SHUFFLE("Shuffle"),
    SIX_ARMED_SNOWFLAKE("6-Armed Snowflake"),
    SMALL_CONTINENTS("Small Continents"),
    SPLINTERED_FRACTAL("Splintered Fractal"),
    TERRA("Terra"),
    TILTED_AXIS("Tilted Axis"),
    TILTED_AXIS_WRAPAROUND("Tilted Axis Wrap around"),
    TRUE_START_LOCATION_EARTH_HUGE("True Start Location Earth Huge"),
    TRUE_START_LOCATION_MEDITERRANEAN("True Start Location Mediterranean"),
    WETLANDS("Wetlands");

    private final String displayName;

    CivMap(String displayName) {
        this.displayName = displayName;
    }

    public static final List<CivMap> STANDARD_MAPS = List.of(PANGEA, SEVEN_SEAS, HIGHLANDS, LAKES);

    @Override
    public String toString() {
        return displayName;
    }
}

