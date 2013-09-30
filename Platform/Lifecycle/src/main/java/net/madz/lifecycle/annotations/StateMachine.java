package net.madz.lifecycle.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StateMachine {

    StateSet states() default @StateSet();

    TransitionSet transitions() default @TransitionSet();

    Class<?> parentOn() default Null.class;
}
