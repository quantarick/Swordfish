package net.madz.bcel.intercept;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.madz.lifecycle.AbsStateMachineRegistry;
import net.madz.lifecycle.LifecycleCommonErrors;
import net.madz.lifecycle.LifecycleEventHandler;
import net.madz.lifecycle.LifecycleException;
import net.madz.lifecycle.LifecycleLockStrategry;
import net.madz.lifecycle.annotations.Null;
import net.madz.lifecycle.annotations.ReactiveObject;
import net.madz.lifecycle.annotations.Transition;
import net.madz.lifecycle.impl.LifecycleContextImpl;
import net.madz.lifecycle.impl.LifecycleEventImpl;
import net.madz.lifecycle.meta.instance.StateMachineObject;
import net.madz.lifecycle.meta.template.StateMetadata;
import net.madz.lifecycle.meta.template.TransitionMetadata;
import net.madz.util.StringUtil;
import net.madz.verification.VerificationException;

public class LifecycleInterceptor<V, R> extends Interceptor<V, R> {

    private static final Logger logger = Logger.getLogger("Lifecycle Framework");

    public static Class<?> scanMethodsOnClasses(Class<?>[] klasses, final Method method) {
        if ( 0 == klasses.length ) throw new IllegalArgumentException();
        final ArrayList<Class<?>> superclasses = new ArrayList<Class<?>>();
        for ( final Class<?> klass : klasses ) {
            if ( klass == Object.class ) continue;
            try {
                Method tmpMethod = klass.getMethod(method.getName(), method.getParameterTypes());
                final Transition transition = method.getAnnotation(Transition.class);
                final Transition tmpTransition = tmpMethod.getAnnotation(Transition.class);
                if ( transition.value() == tmpTransition.value()
                        || ( tmpTransition.value() == Null.class && transition.value().getSimpleName()
                                .equalsIgnoreCase(StringUtil.toUppercaseFirstCharacter(method.getName())) ) ) {
                    return klass;
                }
            } catch (NoSuchMethodException e) {}
            if ( null != klass.getSuperclass() && Object.class != klass ) {
                superclasses.add(klass.getSuperclass());
            }
            for ( Class<?> interfaze : klass.getInterfaces() ) {
                superclasses.add(interfaze);
            }
        }
        return scanMethodsOnClasses(superclasses.toArray(new Class<?>[superclasses.size()]), method);
    }

    public LifecycleInterceptor(Interceptor<V, R> next) {
        super(next);
        if ( logger.isLoggable(Level.FINE) ) {
            logger.fine("Intercepting....instantiating LifecycleInterceptor");
        }
    }

    @Override
    protected void handleException(InterceptContext<V, R> context, Throwable e) {
        context.setFailureCause(e);
        super.handleException(context, e);
    }

    @Override
    protected void preExec(InterceptContext<V, R> context) {
        super.preExec(context);
        final StateMachineObject<?> stateMachine = lookupStateMachine(context);
        if ( isLockEnabled(stateMachine) ) {
            final LifecycleLockStrategry lock = stateMachine.getLifecycleLockStrategy();
            lock.lockWrite(context.getTarget());
        }
        // Set From State Before all instructions.
        context.setFromState(stateMachine.evaluateState(context.getTarget()));
        if ( logger.isLoggable(Level.FINE) ) {
            logger.fine("intercepting  [" + context.getTarget() + "]" + "\n\tfrom state: [" + context.getFromState() + "] ");
        }
        // 1. Validate State validity
        if ( logger.isLoggable(Level.FINE) ) {
            logger.fine("\tStep 1. start validating State [" + context.getFromState() + "]");
        }
        validateStateValidWhiles(stateMachine, context);
        // 2. Validate Transition validity
        if ( logger.isLoggable(Level.FINE) ) {
            logger.fine("\tStep 2. start validating transition: [" + context.getTransitionKey() + "] on state: [" + context.getFromState() + "]");
        }
        validateTransition(stateMachine, context);
        // 3. Validate in-bound Relation constraint if next state is predictable
        // before method invocation
        if ( logger.isLoggable(Level.FINE) ) {
            logger.fine("\tStep 3. start validating inbound relation constraint is next state is predictable before method invocation.");
        }
        if ( nextStateCanBeEvaluatedBeforeTranstion(stateMachine, context) ) {
            validateNextStateInboundWhile(stateMachine, context);
        }
        // 4. Callback before state change
        if ( logger.isLoggable(Level.FINE) ) {
            logger.fine("\tStep 4. start callback before state change from : " + context.getFromState() + " => to : " + context.getToState());
        }
        performCallbacksBeforeStateChange(stateMachine, context);
    }

    @Override
    protected void postExec(InterceptContext<V, R> context) {
        super.postExec(context);
        final StateMachineObject<?> stateMachine = lookupStateMachine(context);
        try {
            // 5. Validate in-bound Relation constraint if next state is
            // predictable after method invocation.
            if ( logger.isLoggable(Level.FINE) ) {
                logger.fine("\tStep 5. start validating inbound relation constraint is next state after method invocation.");
            }
            if ( !nextStateCanBeEvaluatedBeforeTranstion(stateMachine, context) ) {
                validateNextStateInboundWhile(stateMachine, context);
            }
            // 6. Setup next state
            if ( logger.isLoggable(Level.FINE) ) {
                logger.fine("\tStep 6. Set next state to reactiveObject.");
            }
            setNextState(stateMachine, context);
            if ( logger.isLoggable(Level.FINE) ) {
                logger.fine("\tStep 6. ReactiveObject is tranisited to state: [" + context.getToState() + "]");
            }
            // 7. Callback after state change
            if ( logger.isLoggable(Level.FINE) ) {
                logger.fine("\tStep 7. Start Callback after state change from : " + context.getFromState() + " => to : " + context.getToState());
            }
            performCallbacksAfterStateChange(stateMachine, context);
            context.setSuccess(true);
        } finally {
            unlockRelationObjects(context);
            if ( isLockEnabled(stateMachine) ) {
                final LifecycleLockStrategry lock = stateMachine.getLifecycleLockStrategy();
                if ( null != lock ) {
                    lock.unlockWrite(context.getTarget());
                }
            }
            context.end();
            // 8. Fire state change notification events.
            if ( logger.isLoggable(Level.FINE) ) {
                logger.fine("\tStep 8. Start fire state change event.");
            }
            fireLifecycleEvents(stateMachine, context);
        }
    }

