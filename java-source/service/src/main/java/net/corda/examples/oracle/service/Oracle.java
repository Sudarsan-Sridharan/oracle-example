package net.corda.examples.oracle.service;

import net.corda.core.contracts.Command;
import net.corda.core.crypto.*;
import net.corda.core.identity.Party;
import net.corda.core.node.PluginServiceHub;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.node.services.ServiceType;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.core.transactions.FilteredLeaves;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.examples.oracle.contract.Prime;

import java.math.BigInteger;

// We sub-class 'SingletonSerializeAsToken' to ensure that instances of this class are never serialised by Kryo.
// When a flow is check-pointed, the annotated @Suspendable methods and any object referenced from within those
// annotated methods are serialised onto the stack. Kryo, the reflection based serialisation framework we use, crawls
// the object graph and serialises anything it encounters, producing a graph of serialised objects.
// This can cause some issues. For example, we do not want to serialise large objects on the stack or objects which
// may reference databases or other external services (which cannot be serialised!). We therefore mark certain objects
// with tokens. When Kryo encounters one of these tokens, it doesn't serialise the object. Instead, it makes a
// reference to the type of the object. When flows are de-serialised, the token is used to connect up the object reference
// to an instance which should already exist on the stack.
@CordaService
public class Oracle extends SingletonSerializeAsToken {
    private final Party identity;
    private final ServiceHub services;
    // We need a public static ServiceType field named "type". This will allow the node to check if it's declared
    // in the advertisedServices config and only attempt to load the Oracle if it is.
    public static final ServiceType type = PrimeType.getType();

    public Oracle(Party identity, ServiceHub services) {
        this.identity = identity;
        this.services = services;
    }

    // @CordaService requires us to have a constructor that takes in a single parameter of type PluginServiceHub.
    // This is used by the node to automatically install the Oracle.
    // We use the primary constructor for testing.
    public Oracle(PluginServiceHub services) {
        this.identity = services.getMyInfo().serviceIdentities(PrimeType.getType()).get(0);
        this.services = services;
    }

    public Party getIdentity() { return identity; }

    // Finds the nth prime number.
    // The reason why prime numbers were chosen is because they are easy to reason about and reduce the mental load
    // for this tutorial application.
    // Clearly, most developers can generate a list of primes and all but the largest prime numbers can be verified
    // deterministically in reasonable time. As such, it would be possible to add a constraint in the verify()
    // function that checks the nth prime is indeed the specified number.
    public int query(int n) {
        if (n <= 1) {
            throw new IllegalArgumentException("N must be greater than one.");
        }

        for (int primesCounter = 0, nthPrime = 1; true; nthPrime += 1) {
            if (BigInteger.valueOf((long) nthPrime).isProbablePrime(16)) {
                primesCounter += 1;
            }

            if (primesCounter >= n) {
                return nthPrime;
            }
        }
    }

    // Signs over a transaction if the specified nth prime for a particular n is correct.
    // This function takes a filtered transaction which is a partial Merkle tree. Parts of the transaction which
    // the Oracle doesn't need to see to opine over the correctness of the nth prime have been removed. In this case
    // all but the Prime.Create commands have been removed. If the nth prime is correct then the Oracle signs over
    // the Merkle root (the hash) of the transaction.
    public TransactionSignature sign(FilteredTransaction ftx) {

        // Check the partial Merkle tree is valid.
        try {
            if (!ftx.verify()) throw new IllegalArgumentException("Couldn't verify partial Merkle tree.");
        } catch (MerkleTreeException e) {
            throw new IllegalArgumentException("Couldn't verify partial Merkle tree.");
        }

        // Validate the commands.
        FilteredLeaves leaves = ftx.getFilteredLeaves();
        if (!leaves.checkWithFun(this::check)) throw new IllegalArgumentException();

        SignableData signableData = new SignableData(ftx.getId(), new SignatureMetadata(
                services.getMyInfo().getPlatformVersion(),
                Crypto.findSignatureScheme(identity.getOwningKey()).getSchemeNumberID()));

        // Sign over the Merkle root and return the digital signature.
        return services.getKeyManagementService().sign(signableData, identity.getOwningKey());
    }

    // Check that the correct primes are present for the index values specified.
    boolean commandValidator(Command elem) {
        // This Oracle only cares about commands which have its public key in the signers list.
        // This Oracle also only cares about Prime.Create commands.
        // Of course, some of these constraints can be easily amended. E.g. they Oracle can sign over multiple
        // command types.
        if (!(elem.getSigners().contains(identity.getOwningKey()) && elem.getValue() instanceof Prime.Create)) {
            throw new IllegalArgumentException("Oracle received unknown command (not in signers or not Prime.Create).");
        }

        Prime.Create prime = (Prime.Create) elem.getValue();
        // This is where the check the validity of the nth prime.
        return query(prime.getIndex()) == prime.getValue();
    }

    // This function is run for each non-hash leaf of the Merkle tree.
    // We only expect to see commands.
    boolean check(Object elem) {
        if (elem instanceof Command) {
            return commandValidator((Command) elem);
        }
        throw new IllegalArgumentException("Oracle received data of different type than expected.");
    }
}