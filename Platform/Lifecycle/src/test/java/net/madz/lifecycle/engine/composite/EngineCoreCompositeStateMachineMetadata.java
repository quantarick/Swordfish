package net.madz.lifecycle.engine.composite;

import net.madz.lifecycle.annotations.CompositeState;
import net.madz.lifecycle.annotations.Function;
import net.madz.lifecycle.annotations.Functions;
import net.madz.lifecycle.annotations.LifecycleMeta;
import net.madz.lifecycle.annotations.StateMachine;
import net.madz.lifecycle.annotations.StateSet;
import net.madz.lifecycle.annotations.Transition;
import net.madz.lifecycle.annotations.TransitionSet;
import net.madz.lifecycle.annotations.relation.RelateTo;
import net.madz.lifecycle.annotations.relation.Relation;
import net.madz.lifecycle.annotations.relation.RelationSet;
import net.madz.lifecycle.annotations.relation.ValidWhile;
import net.madz.lifecycle.annotations.state.End;
import net.madz.lifecycle.annotations.state.Initial;
import net.madz.lifecycle.annotations.state.LifecycleOverride;
import net.madz.lifecycle.annotations.state.ShortCut;
import net.madz.lifecycle.engine.EngineTestBase;
import net.madz.verification.VerificationException;

import org.junit.BeforeClass;

public class EngineCoreCompositeStateMachineMetadata extends EngineTestBase {

    @BeforeClass
    public static void registerLifecycleMetadata() throws VerificationException {
        registerMetaFromClass(EngineCoreCompositeStateMachineMetadata.class);
    }

    // ///////////////////////////////////////////////////////////////////////////////
    // Non Relational (Composite State Machine without inheritance)
    // ///////////////////////////////////////////////////////////////////////////////
    @StateMachine
    static interface OrderLifecycle {

        @StateSet
        static interface States {

            @Initial
            @Function(transition = Transitions.Start.class, value = Started.class)
            static interface Created {}
            @CompositeState
            @Function(transition = Transitions.Cancel.class, value = Canceled.class)
            static interface Started {

                @StateSet
                static interface SubStates {

                    @Initial
                    @Function(transition = OrderLifecycle.States.Started.SubTransitions.DoProduce.class, value = Producing.class)
                    static interface OrderCreated {}
                    @Function(transition = OrderLifecycle.States.Started.SubTransitions.DoDeliver.class, value = Delivering.class)
                    static interface Producing {}
                    @Function(transition = OrderLifecycle.States.Started.SubTransitions.ConfirmComplete.class, value = Done.class)
                    static interface Delivering {}
                    @End
                    @ShortCut(OrderLifecycle.States.Finished.class)
                    static interface Done {}
                }
                @TransitionSet
                static interface SubTransitions {

                    static interface DoProduce {}
                    static interface DoDeliver {}
                    static interface ConfirmComplete {}
                }
            }
            @End
            static interface Finished {}
            @End
            static interface Canceled {}
        }
        @TransitionSet
        static interface Transitions {

            static interface Start {}
            static interface Cancel {}
        }
    }
    public abstract static class ProductBase extends ReactiveObject {}
    @LifecycleMeta(OrderLifecycle.class)
    public static class ProductOrder extends ProductBase {

        public ProductOrder() {
            initialState(OrderLifecycle.States.Created.class.getSimpleName());
        }

        @Transition
        public void start() {}

        @Transition
        public void cancel() {}

        @Transition
        public void doProduce() {}

        @Transition
        public void doDeliver() {}

        @Transition
        public void confirmComplete() {}
    }
    // ///////////////////////////////////////////////////////////////////////////////
    // Relational
    // ///////////////////////////////////////////////////////////////////////////////
    @StateMachine
    static interface ContractLifecycle {

        @StateSet
        static interface States {

