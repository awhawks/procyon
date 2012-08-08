package com.strobel.reflection.emit;

import com.strobel.core.ReadOnlyList;
import com.strobel.core.VerifyArgument;
import com.strobel.reflection.MethodInfo;
import com.strobel.reflection.MethodList;
import com.strobel.reflection.Type;
import com.strobel.reflection.Types;
import sun.reflect.annotation.AnnotationType;

import java.lang.annotation.Annotation;

/**
 * @author Mike Strobel
 */
public final class AnnotationBuilder<A extends Annotation> {
    private final Type<A> _annotationType;
    private final MethodList _attributes;
    private final ReadOnlyList<Object> _values;
    
    private A _bakedAnnotation;

    private AnnotationBuilder(
        final Type<A> annotationType,
        final MethodList attributes,
        final ReadOnlyList<Object> values) {

        _annotationType = VerifyArgument.notNull(annotationType, "annotationType");
        _attributes = VerifyArgument.notNull(attributes, "properties");
        _values = VerifyArgument.notNull(values, "values");
    }

    public Type<A> getAnnotationType() {
        return _annotationType;
    }

    public MethodList getAttributes() {
        return _attributes;
    }

    public ReadOnlyList<Object> getValues() {
        return _values;
    }

    public static <A extends Annotation> AnnotationBuilder<A> create(
        final Type<A> annotationType,
        final MethodList properties,
        final ReadOnlyList<Object> values) {

        checkProperties(
            VerifyArgument.notNull(annotationType, "annotationType"),
            properties,
            values
        );

        return new AnnotationBuilder<>(
            annotationType,
            properties != null ? properties : MethodList.empty(),
            values != null ? values : ReadOnlyList.emptyList()
        );
    }

    public static <A extends Annotation> AnnotationBuilder<A> create(final Type<A> annotationType) {
        checkProperties(
            VerifyArgument.notNull(annotationType, "annotationType"),
            MethodList.empty(),
            ReadOnlyList.emptyList()
        );

        return new AnnotationBuilder<>(annotationType, MethodList.empty(), ReadOnlyList.emptyList());
    }

    public static <A extends Annotation> AnnotationBuilder<A> create(final Type<A> annotationType, final Object value) {
        VerifyArgument.notNull(annotationType, "annotationType");

        final MethodInfo valueProperty = annotationType.getMethod("value");

        if (valueProperty == null) {
            throw Error.annotationHasNoDefaultAttribute();
        }

        checkProperties(
            annotationType,
            new MethodList(valueProperty),
            new ReadOnlyList<>(value)
        );

        return new AnnotationBuilder<>(
            annotationType,
            new MethodList(valueProperty),
            new ReadOnlyList<>(value)
        );
    }

    @SuppressWarnings("ConstantConditions")
    private static <A extends Annotation> void checkProperties(
        final Type<A> annotationType,
        final MethodList properties,
        final ReadOnlyList<Object> values) {

        final int valueCount;
        final int propertyCount;

        if (!Types.Annotation.isAssignableFrom(annotationType)) {
            throw Error.typeNotAnAnnotation(annotationType);
        }

        if (values != null) {
            valueCount = VerifyArgument.noNullElements(properties, "values").size();
        }
        else {
            valueCount = 0;
        }

        if (properties != null) {
            propertyCount = VerifyArgument.noNullElements(properties, "properties").size();
        }
        else {
            propertyCount = 0;
        }

        if (propertyCount != valueCount) {
            throw Error.attributeValueCountMismatch();
        }

        if (valueCount == 0) {
            final MethodInfo defaultProperty = annotationType.getMethod("value");

            if (defaultProperty != null) {
                throw Error.annotationRequiresValue(annotationType);
            }
        }

        for (int i = 0; i < propertyCount; i++) {
            final Type<?> propertyType = properties.get(i).getReturnType();
            final Object value = values.get(i);

            if (value instanceof AnnotationBuilder) {
                final AnnotationBuilder valueAnnotation = (AnnotationBuilder)value;

                if (!propertyType.isAssignableFrom(valueAnnotation.getAnnotationType())) {
                    throw Error.attributeValueIncompatible(
                        propertyType,
                        valueAnnotation.getAnnotationType()
                    );
                }
            }
            else if (value == null || !propertyType.isAssignableFrom(Type.of(value.getClass()))) {
                throw Error.attributeValueIncompatible(
                    propertyType,
                    value != null ? Type.of(value.getClass()) : null
                );
            }
        }
    }

    void bake() {
        if (_bakedAnnotation != null) {
            return;
        }

        final AnnotationType instance = AnnotationType.getInstance(_annotationType.getErasedClass());
    }
}
