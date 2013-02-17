package com.strobel.assembler.ir;

import com.strobel.assembler.metadata.*;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;

import java.lang.reflect.Array;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 1:31 AM
 */
public final class Instruction {
    private int _offset = -1;
    private OpCode _opCode;
    private Object _operand;
    private Label _label;

    private Instruction _previous;
    private Instruction _next;

    public Instruction(final int offset, final OpCode opCode) {
        _offset = offset;
        _opCode = opCode;
    }

    public Instruction(final OpCode opCode) {
        _opCode = opCode;
        _operand = null;
    }

    public Instruction(final OpCode opCode, final Object operand) {
        _opCode = opCode;
        _operand = operand;
    }

    public Instruction(final OpCode opCode, final Object... operands) {
        _opCode = opCode;
        _operand = VerifyArgument.notNull(operands, "operands");
    }

    public boolean hasOffset() {
        return _offset >= 0;
    }

    public int getOffset() {
        return _offset;
    }

    public void setOffset(final int offset) {
        _offset = offset;
    }

    public OpCode getOpCode() {
        return _opCode;
    }

    public void setOpCode(final OpCode opCode) {
        _opCode = opCode;
    }

    public int getOperandCount() {
        final Object operand = _operand;

        if (operand == null) {
            return 0;
        }

        if (ArrayUtilities.isArray(operand)) {
            return Array.getLength(operand);
        }

        return 1;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOperand(final int index) {
        final Object operand = _operand;

        if (ArrayUtilities.isArray(operand)) {
            VerifyArgument.inRange(0, Array.getLength(operand) - 1, index, "index");
            return (T) Array.get(operand, index);
        }
        else {
            VerifyArgument.inRange(0, 0, index, "index");
            return (T) operand;
        }
    }

    public void setOperand(final Object operand) {
        _operand = operand;
    }

    public boolean hasLabel() {
        return _label != null;
    }

    public Label getLabel() {
        return _label;
    }

    public void setLabel(final Label label) {
        _label = label;
    }

    public Instruction getPrevious() {
        return _previous;
    }

    public void setPrevious(final Instruction previous) {
        _previous = previous;
    }

    public Instruction getNext() {
        return _next;
    }

    public void setNext(final Instruction next) {
        _next = next;
    }

    @Override
    protected Instruction clone() {
        final Instruction copy = new Instruction(_opCode, _operand);

        copy._offset = _offset;
        copy._label = _label;

        return copy;
    }

    // <editor-fold defaultstate="collapsed" desc="Size Calculation">

    public int getSize() {
        final int opCodeSize = _opCode.getSize();
        final OperandType operandType = _opCode.getOperandType();

        switch (operandType) {
            case None:
                return opCodeSize;

            case PrimitiveTypeCode:
            case TypeReference:
            case TypeReferenceU1:
                return opCodeSize + operandType.getBaseSize();

            case MethodReference:
                switch (_opCode) {
                    case INVOKEVIRTUAL:
                    case INVOKESPECIAL:
                    case INVOKESTATIC:
                        return opCodeSize + operandType.getBaseSize();
                    case INVOKEINTERFACE:
                    case INVOKEDYNAMIC:
                        return opCodeSize + operandType.getBaseSize() + 2;
                }
                break;

            case FieldReference:
                return opCodeSize + operandType.getBaseSize();

            case BranchTarget:
                return opCodeSize + (_opCode.isWide() ? 4 : 2);

            case I1:
            case I2:
            case I8:
            case Constant:
            case WideConstant:
                return opCodeSize + operandType.getBaseSize();

            case Switch:
                final Instruction[] targets = ((SwitchInfo) _operand).getTargets();
                final int padding = _offset >= 0 ? ((4 - (_offset % 4)) % 4) : 0;
                switch (_opCode) {
                    case TABLESWITCH:
                        // op + padding + default + low + high + targets
                        return opCodeSize + padding + ((3 + targets.length) * 4);
                    case LOOKUPSWITCH:
                        // op + padding + default + pairs
                        return opCodeSize + padding + 4 + (targets.length * 8);
                }
                break;

            case Local:
                return opCodeSize + (_opCode.isWide() ? 4 : 2);

            case LocalI1:
            case LocalI2:
                return opCodeSize + operandType.getBaseSize();
        }

        throw ContractUtils.unreachable();
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Factory Methods">

    public static Instruction create(final OpCode opCode) {
        VerifyArgument.notNull(opCode, "opCode");

        if (opCode.getOperandType() != OperandType.None) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode);
    }

    public static Instruction create(final OpCode opCode, final Instruction target) {
        VerifyArgument.notNull(opCode, "opCode");
        VerifyArgument.notNull(target, "target");

        if (opCode.getOperandType() != OperandType.BranchTarget) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, target);
    }

