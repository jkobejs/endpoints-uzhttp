package endpoints4s.uzhttp.server

import java.nio.charset.Charset

case class `Content-Type`(
  mediaType: MediaType,
  charset: Option[Charset] = None
) {
  override def toString: String =
    s"${mediaType.mainType}/${mediaType.subType}" + charset.map(ch => s";${ch.displayName()}").getOrElse("")
}

case class MediaType(
  mainType: String,
  subType: String,
  extensions: List[String] = Nil
)

/**
 * Important MIME types for Web developers
 *
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types
 */
object MediaType {
  object application {
    val `application/octet-stream` = MediaType("application", "octet-stream")

    val all = List(`application/octet-stream`)
  }
  object audio       {
    val `audio/wave` = MediaType("audio", "wave", List("wav"))
    val `audio/webm` = MediaType("audio", "webm")
    val `audio/ogg`  = MediaType("audio", "ogg")

    val all = List(`audio/wave`, `audio/webm`, `audio/ogg`)
  }
  object font        {
    val `font/collection` = MediaType("font", "collection")
    val `font/otf`        = MediaType("font", "otf")
    val `font/sfnt`       = MediaType("font", "sfnt")
    val `font/ttf`        = MediaType("font", "ttf")
    val `font/woff`       = MediaType("font", "woff")
    val `font/woff2`      = MediaType("font", "woff2")

    val all = List(`font/collection`, `font/otf`, `font/sfnt`, `font/ttf`, `font/woff`, `font/woff2`)
  }
  object image       {
    val `image/apng`    = MediaType("image", "apng")
    val `image/bmp`     = MediaType("image", "bmp")
    val `image/gif`     = MediaType("image", "gif")
    val `image/x-icon`  = MediaType("image", "x-icon", List("ico", "cur"))
    val `image/jpeg`    = MediaType("image", "jpeg", List("jpg", "jpeg", "jfif", "pjpeg", "pjp"))
    val `image/png`     = MediaType("image", "png")
    val `image/svg+xml` = MediaType("image", "svg+xml", List("svg"))
    val `image/tiff`    = MediaType("image", "tiff", List("tif", "tiff"))
    val `image/webp`    = MediaType("image", "webp", List("webp"))

    val all = List(
      `image/apng`,
      `image/bmp`,
      `image/gif`,
      `image/x-icon`,
      `image/jpeg`,
      `image/png`,
      `image/svg+xml`,
      `image/tiff`,
      `image/webp`
    )
  }
  object text        {
    val `text/plain`      = MediaType("text", "plain")
    val `text/css`        = MediaType("text", "css")
    val `text/html`       = MediaType("text", "html")
    val `text/javascript` = MediaType("text", "javascript", List("js"))

    val all = List(`text/plain`, `text/css`, `text/html`, `text/javascript`)
  }
  object video       {
    val `video/webm` = MediaType("video", "webm")
    val `video/ogg`  = MediaType("video", "ogg")

    val all = List(`video/webm`, `video/ogg`)
  }

  val all = application.all ++ audio.all ++ font.all ++ image.all ++ text.all ++ video.all

  val extensionMap: Map[String, MediaType] = all.flatMap { mediaType =>
    if (mediaType.extensions.isEmpty) List((mediaType.subType, mediaType)) else mediaType.extensions.map((_, mediaType))
  }.toMap
}
