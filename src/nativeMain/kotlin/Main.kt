import kotlinx.cinterop.ExperimentalForeignApi
import parser.ArgumentsParser
import platform.posix.close
import platform.posix.perror

@OptIn(ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    if(args.isEmpty()){
        println(argsNote)
        return
    }
    val parseResult = ArgumentsParser().invoke(args.first())
    if (parseResult.host.isEmpty()) {
        perror("Invalid arguments")
        println(argsNote)
        return
    }
    WinSocketScope {
        val address = getIpAddress(parseResult.host)
        println("Connecting to: ${parseResult.host}")
        val sd = openConnection(address.first(), parseResult.port)
        val rs = sendRequest(sd, parseResult.requestMethod, parseResult.host, parseResult.path)
        if (rs == -1) {
            println("Failed request to: ${address.first().first},\nERROR: $rs ")
            close(sd.toInt())
            return@WinSocketScope
        }
        val response = StringBuilder()
        do {
            val d = receiveResponse(sd)
            d?.let { response.append(d.chunk) }
        } while (d != null)
        println(response)
        close(sd.toInt())
    }
}


val argsNote = """
            Please Provide Arguments
            http://[HOST]/[PATH]
            options:
                -X [Request Method] 
        """.trimIndent()