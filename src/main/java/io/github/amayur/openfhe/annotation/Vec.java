package io.github.amayur.openfhe.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.bytedeco.javacpp.annotation.Adapter;

/** std::vector&lt;T&gt; as a Java array. value = "jni type,c++ type", e.g. "jlong,int64_t" */
@Documented @Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Adapter("jopenfhe::VecAdapter")
public @interface Vec {
    String value();
}
