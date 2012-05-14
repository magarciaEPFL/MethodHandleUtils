package bt4;

import javax.management.RuntimeErrorException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

public class RuntimeMP {

    /* ---------------- initialization before anything else ---------------- */

    public static MethodHandle mh_helperWhileLoop;
    public static MethodHandle mh_unit;
    public static MethodHandle mh_arglessInvoker;

    public static MethodType mt_arglessVoid;

    public static MethodHandle mhLookup(String name, Class retClazz, Class... argsClazzes) {
        try {
            return lookup().findStatic(RuntimeMP.class, name, methodType(retClazz, argsClazzes));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static void initMHs() {
        try {

            mh_helperWhileLoop = mhLookup("helperWhileLoop", void.class, MethodHandle.class, MethodHandle.class);
            mh_unit            = mhLookup("unit",            void.class);
            mh_arglessInvoker  = mhLookup("arglessInvoker",  void.class, MethodHandle.class);

            mt_arglessVoid     = methodType(void.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void unit() { }

    public static void arglessInvoker(MethodHandle mh) {
        try {
            mh.invoke();
        } catch (Throwable t) {
            throw new Error(t);
        }
    }

    private static void helperWhileLoop(MethodHandle cond, MethodHandle body) throws Throwable {
        while ((boolean)cond.invokeExact()) {
            body.invoke();
        }
    }

    /* ---------------- assertions about reified oeprations ---------------- */

    public static Class evalType(MethodHandle mh) {
        return mh.type().returnType();
    }

    public static boolean isArgless(MethodHandle mh) { return mh.type().parameterCount() == 0; }
    public static boolean isArglessVoid(MethodHandle mh) { return isArgless(mh) && isVoidValued(mh); }

    public static boolean isBooleanValued(MethodHandle mh) { return evalType(mh) == boolean.class; }
    public static boolean isIntValued(    MethodHandle mh) { return evalType(mh) == int.class;     }
    public static boolean isFloatValued(  MethodHandle mh) { return evalType(mh) == float.class;   }
    public static boolean isVoidValued(   MethodHandle mh) { return evalType(mh) == void.class;    }

    public static boolean isSetter(MethodHandle mh) {
        boolean  result = mh.type().parameterCount() == 1;
        result &= isVoidValued(mh);
        return result;
    }

    public static boolean isGetter(MethodHandle mh) { return isArgless(mh) && !isVoidValued(mh); }


    /* ---------------- constants ---------------- */
    
    public static MethodHandle booleanConstant(boolean b) { return constant(boolean.class, b); }
    public static MethodHandle floatConstant(float f)     { return constant(float.class,   f); }
    public static MethodHandle intConstant(int i)         { return constant(int.class,     i); }

    public static boolean toBoolean(MethodHandle mh) {
        assert(isBooleanValued(mh));
        try { return (boolean)mh.invokeExact(); } catch (Throwable t) { throw new Error(t); }
    }

    public static float toFloat(MethodHandle mh) {
        assert(isFloatValued(mh));
        try { return (float)mh.invokeExact(); } catch (Throwable t) { throw new Error(t); }
    }

    public static int toInt(MethodHandle mh) {
        assert(isIntValued(mh));
        try { return (int)mh.invokeExact(); } catch (Throwable t) { throw new Error(t); }
    }

    /* ---------------- building blocks ---------------- */

    public static MethodHandle application(MethodHandle target, MethodHandle arg) {
      return  application(target, new MethodHandle[] { arg } );
    }

    public static MethodHandle application(MethodHandle target, MethodHandle arg0, MethodHandle arg1) {
        return  application(target, new MethodHandle[] { arg0, arg1 } );
    }

    public static MethodHandle application(MethodHandle target, MethodHandle[] args) {
        MethodHandle[] invokers = new MethodHandle[args.length];
        for(int idx = 0; idx < args.length; idx++) {
            invokers[idx] = exactInvoker(args[idx].type());
        }
        MethodHandle applied = filterArguments(target, 0, invokers);
        for(int idx = 0; idx < args.length; idx++) {
            applied = applied.bindTo(args[idx]);
        }
        return applied;
    }

    public static MethodHandle assignment(MethodHandle lhs, MethodHandle rhs) {
        assert isSetter(lhs);
        assert isGetter(rhs);
        return application(lhs, new MethodHandle[] { rhs });
    }

    public static MethodHandle blockExpr(MethodHandle[] statements, MethodHandle expr) {
        for(int idx = 0; idx < statements.length; idx++) {
            assert isArglessVoid(statements[idx]);
        }
        assert(isArgless(expr));

        MethodHandle result = null;
        switch(statements.length) {
            case 0 : result = expr; break;
            case 1 : result = foldArguments(expr, statements[0]); break;
            default: result = foldArguments(expr, blockStmt(statements)); break;
        }
        assert isArgless(result);
        assert evalType(result) == evalType(expr);
        return result;
    }

    public static MethodHandle blockStmt(MethodHandle[] statements) {
        for(int idx = 0; idx < statements.length; idx++) {
            assert isArglessVoid(statements[idx]);
        }
        MethodHandle result = mh_unit;
        for(int idx = 0; idx < statements.length; idx++) {
            result = foldArguments(statements[idx], result.asType(methodType(void.class)));
        }
        assert isArglessVoid(result);
        return result;
    }

    public static MethodHandle eval(MethodHandle target) {
        assert target.type().parameterCount() == 0;
        return exactInvoker(target.type());
    }

    public static MethodHandle ifExpr(MethodHandle cond, MethodHandle thenPart, MethodHandle elsePart) {
        assert isArgless(cond) && isBooleanValued(cond);
        assert isArgless(thenPart);
        assert isArgless(elsePart);
        assert isVoidValued(thenPart) == isVoidValued(elsePart); // TODO test whether both branches have common lub.
        return guardWithTest(cond, thenPart, elsePart);
    }

    public static MethodHandle statement(MethodHandle argless) {
        assert isArgless(argless);
        return argless.asType(mt_arglessVoid);
    }

    public static MethodHandle whileLoop(MethodHandle cond, MethodHandle body) {
      assert isArgless(cond) && isBooleanValued(cond);
      assert isArgless(body) && isVoidValued(cond);

      MethodHandle result = mh_helperWhileLoop.bindTo(cond).bindTo(body);
      assert(isArglessVoid(result));
      return result;
    }

    // rather than componentClazz, the JVM-sort (an int) would allow for faster switching.
    public static MethodHandle newArrayRef(Class componentClazz, int length) {
      Object refedArray = java.lang.reflect.Array.newInstance(componentClazz, length);
      return constant(refedArray.getClass(), refedArray);
    }

    /* ---------------- accessors ---------------- */

    public static MethodHandle arraySetterUnbound(Class componentClazz) {
        Class arrayClazz = java.lang.reflect.Array.newInstance(componentClazz, 0).getClass();
        return arrayElementSetter(arrayClazz);
    }

    public static MethodHandle arrayGetterUnbound(Class componentClazz) {
        Class arrayClazz = java.lang.reflect.Array.newInstance(componentClazz, 0).getClass();
        return arrayElementGetter(arrayClazz);
    }

    public static MethodHandle arrayElemSetter(MethodHandle arrRef) {
        Class componentClazz = evalType(arrRef).getComponentType();
        MethodHandle rcvless = arraySetterUnbound(componentClazz);
        MethodHandle result = application(rcvless, new MethodHandle[] { arrRef });
        assert result.type().parameterCount() == 2;
        assert result.type().parameterType(0) == int.class;
        assert result.type().parameterType(1) == evalType(arrRef);
        return result;
    }

    public static MethodHandle arrayElemGetter(MethodHandle arrRef) {
        Class componentClazz = evalType(arrRef).getComponentType();
        MethodHandle rcvless = arrayGetterUnbound(componentClazz);
        MethodHandle result = application(rcvless, new MethodHandle[] { arrRef });
        assert result.type().parameterCount() == 1;
        assert result.type().parameterType(0) == int.class;
        assert evalType(result) == evalType(arrRef);
        return result;
    }

    public static abstract class LocalVar {
        MethodHandle getter = null;
        MethodHandle setter = null;
    }

    public static class FloatLocalVar extends LocalVar {
        float v = 0;

        public FloatLocalVar() {
            try {
                getter = lookup().findGetter(FloatLocalVar.class, "v", float.class).bindTo(this);
                setter = lookup().findSetter(FloatLocalVar.class, "v", float.class).bindTo(this);
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    public static class IntLocalVar extends LocalVar {
        int v = 0;

        public IntLocalVar() {
            try {
                getter = lookup().findGetter(IntLocalVar.class, "v", int.class).bindTo(this);
                setter = lookup().findSetter(IntLocalVar.class, "v", int.class).bindTo(this);
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

}

