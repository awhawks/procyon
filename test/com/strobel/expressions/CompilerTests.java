package com.strobel.expressions;

import com.strobel.reflection.PrimitiveTypes;
import com.strobel.reflection.Type;
import com.strobel.reflection.TypeList;
import com.strobel.reflection.Types;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.strobel.expressions.Expression.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;

/**
 * @author Mike Strobel
 */
public class CompilerTests {
    interface IListRetriever<T> {
        List<T> getList();
    }

    interface INeedsBridgeMethod<T extends Comparable<String>> {
        T invoke(final T t);
    }

    @Test
    public void testGenericMethodCall()
        throws Exception {

        final LambdaExpression listRetriever = lambda(
            Type.of(IListRetriever.class).makeGenericType(Types.String),
            call(
                Type.of(Collections.class),
                "emptyList",
                Type.list(Types.String)
            )
        );

        final Delegate delegate = listRetriever.compileDelegate();
        final Object result = delegate.invokeDynamic();

        assertEquals(Collections.emptyList(), result);
    }
    
    @Test
    public void testBridgeMethodGeneration()
        throws Exception {

        final ParameterExpression arg = parameter(Types.String);
        
        final LambdaExpression listRetriever = lambda(
            Type.of(INeedsBridgeMethod.class).makeGenericType(Types.String),
            arg,
            arg
        );

        final String input = "zomg";
        final Delegate delegate = listRetriever.compileDelegate();
        final Object result = delegate.invokeDynamic(input);

        assertSame(input, result);
    }

    @Test
    public void testSimpleLoop() throws Exception {

        final ParameterExpression lcv = variable(PrimitiveTypes.Integer, "i");

        final LabelTarget breakLabel = label();
        final LabelTarget continueLabel = label();
        final MemberExpression out = field(null, Type.of(System.class).getField("out"));

        final LambdaExpression<Runnable> runnable = lambda(
            Type.of(Runnable.class),
            block(
                new ParameterExpressionList(lcv),
                assign(lcv, constant(0)),
                call(out, "println", constant("Starting the loop...")),
                loop(
                    block(
                        PrimitiveTypes.Void,
                        ifThen(
                            greaterThanOrEqual(lcv, constant(5)),
                            makeBreak(breakLabel)),
                        call(
                            out,
                            "printf",
                            constant("Loop iteration #%d\n"),
                            newArrayInit(
                                Types.Object,
                                convert(lcv, Types.Object)
                            )),
                        preIncrementAssign(lcv)),
                    breakLabel,
                    continueLabel),
                call(out, "println", constant("Finished the loop!"))
            )
        );

        System.out.println();
        System.out.println(runnable);

        final Runnable delegate = runnable.compile();

        System.out.println();

        delegate.run();
    }

    @Test
    public void simpleLambdaTest() throws Exception {
        final ParameterExpression number = parameter(PrimitiveTypes.Integer, "number");

        final LambdaExpression<ITest> lambda = lambda(
            Type.of(ITest.class),
            call(
                condition(
                    equal(
                        number,
                        call(
                            Types.Integer,
                            "parseInt",
                            TypeList.empty(),
                            constant("0")
                        )
                    ),
                    constant("zero"),
                    condition(
                        lessThan(number, constant(0)),
                        constant("negative"),
                        constant("positive")
                    )
                ),
                "toUpperCase",
                TypeList.empty(),
                constant(Locale.getDefault())
            ),
            number
        );

        System.out.println();
        System.out.println(lambda);

        final ITest delegate = lambda.compile();

        assertEquals("NEGATIVE", delegate.testNumber(-15));
        assertEquals("ZERO", delegate.testNumber(0));
        assertEquals("POSITIVE", delegate.testNumber(99));

        System.out.println();

        System.out.println(delegate.testNumber(-15));
        System.out.println(delegate.testNumber(0));
        System.out.println(delegate.testNumber(99));
    }

    @Test
    public void returnLabelTest() throws Exception {
        final ParameterExpression number = parameter(PrimitiveTypes.Integer, "number");
        final LabelTarget returnLabel = label(Types.String);

        final LambdaExpression<ITest> lambda = lambda(
            Type.of(ITest.class),
            block(
                ifThenElse(
                    equal(number, constant(0)),
                    makeReturn(returnLabel, constant("zero")),
                    condition(
                        lessThan(number, constant(0)),
                        makeReturn(returnLabel, constant("negative")),
                        makeReturn(returnLabel, constant("positive"))
                    )
                ),
                label(returnLabel)
            ),
            number
        );

        System.out.println();

        System.out.println(lambda);

        final ITest delegate = lambda.compile();

        System.out.println();

        assertEquals("negative", delegate.testNumber(-15));
        assertEquals("zero", delegate.testNumber(0));
        assertEquals("positive", delegate.testNumber(99));

        System.out.println(delegate.testNumber(-15));
        System.out.println(delegate.testNumber(0));
        System.out.println(delegate.testNumber(99));
    }
}
