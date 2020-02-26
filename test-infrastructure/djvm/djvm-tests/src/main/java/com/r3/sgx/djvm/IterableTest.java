package com.r3.sgx.djvm;

import com.r3.sgx.djvm.util.Log;
import com.r3.sgx.djvm.util.SerializationUtils;
import com.r3.sgx.test.EnclaveJvmTest;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class IterableTest {
    public static class CreateEnclaveTest extends DJVMBase implements EnclaveJvmTest {

        @Override
        public Object apply(Object input) {
            AtomicReference<Object> output = new AtomicReference<>();
            sandbox(ctx -> {
                try {
                    Class<Function<Date, String>> createIterableClass =
                            (Class<Function<Date, String>>) loadClass(ctx, "com.r3.sgx.djvm.auxiliary.IterableTest$Create").getType();

                    Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createRawTaskFactory();
                    Function<? super Object, ?> iterableTask = ctx.getClassLoader().createTaskFor(taskFactory, createIterableClass);
                    Iterable<?> iterable = (Iterable<?>) iterableTask.apply(null);
                    Iterator<?> iterator = iterable.iterator();
                    String result = iterator.getClass().getCanonicalName();
                    output.set(result);
                } catch (Throwable throwable) {
                    output.set(Log.recursiveStackTrace(throwable, this.getClass().getCanonicalName()));
                }
                return null;
            });
            return output.get();
        }

        @Override
        public byte[] serializeTestOutput(Object output) {
            return SerializationUtils.serializeString(output).toByteArray();
        }

        @Override
        public void assertResult(@NotNull byte[] testResult) {
            new com.r3.sgx.djvm.asserters.IterableTest.Create().assertResult(testResult);
        }
    }
}