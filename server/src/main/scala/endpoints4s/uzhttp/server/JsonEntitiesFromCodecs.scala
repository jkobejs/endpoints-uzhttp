package endpoints4s.uzhttp.server

import java.nio.charset.StandardCharsets

import endpoints4s.{ algebra, Invalid, Validated }
import zio.UIO

trait JsonEntitiesFromCodecs extends algebra.JsonEntitiesFromCodecs with Endpoints {

  /** Defines a `RequestEntity[A]` given an implicit `JsonRequest[A]`
   *
   * @group operations */
  override def jsonRequest[A: JsonCodec]: uzhttp.Request => UIO[Validated[A]] =
    _.body match {
      case Some(body) =>
        body.runCollect.either.map {
          case Right(bytes) =>
            stringCodec.decode(new String(bytes.toArray, StandardCharsets.UTF_8))
          case Left(error)  => Invalid(error.getMessage)
        }
      case None       => UIO(Invalid("Body is missing"))
    }

  /** Defines a `Response[A]` given an implicit `JsonResponse[A]`
   *
   * @group operations */
  override def jsonResponse[A: JsonCodec]: A => (Array[Byte], String) =
    a => (stringCodec.encode(a).getBytes(StandardCharsets.UTF_8), "application/json")
}
