package com.strobel.reflection.emit;

import com.strobel.core.ReadOnlyList;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.reflection.*;
import com.strobel.util.TypeUtils;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.jvm.Target;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.util.ArrayList;

import static com.sun.tools.javac.code.Flags.*;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("MismatchedReadAndWriteOfArray")
final class ClassWriter {
    private final static int JAVA_MAGIC = 0xCAFEBABE;

    private final static int DATA_BUFFER_SIZE = 0x0fff0;
    private final static int POOL_BUFFER_SIZE = 0x1fff0;

    private final static int SAME_FRAME_SIZE = 64;
    private final static int SAME_LOCALS_1_STACK_ITEM_EXTENDED = 247;
    private final static int SAME_FRAME_EXTENDED = 251;
    private final static int FULL_FRAME = 255;
    private final static int MAX_LOCAL_LENGTH_DIFF = 4;

    private final BytecodeStream _dataBuffer;
    private final BytecodeStream _poolBuffer;
    private final BytecodeStream _signatureBuffer;
    private final TypeBuilder<?> _typeBuilder;
    private final ConstantPool _constantPool;

    ClassWriter(final TypeBuilder<?> typeBuilder) {
        _typeBuilder = VerifyArgument.notNull(typeBuilder, "typeBuilder");
        _constantPool = typeBuilder.constantPool;
        _dataBuffer = new BytecodeStream(DATA_BUFFER_SIZE);
        _poolBuffer = new BytecodeStream(POOL_BUFFER_SIZE);
        _signatureBuffer = new BytecodeStream();
    }

    public void writeClass(final OutputStream out)
        throws IOException {
        final TypeBuilder<?> t = _typeBuilder;

        assert (t.getModifiers() & Flags.COMPOUND) == 0;

        _dataBuffer.reset(DATA_BUFFER_SIZE);
        _poolBuffer.reset(POOL_BUFFER_SIZE);
        _signatureBuffer.reset();

        final Type<?> baseType = t.getBaseType();
        final TypeList interfaceTypes = t.getInterfaces();

        int flags = t.getModifiers();

        if ((flags & PROTECTED) != 0) {
            flags |= PUBLIC;
        }

        flags = flags & ClassFlags & ~STRICTFP;

        if ((flags & INTERFACE) == 0) {
            flags |= ACC_SUPER;
        }

        if (t.isNested() && StringUtilities.isNullOrEmpty(t.getName())) {
            flags &= ~FINAL;
        }

        _dataBuffer.putShort(flags);
        _dataBuffer.putShort(t.getTypeToken(t));

        if (baseType != null && baseType.isClass()) {
            _dataBuffer.putShort(t.getTypeToken(baseType));
        }
        else {
            _dataBuffer.putShort(0);
        }

        _dataBuffer.putShort(interfaceTypes.size());

        for (final Type interfaceType : interfaceTypes) {
            _dataBuffer.putShort(t.getTypeToken(interfaceType));
        }

        final int fieldCount = t.fieldBuilders.size();
        final int methodCount = t.methodBuilders.size();

        _dataBuffer.putShort(fieldCount);

        writeFields();

        _dataBuffer.putShort(methodCount);

        writeMethods();

        final int attributeCountIndex = beginAttributes();
        int attributeCount = 0;

        boolean signatureRequired = t.isGenericType() ||
                                    baseType != null && baseType.containsGenericParameters();

        if (!signatureRequired) {
            for (final Type interfaceType : interfaceTypes) {
                signatureRequired |= interfaceType.containsGenericParameters();
            }
        }

        if (signatureRequired) {
            final int attributeLengthIndex = writeAttribute("Signature");

            _dataBuffer.putShort(t.getStringToken(t.getGenericSignature()));

            endAttribute(attributeLengthIndex);
            attributeCount++;
        }

        attributeCount += writeFlagAttributes(flags);
        attributeCount += writeJavaAnnotations(t.getCustomAnnotations());
//        attributeCount += writeEnclosingMethodAttribute(t);

        _poolBuffer.putInt(JAVA_MAGIC);
        _poolBuffer.putShort(Target.JDK1_7.minorVersion);
        _poolBuffer.putShort(Target.JDK1_7.majorVersion);

        t.constantPool.write(_poolBuffer);

/*
        final TypeList nestedTypes = t.getNestedTypes(BindingFlags.AllDeclared);

        if (!nestedTypes.isEmpty()) {
            writeInnerTypes();
            attributeCount++;
        }
*/

        endAttributes(attributeCountIndex, attributeCount);

        _poolBuffer.putByteArray(_dataBuffer.getData(), 0, _dataBuffer.getLength());

        out.write(_poolBuffer.getData(), 0, _poolBuffer.getLength());
    }

