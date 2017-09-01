package net.corda.examples.oracle.contract;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

// Contract and state object definition.
public class Prime implements Contract {

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        requireThat(require -> {
            Create command = tx.commandsOfType(Create.class).get(0).getValue();
            State output = tx.outputsOfType(State.class).get(0);
            require.using("The output prime is not correct.",
                    (command.index == output.index && (command.value == output.value)));

            return null;
        });
    }

    // State object with custom properties defined in the constructor.
    // If 'index' is a natural number N then 'value' is the Nth Prime.
    // Requester represents the Party that will store this fact (in the node vault).
    public static class State implements ContractState {
        private final int index;
        private final int value;
        private final AbstractParty requester;
        private final Prime contract = new Prime();

        public State(int index, int value, AbstractParty requester) {
            this.index = index;
            this.value = value;
            this.requester = requester;
        }

        @Override
        public Contract getContract() {
            return contract;
        }

        @Override
        public List<AbstractParty> getParticipants() {
            return ImmutableList.of(requester);
        }

        @Override
        public String toString() {
            return String.format("The %sth prime number is %s.", index, value);
        }
    }

    // Command with data items.
    // Commands that are to be used in conjunction with an Oracle contain properties
    public static class Create implements CommandData {
        private int index;
        private int value;

        public Create(int index, int value) {
            this.index = index;
            this.value = value;
        }

        public int getIndex() { return index; }
        public int getValue() { return value; }
    }
}