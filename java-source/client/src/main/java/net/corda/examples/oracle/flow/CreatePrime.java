package net.corda.examples.oracle.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Command;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.examples.oracle.contract.Prime;
import net.corda.examples.oracle.service.PrimeType;

// This is the client side flow that makes use of the 'QueryPrime' and 'SignPrime' flows to obtain data from the Oracle
// and the Oracle's signature over the transaction containing it.
@InitiatingFlow     // This flow can be started by the node.
@StartableByRPC // Annotation to allow this flow to be started via RPC.
public class CreatePrime extends FlowLogic<SignedTransaction> {
    private final int index;

    public CreatePrime(int index) {
        this.index = index;
    }

    // Progress tracker boilerplate.
    private final ProgressTracker progressTracker = new ProgressTracker(
            INITIALISING,
            QUERYING,
            BUILDING_AND_VERIFYING,
            ORACLE_SIGNING,
            SIGNING,
            FINALISING
    );

    private static final Step INITIALISING = new Step("Initialising flow.");
    private static final Step QUERYING = new Step("Querying Oracle for an nth prime.");
    private static final Step BUILDING_AND_VERIFYING = new Step("Building and verifying transaction.");
    private static final Step ORACLE_SIGNING = new Step("Requesting Oracle signature.");
    private static final Step SIGNING = new Step("Signing transaction.");
    private static final Step FINALISING = new Step("Finalising transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // Get references to all required parties.
        progressTracker.setCurrentStep(INITIALISING);
        Party notary = getServiceHub().getNetworkMapCache().getNotaryNodes().get(0).getNotaryIdentity();
        // The calling node's identity.
        Party me = getServiceHub().getMyInfo().getLegalIdentity();
        // We get the oracle reference by using the ServiceType definition defined in the base CorDapp.
        NodeInfo oracle = getServiceHub().getNetworkMapCache().getNodesWithService(PrimeType.getType()).get(0);
        // **IMPORTANT:** Corda node services use their own key pairs, therefore we need to obtain the Party object for
        // the Oracle service as opposed to the node RUNNING the Oracle service.
        Party oracleService = oracle.serviceIdentities(PrimeType.getType()).get(0);

        // Query the Oracle to get specified nth prime number.
        progressTracker.setCurrentStep(QUERYING);
        // Query the Oracle. Specify the identity of the Oracle we want to query and a natural number N.
        int nthPrime = subFlow(new QueryPrime(oracle.getLegalIdentity(), index));

        // Create a new transaction using the data from the Oracle.
        progressTracker.setCurrentStep(BUILDING_AND_VERIFYING);
        // Build our command.
        // NOTE: The command requires the public key of the oracle, hence we need the signature from the oracle over
        // this transaction.
        Command command = new Command<>(
                new Prime.Create(index, nthPrime),
                ImmutableList.of(oracleService.getOwningKey(), me.getOwningKey()));
        // Create a new prime state.
        Prime.State state = new Prime.State(index, nthPrime, me);
        // Add the state and the command to the builder.
        TransactionBuilder builder = new TransactionBuilder(notary).withItems(command, state);

        // Verify the transaction.
        builder.verify(getServiceHub());

        // Sign the builder to convert it onto a SignedTransaction.
        progressTracker.setCurrentStep(SIGNING);
        SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

        // Get a signature from the Oracle and add it to the transaction.
        progressTracker.setCurrentStep(ORACLE_SIGNING);
        // Build a filtered transaction for the Oracle to sign over.
        // We only want to expose 'Prime.Create' commands if the specified Oracle is a signer.
        FilteredTransaction ftx = ptx.getTx().buildFilteredTransaction(elem -> {
            if (elem instanceof Command) {
                Command cmd = (Command) elem;
                return cmd.getSigners().contains(oracleService.getOwningKey())
                        && cmd.getValue() instanceof Prime.Create;
            }
            return false;
        });
        // Get a signature from the Oracle over the Merkle root of the transaction.
        TransactionSignature oracleSignature = subFlow(new SignPrime(oracle.getLegalIdentity(), ftx));
        // Append the oracle's signature to the transaction.
        SignedTransaction stx = ptx.withAdditionalSignature(oracleSignature);

        // Finalise.
        // We do this by calling finality flow. The transaction will be broadcast to all parties listed in 'participants'.
        progressTracker.setCurrentStep(FINALISING);
        return subFlow(new FinalityFlow(stx)).get(0);
    }
}