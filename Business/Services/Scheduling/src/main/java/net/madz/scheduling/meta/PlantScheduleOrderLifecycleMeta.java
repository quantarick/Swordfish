package net.madz.scheduling.meta;

import net.madz.lifecycle.annotations.Function;
import net.madz.lifecycle.annotations.StateMachine;
import net.madz.lifecycle.annotations.StateSet;
import net.madz.lifecycle.annotations.TransitionSet;
import net.madz.lifecycle.annotations.relation.InboundWhile;
import net.madz.lifecycle.annotations.relation.Parent;
import net.madz.lifecycle.annotations.relation.RelateTo;
import net.madz.lifecycle.annotations.relation.RelationSet;
import net.madz.lifecycle.annotations.state.End;
import net.madz.lifecycle.annotations.state.LifecycleOverride;
import net.madz.scheduling.meta.OrderLifecycleMeta.Transitions.Finish;
import net.madz.scheduling.meta.PlantScheduleOrderLifecycleMeta.Relations.ServiceOrder;

@StateMachine(parentOn = ServiceOrderLifecycleMeta.class)
public interface PlantScheduleOrderLifecycleMeta extends OrderLifecycleMeta {

    @StateSet
    public static class States extends OrderLifecycleMeta.States {

        @InboundWhile(on = { ServiceOrderLifecycleMeta.States.Ongoing.class }, relation = ServiceOrder.class)
        public static class Created extends OrderLifecycleMeta.States.Draft {}
        @InboundWhile(on = { ServiceOrderLifecycleMeta.States.Ongoing.class }, relation = ServiceOrder.class)
        @Function(transition = Finish.class, value = { Finished.class })
        @LifecycleOverride
        public static class Ongoing extends OrderLifecycleMeta.States.Ongoing {}
        @End
        @InboundWhile(on = { ServiceOrderLifecycleMeta.States.Ongoing.class }, relation = ServiceOrder.class)
        public static class Finished extends OrderLifecycleMeta.States.Finished {}
    }
    @TransitionSet
    public static class Transitions extends OrderLifecycleMeta.Transitions {}
    @RelationSet
    public static class Relations {

        @Parent
        @RelateTo(ServiceOrderLifecycleMeta.class)
        public static class ServiceOrder {}
    }
}
