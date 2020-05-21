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
                    case IdResponse(uzResponse: UzResponse) => ZIO.succeed(uzResponse)
                    case PathResponse(path, request, contentType, status, headers) =>
                      UzResponse
                        .fromPath(path, request, contentType, status, headers)
                        .either
                        .map {
                          case Right(response) => Right(response)
                          case Left(error) =>
                            error match {
                              case e: HTTPError.NotFound => Left(e)
                              case e                     => Right(handleServerError(e))
                            }
                        }
                        .absolve
                    case ResourceResponse(name, request, contentType, status, headers) =>
                      UzResponse
                        .fromResource(
                          name = name,
                          request = request,
                          contentType = contentType,
                          status = status,
                          headers = headers
                        )
                        .either
                        .map {
                          case Right(response) => Right(response)
                          case Left(error) =>
                            error match {
                              case e: HTTPError.NotFound => Left(e)
                              case e                     => Right(handleServerError(e))
                            }
                        }
                        .absolve
                  }
                )

            case invalid: Invalid =>
              ZIO.succeed(handleClientErrors(invalid))
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

  /**
   * This method is called by ''endpoints'' when decoding a request failed.
   *
   * The provided implementation calls `clientErrorsResponse` to construct
   * a response containing the errors.
   *
   * This method can be overridden to customize the error reporting logic.
   */
  def handleClientErrors(invalid: Invalid): UzResponse =
    clientErrorsResponse(invalidToClientErrors(invalid)) match {
      case IdResponse(response) => response
      case _ =>
        UzResponse.const(Array.empty, InternalServerError)
    }

  /**
   * This method is called by ''endpoints'' when an exception is thrown during
   * request processing.
   *
   * The provided implementation calls [[serverErrorResponse]] to construct
   * a response containing the error message.
   *
   * This method can be overridden to customize the error reporting logic.
   */
  def handleServerError(throwable: Throwable): UzResponse =
    serverErrorResponse(throwableToServerError(throwable)) match {
      case IdResponse(response) =>
        response
      case _ =>
        UzResponse.const(Array.empty, InternalServerError)
    }
}
