package net.corda.examples.oracle.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.identity.Party;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.oracle.service.Oracle;

// The Service side flow to handle Oracle signing requests.
@InitiatedBy(SignPrime.class)
public class SignHandler extends FlowLogic<Void> {
    private final Party otherParty;

    public SignHandler(Party otherParty) {
        this.otherParty = otherParty;
    }

    private final ProgressTracker progressTracker = new ProgressTracker(
            RECEIVED,
            SENDING
    );

    private static final ProgressTracker.Step RECEIVED = new ProgressTracker.Step("Received sign request");
    private static final ProgressTracker.Step SENDING = new ProgressTracker.Step("Sending sign response");

    @Override
    public ProgressTracker getProgressTracker() { return progressTracker; }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        FilteredTransaction request = receive(FilteredTransaction.class, otherParty).unwrap(it -> it);
        progressTracker.setCurrentStep(SENDING);
        try {
            TransactionSignature response = getServiceHub().cordaService(Oracle.class).sign(request);
            send(otherParty, response);
        } catch (Exception e) {
            throw new FlowException(e);
        }

        return null;
    }
}
