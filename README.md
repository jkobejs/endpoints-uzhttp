# Endpoints uzhttp 

[Endpoints](https://github.com/julienrf/endpoints) server interpreter backed by [uzhttp](https://github.com/polynote/uzhttp).

You Probably Shouldn't Use It Because You Probably Shouldn't Use uzhttp™.
## Server

~~~ scala
"io.github.jkobejs" %% "endpoints-uzhttp-server" % "x.x.x"
~~~

### `Endpoints`

The `Endpoints` interpreter interprets endpoint definitions into partial functions of type
```scala
PartialFunction[UzRequest, ZIO[R with Blocking, HTTPError, UzResponse]]
```
which can easily be chained used `.orElse` method. Chained methods can be used to handle incoming requests using `uzhttp.server.Server.handleSome` method.



```scala
package counter

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
```

### Error handling

When the server processes requests, three kinds of errors can happen: the incoming request doesn’t match
any endpoint, the request does match an endpoint but is invalid (e.g. one parameter has a wrong type), or
an exception is thrown.

#### The incoming request doesn’t match any endpoint

In that case, since interpreters return partial function of type 
```scala
PartialFunction[UzRequest, ZIO[R with Blocking, HTTPError, UzResponse]]
```
uzhttp server will return 404 error.

#### The incoming request is invalid

In that case, *endpoints* returns a “Bad Request” (400) response reporting all the errors in a
JSON array. You can change this behavior by overriding the
[handleClientErrors](https://jkobejs.github.io/endpoints-uzhttp/latest/api/endpoints/uzhttp/server/EndpointsWithCustomErrors.html) method.

#### An exception is thrown

If an exception is thrown during request decoding, or when running the business logic, or when
encoding the response, *endpoints* returns an “Internal Server Error” (500) response reporting
the error in a JSON array. You can change this behavior by overriding the
[handleServerError](https://jkobejs.github.io/endpoints-uzhttp/latest/api/endpoints/uzhttp/server/EndpointsWithCustomErrors.html) method.
