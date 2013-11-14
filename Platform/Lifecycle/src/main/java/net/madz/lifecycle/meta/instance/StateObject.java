package net.madz.lifecycle.meta.instance;

import net.madz.lifecycle.meta.MetaObject;
import net.madz.lifecycle.meta.instance.StateMachineObject.ReadAccessor;
import net.madz.lifecycle.meta.template.RelationMetadata;
import net.madz.lifecycle.meta.template.StateMetadata;

public interface StateObject extends MetaObject<StateObject, StateMetadata> {

    void verifyValidWhile(Object target, RelationMetadata[] relation, ReadAccessor<?> evaluator);

    void verifyInboundWhile(Object transitionKey, Object target,  String nextState, RelationMetadata[] relation, ReadAccessor<?> evaluator);
}
