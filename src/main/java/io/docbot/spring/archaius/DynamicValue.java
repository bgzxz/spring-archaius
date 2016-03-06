package io.docbot.spring.archaius;

import java.lang.annotation.*;
@Target({ ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DynamicValue {
    String value();

    String defaultValue();
}
