package endpoints.uzhttp.server

import endpoints.{ algebra, Invalid }

trait BuiltInErrors extends algebra.BuiltInErrors {
  this: EndpointsWithCustomErrors =>

  override def clientErrorsResponseEntity: ClientErrors => (Array[Byte], String) =
    error => (endpoints.ujson.codecs.invalidCodec.encode(error).getBytes(), "application/json")

  override def serverErrorResponseEntity: ServerError => (Array[Byte], String) =
    error => clientErrorsResponseEntity(Invalid(error.getMessage))
}
