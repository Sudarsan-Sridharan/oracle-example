package net.corda.examples.oracle.flow;

import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.identity.Party;

import java.math.BigInteger;

// Simple flow which takes a reference to an Oracle and a number then returns the corresponding nth prime number.
@InitiatingFlow
class QueryPrime extends FlowLogic<BigInteger> {
    private final Party oracle;
    private final Long n;

    public QueryPrime(Party oracle, Long n) {
        this.oracle = oracle;
        this.n = n;
    }

    @Override public BigInteger call() throws FlowException {
        return sendAndReceive(BigInteger.class, oracle, n).unwrap(it -> it);
    }
}