package net.madz.lifecycle.engine.callback;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;

import net.madz.lifecycle.LifecycleContext;
import net.madz.lifecycle.annotations.Function;
import net.madz.lifecycle.annotations.Functions;
import net.madz.lifecycle.annotations.LifecycleMeta;
import net.madz.lifecycle.annotations.StateMachine;
import net.madz.lifecycle.annotations.StateSet;
import net.madz.lifecycle.annotations.Transition;
import net.madz.lifecycle.annotations.TransitionSet;
import net.madz.lifecycle.annotations.action.Condition;
import net.madz.lifecycle.annotations.action.ConditionSet;
import net.madz.lifecycle.annotations.action.Conditional;
import net.madz.lifecycle.annotations.action.ConditionalTransition;
import net.madz.lifecycle.annotations.callback.PostStateChange;
import net.madz.lifecycle.annotations.callback.PreStateChange;
import net.madz.lifecycle.annotations.relation.InboundWhile;
import net.madz.lifecycle.annotations.relation.RelateTo;
import net.madz.lifecycle.annotations.relation.Relation;
import net.madz.lifecycle.annotations.relation.RelationSet;
import net.madz.lifecycle.annotations.state.End;
import net.madz.lifecycle.annotations.state.Initial;
import net.madz.lifecycle.annotations.state.LifecycleOverride;
import net.madz.lifecycle.engine.EngineTestBase;
import net.madz.lifecycle.engine.EngineTestBase.ReactiveObject;
import net.madz.lifecycle.engine.callback.CallbackTestMetadata.InvoiceStateMachineMeta.Conditions;
import net.madz.lifecycle.engine.callback.CallbackTestMetadata.InvoiceStateMachineMeta.Conditions.Payable;
import net.madz.lifecycle.engine.callback.CallbackTestMetadata.InvoiceStateMachineMeta.States.PaidOff;
import net.madz.lifecycle.engine.callback.CallbackTestMetadata.InvoiceStateMachineMeta.States.PartialPaid;
import net.madz.lifecycle.engine.callback.CallbackTestMetadata.InvoiceStateMachineMeta.Utilities.PayableJudger;
import net.madz.verification.VerificationException;

import org.junit.BeforeClass;

public class CallbackTestMetadata extends EngineTestBase {

    @BeforeClass
    public static void setup() throws VerificationException {
        registerMetaFromClass(CallbackTestMetadata.class);
    }

    @StateMachine
    static interface CallbackStateMachine {

        @StateSet
        static interface States {

            @Initial
            @Function(transition = Transitions.Start.class, value = { States.Started.class })
            static interface New {}
            @Function(transition = Transitions.Finish.class, value = { States.Finished.class })
            static interface Started {}
            @End
            static interface Finished {}
        }
        @TransitionSet
        static interface Transitions {

            static interface Start {}
            static interface Finish {}
        }
    }
    @LifecycleMeta(CallbackStateMachine.class)
    public static class CallbackObjectBase extends ReactiveObject {

        public CallbackObjectBase() {
            initialState(CallbackStateMachine.States.New.class.getSimpleName());
        }

        protected int callbackInvokeCounter = 0;

        @Transition
        public void start() {}

        @Transition
        public void finish() {}

        public int getCallbackInvokeCounter() {
            return this.callbackInvokeCounter;
        }
    }
    @LifecycleMeta(CallbackStateMachine.class)
    public static class PreCallbackFromAnyToAny extends CallbackObjectBase {

        @PreStateChange
        public void interceptPreStateChange(LifecycleContext<PreCallbackFromAnyToAny, String> context) {
            this.callbackInvokeCounter++;
        }
    }
    @LifecycleMeta(CallbackStateMachine.class)
    public static class PreCallbackFromStartToAny extends CallbackObjectBase {

        @PreStateChange(from = CallbackStateMachine.States.Started.class)
        public void interceptPreStateChange(LifecycleContext<PreCallbackFromStartToAny, String> context) {
            this.callbackInvokeCounter++;
        }
    }
    @LifecycleMeta(CallbackStateMachine.class)
    public static class PreCallbackFromAnyToStart extends CallbackObjectBase {

