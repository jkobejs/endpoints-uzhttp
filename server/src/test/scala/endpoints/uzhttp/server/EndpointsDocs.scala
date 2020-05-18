package endpoints.uzhttp.server

import endpoints.algebra
import uzhttp.HTTPError
import zio.ZIO
import zio.blocking.Blocking

trait EndpointsDocs extends Endpoints with algebra.EndpointsDocs {
  //#implementation
  val handles: PartialFunction[uzhttp.Request, ZIO[Any with Blocking, HTTPError, uzhttp.Response]] =
    someResource.interpretPure(x => s"Received $x")
  //#implementation
}