    private void writeInnerTypes() {
        // TODO: Write inner types.
    }

    private void writeFields() {
        for (final FieldBuilder field : _typeBuilder.fieldBuilders) {
            writeField(field);
        }
    }

    private void writeField(final FieldBuilder field) {
        final TypeBuilder t = field.getDeclaringType();

        _dataBuffer.putShort(field.getModifiers());
        _dataBuffer.putShort(t.getFieldToken(field));

        final Object constantValue = field.getConstantValue();
        final int attributeCountPosition = beginAttributes();

        int attributeCount = 0;

        if (constantValue != null) {
            final Type constantType = TypeUtils.getUnderlyingPrimitiveOrSelf(
                Type.of(constantValue.getClass())
            );

            final int attributeLengthIndex = writeAttribute("ConstantValue");

            switch (constantType.getKind()) {
                case BOOLEAN:
                    _dataBuffer.putShort(t.getConstantToken((Boolean)constantValue ? 1 : 0));
                    break;

                case BYTE:
                case SHORT:
                case INT:
                    _dataBuffer.putShort(t.getConstantToken(((Number)constantValue).intValue()));
                    break;

                case LONG:
                    _dataBuffer.putShort(t.getConstantToken(((Number)constantValue).longValue()));
                    break;

                case CHAR:
                    _dataBuffer.putShort(t.getConstantToken((int)((Character)constantValue).charValue()));
                    break;

                case FLOAT:
                    _dataBuffer.putShort(t.getConstantToken(((Number)constantValue).floatValue()));
                    break;

                case DOUBLE:
                    _dataBuffer.putShort(t.getConstantToken(((Number)constantValue).doubleValue()));
                    break;

                default:
                    throw Error.valueMustBeConstant();
            }

            endAttribute(attributeLengthIndex);
            attributeCount++;
        }

        attributeCount += writeMemberAttributes(field);
        endAttributes(attributeCountPosition, attributeCount);
    }

    private void writeMethods() {
        for (final MethodBuilder method : _typeBuilder.methodBuilders) {
            writeMethod(method);
        }
    }

    private void writeMethod(final MethodBuilder method) {
        final TypeBuilder<?> t = _typeBuilder;

        _dataBuffer.putShort(method.getModifiers());
        _dataBuffer.putShort(t.getUtf8StringToken(method.getName()));
        _dataBuffer.putShort(t.getUtf8StringToken(method.getErasedSignature()));

        int attributeCount = 0;
        final int attributeCountIndex = beginAttributes();

        final byte[] body = method.getBody();

        if (body != null) {
            final int attributeLengthIndex = writeAttribute("Code");
            writeBody(method);
            endAttribute(attributeLengthIndex);
            attributeCount++;
        }

        final TypeList exceptions = method.getThrownTypes();

        if (!exceptions.isEmpty()) {
            final int attributeLengthIndex = writeAttribute("Exceptions");

            _dataBuffer.putShort(exceptions.size());

            for (final Type exceptionType : exceptions) {
                _dataBuffer.putShort(t.getTypeToken(exceptionType));
            }

            endAttribute(attributeLengthIndex);
            attributeCount++;
        }

        final Object defaultValue = method.getDefaultValue();

        if (defaultValue != null) {
            final int attributeLengthIndex = writeAttribute("AnnotationDefault");
            writeAttributeType(body);
            endAttribute(attributeLengthIndex);
            attributeCount++;
        }

        attributeCount += writeMemberAttributes(method);
        attributeCount += writeParameterAttributes(method);

        endAttributes(attributeCountIndex, attributeCount);
    }

