package com.r3.conclave.jvmtester.djvm.testsauxiliary;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(TYPE)
public @interface JavaAnnotation {
    String value() default "<default-value>";
}
