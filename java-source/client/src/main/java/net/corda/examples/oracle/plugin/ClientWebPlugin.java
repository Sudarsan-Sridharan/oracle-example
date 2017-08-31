package net.corda.examples.oracle.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.examples.oracle.api.ClientApi;
import net.corda.webserver.services.WebServerPluginRegistry;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

// CorDapp web plugin registry class.
// Here we are registering some static web content and a web API.
public class ClientWebPlugin implements WebServerPluginRegistry {
    @Override
    public List<Function<CordaRPCOps, ?>> getWebApis() {
        return ImmutableList.of(ClientApi::new);
    }

    @Override
    public Map<String, String> getStaticServeDirs() {
        return ImmutableMap.of("primes", getClass().getClassLoader().getResource("primesWeb").toExternalForm());
    }

    @Override
    public void customizeJSONSerialization(ObjectMapper om) {

    }
}
