package parser

class ArgumentsParser {
    private val httpsRegex = Regex("^https://.*")
    private val httpRegex = Regex("^http://.*")
    operator fun invoke(args: String):ParseResult{
        val argList = args.split("\\s+".toRegex())
        return parseArguments(argList)
    }
    private fun parseArguments(args: List<String>):ParseResult{
        var host = ""
        var path = ""
        var port = 80
        var requestMethod = "GET"

        var i = 0
        while (i < args.size){
            when {
                //Parse options
                args[i].first() == '-' -> {
                    when{
                        args[i].getOrNull(1) == 'X' ->{
                            when{
                                args.getOrNull(++i) == "DELETE" ->{requestMethod = "DELETE"}
                                else -> {}
                            }
                        }
                        else -> {}
                    }
                }

                isUrlWithHttpsSchema(args[i]) -> {
                    //Will be handled later
                }
                isUrlWithHttpSchema(args[i]) -> {
                    //Let's keep it simple
                    //first strip away http:// part
                    val uriWithOutSchema = args[i].substring(7..args[i].lastIndex)
                    val (hp, pth) = uriWithOutSchema.split('/', limit = 2)
                    path =  "/".plus(pth)
                    host = hp.split(":")[0]
                    port = hp.split(":").getOrNull(1)?.toInt() ?: 80 //Default 80
                }
                else ->{}
            }
            i++
        }
        return ParseResult("http://",host, path, port, requestMethod)
    }

    private fun isUrlWithHttpsSchema(url: String): Boolean {
        return httpsRegex.matches(url)
    }
    private fun isUrlWithHttpSchema(url: String): Boolean {
        return httpRegex.matches(url)
    }

}

data class ParseResult(
    val schema:String,
    val host:String,
    val path:String,
    val port:Int,
    val requestMethod:String = "GET"
)