        @PreStateChange(to = CallbackStateMachine.States.Started.class)
        public void interceptPreStateChange(LifecycleContext<PreCallbackFromAnyToStart, String> context) {
            this.callbackInvokeCounter++;
        }
    }
    @LifecycleMeta(CallbackStateMachine.class)
    public static class PostCallbackFromAnyToAny extends CallbackObjectBase {

        @PostStateChange
        public void interceptPostStateChange(LifecycleContext<PostCallbackFromAnyToAny, String> context) {
            this.callbackInvokeCounter++;
        }
    }
    @LifecycleMeta(CallbackStateMachine.class)
    public static class PostCallbackFromAnyToStart extends CallbackObjectBase {

        @PostStateChange(to = CallbackStateMachine.States.Started.class)
        public void interceptPostStateChange(LifecycleContext<PostCallbackFromAnyToStart, String> context) {
            this.callbackInvokeCounter++;
        }
    }
    @LifecycleMeta(CallbackStateMachine.class)
    public static class PostCallbackFromStartToAny extends CallbackObjectBase {

        @PostStateChange(from = CallbackStateMachine.States.Started.class)
        public void interceptPostStateChange(LifecycleContext<PostCallbackFromStartToAny, String> context) {
            this.callbackInvokeCounter++;
        }
    }
    @StateMachine
    public static interface InvoiceStateMachineMeta {

        @StateSet
        static interface States {

            @Initial
            @Function(transition = InvoiceStateMachineMeta.Transitions.Post.class, value = { InvoiceStateMachineMeta.States.Posted.class })
            static interface Draft {}
            @Functions({ @Function(transition = InvoiceStateMachineMeta.Transitions.Pay.class, value = { States.PartialPaid.class,
                    InvoiceStateMachineMeta.States.PaidOff.class }) })
            static interface Posted {}
            @Function(transition = InvoiceStateMachineMeta.Transitions.Pay.class, value = { States.PartialPaid.class,
                    InvoiceStateMachineMeta.States.PaidOff.class })
            static interface PartialPaid {}
            @End
            static interface PaidOff {}
        }
        @TransitionSet
        static interface Transitions {

            static interface Post {}
            @Conditional(condition = Payable.class, judger = PayableJudger.class, postEval = true)
            static interface Pay {}
        }
        @ConditionSet
        static interface Conditions {

            static interface Payable {

                BigDecimal getTotalAmount();

                BigDecimal getPayedAmount();
            }
        }
        public static class Utilities {

            public static class PayableJudger implements ConditionalTransition<Payable> {

                @Override
                public Class<?> doConditionJudge(Payable t) {
                    if ( 0 < t.getPayedAmount().compareTo(BigDecimal.ZERO) && 0 < t.getTotalAmount().compareTo(t.getPayedAmount()) ) {
                        return PartialPaid.class;
                    } else if ( 0 >= t.getTotalAmount().compareTo(t.getPayedAmount()) ) {
                        return PaidOff.class;
                    } else {
                        throw new IllegalStateException();
                    }
                }
            }
        }
    }
    @StateMachine
    public static interface InvoiceItemStateMachineMeta {

        @StateSet
        static interface States {

            @Initial
            @Function(transition = InvoiceItemStateMachineMeta.Transitions.Pay.class, value = { InvoiceItemStateMachineMeta.States.Paid.class })
            static interface Unpaid {}
            @End
            @InboundWhile(on = { InvoiceStateMachineMeta.States.Posted.class, InvoiceStateMachineMeta.States.PartialPaid.class },
                    relation = InvoiceItemStateMachineMeta.Relations.ParentInvoice.class)
            static interface Paid {}
        }
        @TransitionSet
        static interface Transitions {

            static interface Pay {}
        }
        @RelationSet
        static interface Relations {

            @RelateTo(InvoiceStateMachineMeta.class)
            static interface ParentInvoice {}
        }
    }
    @LifecycleMeta(InvoiceStateMachineMeta.class)
    public static class Invoice extends ReactiveObject implements InvoiceStateMachineMeta.Conditions.Payable {

