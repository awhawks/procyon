package com.strobel.expressions;

/**
 * Describes the node types for the nodes of an expression tree.
 * @author Mike Strobel
 */
public enum ExpressionType {
    Add,
    And,
    AndAlso,
    ArrayLength,
    ArrayIndex,
    Call,
    Coalesce,
    Conditional,
    Constant,
    Convert,
    ConvertChecked,
    Divide,
    Equal,
    ExclusiveOr,
    GreaterThan,
    GreaterThanOrEqual,
    Invoke,
    Lambda,
    LeftShift,
    LessThan,
    LessThanOrEqual,
    MemberAccess,
    Modulo,
    Multiply,
    Negate,
    UnaryPlus,
    New,
    NewArrayInit,
    NewArrayBounds,
    Not,
    NotEqual,
    Or,
    OrElse,
    Parameter,
    Quote,
    RightShift,
    UnsignedRightShift,
    Subtract,
    InstanceOf,
    Assign,
    Block,
    LineInfo,
    Decrement,
    DefaultValue,
    Extension,
    Goto,
    Increment,
    Label,
    RuntimeVariables,
    Loop,
    Switch,
    Throw,
    Try,
    Unbox,
    AddAssign,
    AndAssign,
    DivideAssign,
    ExclusiveOrAssign,
    LeftShiftAssign,
    ModuloAssign,
    MultiplyAssign,
    OrAssign,
    RightShiftAssign,
    UnsignedRightShiftAssign,
    SubtractAssign,
    PreIncrementAssign,
    PreDecrementAssign,
    PostIncrementAssign,
    PostDecrementAssign,
    TypeEqual,
    OnesComplement,
    IsTrue,
    IsFalse,
}
