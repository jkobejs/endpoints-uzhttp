package sample

import endpoints4s.uzhttp.server.{ BasicAuthentication, Endpoints, JsonEntitiesFromCodecs }
import sample.algebra.{ DocumentedApi, Item }
import uzhttp.HTTPError
import zio.blocking.Blocking
import zio.{ UIO, ZIO }

object Api extends Endpoints with JsonEntitiesFromCodecs with BasicAuthentication with DocumentedApi {
  //#implementation
  val handler: PartialFunction[uzhttp.Request, ZIO[Any with Blocking, HTTPError, uzhttp.Response]] =
    items.interpretPure {
      case (category, page) =>
        List(Item(s"first item from category $category and page $page"))
    } orElse
      item.interpret(id => UIO.some(Item(id.toString)))
  //#implementation
}