    public static Instruction create(final OpCode opCode, final SwitchInfo switchInfo) {
        VerifyArgument.notNull(opCode, "opCode");
        VerifyArgument.notNull(switchInfo, "switchInfo");

        if (opCode.getOperandType() != OperandType.Switch) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, switchInfo);
    }

    public static Instruction create(final OpCode opCode, final int value) {
        VerifyArgument.notNull(opCode, "opCode");

        if (!checkOperand(opCode.getOperandType(), value)) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, value);
    }

    public static Instruction create(final OpCode opCode, final short value) {
        VerifyArgument.notNull(opCode, "opCode");

        if (!checkOperand(opCode.getOperandType(), value)) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, value);
    }

    public static Instruction create(final OpCode opCode, final float value) {
        VerifyArgument.notNull(opCode, "opCode");

        if (opCode.getOperandType() != OperandType.Constant && opCode.getOperandType() != OperandType.WideConstant) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, value);
    }

    public static Instruction create(final OpCode opCode, final double value) {
        VerifyArgument.notNull(opCode, "opCode");

        if (opCode.getOperandType() != OperandType.WideConstant) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, value);
    }

    public static Instruction create(final OpCode opCode, final long value) {
        VerifyArgument.notNull(opCode, "opCode");

        if (opCode.getOperandType() != OperandType.I8 && opCode.getOperandType() != OperandType.WideConstant) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, value);
    }

    public static Instruction create(final OpCode opCode, final VariableReference variable) {
        VerifyArgument.notNull(opCode, "opCode");

        if (opCode.getOperandType() != OperandType.Local) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, variable);
    }

    public static Instruction create(final OpCode opCode, final VariableReference variable, final int operand) {
        VerifyArgument.notNull(opCode, "opCode");
        VerifyArgument.notNull(variable, "variable");

        if (!checkOperand(opCode.getOperandType(), operand)) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, new Object[]{variable, operand});
    }

    public static Instruction create(final OpCode opCode, final TypeReference type) {
        VerifyArgument.notNull(opCode, "opCode");
        VerifyArgument.notNull(type, "type");

        if (!checkOperand(opCode.getOperandType(), type)) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, type);
    }

    public static Instruction create(final OpCode opCode, final TypeReference type, final int operand) {
        VerifyArgument.notNull(opCode, "opCode");

        if (!checkOperand(opCode.getOperandType(), type) || !checkOperand(opCode.getOperandType(), operand)) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, new Object[] { type, operand });
    }

    public static Instruction create(final OpCode opCode, final MethodReference method) {
        VerifyArgument.notNull(opCode, "opCode");

        if (!checkOperand(opCode.getOperandType(), method)) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, method);
    }

    public static Instruction create(final OpCode opCode, final FieldReference field) {
        VerifyArgument.notNull(opCode, "opCode");

        if (!checkOperand(opCode.getOperandType(), field)) {
            throw new IllegalArgumentException(String.format("Invalid operand for OpCode %s.", opCode));
        }

        return new Instruction(opCode, field);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Operand Checks">

    private static final int U1_MIN_VALUE = 0x00;
    private static final int U1_MAX_VALUE = 0xFF;
    private static final int U2_MIN_VALUE = 0x0000;
    private static final int U2_MAX_VALUE = 0xFFFF;

    private static boolean checkOperand(final OperandType operandType, final int value) {
        switch (operandType) {
            case I1:
            case LocalI1:
                return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE;
            case I2:
            case LocalI2:
                return value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
            case TypeReferenceU1:
                return value >= U1_MIN_VALUE && value <= U1_MAX_VALUE;
            default:
                return false;
        }
    }

    private static boolean checkOperand(final OperandType operandType, final TypeReference type) {
        VerifyArgument.notNull(type, "type");

        switch (operandType) {
            case PrimitiveTypeCode:
                return type.getSimpleType().isPrimitive();
            case TypeReference:
            case TypeReferenceU1:
                return true;
            default:
                return false;
        }
    }

    private static boolean checkOperand(final OperandType operandType, final MethodReference method) {
        VerifyArgument.notNull(method, "method");

        switch (operandType) {
            case MethodReference:
                return true;
            default:
                return false;
        }
    }

    private static boolean checkOperand(final OperandType operandType, final FieldReference field) {
        VerifyArgument.notNull(field, "field");

        switch (operandType) {
            case FieldReference:
                return true;
            default:
                return false;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Visitor Acceptor">

    public <P> void accept(final InstructionVisitor<P> visitor, final P parameter) {
        if (hasLabel()) {
            visitor.visitLabel(parameter, _label);
        }

        switch (_opCode.getOperandType()) {
            case None:
                visitor.visit(parameter, _opCode);
                break;

            case PrimitiveTypeCode:
            case TypeReference:
            case TypeReferenceU1:
                visitor.visitType(parameter, _opCode, (TypeReference) _operand);
                break;

            case MethodReference:
                visitor.visitMethod(parameter, _opCode, (MethodReference) _operand);
                break;

            case FieldReference:
                visitor.visitField(parameter, _opCode, (FieldReference) _operand);
                break;

            case BranchTarget:
                visitor.visitBranch(parameter, _opCode, (Instruction) _operand);
                break;

            case I1:
            case I2:
                visitor.visit(parameter, _opCode, ((Number) _operand).intValue());
                break;

            case I8:
                visitor.visit(parameter, _opCode, ((Number) _operand).longValue());
                break;

            case Constant:
            case WideConstant:
                if (_operand instanceof String) {
                    visitor.visit(parameter, _opCode, (String) _operand);
                }
                else if (_operand instanceof TypeReference) {
                    visitor.visit(parameter, _opCode, (TypeReference) _operand);
                }
                else {
                    final Number number = (Number) _operand;

                    if (_operand instanceof Long) {
                        visitor.visit(parameter, _opCode, number.longValue());
                    }
                    else if (_operand instanceof Float) {
                        visitor.visit(parameter, _opCode, number.floatValue());
                    }
                    else if (_operand instanceof Double) {
                        visitor.visit(parameter, _opCode, number.doubleValue());
                    }
                    else {
                        visitor.visit(parameter, _opCode, number.intValue());
                    }
                }
                break;

            case Switch:
                visitor.visitSwitch(parameter, _opCode, (SwitchInfo) _operand);
                break;

            case Local:
                visitor.visitVariable(parameter, _opCode, (VariableReference) _operand);
                break;

            case LocalI1:
            case LocalI2:
                visitor.visitVariable(
                    parameter,
                    _opCode,
                    this.<VariableReference>getOperand(0),
                    this.<Number>getOperand(1).intValue()
                );
                break;
        }
    }

    // </editor-fold>
}