        private final BigDecimal totalAmount;;
        private BigDecimal payedAmount = new BigDecimal(0D);
        private final List<InvoiceItem> items = new ArrayList<>();

        public Invoice(final BigDecimal totalAmount) {
            initialState(InvoiceStateMachineMeta.States.Draft.class.getSimpleName());
            this.totalAmount = totalAmount;
        }

        @Condition(InvoiceStateMachineMeta.Conditions.Payable.class)
        public InvoiceStateMachineMeta.Conditions.Payable getPayable() {
            return this;
        }

        @Override
        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        @Override
        public synchronized BigDecimal getPayedAmount() {
            return payedAmount;
        }

        @Transition
        public void post() {}

        @Transition(InvoiceStateMachineMeta.Transitions.Pay.class)
        @PostStateChange(to = InvoiceItemStateMachineMeta.States.Paid.class, observableName = "items", mappedBy = "parent")
        public synchronized void onItemPaied(LifecycleContext<InvoiceItem, String> context) {
            payedAmount = payedAmount.add(context.getTarget().getPayedAmount());
        }

        public void addItem(InvoiceItem invoiceItem) {
            if ( !items.contains(invoiceItem) ) {
                items.add(invoiceItem);
            }
        }

        public List<InvoiceItem> getItems() {
            return Collections.unmodifiableList(items);
        }
    }
    @LifecycleMeta(InvoiceItemStateMachineMeta.class)
    public static class InvoiceItem extends ReactiveObject {

        private int seq;
        private BigDecimal amount;
        private BigDecimal payedAmount;
        private final Invoice parent;

        public InvoiceItem(Invoice parent, BigDecimal amount) {
            initialState(InvoiceItemStateMachineMeta.States.Unpaid.class.getSimpleName());
            this.amount = amount;
            this.parent = parent;
            this.seq = this.parent.getItems().size() + 1;
            this.parent.addItem(this);
        }

        @Transition
        public void pay(final BigDecimal amount) {
            if ( 0 < this.amount.compareTo(amount) ) {
                throw new IllegalArgumentException("paying amount is not enough to pay this item.");
            }
            this.payedAmount = amount;
        }

        public BigDecimal getPayedAmount() {
            return payedAmount;
        }

        @Relation(InvoiceItemStateMachineMeta.Relations.ParentInvoice.class)
        public Invoice getParent() {
            return this.parent;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( ( parent == null ) ? 0 : parent.hashCode() );
            result = prime * result + seq;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if ( this == obj ) return true;
            if ( obj == null ) return false;
            if ( getClass() != obj.getClass() ) return false;
            InvoiceItem other = (InvoiceItem) obj;
            if ( parent == null ) {
                if ( other.parent != null ) return false;
            } else if ( !parent.equals(other.parent) ) return false;
            if ( seq != other.seq ) return false;
            return true;
        }
    }
    @LifecycleMeta(InvoiceItemStateMachineMeta.class)
    public static class InvoiceItemNonRelationalCallback extends ReactiveObject {

        private int seq;
        private BigDecimal amount = new BigDecimal(0);
        private BigDecimal payedAmount = new BigDecimal(0);
        private final InvoiceNonRelationalCallback parent;

        public InvoiceItemNonRelationalCallback(InvoiceNonRelationalCallback parent, BigDecimal amount) {
            initialState(InvoiceItemStateMachineMeta.States.Unpaid.class.getSimpleName());
            this.amount = amount;
            this.parent = parent;
            this.seq = this.parent.getItems().size() + 1;
            this.parent.addItem(this);
        }

        @Transition
        public void pay(BigDecimal amount) {
            if ( 0 < this.amount.compareTo(amount) ) {
                throw new IllegalArgumentException("paying amount is not enough to pay this item.");
            }
            this.payedAmount = amount;
            // parent.onItemPaied(this);
        }

        @PostStateChange(to = InvoiceItemStateMachineMeta.States.Paid.class)
        public void notifyParent(LifecycleContext<InvoiceItemNonRelationalCallback, String> context) {
            this.parent.onItemPaied(this);
        }

        public BigDecimal getPayedAmount() {
            return payedAmount;
        }

