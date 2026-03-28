// This includes all base files for pets needed for them to actual work
// and the code used to apply sell multipliers

// 3/7/26 | PetsSource
// 3/7/26 | Jackw

package dev.jackwith.skyCoreV2.features.pets.gui;

public enum SortType {
    HIGHEST_BOOST("Highest Boost"),
    LOWEST_BOOST("Lowest Boost"),
    NEWEST("Newest First"),
    OLDEST("Oldest First");

    private final String displayName;
    SortType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }

    public SortType next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}