            @Initial
            @Functions({ @Function(transition = ContractLifecycle.Transitions.Activate.class, value = Active.class),
                    @Function(transition = ContractLifecycle.Transitions.Cancel.class, value = Canceled.class) })
            static interface Draft {}
            @Functions({ @Function(transition = ContractLifecycle.Transitions.Expire.class, value = Expired.class),
                    @Function(transition = ContractLifecycle.Transitions.Cancel.class, value = Canceled.class) })
            static interface Active {}
            @End
            static interface Expired {}
            @End
            static interface Canceled {}
        }
        @TransitionSet
        static interface Transitions {

            static interface Activate {}
            static interface Expire {}
            static interface Cancel {}
        }
    }
    @LifecycleMeta(ContractLifecycle.class)
    public static class Contract extends ReactiveObject {

        public Contract() {
            initialState(ContractLifecycle.States.Draft.class.getSimpleName());
        }

        @Transition
        public void activate() {}

        @Transition
        public void expire() {}

        @Transition
        public void cancel() {}
    }
    // ///////////////////////////////////////////////////////////////////////////////
    // Relational Case 1.
    // ///////////////////////////////////////////////////////////////////////////////
    @StateMachine
    static interface RelationalOrderLifecycleReferencingOuterRelation {

        @StateSet
        static interface States {

            @Initial
            @Function(transition = Transitions.Start.class, value = Started.class)
            @ValidWhile(on = { ContractLifecycle.States.Active.class }, relation = Relations.Contract.class)
            static interface Created {}
            @CompositeState
            @Function(transition = Transitions.Cancel.class, value = Canceled.class)
            static interface Started {

                @StateSet
                static interface SubStates {

                    @Initial
                    @Function(transition = RelationalOrderLifecycleReferencingOuterRelation.States.Started.SubTransitions.DoProduce.class,
                            value = Producing.class)
                    @ValidWhile(on = { ContractLifecycle.States.Active.class },
                            relation = RelationalOrderLifecycleReferencingOuterRelation.Relations.Contract.class)
                    static interface OrderCreated {}
                    @Function(transition = RelationalOrderLifecycleReferencingOuterRelation.States.Started.SubTransitions.DoDeliver.class,
                            value = Delivering.class)
                    @ValidWhile(on = { ContractLifecycle.States.Active.class },
                            relation = RelationalOrderLifecycleReferencingOuterRelation.Relations.Contract.class)
                    static interface Producing {}
                    @Function(transition = RelationalOrderLifecycleReferencingOuterRelation.States.Started.SubTransitions.ConfirmComplete.class,
                            value = Done.class)
                    @ValidWhile(on = { ContractLifecycle.States.Active.class },
                            relation = RelationalOrderLifecycleReferencingOuterRelation.Relations.Contract.class)
                    static interface Delivering {}
                    @End
                    @ShortCut(RelationalOrderLifecycleReferencingOuterRelation.States.Finished.class)
                    // Ignoring : @ValidWhile(on = {
                    // ContractLifecycle.States.Active.class }, relation =
                    // Relations.Contract.class)
                    static interface Done {}
                }
                @TransitionSet
                static interface SubTransitions {

                    static interface DoProduce {}
                    static interface DoDeliver {}
                    static interface ConfirmComplete {}
                }
            }
            @End
            static interface Finished {}
            @End
            static interface Canceled {}
        }
        @TransitionSet
        static interface Transitions {

            static interface Start {}
            static interface Cancel {}
        }
        @RelationSet
        static interface Relations {

            @RelateTo(ContractLifecycle.class)
            static interface Contract {}
        }
    }
    @LifecycleMeta(RelationalOrderLifecycleSharingValidWhile.class)
    public static class ProductOrderSharingValidWhile extends ProductBase {

        private Contract contract;

        public ProductOrderSharingValidWhile(Contract contract) {
            this.contract = contract;
            initialState(RelationalOrderLifecycleSharingValidWhile.States.Created.class.getSimpleName());
        }

        @Relation(RelationalOrderLifecycleSharingValidWhile.Relations.Contract.class)
        public Contract getContract() {
            return this.contract;
        }

