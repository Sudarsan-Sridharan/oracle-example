//package net.corda.examples.oracle;
//
//import com.google.common.collect.ImmutableList;
//import net.corda.core.contracts.Command;
//import net.corda.core.crypto.SecureHash;
//import net.corda.core.crypto.TransactionSignature;
//import net.corda.core.transactions.FilteredTransaction;
//import net.corda.core.transactions.TransactionBuilder;
//import net.corda.core.transactions.WireTransaction;
//import net.corda.examples.oracle.contract.Prime;
//import net.corda.examples.oracle.service.Oracle;
//import net.corda.node.services.schema.NodeSchemaService;
//import net.corda.node.utilities.CordaPersistence;
//import net.corda.testing.node.MockServices;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.rules.ExpectedException;
//
//import java.math.BigInteger;
//
//import static net.corda.node.utilities.CordaPersistenceKt.configureDatabase;
//import static net.corda.testing.TestConstants.*;
//import static org.hamcrest.CoreMatchers.instanceOf;
//import static org.junit.Assert.assertEquals;
//
//class PrimesServiceTests {
//    private final MockServices dummyServices = new MockServices(getCHARLIE_KEY());
//    private Oracle oracle;
//    private CordaPersistence database;
//
//    @Before
//    public void setUp() {
//        // Mock components for testing the Oracle.
//        database = configureDatabase(
//                MockServices.makeTestDataSourceProperties(SecureHash.randomSHA256().toString()),
//                MockServices.makeTestDatabaseProperties(null, null),
//                new NodeSchemaService(),
//                MockServices.makeTestIdentityService());
//        database.transaction(tx -> oracle = new Oracle(getCHARLIE(), dummyServices));
//    }
//
//    @After
//    public void tearDown() {
//        database.close()
//    }
//
//    @Rule
//    public final ExpectedException exception = ExpectedException.none();
//
//    @Test
//    public void successfulQuery() {
//        database.transaction(tx -> {
//            BigInteger result = oracle.query(10000);
//            assertEquals("104729", result.toString());
//            return null;
//        });
//    }
//
//    @Test
//    public void badQueryParameter() {
//        database.transaction(tx -> {
//            exception.expectCause(instanceOf(IllegalArgumentException.class));
//            oracle.query(0);
//
//            exception.expectCause(instanceOf(IllegalArgumentException.class));
//            oracle.query(-1);
//
//            return null;
//        });
//    }
//
//    @Test
//    public void successfulSign() throws Exception {
//        database.transaction(tx -> {
//            Command command = new Command<>(
//                    new Prime.Create(10, BigInteger.valueOf(29)),
//                    ImmutableList.of(getCHARLIE().getOwningKey()));
//            Prime.State state = new Prime.State(10, BigInteger.valueOf(29), getALICE());
//            WireTransaction wtx = new TransactionBuilder(getDUMMY_NOTARY())
//                    .withItems(state, command)
//                    .toWireTransaction()
//            FilteredTransaction ftx = wtx.buildFilteredTransaction(elem -> {
//                if (elem instanceof Command) {
//                    Command cmd = (Command) elem;
//                    return cmd.getSigners().contains(oracle.getIdentity().getOwningKey())
//                            && cmd.getValue() instanceof Prime.Create;
//                }
//                return false;
//            });
//            TransactionSignature signature = oracle.sign(ftx);
//            assert (signature.verify(ftx.getRootHash()));
//
//            return null;
//        });
//    }
//
//    @Test
//    public void incorrectPrimeSpecified() {
//        database.transaction(tx -> {
//            Command command = new Command<>(
//                    new Prime.Create(10, BigInteger.valueOf(1000)),
//                    ImmutableList.of(getCHARLIE().getOwningKey()));
//            Prime.State state = new Prime.State(10, BigInteger.valueOf(29), getALICE());
//            WireTransaction wtx = new TransactionBuilder(getDUMMY_NOTARY()).withItems(state, command).toWireTransaction();
//            FilteredTransaction ftx = wtx.buildFilteredTransaction(elem -> {
//                if (elem instanceof Command) {
//                    Command cmd = (Command) elem;
//                    return cmd.getSigners().contains(oracle.getIdentity().getOwningKey())
//                            && cmd.getValue() instanceof Prime.Create;
//                }
//                return false;
//            });
//
//            exception.expectCause(instanceOf(IllegalArgumentException.class));
//            oracle.sign(ftx);
//
//            return null;
//        });
//    }
//}