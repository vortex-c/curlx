import parser.ArgumentsParser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ArgumentParserTest {

    lateinit var parser: ArgumentsParser
    @BeforeTest
    fun setup(){
        parser = ArgumentsParser()
    }
    @Test
    fun urlWithHttpSchema_HostPortPath_ParsedCorrectly(){
        val args = "http://eu.httpbin.org:90/get"
        val pareResult = parser(args)
        assertEquals(pareResult.host, "eu.httpbin.org")
        assertEquals(pareResult.path, "/get")
        assertEquals(pareResult.port, 90)
    }

    @Test
    fun urlWithHttpSchema_DefaultPort80ForHttp_ParsedCorrectly(){
        val args = "http://eu.httpbin.org/get"
        val pareResult = parser(args)
        assertEquals(pareResult.port, 80)
    }

    @Test
    fun defaultRequestMethodIs_Get(){
        val args = "http://eu.httpbin.org/get"
        val pareResult = parser(args)
        assertEquals(pareResult.requestMethod, "GET")
    }

    @Test
    fun setDeleteRequestMethod(){
        val args = "-X DELETE http://eu.httpbin.org/get"
        val pareResult = parser(args)
        assertEquals(pareResult.requestMethod, "DELETE")
    }

}