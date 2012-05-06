package com.strobel.reflection;

/**
 * @author strobelm
 */
public final class PrimitiveTypes {
    private PrimitiveTypes() {}

    public final static Type Void = new PrimitiveType(java.lang.Void.TYPE, 'V', "void");
    public final static Type Boolean = new PrimitiveType(java.lang.Void.TYPE, 'Z', "boolean");
    public final static Type Byte = new PrimitiveType(java.lang.Void.TYPE, 'B', "byte");
    public final static Type Short = new PrimitiveType(java.lang.Void.TYPE, 'S', "short");
    public final static Type Character = new PrimitiveType(java.lang.Void.TYPE, 'C', "char");
    public final static Type Integer = new PrimitiveType(java.lang.Void.TYPE, 'I', "int");
    public final static Type Long = new PrimitiveType(java.lang.Void.TYPE, 'J', "long");
    public final static Type Float = new PrimitiveType(java.lang.Void.TYPE, 'F', "float");
    public final static Type Double = new PrimitiveType(java.lang.Void.TYPE, 'D', "double");

    static {
        Type.CACHE.add(PrimitiveTypes.Void);
        Type.CACHE.add(PrimitiveTypes.Boolean);
        Type.CACHE.add(PrimitiveTypes.Byte);
        Type.CACHE.add(PrimitiveTypes.Short);
        Type.CACHE.add(PrimitiveTypes.Character);
        Type.CACHE.add(PrimitiveTypes.Integer);
        Type.CACHE.add(PrimitiveTypes.Long);
        Type.CACHE.add(PrimitiveTypes.Float);
        Type.CACHE.add(PrimitiveTypes.Double);
        Type.CACHE.add(Type.Object);

    }
}
