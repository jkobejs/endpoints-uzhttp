package endpoints.uzhttp.server

import _root_.uzhttp.{ HTTPError, Request => UzRequest, Response => UzResponse }
import endpoints.{ algebra, Invalid, Valid }
import zio.blocking.Blocking
import zio.{ RIO, Task, ZIO }

trait Endpoints extends algebra.Endpoints with EndpointsWithCustomErrors with BuiltInErrors {}

trait EndpointsWithCustomErrors extends algebra.EndpointsWithCustomErrors with Requests with Responses {
  case class Endpoint[A, B](request: Request[A], response: Response[B]) {

    /**
     * Interprets endpoint into partial function that receives [[uzhttp.Request]] and returns
     * effect that contains [[uzhttp.Response]], can fail with [[uzhttp.HTTPError]] and requires
     * custom environment `R` together with [[Blocking]].
     *
     * Effect cannot require only `R` because for interpreting asset endpoints we need to make calls
     * to file system which requires blocking thread pool.
     *
     * @param implementation endpoint implementation
     * @tparam R environment that returning effect requires
     * @return partial function that models request, response flow
     */
    def interpret[R](
      implementation: A => RIO[R, B]
    ): PartialFunction[UzRequest, ZIO[R with Blocking, HTTPError, UzResponse]] = {
      val handler: UzRequest => Option[ZIO[R with Blocking, HTTPError, UzResponse]] =
        (uzRequest: UzRequest) => {
          request(uzRequest).map(_.flatMap {
            case Valid(a) =>
              implementation(a).either.flatMap {
                case Right(b) =>
                  response(b).mapError {
                    case error: HTTPError => error
                    case throwable        => HTTPError.InternalServerError(throwable.getMessage, Some(throwable))
                  }
                case Left(throwable) =>
                  handleServerError(throwable).mapError {
                    case error: HTTPError => error
                    case throwable        => HTTPError.InternalServerError(throwable.getMessage, Some(throwable))
                  }
              }
            case invalid: Invalid =>
              handleClientErrors(invalid).mapError {
                case error: HTTPError => error
                case throwable        => HTTPError.InternalServerError(throwable.getMessage, Some(throwable))
              }
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
  def handleClientErrors(invalid: Invalid): RIO[Blocking, UzResponse] =
    clientErrorsResponse(invalidToClientErrors(invalid))

  /**
   * This method is called by ''endpoints'' when an exception is thrown during
   * request processing.
   *
   * The provided implementation calls [[serverErrorResponse]] to construct
   * a response containing the error message.
   *
   * This method can be overridden to customize the error reporting logic.
   */
  def handleServerError(throwable: Throwable): RIO[Blocking, UzResponse] =
    serverErrorResponse(throwableToServerError(throwable))
}
