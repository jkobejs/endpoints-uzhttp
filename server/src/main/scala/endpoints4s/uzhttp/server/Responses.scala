package endpoints4s.uzhttp.server

import java.nio.charset.StandardCharsets

import _root_.uzhttp.{ Response => UzResponse }
import endpoints4s._
import endpoints4s.algebra.Documentation
import zio.blocking.Blocking
import zio.{ RIO, Task }

trait Responses extends algebra.Responses with algebra.Errors with StatusCodes {

  /** An HTTP response (status, headers, and entity) carrying an information of type A
   *
   * @note This type has implicit methods provided by the [[InvariantFunctorSyntax]]
   *       and [[ResponseSyntax]] class
   * @group types
   */
  type Response[A] = A => RIO[Blocking, UzResponse]

  implicit lazy val responseInvariantFunctor: endpoints4s.InvariantFunctor[Response] =
    new endpoints4s.InvariantFunctor[Response] {
      def xmap[A, B](
        fa: Response[A],
        f: A => B,
        g: B => A
      ): Response[B] =
        fa compose g
    }

  /** An HTTP response entity carrying an information of type A
   * It is modeled as function that receives `A` and returns it value
   * serialized to byte array together with content type.
   *
   * @group types
   */
  type ResponseEntity[A] = A => (Array[Byte], String)

  implicit lazy val responseEntityInvariantFunctor: InvariantFunctor[ResponseEntity] =
    new endpoints4s.InvariantFunctor[ResponseEntity] {
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

  /**
   * Information carried by responses’ headers.
   *
   * You can construct values of type `ResponseHeaders` by using the operations [[responseHeader]],
   * [[optResponseHeader]], or [[emptyResponseHeaders]].
   *
   * @note This type has implicit methods provided by the [[SemigroupalSyntax]]
   *       and [[PartialInvariantFunctorSyntax]] classes.
   * @group types
   */
  type ResponseHeaders[A] = A => List[(String, String)]

  implicit lazy val responseHeadersSemigroupal: Semigroupal[ResponseHeaders] =
    new Semigroupal[ResponseHeaders] {
      def product[A, B](fa: ResponseHeaders[A], fb: ResponseHeaders[B])(implicit
        tupler: Tupler[A, B]
      ): ResponseHeaders[tupler.Out] =
        out => {
          val (a, b) = tupler.unapply(out)
          fa(a) ++ fb(b)
        }
    }

  implicit lazy val responseHeadersInvariantFunctor: endpoints4s.PartialInvariantFunctor[ResponseHeaders] =
    new endpoints4s.PartialInvariantFunctor[ResponseHeaders] {

      override def xmapPartial[A, B](
        fa: A => List[(String, String)],
        f: A => Validated[B],
        g: B => A
      ): B => List[(String, String)] =
        fa compose g
    }

  def emptyResponseHeaders: ResponseHeaders[Unit] =
    _ => Nil

  /**
   * Response headers containing a header with the given `name`.
   * Client interpreters should model the header value as `String`, or
   * fail if the response header is missing.
   * Server interpreters should produce such a response header.
   * Documentation interpreters should document this header.
   *
   * Example:
   *
   * {{{
   *   val versionedResource: Endpoint[Unit, (SomeResource, String)] =
   *     endpoint(
   *       get(path / "versioned-resource"),
   *       ok(
   *         jsonResponse[SomeResource],
   *         headers = responseHeader("ETag")
   *       )
   *     )
   * }}}
   *
   * @group operations
   */
  def responseHeader(
    name: String,
    docs: Documentation = None
  ): ResponseHeaders[String] =
    value => List((name, value))

  /**
   * Response headers optionally containing a header with the given `name`.
   * Client interpreters should model the header value as `Some[String]`, or
   * `None` if the response header is missing.
   * Server interpreters should produce such a response header.
   * Documentation interpreters should document this header.
   *
   * @group operations
   */
  def optResponseHeader(
    name: String,
    docs: Documentation = None
  ): ResponseHeaders[Option[String]] = {
    case Some(value) => responseHeader(name, docs)(value)
    case None        => emptyResponseHeaders(())
  }

  /**
   * Server interpreters construct a response with the given status and entity.
   * Client interpreters accept a response only if it has a corresponding status code.
   *
   * @param statusCode Response status code
   * @param entity     Response entity
   * @param docs       Response documentation
   * @param headers    Response headers
   * @group operations
   */
  override def response[A, B, R](
    statusCode: StatusCode,
    entity: ResponseEntity[A],
    docs: Documentation,
    headers: ResponseHeaders[B]
  )(implicit tupler: Tupler.Aux[A, B, R]): Response[R] =
    r => {
      val (a, b)              = tupler.unapply(r)
      val (body, contentType) = entity(a)
      Task(
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
