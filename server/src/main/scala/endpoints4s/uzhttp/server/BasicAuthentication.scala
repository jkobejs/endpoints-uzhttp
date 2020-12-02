package endpoints4s.uzhttp.server

import java.util.Base64

import endpoints4s.algebra.BasicAuthentication.Credentials
import endpoints4s.algebra.Documentation
import endpoints4s.{ algebra, Invalid, Tupler, Valid }
import zio.UIO

import scala.util.Try

trait BasicAuthentication extends Endpoints with algebra.BasicAuthentication {

  private[server] def basicAuthenticationHeader: RequestHeaders[Option[Credentials]] =
    headers =>
      Valid(
        headers
          .get("Authorization")
          .flatMap { authHeader =>
            authHeader
              .split(" ")
              .drop(1)
              .headOption
              .flatMap(encoded =>
                Try(Base64.getDecoder.decode(encoded.getBytes)).toOption
                  .flatMap(decoded =>
                    new String(decoded)
                      .split(":")
                      .toList match {
                      case username :: password :: Nil =>
                        Some(Credentials(username, password))
                      case _                           => None
                    }
                  )
              )
          }
      )

  override private[endpoints4s] def authenticatedRequest[U, E, H, UE, HCred, Out](
    method: Method,
    url: Url[U],
    entity: RequestEntity[E],
    headers: RequestHeaders[H],
    requestDocs: Documentation
  )(implicit
    tuplerUE: Tupler.Aux[U, E, UE],
    tuplerHCred: Tupler.Aux[H, algebra.BasicAuthentication.Credentials, HCred],
    tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Request[Out] =
    extractUrlAndHeaders(method, url, headers ++ basicAuthenticationHeader) {
      case (_, (_, None))              =>
        _ => UIO(Invalid("Credentials are missing"))
      case (u, (h, Some(credentials))) =>
        uzRequest =>
          entity(uzRequest)
            .map(
              _.map(e => tuplerUEHCred(tuplerUE(u, e), tuplerHCred(h, credentials)))
            )
    }
}
