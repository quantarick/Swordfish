package net.madz.lifecycle.meta.builder.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import net.madz.bcel.intercept.Unlockable;
import net.madz.bcel.intercept.UnlockableStack;
import net.madz.lifecycle.LifecycleCommonErrors;
import net.madz.lifecycle.LifecycleContext;
import net.madz.lifecycle.LifecycleException;
import net.madz.lifecycle.LifecycleLockStrategry;
import net.madz.lifecycle.annotations.LifecycleMeta;
import net.madz.lifecycle.annotations.ReactiveObject;
import net.madz.lifecycle.meta.builder.StateMachineObjectBuilder;
import net.madz.lifecycle.meta.builder.StateObjectBuilder;
import net.madz.lifecycle.meta.object.StateMachineObject;
import net.madz.lifecycle.meta.object.StateObject;
import net.madz.lifecycle.meta.type.RelationConstraintMetadata;
import net.madz.lifecycle.meta.type.StateMetadata;
import net.madz.verification.VerificationException;
import net.madz.verification.VerificationFailureSet;

public class StateObjectBuilderImpl<S> extends ObjectBuilderBase<StateObject<S>, StateMachineObject<S>, StateMetadata> implements StateObjectBuilder<S> {

    private final HashMap<String, List<CallbackObject>> preFromStateChangeCallbacksMap = new HashMap<>();
    private final HashMap<String, List<CallbackObject>> preToStateChangeCallbacksMap = new HashMap<>();
    private final HashMap<String, List<CallbackObject>> postFromStateChangeCallbacksMap = new HashMap<>();
    private final HashMap<String, List<CallbackObject>> postToStateChangeCallbacksMap = new HashMap<>();

    protected StateObjectBuilderImpl(StateMachineObjectBuilder<S> parent, StateMetadata stateMetadata) {
        super(parent, "StateSet." + stateMetadata.getDottedPath().getName());
        this.setMetaType(stateMetadata);
    }

    @Override
    public void verifyMetaData(VerificationFailureSet verificationSet) {}

    @Override
    public StateObjectBuilder<S> build(Class<?> klass, StateMachineObject<S> parent) throws VerificationException {
        super.build(klass, parent);
        return this;
    }

    @Override
    public void verifyValidWhile(Object target, RelationConstraintMetadata[] relationMetadataArray, final Object relatedTarget, UnlockableStack stack) {
        try {
            final StateMachineObject<?> relatedStateMachineObject = findRelatedStateMachineWithRelatedTarget(relationMetadataArray, relatedTarget);
            lockRelatedObject(relatedTarget, stack, relatedStateMachineObject);
            final String relatedStateName = relatedStateMachineObject.evaluateState(relatedTarget);
            boolean found = false;
            for ( RelationConstraintMetadata relationMetadata : relationMetadataArray ) {
                for ( StateMetadata stateMetadata : relationMetadata.getOnStates() ) {
                    if ( stateMetadata.getKeySet().contains(relatedStateName) ) {
                        found = true;
                        break;
                    }
                }
            }
            if ( !found ) {
                final LinkedHashSet<String> validRelationStates = new LinkedHashSet<>();
                for ( RelationConstraintMetadata relationMetadata : relationMetadataArray ) {
                    for ( StateMetadata metadata : relationMetadata.getOnStates() ) {
                        validRelationStates.add(metadata.getSimpleName());
                    }
                }
                throw new LifecycleException(getClass(), LifecycleCommonErrors.BUNDLE, LifecycleCommonErrors.STATE_INVALID, target, this.getMetaType()
                        .getSimpleName(), relatedTarget, relatedStateName, Arrays.toString(validRelationStates.toArray(new String[0])));
            } else {
                relatedStateMachineObject.validateValidWhiles(relatedTarget, stack);
            }
        } catch (VerificationException e) {
            throw new IllegalStateException("Cannot happen, it should be defect of syntax verification.");
        }
    }

