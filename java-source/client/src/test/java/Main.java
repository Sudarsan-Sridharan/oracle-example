import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.node.services.ServiceInfo;
import net.corda.node.services.config.VerifierType;
import net.corda.node.services.transactions.ValidatingNotaryService;
import net.corda.nodeapi.User;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;

import static java.util.Collections.*;
import static net.corda.core.utilities.X500NameUtils.getX509Name;
import static net.corda.testing.driver.Driver.driver;

/**
 * This file is exclusively for being able to run your nodes through an IDE (as opposed to running deployNodes)
 * Do not use in a production environment.
 *
 * To debug your CorDapp:
 *
 * 1. Run the "Run Template CorDapp" run configuration.
 * 2. Wait for all the nodes to start.
 * 3. Note the debug ports for each node, which should be output to the console. The "Debug CorDapp" configuration runs
 *    with port 5007, which should be "NodeA". In any case, double-check the console output to be sure.
 * 4. Set your breakpoints in your CorDapp code.
 * 5. Run the "Debug CorDapp" remote debug run configuration.
 */
public class Main {
    public static void main(String[] args) {
        // No permissions required as we are not invoking flows.
        final User user = new User("user1", "test", emptySet());
        driver(
                new DriverParameters().setIsDebug(true),
                dsl -> {
                    dsl.startNode(getX509Name("Controller", "London", "root@city.uk.example"),
                            ImmutableSet.of(new ServiceInfo(ValidatingNotaryService.Companion.getType(), null)),
                            emptyList(),
                            VerifierType.InMemory,
                            emptyMap(),
                            null);

                    try {
                        NodeHandle nodeA = dsl.startNode(getX509Name("NodeA", "Paris", "root@city.fr.example"), emptySet(), ImmutableList.of(user), VerifierType.InMemory, emptyMap(), null).get();
                        NodeHandle nodeB = dsl.startNode(getX509Name("NodeB", "Rome", "root@city.it.example"), emptySet(), ImmutableList.of(user), VerifierType.InMemory, emptyMap(), null).get();
                        NodeHandle nodeC = dsl.startNode(getX509Name("NodeC", "New York", "root@city.us.example"), emptySet(), ImmutableList.of(user), VerifierType.InMemory, emptyMap(), null).get();

                        dsl.startWebserver(nodeA);
                        dsl.startWebserver(nodeB);
                        dsl.startWebserver(nodeC);

                        dsl.waitForAllNodesToFinish();
                    } catch (Throwable e) {
                        System.err.println("Encountered exception in node startup: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                }
        );
    }
}