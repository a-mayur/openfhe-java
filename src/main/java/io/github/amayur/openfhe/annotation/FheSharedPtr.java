package io.github.amayur.openfhe.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.bytedeco.javacpp.annotation.Adapter;

/** shared_ptr&lt;T&gt; / shared_ptr&lt;const T&gt; parameters and return values, see jopenfhe.h */
@Documented @Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Adapter("jopenfhe::FheSharedPtrAdapter")
public @interface FheSharedPtr {
    String value() default "";
}
