package endpoints4s.uzhttp.server

import endpoints4s.{ algebra, Invalid }

trait BuiltInErrors extends algebra.BuiltInErrors {
  this: EndpointsWithCustomErrors =>

  override def clientErrorsResponseEntity: ClientErrors => (Array[Byte], String) =
    error => (endpoints4s.ujson.codecs.invalidCodec.encode(error).getBytes(), "application/json")

  override def serverErrorResponseEntity: ServerError => (Array[Byte], String) =
    error => clientErrorsResponseEntity(Invalid(error.getMessage))
}
