import kotlinx.cinterop.*
import platform.posix.*
import platform.posix.AF_INET
import platform.posix.SOCK_STREAM
import platform.posix.WSACleanup
import platform.posix.WSAStartup
import platform.posix.socket
import platform.posix.connect
import platform.posix.send
import platform.posix.recv
import platform.windows.*


@OptIn(ExperimentalForeignApi::class)
inline fun WinSocketScope(crossinline call: MemScope.() -> Unit) = memScoped {
    try {
        val wsaData = alloc<WSADATA>()
        val winSocket = WSAStartup(2u, wsaData.ptr)
        if (winSocket != 0) {perror("winSocket: ERROR CODE: $winSocket")}
        else{call()}
    } finally {
        WSACleanup()
    }
}

/*
* @returns: list of pair (first is ip address, second is address type)
* */
@OptIn(ExperimentalForeignApi::class)
fun MemScope.getIpAddress(url: String): List<Pair<String, UShort>> {
    val hints = alloc<addrinfo>()
    hints.ai_family = AF_INET
    hints.ai_socktype = SOCK_STREAM
    val result = alloc<CPointerVar<addrinfo>>()
    val code = getaddrinfo(url, null, hints.ptr, result.ptr)
    if (code != 0) perror("getaddrinfo: ERROE CODE: $code")
    return try {
        generateSequence(result.value) { it.pointed.ai_next }
            .mapNotNull { it.pointed.ai_addr }
            .map {
                val socketAddr = when (it.pointed.sa_family) {
                    AF_INET.toUShort() -> {
                        it.reinterpret<sockaddr_in>().pointed.sin_addr
                    }

                    AF_INET6.toUShort() -> {
                        it.reinterpret<sockaddr_in6>().pointed.sin6_addr
                    }

                    else -> null
                }
                val buffer = ByteArray(255)
                buffer.usePinned { b ->
                    inet_ntop(
                        result.pointed!!.ai_family,
                        socketAddr!!.ptr,
                        b.addressOf(0),
                        b.get().size.toULong() - 1u
                    )?.toKString() to it.pointed.sa_family
                }
            }.filter { (ip, _) -> ip != null }.map {
                it.first!! to it.second
            }.toList()
    } finally {
        freeaddrinfo(result.value)
    }
}

@OptIn(ExperimentalForeignApi::class)
fun MemScope.openConnection(address: Pair<String, UShort>, port:Int): ULong {
    val sockAddr: sockaddr? = when (address.second) {
        AF_INET.toUShort() -> {
            val sd = alloc<sockaddr_in>()
            sd.sin_family = AF_INET.toShort()
            sd.sin_port = htons(port.toUShort())
            inet_pton(AF_INET, address.first, sd.sin_addr.ptr)
            sd.reinterpret<sockaddr>()
        }

        AF_INET6.toUShort() -> {
            val sd = alloc<sockaddr_in6>()
            sd.sin6_family = AF_INET.toShort()
            sd.sin6_port = htons(port.toUShort())
            inet_pton(AF_INET, address.first, sd.sin6_addr.ptr)
            sd.reinterpret<sockaddr>()
        }

        else -> null
    }
    if (sockAddr == null) perror("ERROR: Provide valid address family")
    val socketFd: SOCKET = socket(address.second.toInt(), SOCK_STREAM, 0)
    if (socketFd.toInt() == 0) perror("ERROR: Socket File Descriptor: $socketFd")
    val cr = connect(socketFd, sockAddr!!.readValue(), sizeOf<sockaddr>().toInt())
    if (cr != 0) println("Failed connecting to: ${address.first},\nERROR: $cr ")
    return socketFd
}

@OptIn(ExperimentalForeignApi::class)
fun sendRequest(socketFd: ULong, requestMethod:String ,host:String, endPoint: String): Int {
    val request = "$requestMethod $endPoint HTTP/1.1\r\n" +
            "Host: $host\r\n" +
            "Accept: */*\r\n" +
            "Connection: close\r\n\r\n"
    println("Sending Request: $request")
    return send(socketFd, request.cstr, request.cstr.size, 0)
}

@OptIn(ExperimentalForeignApi::class)
fun receiveResponse(socketFd: ULong):Data?{
    val rb = ByteArray(16)
    return rb.usePinned {
        val ml = recv(socketFd, it.addressOf(0), rb.size, 0)
        if (ml != 0){
            val validData = rb.take(ml).toByteArray()
            Data(validData.decodeToString(), ml)
        } else null
    }
}

data class Data(
    val chunk:String,
    val size:Int
)