package net.madz.lifecycle.meta.template;

import net.madz.lifecycle.annotations.action.ConditionalTransition;
import net.madz.lifecycle.meta.MetaType;
import net.madz.lifecycle.meta.template.TransitionMetadata.TransitionTypeEnum;

public interface TransitionMetadata extends MetaType<TransitionMetadata> {

    StateMachineMetadata getStateMachine();

    public static enum TransitionTypeEnum {
        Corrupt,
        Recover,
        Redo,
        Fail,
        Common,
        Other;

        public boolean isUniqueTransition() {
            return this == TransitionTypeEnum.Corrupt || this == TransitionTypeEnum.Recover || this == TransitionTypeEnum.Redo;
        }
    }

    TransitionTypeEnum getType();

    long getTimeout();

    boolean isConditional();

    Class<?> getConditionClass();

    Class<? extends ConditionalTransition<?>> getJudgerClass();

    boolean postValidate();
}
