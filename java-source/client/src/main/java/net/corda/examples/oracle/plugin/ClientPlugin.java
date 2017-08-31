package net.corda.examples.oracle.plugin;

import net.corda.core.node.CordaPluginRegistry;
import net.corda.core.serialization.SerializationCustomization;

import java.math.BigInteger;

// CorDapp plugin registry class.
// We have to whitelist BigInteger as it's not on the default serialisation whitelist.
public class ClientPlugin extends CordaPluginRegistry {
    @Override public boolean customizeSerialization(SerializationCustomization custom) {
        custom.addToWhitelist(BigInteger.class);
        return true;
    }
}