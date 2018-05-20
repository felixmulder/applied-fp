package applied

import cats.Applicative
import cats.implicits._
import cats.effect.{IO, Effect}
import io.circe.Decoder
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.server.blaze.BlazeBuilder

import fs2.{StreamApp, Stream}
import scala.concurrent.ExecutionContext


object service extends StreamApp[IO] {

  import ExecutionContext.Implicits.global

  implicit def jsonEntityDecoder[F[_] : Effect, A : Decoder]: EntityDecoder[F, A] =
    jsonOf[F, A]

  final val endpoints = HttpService[IO] {
    // OBS! Do the others first!
    //
    // 3) - Create an endpoint that accepts a JSON list [1, 2, 3, 4]
    //    - Make requests to `https://jsonplaceholder.typicode.com/posts/{num}` in parallel
    //    - Parse result as JSON
    //    - Using 'https://github.com/http4s/jawn-fs2', stream back the result in an `Ok`
    case _ => Ok("hello, world!")
  }

  object CheckCorrelationId {
    private val KlarnaCorrelationId = "Klarna-Correlation-Id".ci

    // 1) Implement a middleware that checks for the existance of the header:
    //    `Klarna-Correlation-Id`
    def apply[F[_]: Applicative](service: HttpService[F]): HttpService[F] =
      ???
  }

  final override def stream(args: List[String], onShutdown: IO[Unit]) =
    BlazeBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      // 2) Wrap `endpoints` in the CheckCorrelationId middleware
      .mountService(endpoints, "/")
      .serve

}