    private void writeBody(final MethodBuilder method) {
        final BytecodeGenerator generator = method.getCodeGenerator();

        _dataBuffer.putShort(generator.getMaxStackSize());

        int maxLocals = generator.localCount;
        if (!method.isStatic()) {
            maxLocals += 1;
        }

        final ParameterBuilder[] parameters = method.parameterBuilders;

        maxLocals += parameters.length;

        _dataBuffer.putShort(maxLocals);
        _dataBuffer.putInt(generator.offset());

        final byte[] body = method.getBody();

        _dataBuffer.putByteArray(body, 0, body.length);

        final __ExceptionInfo[] exceptionsInfo = generator.getExceptions();

        if (exceptionsInfo != null) {
            _dataBuffer.putShort(exceptionsInfo.length);

            // TODO: Some of these addresses might be wide; put them in the constants table.

            for (final __ExceptionInfo exception : exceptionsInfo) {
                final int[] catchAddresses = exception.getCatchAddresses();
                final int[] catchEndAddresses = exception.getCatchEndAddresses();
                final Type[] catchTypes = exception.getCatchClass();
                final int[] finallyAddresses = exception.getFilterAddresses();

                final int end = catchEndAddresses.length == 0
                                ? exception.getEndAddress()
                                : catchEndAddresses[catchEndAddresses.length - 1];

                for (int i = 0, n = catchAddresses.length; i < n; i++) {
                    _dataBuffer.putShort(exception.getStartAddress());
                    _dataBuffer.putShort(exception.getEndAddress());
                    _dataBuffer.putShort(catchAddresses[i]);
                    _dataBuffer.putShort(_typeBuilder.getTypeToken(catchTypes[i]));
                }

                for (final int finallyAddress : finallyAddresses) {
                    _dataBuffer.putShort(exception.getStartAddress());
                    _dataBuffer.putShort(end);
                    _dataBuffer.putShort(finallyAddress);
                    _dataBuffer.putShort(0);
                }
            }
        }
        else {
            _dataBuffer.putShort(0);
        }

        final int attributeCountIndex = beginAttributes();

        int attributeCount = 0;
        int genericParameterCount = 0;

        final LocalInfo[] locals = getLocalInfo(method);
        final int localCount = locals.length;

        if (localCount > 0) {
            final int attributeLengthIndex = writeAttribute("LocalVariableTable");

            _dataBuffer.putShort(localCount);

            for (final LocalInfo local : locals) {
                _dataBuffer.putShort(local.start);
                _dataBuffer.putShort(local.end - local.start);

                final Type<?> localType = local.type;
                final Type<?> erasedType = localType.getErasedType();

                _dataBuffer.putShort(_typeBuilder.getUtf8StringToken(local.name));

                if (needsLocalVariableTableEntry(localType)) {
                    genericParameterCount++;
                }

                _dataBuffer.putShort(_typeBuilder.getUtf8StringToken(erasedType.getSignature()));
                _dataBuffer.putShort(local.position);
            }

            endAttribute(attributeLengthIndex);
            attributeCount++;
        }

        if (genericParameterCount > 0) {
            final int attributeLengthIndex = writeAttribute("LocalVariableTypeTable");

            _dataBuffer.putShort(genericParameterCount);

            int count = 0;

            for (final LocalInfo local : locals) {
                if (!needsLocalVariableTableEntry(local.type)) {
                    continue;
                }

                ++count;

                _dataBuffer.putShort(local.start);
                _dataBuffer.putShort(local.end - local.start);
                _dataBuffer.putShort(_typeBuilder.getUtf8StringToken(local.name));
                _dataBuffer.putShort(_typeBuilder.getUtf8StringToken(local.type.getSignature()));
                _dataBuffer.putShort(local.position);
            }

            assert count == genericParameterCount;

            endAttribute(attributeLengthIndex);
            attributeCount++;
        }

        // TODO: Stack map?

        endAttributes(attributeCountIndex, attributeCount);
    }

    private boolean needsLocalVariableTableEntry(final Type<?> localType) {
        return !localType.isEquivalentTo(localType.getErasedType()) &&
               !localType.isCompoundType();
    }

