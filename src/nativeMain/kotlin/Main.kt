import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun main() {
    WinSocketScope {
        println("DNS Probing")
        val address = getIpAddress("eu.httpbin.org")
        println("Connecting to: ${address.first().first}")
        val cr = openConnection(address.first())
        if (cr != 0) println("Failed connecting to: ${address.first().first},\nERROR: $cr ")
    }
}

