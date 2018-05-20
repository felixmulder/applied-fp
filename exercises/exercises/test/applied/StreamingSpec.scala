package applied

import org.scalatest._
import scala.concurrent.duration.DurationInt

import cats.effect.{IO, Timer}
import cats.implicits._

import fs2.async.{signalOf, refOf, Ref}
import fs2.async.Ref.Change
import fs2.{Scheduler, Stream}

class StreamingSpec extends FlatSpec with Matchers {
  import scala.concurrent.ExecutionContext.Implicits.global
  import streaming._

  "A stream of a single element" should "only contain one element" in {
    singleElement(1).toList shouldEqual List(1)
  }

  "A stream of multiple elements" should "contain the specified elements" in {
    multipleElements(1, 2, 3).toList shouldEqual List(1, 2, 3)
  }

  "A stream from list" should "contain the correct elements" in {
    streamFromList(List(1, 2)).toList shouldEqual List(1, 2)
  }

  "A stream from lists" should "contain the correct elements in the correct order" in {
    streamFromLists(List(1, 2), List(3, 4), List(5, 6, 7)).toList shouldEqual List.range(1, 8)
  }

  "A concatenated stream" should "be correctly ordered" in {
    val s1 = Stream(1, 2)
    val s2 = Stream(3, 4)

    concatenate(s1, s2).toList shouldEqual List(1, 2, 3, 4)
  }

  "A stream of repeated values" should "contain as many values as taken out" in {
    repeated(1).take(10).toList shouldEqual List.fill(10)(1)
  }

  it should "only contain one value" in {
    val xs = repeated(1).take(12).toList
    assert(
      xs.forall(_ == 1),
      s"- expected only values of 1, but got list of: $xs")
  }

  "An even stream" should "contain only even numbers" in {
    even {
      Stream.emits(List.range(1, 11))
    }
      .toList shouldEqual List(2, 4, 6, 8, 10)
  }

  "A sum of a stream" should "give back a single result" in {
    sum {
      Stream.emits(List.range(1, 11))
    }
      .toList.length shouldEqual 1
  }

  it should "return the correct sum" in {
    sum {
      Stream.emits(List.range(1, 11))
    }
      .toList shouldEqual List(List.range(1, 11).foldLeft(0)(_ + _))
  }

  "Two interleaved streams" should "have the correct length" in {
    val s1 = Stream(1, 3, 5)
    val s2 = Stream(2, 4, 6)

    interleave(s1, s2).toList.length shouldEqual List.range(1, 7).length
  }

  it should "contain the correct elements" in {
    val s1 = Stream(1, 3, 5)
    val s2 = Stream(2, 4, 6)

    interleave(s1, s2).toList shouldEqual List.range(1, 7)
  }

  "A sum of two streams" should "have the correct length" in {
    val s1 = Stream(1, 3, 5)
    val s2 = Stream(2, 4, 6)

    sum(s1, s2).toList.length should be (1)
  }

  it should "have the correct value" in {
    val s1 = Stream(1, 3, 5)
    val s2 = Stream(2, 4, 6)

    sum(s1, s2).toList shouldEqual List(List.range(1, 7).sum)
  }

  "`periodically`" should "emit every 10 ms" in {
    val counter = refOf[IO, Int](0).unsafeRunSync()
    val process = (for {
      sig <- Stream.eval(signalOf[IO, Boolean](false))
      sch <- Scheduler[IO](1)
    } yield {
      implicit val S = sch

      val shutterDowner: Stream[IO, Unit] =
        Stream.eval(Timer[IO].sleep(500.millis) *> sig.set(true))

      val runner =
        periodically(5.millis)
          .evalMap(_ => counter.modify(_ + 1))
          .interruptWhen(sig)
          .drain

      shutterDowner.drain.concurrently(runner)
    }).flatten

    process.compile.drain.unsafeRunSync()
    val count = counter.get.unsafeRunSync()
    assert(
      count > 50 && count < 150,
      s"expected counter to be 50-150, but was: $counter"
    )
  }

  // Fails `n - 1` times and then succeeds
  def succeedAt(p: Int => Boolean, ref: Ref[IO, Int]): IO[Int] =
    ref.modify(_ + 1).flatMap { c =>
      if (p(c.now)) IO.pure(c.now)
      else IO.raiseError(new Exception("not yet ready"))
    }

  "`retryOp`" should "be able to fail 4 times and then succeed" in {
    val timeLimit: IO[Unit] =
      Timer[IO].sleep(57.millis) >>
      IO.raiseError(new Exception("took too long to complete"))

    val retry = for {
      r    <- Stream.eval(refOf[IO, Int](0))
           // fail 4 times, then succeed
      task =  succeedAt(_ >= 5, r)
      s    <- Scheduler[IO](1)
           // retry 6 times with 10 millis between each retry
      c    <- retryOp(task, tries = 6, backOff = 10.millis)(s, global)
    } yield {
      // the fifth time, the op should succeed, and the counter should thus be 5:
      assert(c == 5, s"- the amount of times failed before succeeding was not correct")
    }

    IO.race(timeLimit, retry.compile.drain).unsafeRunSync()
  }

  it should "return the latest error when not succeeding at all" in {
    val timeLimit: IO[Unit] =
      Timer[IO].sleep(57.millis) >>
      IO.raiseError(new Exception("took too long to complete"))

    val retry = for {
      r    <- Stream.eval(refOf[IO, Int](0))
           // succeed at first try after retries should have stopped
      task =  succeedAt(_ >= 7, r)
      s    <- Scheduler[IO](1)
      res  <- retryOp(task, tries = 6, backOff = 10.millis)(s, global).attempt
    } yield assert(res.isLeft)

    IO.race(timeLimit, retry.compile.drain).unsafeRunSync()
  }

  "`periodicallyRetry`" should "evaluate the task twice within the period" in {
    val timeLimit: IO[Unit] =
      Timer[IO].sleep(257.millis) >>
      IO.raiseError(new Exception("took too long to complete"))

    val periodicRetry: Stream[IO, Int] = for {
      r    <- Stream.eval(refOf[IO, Int](0))
      task =  succeedAt(n => n == 5 || n == 10, r)
      s    <- Scheduler[IO](1)
      res  <- periodicallyRetry(task, tries = 6, backOff = 10.millis, period = 100.millis)(s, global)
    } yield res

    val testPeriodicRetry =
      periodicRetry.take(2).compile.toList.map { xs =>
        assert(xs == List(5, 10))
      }


    IO.race(timeLimit, testPeriodicRetry).unsafeRunSync()
  }

  it should "interrupt the second period if it takes too long" in {
    val sig = signalOf[IO, Boolean](false).unsafeRunSync()
    val shutterDowner: Stream[IO, Unit] =
      Stream.eval(Timer[IO].sleep(250.millis) >> sig.set(true))

    val periodicRetry: Stream[IO, Either[Throwable, Int]] = (for {
      r    <- Stream.eval(refOf[IO, Int](0))
      task =  succeedAt(n => n == 1, r)
      s    <- Scheduler[IO](1)
      res  <- periodicallyRetry(task, tries = 3, backOff = 50.millis, period = 100.millis)(s, global)
    } yield res).attempt.interruptWhen(sig)

    periodicRetry.concurrently(shutterDowner).compile.toList.map { xs =>
      assert(xs == List(Right(1)))
    }
    .unsafeRunSync()
  }
}