    private LocalInfo[] getLocalInfo(final MethodBuilder builder) {
        final BytecodeGenerator generator = builder.generator;
        final boolean hasThis = !builder.isStatic();
        final int localCount = generator.localCount + builder.getParameterTypes().size() + (hasThis ? 1 : 0);

        int position = 0;

        final LocalInfo[] localInfo = new LocalInfo[localCount];

        if (hasThis) {
            final LocalInfo thisInfo = new LocalInfo(
                "this",
                builder.getDeclaringType(),
                position++,
                0,
                builder.generator.offset()
            );

            localInfo[thisInfo.position] = thisInfo;
        }

        for (final ParameterBuilder p : builder.parameterBuilders) {
            final LocalInfo pInfo = new LocalInfo(
                p.getName(),
                p.getParameterType(),
                position++,
                0,
                builder.generator.offset()
            );

            localInfo[pInfo.position] = pInfo;
        }

        final LocalBuilder[] locals = builder.generator.locals;

        if (locals != null) {
            for (final LocalBuilder l : locals) {
                final LocalInfo lInfo = new LocalInfo(
                    l.getName(),
                    l.getLocalType(),
                    position++,
                    l.startOffset < 0 ? 0 : l.startOffset,
                    l.endOffset < 0 ? builder.generator.offset() : l.endOffset
                );

                localInfo[lInfo.position] = lInfo;
            }
        }

        return localInfo;
    }

    // <editor-fold defaultstate="collapsed" desc="Attributes">

    private int writeMemberAttributes(final MemberInfo member) {
        final long flags = member.getModifiers();
        final String signature;
        final TypeList thrownTypes;
        final ReadOnlyList<AnnotationBuilder> annotations;

        int attributeCount = writeFlagAttributes(member.getModifiers());

        switch (member.getMemberType()) {
            case Field:
                final FieldBuilder field = (FieldBuilder)member;
                signature = field.getFieldType().getErasedSignature();
                thrownTypes = TypeList.empty();
                annotations = field.getCustomAnnotations();
                break;

            case Method:
                final MethodBuilder method = (MethodBuilder)member;
                thrownTypes = method.getThrownTypes();
                signature = method.getErasedSignature();
                annotations = method.getCustomAnnotations();
                break;

            case Constructor:
                final ConstructorBuilder constructor = (ConstructorBuilder)member;
                signature = constructor.getErasedSignature();
                thrownTypes = constructor.getThrownTypes();
                annotations = constructor.getCustomAnnotations();
                break;

            default:
                signature = ((Type<?>)member).getErasedSignature();
                thrownTypes = TypeList.empty();
                annotations = ReadOnlyList.emptyList();
                break;
        }

        if ((flags & (SYNTHETIC | BRIDGE)) != SYNTHETIC &&
            (flags & ANONCONSTR) == 0/* &&
            (type.containsGenericParameters() || thrownTypes.containsGenericParameters())*/) {

            // A local class with captured variables will get a signature attribute.
            final int attributeIndex = writeAttribute("Signature");
            _dataBuffer.putShort(_typeBuilder.getUtf8StringToken(signature));
            endAttribute(attributeIndex);
            attributeCount++;
        }

        attributeCount += writeJavaAnnotations(annotations);

        return attributeCount;
    }

    private int writeParameterAttributes(final MethodBuilder method) {
        boolean hasVisible = false;
        boolean hasInvisible = false;
        final ParameterBuilder[] parameters = method.parameterBuilders;

        if (parameters != null) {
            for (final ParameterBuilder parameter : parameters) {
                for (final AnnotationBuilder a : parameter.getCustomAnnotations()) {
                    switch (getAnnotationRetention(a)) {
                        case SOURCE:
                            break;
                        case CLASS:
                            hasInvisible = true;
                            break;
                        case RUNTIME:
                            hasVisible = true;
                            break;
                    }
                }
            }
        }

        int attributeCount = 0;

        if (hasVisible) {
            final int attributeIndex = writeAttribute("RuntimeVisibleParameterAnnotations");

            _dataBuffer.putByte(parameters.length);

            for (final ParameterBuilder p : parameters) {
                final ArrayList<AnnotationBuilder> annotations = new ArrayList<>(p.getCustomAnnotations().size());

                for (final AnnotationBuilder a : p.getCustomAnnotations()) {
                    if (getAnnotationRetention(a) == RetentionPolicy.RUNTIME) {
                        annotations.add(a);
                    }
                }

                _dataBuffer.putShort(annotations.size());

                for (final AnnotationBuilder a : annotations) {
                    writeAnnotation(a);
                }
            }

            endAttribute(attributeIndex);
            attributeCount++;
        }

        if (hasInvisible) {
            final int attributeIndex = writeAttribute("RuntimeInvisibleParameterAnnotations");

            _dataBuffer.putByte(parameters.length);

            for (final ParameterBuilder p : parameters) {
                final ArrayList<AnnotationBuilder> annotations = new ArrayList<>(p.getCustomAnnotations().size());

                for (final AnnotationBuilder a : p.getCustomAnnotations()) {
                    if (getAnnotationRetention(a) == RetentionPolicy.CLASS) {
                        annotations.add(a);
                    }
                }

                _dataBuffer.putShort(annotations.size());

                for (final AnnotationBuilder a : annotations) {
                    writeAnnotation(a);
                }
            }

            endAttribute(attributeIndex);
            attributeCount++;
        }

        return attributeCount;
    }

