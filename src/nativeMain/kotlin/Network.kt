import kotlinx.cinterop.*
import platform.posix.AF_INET
import platform.posix.AF_INET6
import platform.posix.SOCK_STREAM
import platform.posix.WSACleanup
import platform.posix.WSADATA
import platform.posix.WSAStartup
import platform.posix.perror
import platform.posix.sockaddr_in
import platform.windows.*


@OptIn(ExperimentalForeignApi::class)
inline fun WinSocketScope(crossinline call: MemScope.() -> Unit) = memScoped {
    try {
        val wsaData = alloc<WSADATA>()
        val winSocket = WSAStartup(2u, wsaData.ptr)
        if (winSocket != 0) perror("winSocket: ERROR CODE: $winSocket")
        call()
    } finally {
        WSACleanup()
    }
}

@OptIn(ExperimentalForeignApi::class)
fun MemScope.getIpAddress(url: String): List<String> {
    val hints = alloc<addrinfo>()
    hints.ai_family = AF_INET
    hints.ai_socktype = SOCK_STREAM
    val result = alloc<CPointerVar<addrinfo>>()
    val code = getaddrinfo(url, null, hints.ptr, result.ptr)
    if (code != 0) perror("getaddrinfo: ERROE CODE: $code")
    return try {
        generateSequence(result.value) { it.pointed.ai_next }
            .mapNotNull { it.pointed.ai_addr }
            .mapNotNull {
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
                    )?.toKString()
                }
            }.toList()
    } finally {
        freeaddrinfo(result.value)
    }
}