package endpoints4s.uzhttp.server

import java.net.{ InetSocketAddress, ServerSocket, URI }

import endpoints4s.algebra.server.{
  BasicAuthenticationTestSuite,
  DecodedUrl,
  EndpointsTestSuite,
  JsonEntitiesFromSchemasTestSuite
}
import endpoints4s.{ Invalid, Valid }
import uzhttp.server.Server
import zio.{ BootstrapRuntime, ZIO }

class ServerInterpreterTest
    extends EndpointsTestSuite[EndpointsTestApi]
    with BasicAuthenticationTestSuite[EndpointsTestApi]
    with JsonEntitiesFromSchemasTestSuite[EndpointsTestApi]
    with BootstrapRuntime {
  val serverApi = new EndpointsTestApi()

  def decodeUrl[A](url: serverApi.Url[A])(rawValue: String): DecodedUrl[A] =
    url.decodeUri(new URI(rawValue)) match {
      case None                  => DecodedUrl.NotMatched
      case Some(Invalid(errors)) => DecodedUrl.Malformed(errors)
      case Some(Valid(a))        => DecodedUrl.Matched(a)
    }

  /**
   * @param runTests A function that is called after the server is started and before it is stopped. It takes
   *                 the TCP port number as parameter.
   */
  override def serveEndpoint[Resp](endpoint: serverApi.Endpoint[_, Resp], response: => Resp)(
    runTests: Int => Unit
  ): Unit = {

    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }

    val server = Server
      .builder(new InetSocketAddress("127.0.0.1", port))
      .handleSome(
        endpoint.interpretPure(_ => response)
      )
      .serve
      .use(_ => ZIO(runTests(port)))

    unsafeRunSync(server)
  }
}