    private RetentionPolicy getAnnotationRetention(final AnnotationBuilder a) {
        if (a.getAnnotationType().isAnnotationPresent(Retention.class)) {
            return a.getAnnotationType().getAnnotation(Retention.class).value();
        }
        return RetentionPolicy.CLASS;
    }

    private int writeJavaAnnotations(final ReadOnlyList<AnnotationBuilder> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            return 0;
        }

        final ArrayList<AnnotationBuilder> visible = new ArrayList<>();
        final ArrayList<AnnotationBuilder> invisible = new ArrayList<>();

        for (final AnnotationBuilder a : annotations) {
            switch (getAnnotationRetention(a)) {
                case SOURCE:
                    break;
                case CLASS:
                    invisible.add(a);
                    break;
                case RUNTIME:
                    visible.add(a);
                    break;
            }
        }

        int attributeCount = 0;

        if (!visible.isEmpty()) {
            final int attributeIndex = writeAttribute("RuntimeVisibleAnnotations");

            _dataBuffer.putShort(visible.size());

            for (final AnnotationBuilder a : visible) {
                writeAnnotation(a);
            }

            endAttribute(attributeIndex);
            attributeCount++;
        }

        if (!invisible.isEmpty()) {
            final int attributeIndex = writeAttribute("RuntimeInvisibleAnnotations");

            _dataBuffer.putShort(invisible.size());

            for (final AnnotationBuilder a : invisible) {
                writeAnnotation(a);
            }

            endAttribute(attributeIndex);
            attributeCount++;
        }

