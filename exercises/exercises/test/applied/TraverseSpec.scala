package applied

import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

import cats.effect.IO
import cats.effect.Timer
import cats.data._
import cats.implicits._
import fs2.async.refOf

import applied.traverse._

class TraverseSpec extends FlatSpec with Matchers {

  private[this] val iae = new IllegalArgumentException

  "`allOrNothing`" should "work on an empty list" in {
    allOrNothing(List.empty[Either[Throwable, Int]]) should be (Right(Nil))
  }

  it should "work on non-empty lists" in {
    val iae = new IllegalArgumentException
    allOrNothing {
      List(Right(1), Right(2), Left(iae))
    } should be (Left(iae))

    allOrNothing {
      List(Right(1), Left(iae), Right(3))
    } should be (Left(iae))

    allOrNothing {
      List(Right(1), Right(2), Right(3))
    } should be (Right(List(1, 2, 3)))
  }

  "`allOrErrors`" should "work on an empty list" in {
    allOrErrors(List.empty[Either[Throwable, Int]]) should be (Validated.valid(Nil))
  }

  it should "work on non-empty lists" in {
    allOrErrors {
      List(Right(1), Right(2), Left(iae))
    } should be (Validated.invalid(NonEmptyList(iae, Nil)))

    allOrErrors {
      List(Right(1), Left(iae), Left(iae), Right(4))
    } should be (Validated.invalid(NonEmptyList(iae, iae :: Nil)))

    allOrErrors {
      List(Right(1), Right(2), Right(3))
    } should be (Validated.valid(List(1, 2, 3)))
  }


  "`requestsInPar`" should "work on an empty list" in {
    requestsInPar(List.empty[IO[Response]]).unsafeRunSync() should be (Validated.valid(Nil))
  }

  it should "work on non-empty lists" in {
    val xs1: List[IO[Response]] = List(
      IO(Response(200, "1")),
      IO(Response(200, "2")),
      IO.raiseError(iae),
    )

    requestsInPar(xs1).unsafeRunSync() should be (Validated.invalid(NonEmptyList(iae, Nil)))

    val xs2: List[IO[Response]] = List(
      IO(Response(200, "1")),
      IO(Response(200, "2")),
      IO.raiseError(iae),
      IO.raiseError(iae),
    )

    requestsInPar(xs2).unsafeRunSync() should be (Validated.invalid(NonEmptyList(iae, iae :: Nil)))

    val xs3: List[IO[Response]] = List(
      IO(Response(200, "1")),
      IO(Response(200, "2")),
      IO(Response(200, "3")),
    )

    requestsInPar(xs3).unsafeRunSync() should be (Validated.valid(xs3.map(_.unsafeRunSync())))
  }

  it should "work in parallel" in {
    val ref = refOf[IO, List[Int]](List.empty).unsafeRunSync()

    val xs3: List[IO[Response]] = List(
      Timer[IO].sleep(50.millis) *> IO(Response(200, "1")) <* ref.modify(_ :+ 1),
      Timer[IO].sleep(80.millis) *> IO(Response(200, "2")) <* ref.modify(_ :+ 2),
      Timer[IO].sleep(10.millis) *> IO(Response(200, "3")) <* ref.modify(_ :+ 3),
    )

    requestsInPar(xs3).unsafeRunSync() should be (Validated.valid(List(
      Response(200, "1"),
      Response(200, "2"),
      Response(200, "3"),
    )))

    ref.get.unsafeRunSync() should be (List(3, 1, 2))
  }

}
