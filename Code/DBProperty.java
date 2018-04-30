package cn.wwt.frame;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DBProperty {
    String TABLE_NAME() default"";
    String PRIMARY_KEY_NAME() default"";

}
