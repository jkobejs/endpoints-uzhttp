package counter

//#domain-model
// Our domain model just contains a counter value
case class Counter(value: Int)

// The operations that we can apply to our counter
sealed trait Operation
object Operation {
  case class Set(value: Int) extends Operation
  case class Add(delta: Int) extends Operation
}
//#domain-model

//#endpoint-description
import endpoints.{ algebra, generic }

trait CounterEndpoints
    extends algebra.EndpointsWithCustomErrors
    with algebra.JsonEntitiesFromSchemas
    with algebra.BasicAuthentication
    with generic.JsonSchemas {

  // HTTP endpoint for querying the current value of the counter. Uses the HTTP
  // verb ''GET'' and the path ''/counter''. Returns the current value of the counter
  // in a JSON object. (see below for the `counterJson` definition)
  val currentValue =
    authenticatedEndpoint(Get, (path / "counter"), counterJsonResponse)

  // HTTP endpoint for updating the value of the counter. Uses the HTTP verb ''POST''
  // and the path ''/counter''. The request entity contains an `Operation` object encoded
  // in JSON. The endpoint returns the current value of the counter in a JSON object.
  val update = authenticatedEndpoint(Post, path / "counter", counterJsonResponse, jsonRequest[Operation])

  // Since both the `currentValue` and `update` endpoints return the same
  // information, we define it once and just reuse it. Here, we say
  // that they return an HTTP response whose entity contains a JSON document
  // with the counter value
  lazy val counterJsonResponse =
    ok(jsonResponse[Counter], docs = Some("The counter current value"))

  // We generically derive a data type schema. This schema
  // describes that the case class `Counter` has one field
  // of type `Int` named “value”
  implicit lazy val jsonSchemaCounter: JsonSchema[Counter] = genericJsonSchema
  // Again, we generically derive a schema for the `Operation`
  // data type. This schema describes that `Operation` can be
  // either `Set` or `Add`, and that `Set` has one `Int` field
  // name `value`, and `Add` has one `Int` field named `delta`
  implicit lazy val jsonSchemaOperation: JsonSchema[Operation] =
    genericJsonSchema
}
//#endpoint-description

//#endpoint-documentation
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
//#endpoint-documentation

//#endpoint-implementation
import java.util.concurrent.atomic.AtomicInteger
import endpoints.uzhttp.server._
import zio.Task

object CounterServer extends CounterEndpoints with Endpoints with BasicAuthentication with JsonEntitiesFromSchemas {
  parent =>

  private val value = new AtomicInteger(0)

  val username = "username"
  val password = "password"

  val handlers = (
    currentValue.interpret { credentials =>
      if (credentials.username == username && credentials.password == password)
        Task.some(Counter(value.get()))
      else
        Task.none
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
//#endpoint-implementation

//#documentation-implementation
object DocumentationServer extends Endpoints with JsonEntitiesFromEncodersAndDecoders with Assets {

  // HTTP endpoint serving documentation. Uses the HTTP verb ''GET'' and the path
  // ''/documentation.json''. Returns an OpenAPI document.
  val documentation = endpoint[Unit, OpenApi](
    get(path / "documentation.json"),
    ok(jsonResponse[OpenApi])
  )

  // We “render” the OpenAPI document using the swagger-ui, provided as static assets
  val assets = assetsEndpoint(path / "assets" / assetSegments())

  val handlers = documentation.interpretPure(_ => CounterDocumentation.api) orElse
    assets.interpretPure(assetResources(pathPrefix = Some("public")))

  override def digests: Map[String, String] = Map.empty
}
//#documentation-implementation

//#main
import java.net.InetSocketAddress
import uzhttp.server.Server
import zio.{ App, ZIO }

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
//#main
