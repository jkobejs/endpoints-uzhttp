package endpoints.uzhttp.server

import endpoints.algebra
import endpoints.algebra.Codec

trait JsonEntitiesFromSchemas
    extends algebra.JsonEntitiesFromSchemas
    with JsonEntitiesFromCodecs
    with endpoints.ujson.JsonSchemas {

  def stringCodec[A](implicit codec: JsonCodec[A]): Codec[String, A] =
    codec.stringCodec
}
