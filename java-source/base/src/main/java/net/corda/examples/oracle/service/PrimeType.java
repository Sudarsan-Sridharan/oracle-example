package net.corda.examples.oracle.service;

import net.corda.core.node.services.ServiceType;

// Service type definition, required by both client and service so served within the base CorDapp.
// The service definition is required to pick out the Primes Oracle from the network map.
public class PrimeType {
    private static final ServiceType type = ServiceType.getServiceType("net.corda.examples", "primes_oracle");
    public static ServiceType getType() { return type; }
}
