package com.strobel.reflection;

import com.strobel.util.ContractUtils;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 2:50 PM
 */
public enum SimpleType {
    Boolean,
    Byte,
    Character,
    Short,
    Integer,
    Long,
    Float,
    Double,
    Object,
    Array,
    Void;

    public final boolean isPrimitive() {
        switch (this) {
            case Object:
            case Array:
            case Void:
                return false;
            default:
                return true;
        }
    }

    public final boolean isPrimitiveOrVoid() {
        switch (this) {
            case Object:
            case Array:
                return false;
            default:
                return true;
        }
    }

    public final int bitWidth() {
        switch (this) {
            case Boolean:
                return 1;
            case Byte:
                return 8;
            case Character:
            case Short:
                return 16;
            case Integer:
                return 32;
            case Long:
                return 64;
            case Float:
                return 32;
            case Double:
                return 64;
            case Object:
            case Array:
            case Void:
                return 0;
        }
        throw ContractUtils.unreachable();
    }

    public final int stackSlots() {
        switch (this) {
            case Long:
            case Double:
                return 2;
            case Void:
                return 0;
            default:
                return 1;
        }
    }

    public final boolean isSingleWord() {
        switch (this) {
            case Boolean:
            case Byte:
            case Character:
            case Short:
            case Integer:
            case Float:
            case Object:
            case Array:
                return true;
            case Long:
            case Double:
            case Void:
                return false;
        }
        throw ContractUtils.unreachable();
    }

    public final boolean isDoubleWord() {
        switch (this) {
            case Long:
            case Double:
                return true;
            default:
                return false;
        }
    }

    public final boolean isNumeric() {
        switch (this) {
            case Boolean:
            case Byte:
            case Character:
            case Short:
            case Integer:
            case Long:
            case Float:
            case Double:
                return true;
            default:
                return false;
        }
    }

    public final boolean isIntegral() {
        switch (this) {
            case Boolean:
            case Byte:
            case Character:
            case Short:
            case Integer:
            case Long:
                return true;
            default:
            case Float:
            case Double:
                return false;
        }
    }

    public final boolean isSubWordOrInt32() {
        switch (this) {
            case Boolean:
            case Byte:
            case Character:
            case Short:
            case Integer:
                return true;
            default:
                return false;
        }
    }

    public final boolean isSigned() {
        switch (this) {
            default:
            case Boolean:
            case Character:
                return false;
            case Byte:
            case Short:
            case Integer:
            case Long:
            case Float:
            case Double:
                return true;
        }
    }

    public final boolean isUnsigned() {
        switch (this) {
            case Boolean:
            case Character:
                return true;
            default:
                return false;
        }
    }

    public final boolean isFloating() {
        switch (this) {
            case Float:
            case Double:
                return true;
            default:
                return false;
        }
    }

    public final boolean isOther() {
        switch (this) {
            case Object:
            case Array:
            case Void:
                return true;
            default:
                return false;
        }
    }
}
