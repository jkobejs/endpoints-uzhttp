package counter

//#endpoint-definition
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

import endpoints.uzhttp.server._
import uzhttp.server.Server
import zio.{ App, ZIO }

case class Counter(value: Int)

sealed trait Operation
object Operation {
  case class Set(value: Int) extends Operation
  case class Add(delta: Int) extends Operation
}

import endpoints.{ algebra, generic }

trait CounterEndpoints
    extends algebra.EndpointsWithCustomErrors
    with algebra.JsonEntitiesFromSchemas
    with algebra.BasicAuthentication
    with generic.JsonSchemas {

  val currentValue =
    authenticatedEndpoint(Get, (path / "counter"), counterJsonResponse)

  val update = authenticatedEndpoint(Post, path / "counter", counterJsonResponse, jsonRequest[Operation])

  lazy val counterJsonResponse =
    ok(jsonResponse[Counter], docs = Some("The counter current value"))

  implicit lazy val jsonSchemaCounter: JsonSchema[Counter] = genericJsonSchema
  implicit lazy val jsonSchemaOperation: JsonSchema[Operation] =
    genericJsonSchema
}

import endpoints.openapi
import endpoints.openapi.model.{ Info, OpenApi }

object CounterDocumentation
    extends CounterEndpoints
    with openapi.Endpoints
    with openapi.BasicAuthentication
    with openapi.JsonEntitiesFromSchemas {

  val api: OpenApi =
    openApi(
      Info(title = "API to manipulate a counter", version = "1.0.0")
    )(currentValue, update)
}

object CounterServer extends CounterEndpoints with Endpoints with BasicAuthentication with JsonEntitiesFromSchemas {
  parent =>

  private val value = new AtomicInteger(0)

  val username = "username"
  val password = "password"

  val handlers = (
    currentValue.interpret { credentials =>
      ZIO
        .environment[zio.console.Console]
        .flatMap(console =>
          if (credentials.username == username && credentials.password == password)
            ZIO(Some(Counter(value.get())))
          else
            console.get.putStr(s"Invalid credentials $credentials").map(_ => None)
        )
    } orElse
      update.interpretPure {
        case (Operation.Set(newValue), credentials)
            if (credentials.username == username && credentials.password == password) =>
          value.set(newValue)
          Some(Counter(newValue))
        case (Operation.Add(delta), credentials)
            if (credentials.username == username && credentials.password == password) =>
          val newValue = value.addAndGet(delta)
          Some(Counter(newValue))
        case _ => None
      }
  )
}

object DocumentationServer extends Endpoints with JsonEntitiesFromEncodersAndDecoders with Assets {

  val documentation = endpoint[Unit, OpenApi](
    get(path / "documentation.json"),
    ok(jsonResponse[OpenApi])
  )

  val assets = assetsEndpoint(path / "assets" / assetSegments())

  val handlers = documentation.interpretPure(_ => CounterDocumentation.api) orElse
    assets.interpretPure(assetResources(pathPrefix = Some("public")))

  override def digests: Map[String, String] = Map.empty
}

object Main extends App {
  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    Server
      .builder(new InetSocketAddress("127.0.0.1", 8080))
      .handleSome(
        CounterServer.handlers orElse DocumentationServer.handlers
      )
      .serve
      .useForever
      .orDie
}

//#endpoint-definition
