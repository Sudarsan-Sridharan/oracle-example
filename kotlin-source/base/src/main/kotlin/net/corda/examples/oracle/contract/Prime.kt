package net.corda.examples.oracle.contract

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.requireThat
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction

// Contract and state object definition.
class Prime : Contract {
    // State object with custom properties defined in the constructor.
    // If 'index' is a natural number N then 'value' is the Nth Prime.
    // Requester represents the Party that will store this fact (in the node vault).
    data class State(val index: Int,
                     val value: Int,
                     val requester: AbstractParty) : ContractState {
        override val contract: Contract get() = Prime()
        override val participants: List<AbstractParty> get() = listOf(requester)
        override fun toString() = "The ${index}th prime number is $value."
    }

    // Command with data items.
    // Commands that are to be used in conjunction with an Oracle contain properties
    class Create(val index: Int, val value: Int) : CommandData

    // Contract code.
    // Here, we are only checking that the properties in the state match those in the command.
    // We are relying on the Oracle to provide the correct nth prime.
    override fun verify(tx: LedgerTransaction) = requireThat {
        val command = tx.commandsOfType<Create>().single().value
        val output = tx.outputsOfType<State>().single()
        "The output prime is not correct." using (command.index == output.index && (command.value == output.value))
    }
}
