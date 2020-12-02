package endpoints4s.uzhttp.server

import java.nio.file.Paths

import endpoints4s.algebra.Documentation
import endpoints4s.{ algebra, Valid }
import uzhttp.{ Request => UzRequest, Response => UzResponse }
import zio.blocking.Blocking
import zio.{ RIO, UIO }

trait Assets extends algebra.Assets with Endpoints {

  case class AssetRequest(assetPath: AssetPath, request: UzRequest)

  case class AssetPath(path: Seq[String], name: String)

  type AssetResponse = RIO[Blocking, UzResponse]

  def assetSegments(name: String, docs: Documentation): Path[AssetPath] =
    new Path[AssetPath] {
      def decode(segments: List[String]) =
        segments.reverse match {
          case name :: p =>
            Some((Valid(AssetPath(p.reverse, name)), Nil))
          case Nil       => None
        }
    }

  override def assetsEndpoint(
    url: Url[AssetPath],
    docs: Documentation,
    notFoundDocs: Documentation
  ): Endpoint[AssetRequest, AssetResponse] = {
    val requestEntity = (uzRequest: UzRequest) => UIO.succeed(Valid(uzRequest))
    val req           = request(Get, url, requestEntity).xmap({
      case (path, request) => AssetRequest(path, request)
    })(assetRequest => (assetRequest.assetPath, assetRequest.request))

    endpoint(req, identity[AssetResponse])
  }

  /**
   * @param pathPrefix Prefix to use to look up the resources in the classpath. You
   *                   most probably never want to publish all your classpath resources.
   * @param cacheStrategy Strategy to use for constructing cache related response headers
   * @param serveFromFilesystem Flag that determines if assets will be read from filesystem or classpath resources
   * @return A function that, given an [[AssetRequest]], builds an [[AssetResponse]] by
   *         looking for the requested asset in the classpath resources or anywhere on filesystem.
   */
  def assetResources(
    pathPrefix: Option[String] = None,
    cacheStrategy: CacheStrategy = CacheStrategy.Default,
    serveFromFilesystem: Boolean = false
  ): AssetRequest => AssetResponse =
    assetRequest => {
      val assetInfo    = assetRequest.assetPath
      val path         =
        if (assetInfo.path.nonEmpty)
          assetInfo.path.mkString("", "/", s"/${assetInfo.name}")
        else assetInfo.name
      val resourcePath = pathPrefix.map(_ ++ s"/$path").getOrElse(path)

      val contentType = nameToContentType(assetRequest.assetPath.name)

      val headers =
        cacheStrategy match {
          case CacheStrategy.Default         =>
            if (contentType.mediaType == MediaType.text.`text/html`)
              List(("Cache-Control", "no-cache"))
            else
              List(("Cache-Control", "public, max-age=31536000, immutable"))
          case CacheStrategy.Custom(decider) =>
            decider(contentType.mediaType)
        }

      if (serveFromFilesystem)
        UzResponse
          .fromPath(
            Paths.get(resourcePath),
            assetRequest.request,
            contentType = contentType.toString,
            headers = headers
          )
      else
        UzResponse
          .fromResource(
            resourcePath,
            assetRequest.request,
            contentType = contentType.toString,
            headers = headers
          )
    }

  private def nameToContentType(name: String): `Content-Type` =
    name.lastIndexOf('.') match {
      case -1 => `Content-Type`(`MediaType`.application.`application/octet-stream`)
      case i  =>
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
