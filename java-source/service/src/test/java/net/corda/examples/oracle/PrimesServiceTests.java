package net.corda.examples.oracle;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Command;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.transactions.WireTransaction;
import net.corda.examples.oracle.contract.Prime;
import net.corda.examples.oracle.service.Oracle;
import net.corda.testing.TestDependencyInjectionBase;
import net.corda.testing.node.MockServices;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static net.corda.testing.TestConstants.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;

public class PrimesServiceTests extends TestDependencyInjectionBase {
    private Oracle oracle;

    @Before
    public void setUp() {
        oracle = new Oracle(getCHARLIE(), new MockServices(getCHARLIE_KEY()));
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void successfulQuery() {
        int result = oracle.query(10000);
        assertEquals("104729", String.valueOf(result));
    }

    @Test
    public void badQueryParameter() {
        exception.expect(instanceOf(IllegalArgumentException.class));
        oracle.query(0);

        exception.expect(instanceOf(IllegalArgumentException.class));
        oracle.query(-1);
    }

    @Test
    public void successfulSign() throws Exception {
        Command command = new Command<>(
                new Prime.Create(10, 29),
                ImmutableList.of(getCHARLIE().getOwningKey()));
        Prime.State state = new Prime.State(10, 29, getALICE());
        WireTransaction wtx = new TransactionBuilder(getDUMMY_NOTARY())
                .withItems(state, command)
                .toWireTransaction();
        FilteredTransaction ftx = wtx.buildFilteredTransaction(this::filterOracleCmds);
        TransactionSignature signature = oracle.sign(ftx);
        assert (signature.verify(ftx.getId()));
    }

    @Test
    public void incorrectPrimeSpecified() {
        Command command = new Command<>(
                new Prime.Create(10, 1000),
                ImmutableList.of(getCHARLIE().getOwningKey()));
        Prime.State state = new Prime.State(10, 29, getALICE());
        WireTransaction wtx = new TransactionBuilder(getDUMMY_NOTARY()).withItems(state, command).toWireTransaction();
        FilteredTransaction ftx = wtx.buildFilteredTransaction(this::filterOracleCmds);

        exception.expect(instanceOf(IllegalArgumentException.class));
        oracle.sign(ftx);
    }

    /**
     * Returns true if the transaction object is a command relevant to the primes oracle.
     *
     * @param elem the transaction object to check.
     */
    private boolean filterOracleCmds(Object elem) {
        if (elem instanceof Command) {
            Command cmd = (Command) elem;
            return cmd.getSigners().contains(oracle.getIdentity().getOwningKey())
                    && cmd.getValue() instanceof Prime.Create;
        }
        return false;
    }
}