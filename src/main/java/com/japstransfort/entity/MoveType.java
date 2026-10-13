package com.japstransfort.entity;

/**
 * Matches the PostgreSQL move_type ENUM type:
 * ('in', 'out', 'adjust', 'return').
 * 'return' is a reserved keyword in Java, represented as 'return_'.
 */
public enum MoveType {
    in("in"),
    out("out"),
    adjust("adjust"),
    return_("return");

    private final String dbValue;

    MoveType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static MoveType fromDbValue(String value) {
        if (value == null) return null;
        for (MoveType m : values()) {
            if (m.dbValue.equalsIgnoreCase(value) || m.name().equalsIgnoreCase(value)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Unknown MoveType: " + value);
    }
}
