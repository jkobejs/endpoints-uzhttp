package endpoints4s.uzhttp.server

import java.net.{ URI, URLDecoder, URLEncoder }
import java.nio.charset.StandardCharsets.UTF_8

import endpoints4s._
import endpoints4s.algebra.Documentation

import scala.collection.compat._
import scala.collection.mutable

trait Urls extends algebra.Urls {
  type Params = Map[String, Seq[String]]

  override type QueryString[A] = Params => Validated[A]

  trait QueryStringParam[A] {
    def decode(name: String, params: Params): Validated[A]
  }

  val utf8Name = UTF_8.name()

  /** Concatenates two `QueryString`s */
  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit
    tupler: Tupler[A, B]
  ): QueryString[tupler.Out] =
    map => first(map).zip(second(map))

  override def qs[A](name: String, docs: Documentation)(implicit value: QueryStringParam[A]): Params => Validated[A] =
    params =>
      value
        .decode(name, params)
        .mapErrors(_.map(error => s"$error for query parameter '$name'"))

  implicit def optionalQueryStringParam[A](implicit
    param: QueryStringParam[A]
  ): QueryStringParam[Option[A]] =
    (name, params) =>
      params.get(name) match {
        case None    => Valid(None)
        case Some(_) => param.decode(name, params).map(Some(_))
      }

  implicit def repeatedQueryStringParam[A, CC[X] <: Iterable[X]](implicit
    param: QueryStringParam[A],
    factory: Factory[A, CC[A]]
  ): QueryStringParam[CC[A]] =
    (name: String, qs: Map[String, Seq[String]]) =>
      qs.get(name) match {
        case None     => Valid(factory.newBuilder.result())
        case Some(vs) =>
          // ''traverse'' the list of decoded values
          vs.foldLeft[Validated[mutable.Builder[A, CC[A]]]](
              Valid(factory.newBuilder)
            ) {
              case (inv: Invalid, v) =>
                // Pretend that this was the query string and delegate to the `A` query string param
                param
                  .decode(name, Map(name -> (v :: Nil)))
                  .fold(_ => inv, errors => Invalid(inv.errors ++ errors))
              case (Valid(b), v)     =>
                // Pretend that this was the query string and delegate to the `A` query string param
                param.decode(name, Map(name -> (v :: Nil))).map(b += _)
            }
            .map(_.result())
      }

  implicit lazy val queryStringParamPartialInvariantFunctor: PartialInvariantFunctor[QueryStringParam] =
    new PartialInvariantFunctor[QueryStringParam] {
      def xmapPartial[A, B](
        fa: QueryStringParam[A],
        f: A => Validated[B],
        g: B => A
      ): QueryStringParam[B] =
        (str, params) => fa.decode(str, params).flatMap(f)
    }

  implicit def stringQueryString: QueryStringParam[String] =
    (name, params) => {
      val maybeValue = params.get(name).flatMap(_.headOption)
      Validated.fromOption(maybeValue)("Missing value")
    }

  trait Segment[A] {
    def decode(rawSegment: String): Validated[A]
  }

  implicit lazy val segmentPartialInvariantFunctor: PartialInvariantFunctor[Segment] =
    new PartialInvariantFunctor[Segment] {
      def xmapPartial[A, B](
        fa: Segment[A],
        f: A => Validated[B],
        g: B => A
      ): Segment[B] =
        s => fa.decode(s).flatMap(f)
    }

  implicit val stringSegment: Segment[String] = Valid(_)

  def segment[A](name: String = "", docs: Documentation = None)(implicit
    A: Segment[A]
  ): Path[A] =
    new Path[A] {
      def decode(segments: List[String]): Option[(Validated[A], List[String])] =
        segments match {
          case head :: tail =>
            val validatedA =
              A.decode(head)
                .mapErrors(
                  _.map(error => s"$error for segment${if (name.isEmpty) "" else s" '$name'"}")
                )
            Some((validatedA, tail))
          case Nil          => None
        }
    }

  trait Path[A] extends Url[A] {
    def decode(paths: List[String]): Option[(Validated[A], List[String])]

    final def decodeUri(uri: URI): Option[Validated[A]] =
      pathExtractor(this, uri)
  }

  private def pathExtractor[A](
    path: Path[A],
    uri: URI
  ): Option[Validated[A]] = {
    val segments =
      uri.getRawPath
        .split("/")
        .map(URLDecoder.decode(_, utf8Name))
        .toList

    path.decode(if (segments.isEmpty) List("") else segments).flatMap {
      case (validated, Nil) => Some(validated)
      case (_, _)           => None
    }
  }

  def staticPathSegment(segment: String): Path[Unit] =
    new Path[Unit] {
      def decode(paths: List[String]): Option[(Validated[Unit], List[String])] =
        paths match {
          case s :: ss if s == segment => Some((Valid(()), ss))
          case _                       => None
        }
    }

  implicit def pathPartialInvariantFunctor: PartialInvariantFunctor[Path] =
    new PartialInvariantFunctor[Path] {
      def xmapPartial[A, B](
        fa: Path[A],
        f: A => Validated[B],
        g: B => A
      ): Path[B] =
        new Path[B] {

          def decode(
            paths: List[String]
          ): Option[(Validated[B], List[String])] =
            fa.decode(paths).map {
              case (validA, rs) => (validA.flatMap(f), rs)
            }
        }
    }

  def remainingSegments(
    name: String = "",
    docs: Documentation = None
  ): Path[String] =
    new Path[String] {
      def decode(
        segments: List[String]
      ): Option[(Validated[String], List[String])] =
        if (segments.isEmpty) None
        else
          Some(
            (
              Valid(segments.map(URLEncoder.encode(_, utf8Name)).mkString("/")),
              Nil
            )
          )
    }

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit
    tupler: Tupler[A, B]
  ): Path[tupler.Out] =
    new Path[tupler.Out] {
      def decode(
        segments: List[String]
      ): Option[(Validated[tupler.Out], List[String])] =
        first.decode(segments).flatMap {
          case (validA, segments2) =>
            second.decode(segments2).map {
              case (validB, segments3) =>
                (validA zip validB, segments3)
            }
        }
    }

  trait Url[A] {
    def decodeUri(uri: URI): Option[Validated[A]]
  }

  implicit def urlPartialInvariantFunctor: PartialInvariantFunctor[Url] =
    new PartialInvariantFunctor[Url] {
      def xmapPartial[A, B](
        fa: Url[A],
        f: A => Validated[B],
        g: B => A
      ): Url[B] =
        new Url[B] {
          def decodeUri(uri: URI): Option[Validated[B]] =
            fa.decodeUri(uri).map(_.flatMap(f))
        }
    }

  /** Builds an URL from the given path and query string */
  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit
    tupler: Tupler[A, B]
  ): Url[tupler.Out] =
    new Url[tupler.Out] {
      def decodeUri(uri: URI): Option[Validated[tupler.Out]] = {
        // TODO: Improve
        val params =
          if (uri.getRawQuery == null) Map.empty[String, List[String]]
          else
            uri.getRawQuery
              .split("&")
              .toList
              .map(_.split("=").toList)
              .collect {
                case key :: value :: Nil =>
                  (key, URLDecoder.decode(value, utf8Name))
                case key :: Nil          =>
                  (key, "")
              }
              .groupBy(_._1)
              .map(pair => (pair._1, pair._2.map(_._2)))

        pathExtractor(path, uri).map(_.zip(qs(params)))
      }
    }

  implicit def queryStringPartialInvariantFunctor: PartialInvariantFunctor[QueryString] =
    new PartialInvariantFunctor[QueryString] {
      def xmapPartial[A, B](
        fa: Params => Validated[A],
        f: A => Validated[B],
        g: B => A
      ): Params => Validated[B] =
        params => fa(params).flatMap(f)
    }
}
