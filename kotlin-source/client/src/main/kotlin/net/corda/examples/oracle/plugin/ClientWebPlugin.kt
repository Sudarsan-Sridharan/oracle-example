package net.corda.examples.oracle.plugin

import net.corda.examples.oracle.api.ClientApi
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

// CorDapp web plugin registry class.
// Here we are registering some static web content and a web API.
class ClientWebPlugin : WebServerPluginRegistry {
    override val webApis = listOf(Function(::ClientApi))

    override val staticServeDirs: Map<String, String> = mapOf(
            "primes" to javaClass.classLoader.getResource("primesWeb").toExternalForm()
    )
}
