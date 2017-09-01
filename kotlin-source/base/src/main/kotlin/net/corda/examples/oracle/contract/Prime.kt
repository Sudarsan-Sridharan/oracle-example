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
    // 'index' is a natural number n and 'value' is the nth prime number.
    // 'Requester' represents the Party that will store this fact in the node's vault.
    data class State(val index: Int,
                     val value: Int,
                     val requester: AbstractParty) : ContractState {
        override val contract: Contract get() = Prime()
        override val participants: List<AbstractParty> get() = listOf(requester)
        override fun toString() = "The ${index}th prime number is $value."
    }

    // This command with data is be used in conjunction with an Oracle.
    class Create(val index: Int, val value: Int) : CommandData

    // Contract code that only checks that the properties in the state match those in the command.
    // We are relying on the Oracle to provide the correct nth prime.
    override fun verify(tx: LedgerTransaction) = requireThat {
        val command = tx.commandsOfType<Create>().single().value
        val output = tx.outputsOfType<State>().single()
        "The output prime is not correct." using (command.index == output.index && (command.value == output.value))
    }
}
