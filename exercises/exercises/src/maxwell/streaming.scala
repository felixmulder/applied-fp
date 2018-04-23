package maxwell

import fs2.{Pure, Scheduler, Stream}
import cats.effect.IO
import cats.implicits._

import scala.concurrent.duration.{FiniteDuration, DurationInt}
import scala.concurrent.ExecutionContext

object streaming {

  // Building streams
  // ================
  // This first section focuses on using functions available in the `Stream`
  // object and type, as such, use the obvious defined methods if applicable!

  /** Creates a stream which emits a single element */
  def singleElement[A](a: A): Stream[Pure, A] =
    ???

  /** Creates a stream which emits all elements in `xs` */
  def multipleElements[A](xs: A*): Stream[Pure, A] =
    ???

  /** Creates a stream which emits all elements in `xs` */
  def streamFromList[A](xs: List[A]): Stream[Pure, A] =
    ???

  /** Creates a stream which emits all elements in `xxs` in order */
  def streamFromLists[A](xxs: List[A]*): Stream[Pure, A] =
    ???

  /** Combines two streams so that all the elements from `s1` preceed `s2` */
  def concatenate[A](s1: => Stream[Pure, A], s2: => Stream[Pure, A]): Stream[Pure, A] =
    ???

  // Manipulating streams
  // ====================
  // In this section we focus on manipulating streams. Here, we don't want you
  // to use the built in functions with the same name if they already exist -
  // but instead either rely on the methods defined above or the combinator
  // methods on the `Stream` type

  /** Creates a stream which repeats the element `a` */
  def repeated[A](a: A): Stream[Pure, A] =
    ???

  /** Returns a list with only even elements */
  def even(s: Stream[Nothing, Int]): Stream[Pure, Int] =
    ???

  /** Returns a sum of all elements in the stream */
  def sum[A: Numeric](s: Stream[Pure, A]): Stream[Pure, A] =
    ???

  /** Combines two streams so that every other element (in the synchronous
    * case) is belongs to `s1` and `s2` respectively
    *
    * Ergo if the streams were lists, this would be the expected behaviour:
    * {{{
    * val xs1 = List(1, 3)
    * val xs2 = List(2, 4)
    *
    * combine(xs1, xs2) == List(1, 2, 3 ,4)
    * }}}
    *
    */
  def interleave[A](s1: Stream[Pure, A], s2: Stream[Pure, A]): Stream[Pure, A] =
    ???

  /** Calculate the sum of two streams of integers */
  def sum[A: Numeric](s1: Stream[Pure, A], s2: Stream[Pure, A]): Stream[Pure, A] =
    ???

  // Streams as processes
  // ====================
  // Streams can also be used to model control flow and as such we can build
  // processes that do retry with exponential back-off and the like.
  //
  // Throghout the exercise below, we'll implement a `Stream[IO, Unit]` that
  // periodically makes a check against a webserver to see what time it is and
  // then prints the time.

  /** Create a stream that produces a `()` every `period`
   *
   *  When run with `period= 1s` this should produce a `()` at:
   *
   *  0s, 1s, 2s, 3s...
   */
  def periodically(period: FiniteDuration)(
    implicit
    S: Scheduler,
    EC: ExecutionContext
  ): Stream[IO, Unit] =
    ???

  /** Retry an `op` for a maximum of `retries`, backing off `backOff` between
   *  each retry
   *
   *  Meaning with: `retryOp(io, 5, 2.seconds)`, there would be an attempt at:
   *
   *  0s, 2s, 4s, 6s, 8s
   *
   *  If a valid return value hasn't been produced - the stream will fail
   */
  def retryOp[A](op: IO[A], retries: Int, backOff: FiniteDuration)(
    implicit
    S: Scheduler,
    EC: ExecutionContext
  ): Stream[IO, A] =
    ???

  /** Tries to evaluate `op` every `period` with `retries` amount of retries
   *
   *  If the evaluation fails more than `retries` - the stream should log the
   *  failure and then continue evaluation as if nothing happened.
   */
  def periodicallyRetry[A](
    op: IO[A],
    retries: Int,
    backOff: FiniteDuration,
    period: FiniteDuration
  )(implicit S: Scheduler, EC: ExecutionContext): Stream[IO, A] =
    ???
}
