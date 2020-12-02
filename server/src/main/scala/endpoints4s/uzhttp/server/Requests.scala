package endpoints4s.uzhttp.server

import java.nio.charset.StandardCharsets

import _root_.uzhttp.{ Request => UzRequest }
import endpoints4s._
import endpoints4s.algebra.Documentation
import zio.UIO

trait Requests extends endpoints4s.algebra.Requests with Methods with Urls {

  /**
   * An attempt to extract an `A` from a request headers.
   *
   * Models failure by returning a `Left(result)`. That makes it possible
   * to early return an HTTP response if a header is wrong (e.g. if
   * an authentication information is missing)
   */
  type RequestHeaders[A] = Map[String, String] => Validated[A]

  /** Always succeeds in extracting no information from the headers */
  def emptyRequestHeaders: RequestHeaders[Unit] = _ => Valid(())

  def requestHeader(
    name: String,
    docs: Documentation = None
  ): RequestHeaders[String] =
    headers =>
      headers.get(name) match {
        case Some(value) => Valid(value)
        case None        => Invalid(s"Missing header $name")
      }

  def optRequestHeader(
    name: String,
    docs: Documentation = None
  ): RequestHeaders[Option[String]] =
    headers => Valid(headers.get(name))

  implicit lazy val requestHeadersSemigroupal: Semigroupal[RequestHeaders] =
    new Semigroupal[RequestHeaders] {
      def product[A, B](fa: RequestHeaders[A], fb: RequestHeaders[B])(implicit
        tupler: Tupler[A, B]
      ): RequestHeaders[tupler.Out] =
        headers =>
          fa(headers)
            .flatMap(a => fb(headers).map(b => tupler(a, b)))
    }

  implicit lazy val requestHeadersPartialInvariantFunctor: PartialInvariantFunctor[RequestHeaders] =
    new PartialInvariantFunctor[RequestHeaders] {
      def xmapPartial[From, To](
        f: RequestHeaders[From],
        map: From => Validated[To],
        contramap: To => From
      ): RequestHeaders[To] =
        headers => f(headers).flatMap(map)
    }

  /**
   * An HTTP request.
   *
   * It receives uzhttp.Request and returns:
   * - `None` if request url cannot be matched
   * - Some(Valid(a)) if `A` can be extracted from request
   * - Invalid if `A` cannot be extracted from request
   *
   * Has an instance of `InvariantFunctor`.
   */
  type Request[A] = UzRequest => Option[UIO[Validated[A]]]

  implicit def requestPartialInvariantFunctor: PartialInvariantFunctor[Request] =
    new PartialInvariantFunctor[Request] {
      def xmapPartial[A, B](
        fa: Request[A],
        f: A => Validated[B],
        g: B => A
      ): Request[B] =
        uzRequest => fa(uzRequest).map(_.map(_.flatMap(f)))
    }

  /**
   * Information carried by request entity.
   * It is modeled as `UIO[Validate[A]]` because it is result of collecting data from stream chunk.
   * If any error happens during collecting data or collected data cannot be transformed to wanted information
   * we store error in `Invalid` data structure which is later transformed to `Bad Request` response.
   */
  type RequestEntity[A] = UzRequest => UIO[Validated[A]]

  override implicit def requestEntityPartialInvariantFunctor: PartialInvariantFunctor[RequestEntity] =
    new PartialInvariantFunctor[RequestEntity] {
      def xmapPartial[From, To](
        f: RequestEntity[From],
        map: From => Validated[To],
        contramap: To => From
      ): RequestEntity[To] =
        body => f(body).map(_.flatMap(map))
    }

  def emptyRequest: RequestEntity[Unit] = _ => UIO(Valid(()))

  def textRequest: RequestEntity[String] =
    req =>
      req.body match {
        case Some(streamChunk) =>
          streamChunk.runCollect.either.map {
            case Right(bytes) =>
              Valid(new String(bytes.toArray, StandardCharsets.UTF_8))
            case Left(error)  => Invalid(error.getMessage)
          }
        case None              => UIO(Invalid("Request body is missing"))
      }

  def request[UrlP, BodyP, HeadersP, UrlAndBodyPTupled, Out](
    method: Method,
    url: Url[UrlP],
    entity: RequestEntity[BodyP] = emptyRequest,
    docs: Documentation = None,
    headers: RequestHeaders[HeadersP] = emptyRequestHeaders
  )(implicit
    tuplerUB: Tupler.Aux[UrlP, BodyP, UrlAndBodyPTupled],
    tuplerUBH: Tupler.Aux[UrlAndBodyPTupled, HeadersP, Out]
  ): Request[Out] =
    extractUrlAndHeaders(method, url, headers) {
      case (u, h) =>
        uzRequest =>
          entity(uzRequest).map(
            _.map(body => tuplerUBH(tuplerUB(u, body), h))
          )
    }

  private[server] def extractUrlAndHeaders[U, H, E](
    method: Method,
    url: Url[U],
    headers: RequestHeaders[H]
  )(
    entity: ((U, H)) => RequestEntity[E]
  ): Request[E] =
    uzRequest =>
      if (uzRequest.method == method)
        url
          .decodeUri(uzRequest.uri)
          .map(_.zip(headers(uzRequest.headers)))
          .map {
            case Valid(urlAndHeaders) => entity(urlAndHeaders)(uzRequest)
            case inv: Invalid         => UIO(inv)
          }
      else None

}
