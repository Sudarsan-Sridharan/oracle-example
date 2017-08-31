package net.corda.examples.oracle.plugin

import net.corda.core.node.CordaPluginRegistry
import net.corda.core.serialization.SerializationCustomization
import net.corda.examples.oracle.api.ClientApi
import java.math.BigInteger
import java.util.function.Function

// CorDapp plugin registry class.
// We have to whitelist BigInteger as it's not on the default serialisation whitelist.
class ClientPlugin : CordaPluginRegistry() {
    override fun customizeSerialization(custom: SerializationCustomization): Boolean {
        custom.addToWhitelist(BigInteger::class.java)
        return true
    }
}
