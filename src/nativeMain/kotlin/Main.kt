import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun main() {
    WinSocketScope {
        println("DNS Probing")
        val address = getIpAddress("www.google.com")
        println("HOST IP ADDRESS: $address")
    }
}

