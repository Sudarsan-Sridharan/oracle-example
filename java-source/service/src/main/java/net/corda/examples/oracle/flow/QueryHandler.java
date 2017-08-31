package net.corda.examples.oracle.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.identity.Party;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.oracle.service.Oracle;

import java.math.BigInteger;

// The Service side flow to handle Oracle queries.
@InitiatedBy(QueryPrime.class)
public class QueryHandler extends FlowLogic<Void> {
    private final Party otherParty;

    public QueryHandler(Party otherParty) {
        this.otherParty = otherParty;
    }

    private final ProgressTracker progressTracker = new ProgressTracker(
            RECEIVED,
            SENDING
    );

    private static final ProgressTracker.Step RECEIVED = new ProgressTracker.Step("Received query request");
    private static final ProgressTracker.Step SENDING = new ProgressTracker.Step("Sending query response");

    @Override
    public ProgressTracker getProgressTracker() { return progressTracker; }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        // Receive the request.
        int request = receive(Integer.class, otherParty).unwrap(it -> it);
        progressTracker.setCurrentStep(SENDING);
        try {
            // Get the nth prime from the Oracle.
            BigInteger response = getServiceHub().cordaService(Oracle.class).query(request);
            // Send back the result.
            send(otherParty, response);
        } catch (Exception e) {
            // Re-throw exceptions as Flow Exceptions so they are propagated to other nodes.
            throw new FlowException(e);
        }

        return null;
    }
}
