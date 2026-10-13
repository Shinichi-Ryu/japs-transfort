package com.japstransfort.entity;

import org.hibernate.type.descriptor.java.EnumJavaType;

/**
 * Custom Hibernate JavaType for MoveType enum mapping to PostgreSQL native enum 'move_type'.
 * Translates 'return_' enum constant to/from database string 'return'.
 */
public class MoveTypeJavaType extends EnumJavaType<MoveType> {

    public MoveTypeJavaType() {
        super(MoveType.class);
    }

    @Override
    public String toName(MoveType value) {
        return value != null ? value.getDbValue() : null;
    }

    @Override
    public MoveType fromName(String name) {
        return MoveType.fromDbValue(name);
    }

    @Override
    public String toString(MoveType value) {
        return value != null ? value.getDbValue() : null;
    }

    @Override
    public MoveType fromString(CharSequence string) {
        return string != null ? MoveType.fromDbValue(string.toString()) : null;
    }
}
