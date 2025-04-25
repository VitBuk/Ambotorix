package vitbuk.com.Ambotorix.entities;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum CivMap {
    ARCHIPELAGO("Archipelago"),
    CONTINENTS("Continents"),
    CONTINENTS_AND_ISLANDS("ContinentsAndIslands"),
    EARTH("Earth"),
    EARTH_HUGE("EarthHuge"),
    FOUR_LEAF_CLOVER("Clover"),
    FRACTAL("Fractal"),
    HIGHLANDS("Highlands"),
    INLAND_SEA("InlandSea"),
    ISLAND_PLATES("IslandPlates"),
    LAKES("Lakes"),
    MEDITERRANEAN_LARGE("MediterraneanLarge"),
    MIRROR("Mirror"),
    PANGEA("Pangea"),
    PRIMORDIAL("Primordial"),
    RICH_HIGHLANDS("RichHighlands"),
    SEVEN_SEAS("SevenSeas"),
    SHUFFLE("Shuffle"),
    SIX_ARMED_SNOWFLAKE("Snowflake"),
    SMALL_CONTINENTS("SmallContinents"),
    SPLINTERED_FRACTAL("SplinteredFractal"),
    TERRA("Terra"),
    TILTED_AXIS("TiltedAxis"),
    TILTED_AXIS_WRAPAROUND("TiltedAxisWrapAround"),
    TRUE_START_LOCATION_EARTH_HUGE("TrueStartEarthHuge"),
    TRUE_START_LOCATION_MEDITERRANEAN("TrueStartMediterranean"),
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

    public static Optional<CivMap> fromDisplayNameIgnoreCase(String name) {
        return Arrays.stream(values())
                .filter(c -> c.displayName.equalsIgnoreCase(name))
                .findFirst();
    }

}