        @Transition
        public void start() {}

        @Transition
        public void cancel() {}

        @Transition
        public void doProduce() {}

        @Transition
        public void doDeliver() {}

        @Transition
        public void confirmComplete() {}
    }
    // ///////////////////////////////////////////////////////////////////////////////
    // Relational Case 2.
    // ///////////////////////////////////////////////////////////////////////////////
    @StateMachine
    static interface RelationalOrderLifecycleSharingValidWhile {

        @StateSet
        static interface States {

            @Initial
            @Function(transition = Transitions.Start.class, value = Started.class)
            @ValidWhile(on = { ContractLifecycle.States.Active.class }, relation = Relations.Contract.class)
            static interface Created {}
            @CompositeState
            @Function(transition = Transitions.Cancel.class, value = Canceled.class)
            @ValidWhile(on = { ContractLifecycle.States.Active.class }, relation = Relations.Contract.class)
            static interface Started {

                @StateSet
                static interface SubStates {

                    @Initial
                    @Function(transition = RelationalOrderLifecycleSharingValidWhile.States.Started.SubTransitions.DoProduce.class, value = Producing.class)
                    static interface OrderCreated {}
                    @Function(transition = RelationalOrderLifecycleSharingValidWhile.States.Started.SubTransitions.DoDeliver.class, value = Delivering.class)
                    static interface Producing {}
                    @Function(transition = RelationalOrderLifecycleSharingValidWhile.States.Started.SubTransitions.ConfirmComplete.class, value = Done.class)
                    static interface Delivering {}
                    @End
                    @ShortCut(RelationalOrderLifecycleSharingValidWhile.States.Finished.class)
                    static interface Done {}
                }
                @TransitionSet
                static interface SubTransitions {

                    static interface DoProduce {}
                    static interface DoDeliver {}
                    static interface ConfirmComplete {}
                }
            }
            @End
            static interface Finished {}
            @End
            static interface Canceled {}
        }
        @TransitionSet
        static interface Transitions {

            static interface Start {}
            static interface Cancel {}
        }
        @RelationSet
        static interface Relations {

            @RelateTo(ContractLifecycle.class)
            static interface Contract {}
        }
    }
    @LifecycleMeta(RelationalOrderLifecycleReferencingOuterRelation.class)
    public static class ProductOrderOuterRelation extends ProductBase {

        private final Contract contract;

        public ProductOrderOuterRelation(Contract contract) {
            initialState(RelationalOrderLifecycleReferencingOuterRelation.States.Created.class.getSimpleName());
            this.contract = contract;
        }

        @Relation(RelationalOrderLifecycleReferencingOuterRelation.Relations.Contract.class)
        public Contract getContract() {
            return this.contract;
        }

        @Transition
        public void start() {}

        @Transition
        public void cancel() {}

        @Transition
        public void doProduce() {}

        @Transition
        public void doDeliver() {}

        @Transition
        public void confirmComplete() {}
    }
    @StateMachine
    static interface RelationalOrderLifecycleReferencingInnerValidWhile {

        @StateSet
        static interface States {

            @Initial
            @Function(transition = Transitions.Start.class, value = Started.class)
            static interface Created {}
            @CompositeState
            @Function(transition = Transitions.Cancel.class, value = Canceled.class)
            static interface Started {

                @StateSet
                static interface SubStates {

                    @Initial
                    @Function(transition = RelationalOrderLifecycleReferencingInnerValidWhile.States.Started.SubTransitions.DoProduce.class,
                            value = Producing.class)
                    @ValidWhile(on = { ContractLifecycle.States.Active.class }, relation = Relations.Contract.class)
                    static interface OrderCreated {}
                    @Function(transition = RelationalOrderLifecycleReferencingInnerValidWhile.States.Started.SubTransitions.DoDeliver.class,
                            value = Delivering.class)
                    static interface Producing {}
                    @Function(transition = RelationalOrderLifecycleReferencingInnerValidWhile.States.Started.SubTransitions.ConfirmComplete.class,
                            value = Done.class)
                    static interface Delivering {}
                    @End
                    @ShortCut(RelationalOrderLifecycleReferencingInnerValidWhile.States.Finished.class)
                    static interface Done {}
                }
                @TransitionSet
                static interface SubTransitions {

