package at.haha007.edenclient.utils.singleton;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//has to have a no-argument constructor, can be private.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Singleton {
    //initialization from lowest to highest
    int priority() default 0;
}
