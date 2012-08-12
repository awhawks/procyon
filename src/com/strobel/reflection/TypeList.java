package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike Strobel
 */
public class TypeList extends MemberList<Type> {
    private final static TypeList EMPTY = new TypeList();

    public static TypeList empty() {
        return EMPTY;
    }

    public static TypeList combine(final TypeList first, final TypeList second) {
        return combineCore(first, second, false);
    }

    private static TypeList combineCore(final TypeList first, final TypeList second, final boolean merge) {
        VerifyArgument.notNull(first, "first");
        VerifyArgument.notNull(second, "second");

        if (first.isEmpty()) {
            return second;
        }

        if (second.isEmpty()) {
            return first;
        }

        final ArrayList<Type<?>> types = new ArrayList<>();

        for (int i = 0, n = first.size(); i < n; i++) {
            final Type type = first.get(i);
            if (!merge || !types.contains(type)) {
                types.add(type);
            }
        }

        for (int i = 0, n = second.size(); i < n; i++) {
            final Type type = second.get(i);
            if (!merge || !types.contains(type)) {
                types.add(type);
            }
        }

        return new TypeList(types);
    }

    public TypeList(final Type... elements) {
        super(Type.class, elements);
    }

    public TypeList(final List<? extends Type> elements) {
        super(Type.class, elements);
    }

    public TypeList(final Type[] elements, final int offset, final int length) {
        super(Type.class, elements, offset, length);
    }

    @Override
    public TypeList subList(final int fromIndex, final int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size());

        final int offset = getOffset() + fromIndex;
        final int length = toIndex - fromIndex;

        if (length == 0) {
            return empty();
        }

        return new TypeList(getElements(), offset, length);
    }

    public final boolean containsGenericParameters() {
        for (int i = 0, n = this.size(); i < n; i++) {
            if (this.get(i).containsGenericParameters()) {
                return true;
            }
        }
        return false;
    }

    public final boolean containsSubTypeOf(final Type<?> type) {
        for (int i = 0, n = this.size(); i < n; i++) {
            if (this.get(i).isSubTypeOf(type)) {
                return true;
            }
        }
        return false;
    }

    public final boolean containsSuperTypeOf(final Type<?> type) {
        for (int i = 0, n = this.size(); i < n; i++) {
            if (type.isSubTypeOf(this.get(i))) {
                return true;
            }
        }
        return false;
    }

    public final boolean containsTypeAssignableFrom(final Type<?> type) {
        for (int i = 0, n = this.size(); i < n; i++) {
            if (this.get(i).isAssignableFrom(type)) {
                return true;
            }
        }
        return false;
    }
}