                    static interface DoProduce {}
                    static interface DoDeliver {}
                    static interface ConfirmComplete {}
                }
                @RelationSet
                static interface Relations {

                    @RelateTo(ContractLifecycle.class)
                    static interface Contract {}
                }
            }
            @End
            static interface Finished {}
            @End
            static interface Canceled {}
        }
        @TransitionSet
        static interface Transitions {

            static interface Start {}
            static interface Cancel {}
        }
    }
    // ///////////////////////////////////////////////////////////////////////////////
    // Relational Case 3.
    // ///////////////////////////////////////////////////////////////////////////////
    @LifecycleMeta(RelationalOrderLifecycleReferencingInnerValidWhile.class)
    public static class ProductOrderInnerValidWhile extends ProductBase {

        private final Contract contract;

        public ProductOrderInnerValidWhile(Contract contract) {
            initialState(RelationalOrderLifecycleReferencingInnerValidWhile.States.Created.class.getSimpleName());
            this.contract = contract;
        }

        @Relation(RelationalOrderLifecycleReferencingInnerValidWhile.States.Started.Relations.Contract.class)
        public Contract getContract() {
            return contract;
        }

        @Transition
        public void start() {}

        @Transition
        public void cancel() {}

        @Transition
        public void doProduce() {}

        @Transition
        public void doDeliver() {}

        @Transition
        public void confirmComplete() {}
    }
    // /////////////////////////////////////////////////////////////////////////////////////////////////////
    // Part II: composite state machine with inheritance) According to Image
    // File:
    // Composite State Machine Visibility Scope.png
    // /////////////////////////////////////////////////////////////////////////////////////////////////////
    @StateMachine
    public static interface SM2 {

        @StateSet
        static interface States {

            @Initial
            @Function(transition = SM2.Transitions.T6.class, value = SM2.States.S1.class)
            static interface S0 {}
            @CompositeState
            @Function(transition = SM2.Transitions.T6.class, value = SM2.States.S2.class)
            @ValidWhile(relation = SM2.Relations.R6.class, on = { ContractLifecycle.States.Active.class })
            static interface S1 {

                @StateSet
                static interface CStates {

                    @Initial
                    @Function(transition = SM2.States.S1.CTransitions.T4.class, value = SM2.States.S1.CStates.CS1.class)
                    @ValidWhile(relation = SM2.States.S1.CRelations.R4.class, on = { ContractLifecycle.States.Expired.class })
                    static interface CS0 {}
                    @End
                    @ShortCut(SM2.States.S2.class)
                    static interface CS1 {}
                }
                @TransitionSet
                static interface CTransitions {

                    static interface T4 {}
                }
                @RelationSet
                static interface CRelations {

                    @RelateTo(ContractLifecycle.class)
                    static interface R4 {}
                }
            }
            @CompositeState
            @Function(transition = SM2.Transitions.T6.class, value = SM2.States.S3.class)
            static interface S2 {

                @StateSet
                static interface CStates {

                    @Initial
                    @Function(transition = SM2.States.S2.CTransitions.T5.class, value = SM2.States.S2.CStates.CS3.class)
                    static interface CS2 {}
                    @End
                    @ShortCut(SM2.States.S3.class)
                    static interface CS3 {}
                }
                @TransitionSet
                static interface CTransitions {

                    static interface T5 {}
                }
                @RelationSet
                static interface CRelations {

                    @RelateTo(ContractLifecycle.class)
                    static interface R5 {}
                }
            }
            @End
            static interface S3 {}
        }
        @TransitionSet
        static interface Transitions {

            static interface T6 {}
        }
        @RelationSet
        static interface Relations {

