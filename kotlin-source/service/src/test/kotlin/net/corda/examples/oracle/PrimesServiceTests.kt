package net.corda.examples.oracle

import net.corda.core.contracts.Command
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.examples.oracle.contract.Prime
import net.corda.examples.oracle.service.Oracle
import net.corda.testing.*
import net.corda.testing.node.MockServices
import org.junit.Before
import org.junit.Test
import java.util.function.Predicate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrimesServiceTests : TestDependencyInjectionBase() {
    lateinit var oracle: Oracle

    @Before
    fun setUp() {
        oracle = Oracle(CHARLIE, MockServices(CHARLIE_KEY))
    }

    @Test
    fun `successful query`() {
        val result = oracle.query(10000)
        assertEquals("104729", result.toString())
    }

    @Test
    fun `bad query parameter`() {
        assertFailsWith<IllegalArgumentException> { oracle.query(0) }
        assertFailsWith<IllegalArgumentException> { oracle.query(-1) }
    }

    @Test
    fun `successful sign`() {
        val command = Command(Prime.Create(10, 29), listOf(CHARLIE.owningKey))
        val state = Prime.State(10, 29, ALICE)
        val wtx: WireTransaction = TransactionBuilder(DUMMY_NOTARY)
                .withItems(state, command)
                .toWireTransaction()
        val ftx: FilteredTransaction = wtx.buildFilteredTransaction(Predicate {
            (it is Command<*>) && (oracle.identity.owningKey in it.signers) && (it.value is Prime.Create)
        })
        val signature = oracle.sign(ftx)
        assert(signature.verify(ftx.id))
    }

    @Test
    fun `incorrect prime specified`() {
        val command = Command(Prime.Create(10, 1000), listOf(CHARLIE.owningKey))
        val state = Prime.State(10, 29, ALICE)
        val wtx = TransactionBuilder(DUMMY_NOTARY).withItems(state, command).toWireTransaction()
        val ftx = wtx.buildFilteredTransaction(Predicate {
            (it is Command<*>) && (oracle.identity.owningKey in it.signers) && (it.value is Prime.Create)
        })
        assertFailsWith<IllegalArgumentException> { oracle.sign(ftx) }
    }
}
