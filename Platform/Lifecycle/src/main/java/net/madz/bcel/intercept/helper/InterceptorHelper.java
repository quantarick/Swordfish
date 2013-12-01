package net.madz.bcel.intercept.helper;

import java.lang.reflect.Method;
import java.util.ArrayList;

import net.madz.bcel.intercept.InterceptContext;
import net.madz.lifecycle.AbsStateMachineRegistry;
import net.madz.lifecycle.annotations.ReactiveObject;
import net.madz.lifecycle.annotations.Transition;
import net.madz.lifecycle.meta.object.StateMachineObject;
import net.madz.util.StringUtil;
import net.madz.utils.Null;
import net.madz.verification.VerificationException;

public final class InterceptorHelper {

    public static synchronized StateMachineObject<?> lookupStateMachine(InterceptContext<?, ?> context) {
        try {
            return AbsStateMachineRegistry.getInstance().loadStateMachineObject(extractLifecycleMetaClass(context));
        } catch (VerificationException e) {
            throw new IllegalStateException("Should not encounter syntax verification exception at intercepting runtime", e);
        }
    }

    private static boolean doMatchMethod(final Method subClassMethod, final Class<?> klass) {
        if ( klass == Object.class ) return false;
        try {
            final Method tmpMethod = klass.getMethod(subClassMethod.getName(), subClassMethod.getParameterTypes());
            final Transition transition = subClassMethod.getAnnotation(Transition.class);
            final Transition tmpTransition = tmpMethod.getAnnotation(Transition.class);
            if ( hasSameTransitionKey(transition, tmpTransition) || hasSameTransitionName(subClassMethod, transition, tmpTransition) ) {
                return true;
            }
        } catch (NoSuchMethodException ignore) {}
        return false;
    }

    private static Class<? extends Object> extractLifecycleMetaClass(InterceptContext<?, ?> context) {
        if ( null != context.getTarget().getClass().getAnnotation(ReactiveObject.class) ) {
            Method method = context.getMethod();
            Object target = context.getTarget();
            Class<?> lifecycleMetaClass = findLifecycleMetaClass(target.getClass(), method);
            return lifecycleMetaClass;
        } else {
            return context.getTarget().getClass();
        }
    }

    private static Class<?> findLifecycleMetaClass(final Class<?> implClass, final Method method) {
        final Transition transition = method.getAnnotation(Transition.class);
        if ( Null.class == transition.value() ) {
            throw new IllegalStateException("With @ReactiveObject, transition.value has to be explicitly specified.");
        } else {
            return scanMethodsOnClasses(implClass.getInterfaces(), method);
        }
    }

    private static boolean hasSameTransitionKey(final Transition transition, final Transition tmpTransition) {
        return tmpTransition.value() != Null.class && transition.value() == tmpTransition.value();
    }

    private static boolean hasSameTransitionName(final Method subClassMethod, final Transition transition, final Transition tmpTransition) {
        return tmpTransition.value() == Null.class
                && transition.value().getSimpleName().equalsIgnoreCase(StringUtil.toUppercaseFirstCharacter(subClassMethod.getName()));
    }

    private static void populateSuperclasses(final ArrayList<Class<?>> superclasses, final Class<?> klass) {
        if ( null != klass.getSuperclass() && Object.class != klass ) {
            superclasses.add(klass.getSuperclass());
        }
        for ( Class<?> interfaze : klass.getInterfaces() ) {
            superclasses.add(interfaze);
        }
    }

    private static Class<?> scanMethodsOnClasses(Class<?>[] klasses, final Method subClassMethod) {
        if ( 0 == klasses.length ) throw new IllegalArgumentException();
        final ArrayList<Class<?>> superclasses = new ArrayList<Class<?>>();
        for ( final Class<?> klass : klasses ) {
            if ( doMatchMethod(subClassMethod, klass) ) {
                return klass;
            }
            populateSuperclasses(superclasses, klass);
        }
        return scanMethodsOnClasses(superclasses.toArray(new Class<?>[superclasses.size()]), subClassMethod);
    }
}
