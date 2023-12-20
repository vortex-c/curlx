import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.addrinfo
import platform.windows.freeaddrinfo
import platform.windows.getaddrinfo


@OptIn(ExperimentalForeignApi::class)
class Network {
    fun getIpAddress(url: String): String? = memScoped {

        val wsaData = alloc<WSADATA>()
        val winSocket = WSAStartup(2u, wsaData.ptr)

        if (winSocket != 0) perror("winSocket: ERROR CODE: $winSocket")

        val hints = alloc<addrinfo>()
        hints.ai_family = AF_UNSPEC
        hints.ai_socktype = SOCK_STREAM
        val result = alloc<CPointerVar<addrinfo>>()
        val code = getaddrinfo(url, null, hints.ptr, result.ptr)
        if (code != 0) perror("getaddrinfo: ERROE CODE: $code")
        return try {
            val addr = result
                .value
                ?.pointed
                ?.ai_addr
                ?.reinterpret<sockaddr_in>()
                ?.pointed
                ?.sin_addr
                ?.readValue()
            if (addr == null) perror("in_addr: null")
            inet_ntoa(addr!!)?.toKString()
        } finally {
            freeaddrinfo(result.value)
            WSACleanup()
        }
    }
}