    @Override
    protected void cleanup(InterceptContext<V, R> context) {
        super.cleanup(context);
        if ( logger.isLoggable(Level.FINE) ) {
            logger.fine("Intercepting....LifecycleInterceptor is doing cleanup ...");
            if ( !context.isSuccess() ) {
                String toStateString = null == context.getToState() ? "(Had Not Been Evaluated)" : context.getToState();
                logger.severe("ReactiveObject: [" + context.getTarget() + "] was failed to transit from state: [" + context.getFromState() + "] to state: ["
                        + toStateString + "] with following error: ");
                logger.severe(context.getFailureCause().getLocalizedMessage());
            }
        }
        unlockRelationObjects(context);
        final StateMachineObject<?> stateMachine = lookupStateMachine(context);
        if ( !context.isSuccess() && isLockEnabled(stateMachine) ) {
            final LifecycleLockStrategry lockStrategry = stateMachine.getLifecycleLockStrategy();
            if ( null != lockStrategry ) {
                lockStrategry.unlockWrite(context.getTarget());
            }
        }
    }

    private void fireLifecycleEvents(StateMachineObject<?> stateMachine, InterceptContext<V, R> context) {
        final LifecycleEventHandler eventHandler = AbsStateMachineRegistry.getInstance().getLifecycleEventHandler();
        if ( null != eventHandler ) {
            eventHandler.onEvent(new LifecycleEventImpl(context));
        }
    }

    private boolean isLockEnabled(StateMachineObject<?> stateMachine) {
        return null != stateMachine.getLifecycleLockStrategy();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void performCallbacksAfterStateChange(StateMachineObject stateMachine, InterceptContext<V, R> context) {
        stateMachine.performPostStateChangeCallback(context);
    }

    private void setNextState(StateMachineObject<?> stateMachine, InterceptContext<V, R> context) {
        final String stateName = stateMachine.getNextState(context.getTarget(), context.getTransitionKey());
        stateMachine.setTargetState(context.getTarget(), stateName);
        context.setToState(stateName);
    }

    private void validateNextStateInboundWhile(StateMachineObject<?> stateMachine, InterceptContext<V, R> context) {
        stateMachine.validateInboundWhiles(context);
    }

    private boolean nextStateCanBeEvaluatedBeforeTranstion(StateMachineObject<?> stateMachine, InterceptContext<V, R> context) {
        if ( hasOnlyOneStateCandidate(stateMachine, context) ) {
            return true;
        } else if ( canEvaluateConditionBeforeTransition(stateMachine, context) ) {
            return true;
        }
        return false;
    }

    private boolean canEvaluateConditionBeforeTransition(StateMachineObject<?> stateMachine, InterceptContext<V, R> context) {
        return stateMachine.evaluateConditionBeforeTransition(context.getTransitionKey());
    }

    private boolean hasOnlyOneStateCandidate(StateMachineObject<?> stateMachine, InterceptContext<V, R> context) {
        final String stateName = stateMachine.evaluateState(context.getTarget());
        final StateMetadata state = stateMachine.getMetaType().getState(stateName);
        if ( state.hasMultipleStateCandidatesOn(context.getTransitionKey()) ) {
            return false;
        } else {
            return true;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void performCallbacksBeforeStateChange(StateMachineObject stateMachine, InterceptContext<V, R> context) {
        stateMachine.performPreStateChangeCallback(context);
    }

    private void validateTransition(StateMachineObject<?> stateMachine, InterceptContext<V, R> context) {
        StateMetadata stateMetadata = stateMachine.getMetaType().getState(context.getFromState());
        if ( !stateMetadata.isTransitionValid(context.getTransitionKey()) ) {
            throw new LifecycleException(getClass(), "lifecycle_common", LifecycleCommonErrors.ILLEGAL_TRANSITION_ON_STATE, context.getTransitionKey(),
                    context.getFromState(), context.getTarget());
        } else {
            TransitionMetadata transition = stateMetadata.getTransition(context.getTransitionKey());
            context.setTransitionType(transition.getType());
            context.setTransition(transition.getDottedPath().getName());
        }
    }

    private void validateStateValidWhiles(StateMachineObject<?> stateMachine, InterceptContext<V, R> context) {
        stateMachine.validateValidWhiles(context);
    }

    private static synchronized StateMachineObject<?> lookupStateMachine(InterceptContext<?, ?> context) {
        try {
            return AbsStateMachineRegistry.getInstance().loadStateMachineObject(extractLifecycleMetaClass(context));
        } catch (VerificationException e) {
            throw new IllegalStateException("Should not encounter syntax verification exception at intercepting runtime", e);
        }
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

    private void unlockRelationObjects(UnlockableStack stack) {
        Unlockable unlockable = null;
        while ( !stack.isEmpty() ) {
            unlockable = stack.popUnlockable();
            unlockable.unlock();
        }
    }
}