    private StateMachineObject<?> findRelatedStateMachineWithRelatedTarget(RelationConstraintMetadata[] relationMetadataArray, final Object relatedTarget)
            throws VerificationException {
        Class<?> relatedKey = null;
        if ( null != relatedTarget.getClass().getAnnotation(ReactiveObject.class) ) {
            final RelationConstraintMetadata relationConstraintMetadata = relationMetadataArray[0];
            relatedKey = findRelationKey(relatedTarget, relationConstraintMetadata);
        } else {
            relatedKey = relatedTarget.getClass();
        }
        final StateMachineObject<?> relatedStateMachineObject = this.getRegistry().loadStateMachineObject(relatedKey);
        return relatedStateMachineObject;
    }

    private void lockRelatedObject(final Object relatedTarget, UnlockableStack stack, final StateMachineObject<?> relatedStateMachineObject) {
        if ( !relatedStateMachineObject.isLockEnabled() ) {
            return;
        }
        final LifecycleLockStrategry lifecycleLockStrategy = relatedStateMachineObject.getLifecycleLockStrategy();
        lifecycleLockStrategy.lockRead(relatedTarget);
        stack.pushUnlockable(new Unlockable() {

            @Override
            public void unlock() {
                lifecycleLockStrategy.unlockRead(relatedTarget);
            }
        });
    }

    @Override
    public void verifyInboundWhileAndLockRelatedObjects(Object transitionKey, Object target, String nextState, RelationConstraintMetadata[] relationMetadataArray,
            Object relatedTarget, UnlockableStack stack) {
        try {
            final StateMachineObject<?> relatedStateMachineObject = findRelatedStateMachineWithRelatedTarget(relationMetadataArray, relatedTarget);
            lockRelatedObject(relatedTarget, stack, relatedStateMachineObject);
            final String relatedEvaluateState = relatedStateMachineObject.evaluateState(relatedTarget);
            boolean find = false;
            for ( RelationConstraintMetadata relationMetadata : relationMetadataArray ) {
                for ( StateMetadata stateMetadata : relationMetadata.getOnStates() ) {
                    if ( stateMetadata.getKeySet().contains(relatedEvaluateState) ) {
                        find = true;
                        break;
                    }
                }
            }
            if ( !find ) {
                final LinkedHashSet<String> validRelationStates = new LinkedHashSet<>();
                for ( RelationConstraintMetadata relationMetadata : relationMetadataArray ) {
                    for ( StateMetadata metadata : relationMetadata.getOnStates() ) {
                        validRelationStates.add(metadata.getSimpleName());
                    }
                }
                throw new LifecycleException(getClass(), LifecycleCommonErrors.BUNDLE, LifecycleCommonErrors.VIOLATE_INBOUND_WHILE_RELATION_CONSTRAINT,
                        transitionKey, nextState, target, relatedTarget, relatedEvaluateState, Arrays.toString(validRelationStates.toArray(new String[0])));
            } else {
                relatedStateMachineObject.validateValidWhiles(relatedTarget, stack);
            }
        } catch (VerificationException e) {
            throw new IllegalStateException("Cannot happen, it should be defect of syntax verification.");
        }
    }

    private Class<?> findRelationKey(Object relatedTarget, final RelationConstraintMetadata relationConstraintMetadata) {
        Class<?> relatedKey;
        final Class<?> stateMachineClass = (Class<?>) relationConstraintMetadata.getRelatedStateMachine().getPrimaryKey();
        final Class<?>[] interfaces = relatedTarget.getClass().getInterfaces();
        relatedKey = findRelationClass(stateMachineClass, interfaces);
        if ( null == relatedKey ) {
            throw new IllegalArgumentException("Cannot find " + stateMachineClass + " from " + Arrays.toString(interfaces));
        }
        return relatedKey;
    }

    private Class<?> findRelationClass(final Class<?> stateMachineClass, final Class<?>[] interfaces) {
        for ( final Class<?> interfaze : interfaces ) {
            for ( Annotation annotation : interfaze.getDeclaredAnnotations() ) {
                if ( LifecycleMeta.class == annotation.annotationType() ) {
                    LifecycleMeta meta = (LifecycleMeta) annotation;
                    if ( stateMachineClass == meta.value() ) {
                        return interfaze;
                    }
                }
            }
            if ( interfaze.getInterfaces().length > 0 ) {
                Class<?> result = findRelationClass(stateMachineClass, interfaze.getInterfaces());
                if ( null != result ) {
                    return result;
                }
            }
        }
        return null;
    }

