package bt4;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.lookup;

public class Test extends RuntimeMP {

    public static MethodHandle mh_p;

    public static void initMHs2() {
        initMHs();
    }

    public static void p() {
        System.out.println("abc");
    }

    public static void main(String[] args) throws Throwable {
        initMHs2();

        // MethodHandle ast =  ifExpr(booleanConstant(true), mh_p, mh_unit);
        // MethodHandle ast =  whileLoop(booleanConstant(true), mh_p);

        MethodHandle aref   = newArrayRef(int.class, 4);
        MethodHandle getter = arrayElemGetter(aref);
        MethodHandle setter = arrayElemSetter(aref);
        setter.invoke(0, 1);
        setter.invoke(1, 2);
        setter.invoke(2, 3);
        setter.invoke(3, 4);

        MethodHandle ast = SummingUp.summationMaker(aref);

        System.out.println(ast.invoke());

        // ast.invoke();

    }

}

