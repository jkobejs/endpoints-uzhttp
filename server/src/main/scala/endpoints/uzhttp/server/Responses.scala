package endpoints.uzhttp.server

import java.nio.charset.StandardCharsets
import java.nio.file.Path

import _root_.uzhttp.{ Status, Request => UzRequest, Response => UzResponse }
import endpoints._
import endpoints.algebra.Documentation

trait Responses extends algebra.Responses with algebra.Errors with StatusCodes {

  sealed trait HttpResponse
  case class PathResponse(
    path: Path,
    request: UzRequest,
    contentType: String,
    status: StatusCode,
    headers: List[(String, String)]
  ) extends HttpResponse
  case class ResourceResponse(
    name: String,
    request: UzRequest,
    contentType: String,
    status: Status = Status.Ok,
    headers: List[(String, String)] = Nil
  ) extends HttpResponse

  case class ConstResponse(response: UzResponse) extends HttpResponse

  type Response[A] = A => HttpResponse

  implicit lazy val responseInvariantFunctor: endpoints.InvariantFunctor[Response] =
    new endpoints.InvariantFunctor[Response] {
      def xmap[A, B](
        fa: Response[A],
        f: A => B,
        g: B => A
      ): Response[B] =
        fa compose g
    }

  // Return content lenght with type
  case class Entity(
    data: Array[Byte],
    contentType: Option[String],
    contentLength: Option[Long]
  )

  type ResponseEntity[A] = A => (Array[Byte], String)

  implicit lazy val responseEntityInvariantFunctor: InvariantFunctor[ResponseEntity] =
    new endpoints.InvariantFunctor[ResponseEntity] {
      def xmap[A, B](
        fa: ResponseEntity[A],
        f: A => B,
        g: B => A
      ): ResponseEntity[B] =
        gv => fa(g(gv))
    }

  def emptyResponse: ResponseEntity[Unit] =
    _ => (Array.empty, "")

  def textResponse: ResponseEntity[String] =
    string => {
      (string.getBytes(StandardCharsets.UTF_8), s"text/plain; charset=${StandardCharsets.UTF_8.name()}")
    }

  type ResponseHeaders[A] = A => List[(String, String)]

  implicit lazy val responseHeadersSemigroupal: Semigroupal[ResponseHeaders] =
    new Semigroupal[ResponseHeaders] {
      def product[A, B](fa: ResponseHeaders[A], fb: ResponseHeaders[B])(
        implicit tupler: Tupler[A, B]
      ): ResponseHeaders[tupler.Out] =
        out => {
          val (a, b) = tupler.unapply(out)
          fa(a) ++ fb(b)
        }
    }

  implicit lazy val responseHeadersInvariantFunctor: endpoints.PartialInvariantFunctor[ResponseHeaders] =
    new endpoints.PartialInvariantFunctor[ResponseHeaders] {

      override def xmapPartial[A, B](
        fa: A => List[(String, String)],
        f: A => Validated[B],
        g: B => A
      ): B => List[(String, String)] =
        fa compose g
    }

  def emptyResponseHeaders: ResponseHeaders[Unit] =
    _ => Nil

  def responseHeader(
    name: String,
    docs: Documentation = None
  ): ResponseHeaders[String] =
    value => List((name, value))

  def optResponseHeader(
    name: String,
    docs: Documentation = None
  ): ResponseHeaders[Option[String]] = {
    case Some(value) => responseHeader(name, docs)(value)
    case None        => emptyResponseHeaders(())
  }

  override def response[A, B, R](
    statusCode: StatusCode,
    entity: ResponseEntity[A],
    docs: Documentation,
    headers: ResponseHeaders[B]
  )(implicit tupler: Tupler.Aux[A, B, R]): Response[R] =
    r => {
      val (a, b)              = tupler.unapply(r)
      val (body, contentType) = entity(a)
      ConstResponse(
        UzResponse.const(
          body = body,
          status = statusCode,
          headers = headers(b),
          contentType = contentType
        )
      )
    }

  override def choiceResponse[A, B](
    responseA: Response[A],
    responseB: Response[B]
  ): Response[Either[A, B]] = {
    case Left(a)  => responseA(a)
    case Right(b) => responseB(b)
  }
}
