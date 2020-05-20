package endpoints.uzhttp.server

import endpoints.algebra.Documentation
import endpoints.{ algebra, Valid }
import uzhttp.{ Request => UzRequest }
import zio.UIO

trait Assets extends algebra.Assets with Endpoints {

  case class AssetRequest(assetPath: AssetPath, request: UzRequest)

  case class AssetPath(path: Seq[String], name: String)

  type AssetResponse = HttpResponse

  def assetSegments(name: String, docs: Documentation): Path[AssetPath] =
    new Path[AssetPath] {
      def decode(segments: List[String]) =
        segments.reverse match {
          case name :: p =>
            Some((Valid(AssetPath(p.reverse, name)), Nil))
          case Nil => None
        }
    }

  override def assetsEndpoint(
    url: Url[AssetPath],
    docs: Documentation,
    notFoundDocs: Documentation
  ): Endpoint[AssetRequest, AssetResponse] = {
    val requestEntity = (uzRequest: UzRequest) => UIO.succeed(Valid(uzRequest))
    val req = request(Get, url, requestEntity).xmap({
      case (path, request) => AssetRequest(path, request)
    })(assetRequest => (assetRequest.assetPath, assetRequest.request))

    endpoint(req, identity[AssetResponse])
  }

  def assetResources(
    pathPrefix: Option[String] = None,
    cacheStrategy: CacheStrategy = CacheStrategy.Default
  ): AssetRequest => AssetResponse = assetRequest => {
    val assetInfo = assetRequest.assetPath
    val path =
      if (assetInfo.path.nonEmpty)
        assetInfo.path.mkString("", "/", s"/${assetInfo.name}")
      else assetInfo.name
    val resourcePath = pathPrefix.map(_ ++ s"/$path").getOrElse(path)

    val contentType = nameToContentType(assetRequest.assetPath.name)

    val headers =
      cacheStrategy match {
        case CacheStrategy.Default =>
          if (contentType.mediaType == MediaType.text.`text/html`)
            List(("Cache-Control", "no-cache"))
          else
            List(("Cache-Control", "public, max-age=31536000, immutable"))
        case CacheStrategy.Custom(decider) =>
          decider(contentType.mediaType)
      }

    ResourceResponse(
      resourcePath,
      assetRequest.request,
      contentType = contentType.toString,
      headers = headers
    )
  }

  private def nameToContentType(name: String): `Content-Type` =
    name.lastIndexOf('.') match {
      case -1 => `Content-Type`(`MediaType`.application.`application/octet-stream`)
      case i =>
        MediaType.extensionMap
          .get(name.substring(i + 1))
          .map(`Content-Type`(_))
          .getOrElse(`Content-Type`(MediaType.application.`application/octet-stream`))
    }
}

/**
 * Strategy that can be used for adding caching related response headers.
 */
sealed trait CacheStrategy
object CacheStrategy {

  /**
   * Cache all non html assets.
   */
  object Default extends CacheStrategy

  /**
   * Custom strategy that uses decider to determine which cache control headers will be used for given media type.
   */
  case class Custom(decider: MediaType => List[(String, String)]) extends CacheStrategy
}
