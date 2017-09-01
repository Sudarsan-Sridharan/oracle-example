package net.corda.examples.oracle.service

import net.corda.core.node.services.ServiceType

// Service type definition. Required by both the client and the service so placed in the base CorDapp.
// The service definition is used to select the Primes Oracle from the network map.
object PrimeType {
    val type = ServiceType.getServiceType("net.corda.examples", "primes_oracle")
}
