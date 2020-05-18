package endpoints.uzhttp.server

import _root_.uzhttp.{ HTTPError, Request => UzRequest, Response => UzResponse }
import endpoints.{ algebra, Invalid, Valid }
import zio.blocking.Blocking
import zio.{ Task, ZIO }

trait Endpoints extends algebra.Endpoints with EndpointsWithCustomErrors with BuiltInErrors {}

trait EndpointsWithCustomErrors extends algebra.EndpointsWithCustomErrors with Requests with Responses {
  case class Endpoint[A, B](request: Request[A], response: Response[B]) {
    def interpret[R](
      implementation: A => ZIO[R with Blocking, Throwable, B]
    ): PartialFunction[UzRequest, ZIO[R with Blocking, HTTPError, UzResponse]] = {
      val handler: UzRequest => Option[ZIO[R with Blocking, HTTPError, UzResponse]] =
        (uzRequest: UzRequest) => {
          request(uzRequest).map(_.flatMap {
            case Valid(a) =>
              implementation(a)
                .mapError(throwable => HTTPError.InternalServerError(throwable.getMessage, Some(throwable)))
                .flatMap(b =>
                  response(b) match {
                    case ConstResponse(uzResponse: UzResponse) => ZIO.succeed(uzResponse)
                    case PathResponse(path, request, contentType, status, headers) =>
                      UzResponse
                        .fromPath(path, request, contentType, status, headers)
                        .mapError {
                          case httpError: HTTPError => httpError
                          case throwable =>
                            HTTPError.InternalServerError(throwable.getMessage, Some(throwable))
                        }
                    case ResourceResponse(name, request, contentType, status, headers) =>
                      UzResponse
                        .fromResource(
                          name = name,
                          request = request,
                          contentType = contentType,
                          status = status,
                          headers = headers
                        )
                        .mapError {
                          case httpError: HTTPError => httpError
                          case throwable =>
                            HTTPError.InternalServerError(throwable.getMessage, Some(throwable))
                        }
                  }
                )

            case invalid: Invalid =>
              ZIO.fail(HTTPError.BadRequest(invalid.errors.mkString(", ")))
          })
        }

      Function.unlift(handler)
    }

    def interpretPure(
      implementation: A => B
    ): PartialFunction[UzRequest, ZIO[Any with Blocking, HTTPError, UzResponse]] =
      interpret(a => Task(implementation(a)))
  }

  def endpoint[A, B](request: Request[A], response: Response[B], docs: EndpointDocs): Endpoint[A, B] =
    Endpoint(request, response)
}