        @Relation(InvoiceItemStateMachineMeta.Relations.ParentInvoice.class)
        public InvoiceNonRelationalCallback getParent() {
            return this.parent;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( ( parent == null ) ? 0 : parent.hashCode() );
            result = prime * result + seq;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if ( this == obj ) return true;
            if ( obj == null ) return false;
            if ( getClass() != obj.getClass() ) return false;
            InvoiceItemNonRelationalCallback other = (InvoiceItemNonRelationalCallback) obj;
            if ( parent == null ) {
                if ( other.parent != null ) return false;
            } else if ( !parent.equals(other.parent) ) return false;
            if ( seq != other.seq ) return false;
            return true;
        }
    }
    @LifecycleMeta(InvoiceStateMachineMeta.class)
    public static class InvoiceNonRelationalCallback extends ReactiveObject implements Conditions.Payable {

        private final BigDecimal totalAmount;
        private BigDecimal payedAmount = new BigDecimal(0D);
        private final List<InvoiceItemNonRelationalCallback> items = new ArrayList<>();

        public InvoiceNonRelationalCallback(final BigDecimal totalAmount) {
            initialState(InvoiceStateMachineMeta.States.Draft.class.getSimpleName());
            this.totalAmount = totalAmount;
        }

        @Condition(InvoiceStateMachineMeta.Conditions.Payable.class)
        public Conditions.Payable getPayable() {
            return this;
        }

        @Override
        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        @Override
        public synchronized BigDecimal getPayedAmount() {
            return payedAmount;
        }

        @Transition
        public void post() {}

        @Transition(InvoiceStateMachineMeta.Transitions.Pay.class)
        public synchronized void onItemPaied(InvoiceItemNonRelationalCallback item) {
            payedAmount = payedAmount.add(item.getPayedAmount());
        }

        public void addItem(InvoiceItemNonRelationalCallback invoiceItem) {
            if ( !items.contains(invoiceItem) ) {
                items.add(invoiceItem);
            }
        }

        public List<InvoiceItemNonRelationalCallback> getItems() {
            return Collections.unmodifiableList(items);
        }
    }
    @StateMachine
    public static interface OrderStateMachine {

        @StateSet
        public static interface States {

            @Initial
            @Function(transition = Transitions.Pay.class, value = { States.Paid.class })
            public static interface New {}
            @Function(transition = Transitions.Deliver.class, value = { States.Delivered.class })
            public static interface Paid {}
            @End
            public static interface Delivered {}
        }
        @TransitionSet
        public static interface Transitions {

            public static interface Pay {}
            public static interface Deliver {}
        }
    }
    @StateMachine
    public static interface BigProductOrderStateMachine extends OrderStateMachine {

        @StateSet
        public static interface States extends OrderStateMachine.States {

            @Function(transition = Transitions.Cancel.class, value = { Cancelled.class })
            public static interface New extends OrderStateMachine.States.New {}
            @LifecycleOverride
            @Function(transition = Transitions.Install.class, value = { States.Installed.class })
            public static interface Delivered extends OrderStateMachine.States.Delivered {}
            @End
            public static interface Installed {}
            @End
            public static interface Cancelled {}
        }
        @TransitionSet
        public static interface Transitions extends OrderStateMachine.Transitions {

            public static interface Install {}
            public static interface Cancel {}
        }
    }
    @LifecycleMeta(OrderStateMachine.class)
    public static class OrderObject<T> extends ReactiveObject {

        protected int count = 0;

        public OrderObject() {
            initialState(OrderStateMachine.States.New.class.getSimpleName());
        }

        @Transition
        public void pay() {}

        @Transition
        public void deliver() {}

        public int getCount() {
            return count;
        }

        @PostStateChange(from = OrderStateMachine.States.New.class)
        public void interceptPostStateChange(LifecycleContext<T, String> context) {
            count++;
            System.out.println("Order is created.");
        }

