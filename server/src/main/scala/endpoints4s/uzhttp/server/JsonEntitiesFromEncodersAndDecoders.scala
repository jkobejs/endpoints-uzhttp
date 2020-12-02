package endpoints4s.uzhttp.server

import java.nio.charset.StandardCharsets

import endpoints4s.{ algebra, Decoder, Encoder, Invalid }
import zio.UIO

trait JsonEntitiesFromEncodersAndDecoders extends algebra.JsonEntities with Endpoints {
  type JsonResponse[A] = Encoder[A, String]
  type JsonRequest[A]  = Decoder[String, A]

  def jsonRequest[A](implicit decoder: JsonRequest[A]): RequestEntity[A] =
    _.body match {
      case Some(body) =>
        body.runCollect.either.map {
          case Right(bytes) =>
            decoder.decode(new String(bytes.toArray, StandardCharsets.UTF_8))
          case Left(error)  => Invalid(error.getMessage)
        }
      case None       => UIO(Invalid("Body is missing"))
    }

  def jsonResponse[A](implicit encoder: JsonResponse[A]): ResponseEntity[A] =
    a => (encoder.encode(a).getBytes(StandardCharsets.UTF_8), "application/json")
}