            @RelateTo(ContractLifecycle.class)
            static interface R6 {}
        }
    }
    @StateMachine
    public static interface SM1_No_Overrides extends SM2 {

        @StateSet
        static interface States extends SM2.States {

            @Initial
            @Function(transition = SM1_No_Overrides.Transitions.T2.class, value = SM1_No_Overrides.States.S1.class)
            @LifecycleOverride
            static interface S0 extends SM2.States.S0 {}
            @CompositeState
            @Function(transition = SM1_No_Overrides.Transitions.T2.class, value = SM1_No_Overrides.States.S2.class)
            @ValidWhile(relation = SM1_No_Overrides.Relations.R2.class, on = { ContractLifecycle.States.Draft.class })
            static interface S1 extends SM2.States.S1 {

                @StateSet
                static interface CStates extends SM2.States.S1.CStates {

                    @Initial
                    @Function(transition = SM1_No_Overrides.States.S1.CTransitions.T1.class, value = SM1_No_Overrides.States.S1.CStates.CS1.class)
                    static interface CS0 extends SM2.States.S1.CStates.CS0 {}
                    @End
                    @ShortCut(SM1_No_Overrides.States.S2.class)
                    static interface CS1 {}
                }
                @TransitionSet
                static interface CTransitions extends SM2.States.S1.CTransitions {

                    static interface T1 {}
                }
                @RelationSet
                static interface CRelations extends SM2.States.S1.CRelations {

                    @RelateTo(ContractLifecycle.class)
                    static interface R1 {}
                }
            }
            @CompositeState
            @Function(transition = SM1_No_Overrides.Transitions.T2.class, value = SM1_No_Overrides.States.S3.class)
            static interface S2 extends SM2.States.S2 {

                @StateSet
                static interface CStates {

                    @Initial
                    @Function(transition = SM1_No_Overrides.States.S2.CTransitions.T3.class, value = SM1_No_Overrides.States.S2.CStates.CS3.class)
                    static interface CS2 extends SM2.States.S2.CStates.CS2 {}
                    @End
                    @ShortCut(SM1_No_Overrides.States.S3.class)
                    static interface CS3 extends SM2.States.S2.CStates.CS3 {}
                }
                @TransitionSet
                static interface CTransitions {

                    static interface T3 {}
                }
                @RelationSet
                static interface CRelations {

                    @RelateTo(ContractLifecycle.class)
                    static interface R3 {}
                }
            }
            @End
            static interface S3 extends SM2.States.S3 {}
        }
        @TransitionSet
        static interface Transitions extends SM2.Transitions {

            static interface T2 {}
        }
        @RelationSet
        static interface Relations extends SM2.Relations {

            @RelateTo(ContractLifecycle.class)
            static interface R2 {}
        }
    }
    @StateMachine
    public static interface SM1_Overrides extends SM2 {

        @StateSet
        static interface States extends SM2.States {

            @Initial
            @Function(transition = SM1_Overrides.Transitions.T2.class, value = SM1_Overrides.States.S1.class)
            @LifecycleOverride
            static interface S0 extends SM2.States.S0 {}
            @CompositeState
            @Function(transition = SM1_Overrides.Transitions.T2.class, value = SM1_Overrides.States.S2.class)
            @ValidWhile(relation = SM1_Overrides.Relations.R2.class, on = { ContractLifecycle.States.Draft.class })
            @LifecycleOverride
            static interface S1 extends SM2.States.S1 {

                @StateSet
                static interface CStates {

                    @Initial
                    @Function(transition = SM1_Overrides.States.S1.CTransitions.T1.class, value = SM1_Overrides.States.S1.CStates.CS1.class)
                    @ValidWhile(relation = SM1_Overrides.States.S1.CRelations.R1.class, on = { ContractLifecycle.States.Expired.class })
                    static interface CS0 {}
                    @End
                    @ShortCut(SM1_Overrides.States.S2.class)
                    static interface CS1 {}
                }
                @TransitionSet
                static interface CTransitions {