        @PostStateChange(to = OrderStateMachine.States.Delivered.class)
        public void interceptPostStateChangeWhenOrderFinished(LifecycleContext<T, String> context) {
            count++;
            System.out.println("Order is delivered.");
        }
    }
    @LifecycleMeta(BigProductOrderStateMachine.class)
    public static class BigProductOrderObjectWithExtendsCallbackDefinition extends OrderObject<BigProductOrderObjectWithExtendsCallbackDefinition> {

        @Transition
        public void install() {}

        @Transition
        public void cancel() {}

        /**
         * Use case 1: extends the call back method on the non overridden
         * state "New".<br/>
         * Scenario: <li>StateOverride: No</li> <li>Lifecycle Override: No</li>
         * 
         * Expected Behavior:<br/>
         * When state transit from New or Delivered state, the
         * call back method will be invoked.
         */
        @PostStateChange(from = BigProductOrderStateMachine.States.Delivered.class)
        @Override
        public void interceptPostStateChange(LifecycleContext<BigProductOrderObjectWithExtendsCallbackDefinition, String> context) {
            count++;
            System.out.println("interceptPostStateChange in " + getClass().getSimpleName());
        }
    }
    @LifecycleMeta(BigProductOrderStateMachine.class)
    public static class BigProductOrderObjectWithOverridesCallbackDefinition extends OrderObject<BigProductOrderObjectWithOverridesCallbackDefinition> {

        @Transition
        public void install() {}

        @Transition
        public void cancel() {}

        /**
         * Use case 2: overrides the call back definition on the
         * overridden state "Delivered".<br/>
         * Scenario: <li>StateOverride: Yes</li> <li>Lifecycle Override: Yes</li>
         * Expected Behavior:<br/>
         * When state transit to state "Delivered",
         * this call back method will not be
         * invoked.
         * When state transit to state "Installed", this method will be
         * invoked.
         */
        @LifecycleOverride
        @PostStateChange(to = BigProductOrderStateMachine.States.Installed.class)
        @Override
        public void interceptPostStateChangeWhenOrderFinished(LifecycleContext<BigProductOrderObjectWithOverridesCallbackDefinition, String> context) {
            count++;
            System.out.println("Big Product Order is finished.");
        }
    }
    @LifecycleMeta(BigProductOrderStateMachine.class)
    public static class OrderWithSpecifiedFromToCallback extends OrderObject<OrderWithSpecifiedFromToCallback> {

        @Transition
        public void install() {}

        @Transition
        public void cancel() {}

        @PostStateChange(from = BigProductOrderStateMachine.States.New.class, to = BigProductOrderStateMachine.States.Cancelled.class)
        public void intercept(LifecycleContext<OrderWithSpecifiedFromToCallback, String> context) {
            count++;
            System.out.println("OrderWithSpecifiedFromToCallback is finished.");
        }
    }
    @LifecycleMeta(BigProductOrderStateMachine.class)
    public static class OrderWithSpecifiedPreFromToCallback extends OrderObject<OrderWithSpecifiedPreFromToCallback> {

        @Transition
        public void install() {}

        @Transition
        public void cancel() {}

        @PreStateChange(from = BigProductOrderStateMachine.States.Delivered.class, to = BigProductOrderStateMachine.States.Installed.class)
        public void intercept(LifecycleContext<OrderWithSpecifiedPreFromToCallback, String> context) {
            Assert.assertEquals(BigProductOrderStateMachine.States.Installed.class.getSimpleName(), context.getToStateName());
            Assert.assertEquals(BigProductOrderStateMachine.States.Installed.class.getSimpleName(), context.getToState());
            Assert.assertEquals(BigProductOrderStateMachine.States.Delivered.class.getSimpleName(), context.getFromStateName());
            Assert.assertEquals(BigProductOrderStateMachine.States.Delivered.class.getSimpleName(), context.getFromState());
            Assert.assertEquals(this, context.getTarget());
            try {
                Assert.assertEquals(OrderWithSpecifiedPreFromToCallback.class.getMethod("install"), context.getTransitionMethod());
            } catch (NoSuchMethodException | SecurityException ignored) {}
            Assert.assertEquals(0, context.getArguments().length);
            count++;
            System.out.println("OrderWithSpecifiedPreFromToCallback is finished.");
        }
    }
}
