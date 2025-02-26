package vitbuk.com.Ambotorix.entities;

public enum CivMap {
    CONTINENTS("Continents"),
    FRACTAL("Fractal"),
    INLAND_SEA("Inland Sea"),
    ISLAND_PLATES("Island Plates"),
    LAKES("Lakes"),
    PANGEA("Pangea"),
    TRUE_START_LOCATION_EARTH_HUGE("True Start Location Earth Huge"),
    SEVEN_SEAS("Seven Seas"),
    SHUFFLE("Shuffle"),
    SMALL_CONTINENTS("Small Continents"),
    TERRA("Terra"),
    MEDITERRANEAN_LARGE("Mediterranean Large"),
    FOUR_LEAF_CLOVER("4-Leaf Clover"),
    SIX_ARMED_SNOWFLAKE("6-Armed Snowflake"),
    MIRROR("Mirror"),
    TRUE_START_LOCATION_MEDITERRANEAN("True Start Location Mediterranean"),
    EARTH("Earth"),
    EARTH_HUGE("Earth Huge"),
    ARCHIPELAGO("Archipelago"),
    HIGHLANDS("Highlands"),
    WETLANDS("Wetlands"),
    CONTINENTS_AND_ISLANDS("Continents and Islands"),
    PRIMORDIAL("Primordial"),
    SPLINTERED_FRACTAL("Splintered Fractal"),
    TILTED_AXIS("Tilted Axis"),
    TILTED_AXIS_WRAPAROUND("Tilted Axis Wraparound"),
    RICH_HIGHLANDS("Rich Highlands");

    private final String displayName;

    CivMap(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