                    static interface T1 {}
                }
                @RelationSet
                static interface CRelations {

                    @RelateTo(ContractLifecycle.class)
                    static interface R1 {}
                }
            }
            @CompositeState
            @Function(transition = SM1_Overrides.Transitions.T2.class, value = SM1_Overrides.States.S3.class)
            @LifecycleOverride
            static interface S2 extends SM2.States.S2 {

                @StateSet
                static interface CStates {

                    @Initial
                    @Function(transition = SM1_Overrides.States.S2.CTransitions.T3.class, value = SM1_Overrides.States.S2.CStates.CS3.class)
                    static interface CS2 {}
                    @End
                    @ShortCut(SM1_Overrides.States.S3.class)
                    static interface CS3 {}
                }
                @TransitionSet
                static interface CTransitions {

                    static interface T3 {}
                }
                @RelationSet
                static interface CRelations {

                    @RelateTo(ContractLifecycle.class)
                    static interface R3 {}
                }
            }
            @End
            static interface S3 extends SM2.States.S3 {}
        }
        @TransitionSet
        static interface Transitions extends SM2.Transitions {

            static interface T2 {}
        }
        @RelationSet
        static interface Relations extends SM2.Relations {

            @RelateTo(ContractLifecycle.class)
            static interface R2 {}
        }
    }
    @LifecycleMeta(SM2.class)
    public static class Base extends ReactiveObject {

        private final Contract contract;

        public Base(Contract contract) {
            initialState(SM2.States.S0.class.getSimpleName());
            this.contract = contract;
        }

        @Relation(SM2.Relations.R6.class)
        public Contract getContract() {
            return contract;
        }

        @Relation(SM2.States.S1.CRelations.R4.class)
        public Contract getContract2() {
            return contract;
        }

        @Relation(SM2.States.S2.CRelations.R5.class)
        public Contract getContract3() {
            return contract;
        }

        @Transition(SM2.Transitions.T6.class)
        public void doActionT6() {}

        @Transition(SM2.States.S1.CTransitions.T4.class)
        public void doActionT4() {}

        @Transition(SM2.States.S2.CTransitions.T5.class)
        public void doActionT5() {}
    }
    @LifecycleMeta(SM1_No_Overrides.class)
    public static class NoOverrideComposite extends Base {

        public NoOverrideComposite(Contract contract) {
            super(contract);
        }

        @Transition(SM1_No_Overrides.Transitions.T2.class)
        public void doActionT2() {}

        @Transition(SM1_No_Overrides.States.S1.CTransitions.T1.class)
        public void doActionT1() {}

        @Transition(SM1_No_Overrides.States.S2.CTransitions.T3.class)
        public void doActionT3() {}

        @Relation(SM1_No_Overrides.Relations.R2.class)
        public Contract getContractR2() {
            return getContract();
        }

        @Relation(SM1_No_Overrides.States.S1.CRelations.R1.class)
        public Contract getContractR1() {
            return getContract();
        }

        @Relation(SM1_No_Overrides.States.S2.CRelations.R3.class)
        public Contract getContractR3() {
            return getContract();
        }
    }
    @LifecycleMeta(SM1_Overrides.class)
    public static class OverridesComposite extends Base {

        public OverridesComposite(Contract contract) {
            super(contract);
        }

        @Transition(SM1_Overrides.Transitions.T2.class)
        public void doActionT2() {}

        @Transition(SM1_Overrides.States.S1.CTransitions.T1.class)
        public void doActionT1() {}

        @Transition(SM1_Overrides.States.S2.CTransitions.T3.class)
        public void doActionT3() {}

        @Relation(SM1_Overrides.Relations.R2.class)
        public Contract getContractR2() {
            return getContract();
        }

        @Relation(SM1_Overrides.States.S1.CRelations.R1.class)
        public Contract getContractR1() {
            return getContract();
        }

        @Relation(SM1_Overrides.States.S2.CRelations.R3.class)
        public Contract getContractR3() {
            return getContract();
        }
    }
}