        return attributeCount;
    }

    private void writeAnnotation(final AnnotationBuilder a) {
        _dataBuffer.putShort(_typeBuilder.getUtf8StringToken(a.getAnnotationType().getSignature()));
        _dataBuffer.putShort(a.getValues().size());

        final MethodList attributes = a.getAttributes();
        final ReadOnlyList<Object> values = a.getValues();

        for (int i = 0, n = attributes.size(); i < n; i++) {
            final MethodInfo attribute = attributes.get(i);
            _dataBuffer.putShort(_typeBuilder.getMethodToken(attribute));
            writeAttributeType(values.get(i));
        }
    }

    private void writeAttributeType(final Object value) {
        final Type<?> valueType = Type.of(value.getClass());

        switch (valueType.getKind()) {
            case BOOLEAN:
                _dataBuffer.putByte(valueType.getErasedSignature().charAt(0));
                _dataBuffer.putShort(_typeBuilder.getConstantToken((Boolean)value ? 1 : 0));
                break;

            case BYTE:
            case SHORT:
            case INT:
                _dataBuffer.putByte(valueType.getErasedSignature().charAt(0));
                _dataBuffer.putShort(_typeBuilder.getConstantToken(((Number)value).intValue()));
                break;

            case LONG:
                _dataBuffer.putByte(valueType.getErasedSignature().charAt(0));
                _dataBuffer.putShort(_typeBuilder.getConstantToken(((Number)value).longValue()));
                break;

            case CHAR:
                _dataBuffer.putByte(valueType.getErasedSignature().charAt(0));
                _dataBuffer.putShort(_typeBuilder.getConstantToken(((Character)value).charValue()));
                break;

            case FLOAT:
                _dataBuffer.putByte(valueType.getErasedSignature().charAt(0));
                _dataBuffer.putShort(_typeBuilder.getConstantToken(((Number)value).floatValue()));
                break;

            case DOUBLE:
                _dataBuffer.putByte(valueType.getErasedSignature().charAt(0));
                _dataBuffer.putShort(_typeBuilder.getConstantToken(((Number)value).doubleValue()));
                break;

            case ARRAY:
                final int arrayLength = Array.getLength(value);
                _dataBuffer.putByte('[');
                _dataBuffer.putShort(arrayLength);
                for (int i = 0; i < arrayLength; i++) {
                    writeAttributeType(Array.get(value, i));
                }
                break;

            case DECLARED:
                if (valueType.isEnum()) {
                    _dataBuffer.putByte('e');
                    _dataBuffer.putShort(_typeBuilder.getUtf8StringToken(valueType.getSignature()));
                    _dataBuffer.putShort(_typeBuilder.getUtf8StringToken(value.toString()));
                }
                else if (valueType == Types.String) {
                    _dataBuffer.putByte('s');
                    _dataBuffer.putShort(_typeBuilder.getUtf8StringToken(value.toString()));
                }
                else if (valueType == Types.Class) {
                    final Type<?> type = Type.of((Class<?>)value);
                    _dataBuffer.putByte('c');
                    _dataBuffer.putShort(_typeBuilder.getUtf8StringToken(type.getSignature()));
                }
                else if (Type.of(Type.class).isAssignableFrom(valueType)) {
                    _dataBuffer.putByte('c');
                    _dataBuffer.putShort(_typeBuilder.getUtf8StringToken(((Type<?>)value).getSignature()));
                }
                else {
                    _dataBuffer.putByte('@');
                    writeAnnotation((AnnotationBuilder)value);
                }
                break;
        }
    }

    private int writeAttribute(final String attributeName) {
        _dataBuffer.putShort(_typeBuilder.getUtf8StringToken(attributeName));
        _dataBuffer.putInt(0);
        return _dataBuffer.getLength();
    }

    private int beginAttributes() {
        _dataBuffer.putShort(0);
        return _dataBuffer.getLength();
    }

    private void endAttributes(final int index, final int count) {
        putChar(_dataBuffer, index - 2, count);
    }

    private void endAttribute(final int index) {
        putInt(_dataBuffer, index - 4, _dataBuffer.getLength() - index);
    }

    private int writeFlagAttributes(final long flags) {
        int count = 0;

        if ((flags & DEPRECATED) != 0) {
            endAttribute(writeAttribute("Deprecated"));
            count++;
        }

        if ((flags & ENUM) != 0) {
            endAttribute(writeAttribute("Enum"));
            count++;
        }

        if ((flags & SYNTHETIC) != 0) {
            endAttribute(writeAttribute("Synthetic"));
            count++;
        }

        if ((flags & BRIDGE) != 0) {
            endAttribute(writeAttribute("Bridge"));
            count++;
        }

        if ((flags & VARARGS) != 0) {
            endAttribute(writeAttribute("Varargs"));
            count++;
        }

        if ((flags & ANNOTATION) != 0) {
            endAttribute(writeAttribute("Annotation"));
            count++;
        }

        return count;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Direct Output">

    void putChar(final BytecodeStream buf, final int op, final int x) {
        buf.ensureCapacity(op + 2);
        final byte[] data = buf.getData();
        data[op] = (byte)((x >> 8) & 0xFF);
        data[op + 1] = (byte)((x) & 0xFF);
    }

    void putInt(final BytecodeStream buf, final int adr, final int x) {
        buf.ensureCapacity(adr + 4);
        final byte[] data = buf.getData();
        data[adr] = (byte)((x >> 24) & 0xFF);
        data[adr + 1] = (byte)((x >> 16) & 0xFF);
        data[adr + 2] = (byte)((x >> 8) & 0xFF);
        data[adr + 3] = (byte)((x) & 0xFF);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="LocalInfo Class">

    private final static class LocalInfo {
        final String name;
        final Type<?> type;
        final int position;
        final int start;
        final int end;

        LocalInfo(final String name, final Type<?> type, final int position, final int start, final int end) {
            this.name = name;
            this.type = type;
            this.position = position;
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return "LocalInfo{" +
                   "name='" + name + '\'' +
                   ", type=" + type +
                   ", position=" + position +
                   ", start=" + start +
                   ", end=" + end +
                   '}';
        }
    }

    // </editor-fold>
}
