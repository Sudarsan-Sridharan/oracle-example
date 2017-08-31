package net.corda.examples.oracle

import net.corda.core.contracts.Command
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.examples.oracle.contract.Prime
import net.corda.examples.oracle.service.Oracle
import net.corda.node.utilities.CordaPersistence
import net.corda.node.utilities.configureDatabase
import net.corda.testing.ALICE
import net.corda.testing.CHARLIE
import net.corda.testing.CHARLIE_KEY
import net.corda.testing.DUMMY_NOTARY
import net.corda.testing.node.MockServices
import net.corda.testing.node.makeTestDataSourceProperties
import net.corda.testing.node.makeTestDatabaseProperties
import net.corda.testing.node.makeTestIdentityService
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigInteger
import java.util.function.Predicate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrimesServiceTests {
    val dummyServices = MockServices(CHARLIE_KEY)
    lateinit var oracle: Oracle
    lateinit var database: CordaPersistence

    @Before
    fun setUp() {
        // Mock components for testing the Oracle.
        database = configureDatabase(
                makeTestDataSourceProperties(),
                makeTestDatabaseProperties(),
                createIdentityService = { makeTestIdentityService() })
        database.transaction {
            oracle = Oracle(CHARLIE, dummyServices)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `successful query`() {
        database.transaction {
            val result = oracle.query(10000)
            assertEquals("104729", result.toString())
        }
    }

    @Test
    fun `bad query parameter`() {
        database.transaction {
            assertFailsWith<IllegalArgumentException> { oracle.query(0) }
            assertFailsWith<IllegalArgumentException> { oracle.query(-1) }
        }
    }

    @Test
    fun `successful sign`() {
        database.transaction {
            val command = Command(Prime.Create(10, BigInteger.valueOf(29)), listOf(CHARLIE.owningKey))
            val state = Prime.State(10, BigInteger.valueOf(29), ALICE)
            val wtx: WireTransaction = TransactionBuilder(DUMMY_NOTARY)
                    .withItems(state, command)
                    .toWireTransaction()
            val ftx: FilteredTransaction = wtx.buildFilteredTransaction(Predicate {
                (it is Command<*>) && (oracle.identity.owningKey in it.signers) && (it.value is Prime.Create)
            })
            val signature = oracle.sign(ftx)
            assert(signature.verify(ftx.rootHash))
        }
    }

    @Test
    fun `incorrect prime specified`() {
        database.transaction {
            val command = Command(Prime.Create(10, BigInteger.valueOf(1000)), listOf(CHARLIE.owningKey))
            val state = Prime.State(10, BigInteger.valueOf(29), ALICE)
            val wtx: WireTransaction = TransactionBuilder(DUMMY_NOTARY).withItems(state, command).toWireTransaction()
            val ftx: FilteredTransaction = wtx.buildFilteredTransaction(Predicate {
                (it is Command<*>) && (oracle.identity.owningKey in it.signers) && (it.value is Prime.Create)
            })
            assertFailsWith<IllegalArgumentException> { oracle.sign(ftx) }
        }
    }
}
