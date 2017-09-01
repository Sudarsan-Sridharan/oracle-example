package net.corda.examples.oracle.api;

import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowProgressHandle;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import net.corda.examples.oracle.contract.Prime;
import net.corda.examples.oracle.flow.CreatePrime;
import org.bouncycastle.asn1.x500.X500Name;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Path("primes")
public class ClientApi {
    private final CordaRPCOps services;
    private final X500Name myLegalName;

    public ClientApi(CordaRPCOps services) {
        this.services = services;
        this.myLegalName = services.nodeIdentity().getLegalIdentity().getName();
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, X500Name> whoami() {
        return ImmutableMap.of("me", myLegalName);
    }

    /**
     * Returns all parties registered with the [NetworkMapService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<X500Name>> getPeers() {
        List<NodeInfo> nodeInfoSnapshot = services.networkMapSnapshot();
        List<X500Name> peers = nodeInfoSnapshot
                .stream()
                .map(node -> node.getLegalIdentity().getName())
                .collect(toList());
        return ImmutableMap.of("peers", peers);
    }

    /**
     * Enumerates all the prime numbers we currently have in the vault.
     */
    @GET
    @Path("primes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<Prime.State>> primes() {
        return services.vaultQuery(Prime.State.class).getStates();
    }

    /**
     * Creates a new prime number by consulting the primes Oracle.
     */
    @GET
    @Path("create-prime")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPrime(@QueryParam(value = "n") int n) {
        Response.Status status;
        String msg;
        try {
            // Start the CreatePrime flow. We block and wait for the flow to return.
            FlowProgressHandle<SignedTransaction> flowHandle = services
                    .startTrackedFlowDynamic(CreatePrime.class, n);
            final SignedTransaction result = flowHandle.getReturnValue().get();
            final Prime.State state = (Prime.State) result.getTx().getOutputs().get(0).getData();

            // Return the response.
            status = Response.Status.CREATED;
            msg = state.toString();
        } catch (Throwable ex) {
            // For the purposes of this demo app, we do not differentiate by exception type.
            status = Response.Status.BAD_REQUEST;
            msg = ex.getMessage();
        }

        return Response.status(status).entity(msg).build();

    }
}