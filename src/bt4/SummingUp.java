package bt4;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.lookup;

public class SummingUp extends RuntimeMP {

    public static MethodHandle mhLookup(String name, Class retClazz, Class... argsClazzes) {
        try {
            return lookup().findStatic(SummingUp.class, name, methodType(retClazz, argsClazzes));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static MethodHandle jlrArrayLookup(String name, Class retClazz, Class... argsClazzes) {
        try {
            return lookup().findStatic(java.lang.reflect.Array.class, name, methodType(retClazz, argsClazzes));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static MethodHandle mh_intLessThan = mhLookup("intLessThan", boolean.class, int.class, int.class);
    public static boolean intLessThan(int a, int b) { return (a < b); }

    public static MethodHandle mh_addII = mhLookup("addII", int.class, int.class, int.class);
    public static int addII(int a, int b) { return (a + b); }

    public static MethodHandle mh_array_length = jlrArrayLookup("getLength", int.class, java.lang.Object.class);


    public static MethodHandle summationMaker(MethodHandle aref) {

        MethodHandle arr_g = arrayElemGetter(aref);
        MethodHandle arr_s = arrayElemSetter(aref);

        IntLocalVar idx = new IntLocalVar();
        IntLocalVar acc = new IntLocalVar();
        
        MethodHandle line0A = assignment(idx.setter, intConstant(0));
        MethodHandle line0B = assignment(acc.setter, intConstant(0));

        MethodHandle astLength  = application(mh_array_length, aref.asType(methodType(java.lang.Object.class)));
        MethodHandle line1_cond = application(mh_intLessThan, idx.getter, astLength);

        MethodHandle body_A1 = application(mh_addII, acc.getter, application(arr_g, idx.getter) );
        MethodHandle body_A  = assignment(acc.setter, body_A1);
        MethodHandle body_B  = assignment(idx.setter, application(mh_addII, idx.getter, intConstant(1)));

        MethodHandle body = blockStmt( new MethodHandle[] { body_A, body_B });

        MethodHandle line1 = whileLoop(line1_cond, body);
        
        MethodHandle result = blockExpr(new MethodHandle[] {
            line0A, line0B, line1
          },
          acc.getter
        );

        return result;
    }

}