    @Override
    public void invokeFromPreStateChangeCallbacks(LifecycleContext<?, S> callbackContext) {
        if ( preFromStateChangeCallbacksMap.containsKey(callbackContext.getFromStateName()) ) {
            interatorInvokeCallback(callbackContext, preFromStateChangeCallbacksMap.get(callbackContext.getFromStateName()));
        }
    }

    @Override
    public void invokeToPreStateChangeCallbacks(LifecycleContext<?, S> callbackContext) {
        if ( preToStateChangeCallbacksMap.containsKey(callbackContext.getToStateName()) ) {
            interatorInvokeCallback(callbackContext, preToStateChangeCallbacksMap.get(callbackContext.getToStateName()));
        }
    }

    @Override
    public void invokeFromPostStateChangeCallbacks(LifecycleContext<?, S> callbackContext) {
        if ( postFromStateChangeCallbacksMap.containsKey(callbackContext.getFromStateName()) ) {
            interatorInvokeCallback(callbackContext, postFromStateChangeCallbacksMap.get(callbackContext.getFromStateName()));
        }
    }

    @Override
    public void invokeToPostStateChangeCallbacks(LifecycleContext<?, S> callbackContext) {
        if ( postToStateChangeCallbacksMap.containsKey(callbackContext.getToStateName()) ) {
            interatorInvokeCallback(callbackContext, postToStateChangeCallbacksMap.get(callbackContext.getToStateName()));
        }
    }

    private void interatorInvokeCallback(final LifecycleContext<?, S> callbackContext, final List<CallbackObject> callbackObjects) {
        for ( CallbackObject callbackObject : callbackObjects ) {
            callbackObject.doCallback(callbackContext);
        }
    }

    @Override
    public void addPreToCallbackObject(Class<?> to, final CallbackObject callbackObject) {
        final String toStateClassName = to.getSimpleName();
        if ( this.preToStateChangeCallbacksMap.containsKey(toStateClassName) ) {
            this.preToStateChangeCallbacksMap.get(toStateClassName).add(callbackObject);
        } else {
            final List<CallbackObject> callbackObjects = new ArrayList<>();
            callbackObjects.add(callbackObject);
            this.preToStateChangeCallbacksMap.put(toStateClassName, callbackObjects);
        }
    }

    @Override
    public void addPreFromCallbackObject(Class<?> from, final CallbackObject callbackObject) {
        final String fromStateClassName = from.getSimpleName();
        if ( this.preFromStateChangeCallbacksMap.containsKey(fromStateClassName) ) {
            this.preFromStateChangeCallbacksMap.get(fromStateClassName).add(callbackObject);
        } else {
            final List<CallbackObject> callbackObjects = new ArrayList<>();
            callbackObjects.add(callbackObject);
            this.preFromStateChangeCallbacksMap.put(fromStateClassName, callbackObjects);
        }
    }

    @Override
    public void addPostToCallbackObject(Class<?> to, final CallbackObject item) {
        final String toStateClassName = to.getSimpleName();
        if ( this.postToStateChangeCallbacksMap.containsKey(toStateClassName) ) {
            this.postToStateChangeCallbacksMap.get(toStateClassName).add(item);
        } else {
            final List<CallbackObject> callbackObjects = new ArrayList<>();
            callbackObjects.add(item);
            this.postToStateChangeCallbacksMap.put(toStateClassName, callbackObjects);
        }
    }

    @Override
    public void addPostFromCallbackObject(Class<?> from, final CallbackObject item) {
        final String fromStateClassName = from.getSimpleName();
        if ( this.postFromStateChangeCallbacksMap.containsKey(fromStateClassName) ) {
            this.postFromStateChangeCallbacksMap.get(fromStateClassName).add(item);
        } else {
            final List<CallbackObject> callbackObjects = new ArrayList<>();
            callbackObjects.add(item);
            this.postFromStateChangeCallbacksMap.put(fromStateClassName, callbackObjects);
        }
    }
}
