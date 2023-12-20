fun main() {
    println("DNS Probing")
    val network = Network()
    val addresses = network.getIpAddress("www.google.com")
    println("RESOLVED HOST IP: $addresses")
}