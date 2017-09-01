package net.corda.examples.oracle.flow;

import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.identity.Party;

// Simple flow which takes a reference to an Oracle and a number n, and returns the nth prime number.
@InitiatingFlow
class QueryPrime extends FlowLogic<Integer> {
    private final Party oracle;
    private final int n;

    public QueryPrime(Party oracle, int n) {
        this.oracle = oracle;
        this.n = n;
    }

    @Override public Integer call() throws FlowException {
        return sendAndReceive(Integer.class, oracle, n).unwrap(it -> it);
    }
}