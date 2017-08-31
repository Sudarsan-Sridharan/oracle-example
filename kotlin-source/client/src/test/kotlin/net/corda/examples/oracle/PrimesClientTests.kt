package net.corda.examples.oracle

import net.corda.core.node.services.ServiceInfo
import net.corda.core.utilities.getOrThrow
import net.corda.examples.oracle.contract.Prime
import net.corda.examples.oracle.flow.CreatePrime
import net.corda.examples.oracle.flow.QueryHandler
import net.corda.examples.oracle.flow.SignHandler
import net.corda.examples.oracle.service.Oracle
import net.corda.examples.oracle.service.PrimeType
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class PrimesClientTests {
    lateinit var mockNet: MockNetwork
    lateinit var a: MockNetwork.MockNode

    @Before
    fun setUp() {
        mockNet = MockNetwork()
        val nodes = mockNet.createSomeNodes(2)
        a = nodes.partyNodes[0]
        val serviceInfo = ServiceInfo(PrimeType.type)
        val oracle = mockNet.createNode(nodes.mapNode.network.myAddress, advertisedServices = serviceInfo)
        oracle.installCordaService(Oracle::class.java)
        oracle.registerInitiatedFlow(QueryHandler::class.java)
        oracle.registerInitiatedFlow(SignHandler::class.java)
        mockNet.runNetwork()
    }

    @After
    fun tearDown() {
        mockNet.stopNodes()
    }

    @Test
    fun `oracle test`() {
        val flow = a.services.startFlow(CreatePrime(100))
        mockNet.runNetwork()
        val result = flow.resultFuture.getOrThrow().tx.outputStates.single() as Prime.State
        assertEquals("The 100th prime number is 541.", result.toString())
        println(result)
    }

}
