package net.corda.examples.oracle;

import net.corda.core.internal.FlowStateMachine;
import net.corda.core.node.services.ServiceInfo;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.oracle.contract.Prime;
import net.corda.examples.oracle.flow.CreatePrime;
import net.corda.examples.oracle.flow.QueryHandler;
import net.corda.examples.oracle.flow.SignHandler;
import net.corda.examples.oracle.service.Oracle;
import net.corda.examples.oracle.service.PrimeType;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetwork.BasketOfNodes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;

import static net.corda.core.crypto.CryptoUtils.random63BitValue;
import static org.junit.Assert.assertEquals;

public class PrimesClientTests {
    private MockNetwork mockNet;
    private MockNetwork.MockNode a;

    @Before
    public void setUp() {
        mockNet = new MockNetwork();
        BasketOfNodes nodes = mockNet.createSomeNodes(2);
        a = nodes.getPartyNodes().get(0);
        ServiceInfo serviceInfo = new ServiceInfo(PrimeType.getType(), null);
        MockNetwork.MockNode oracle = mockNet.createNode(
                nodes.getMapNode().network.getMyAddress(),
                null, true, null, null,
                BigInteger.valueOf(random63BitValue()),
                new ServiceInfo[]{serviceInfo},
                it -> it);
        oracle.installCordaService(Oracle.class);
        oracle.registerInitiatedFlow(QueryHandler.class);
        oracle.registerInitiatedFlow(SignHandler.class);
        mockNet.runNetwork();
    }

    @After
    public void tearDown() {
        mockNet.stopNodes();
    }

    @Test
    public void oracleTest() throws Exception {
        FlowStateMachine flow = a.getServices().startFlow(new CreatePrime(100));
        mockNet.runNetwork();
        SignedTransaction stx = (SignedTransaction) flow.getResultFuture().get();
        Prime.State result = (Prime.State) stx.getTx().getOutputStates().get(0);
        assertEquals("The 100th prime number is 541.", result.toString());
        System.out.println(result);
    }
}
