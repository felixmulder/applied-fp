package applied

import cats.implicits._
import cats.Applicative
import cats.effect.{IO, Fiber}
import scala.concurrent.ExecutionContext

object effect {

  final implicit val EC: ExecutionContext = ExecutionContext.global

  /** Creates an IO that is not evaluated immediately */
  def later[A](a: => A): IO[A] =
    IO(a)

  /** Creates a task that lifts the computation into an either - catching
    * exceptions in the left part of the disjunction.
    */
  def safeIO[A](a: IO[A]): IO[Either[Throwable, A]] =
    a.attempt

  /** If the first IO `t1` throws an exception, the IO `t2` is returned in
    * its stead
    */
  def recover[A, B >: A](t1: IO[A])(t2: IO[B]): IO[B] =
    t1.attempt.flatMap {
      _.fold(_ => t2, IO.pure)
    }

  /** Ensures the result of the effect conforms to the predicate and otherwise
   *  fails the IO operation
    */
  def ensure[A](t: IO[A])(p: A => Boolean, ex: => Throwable): IO[A] =
    t.ensure(ex)(p)

  /** Combines two IOs in a tuple */
  def product[A, B](t1: IO[A], t2: IO[B]): IO[(A, B)] =
    for {
      x <- t1
      y <- t2
    } yield (x, y)

  /* Flattens a list of IO to an IO of a list */
  def flatten[A](xs: List[IO[A]]): IO[List[A]] =
    traverse(xs)(_.pure[IO])

  /** Traverse a list of IO into an IO of list where the function has been
    * applied to each element
    */
  def traverse[A, B](xs: List[IO[A]])(f: A => IO[B]): IO[List[B]] =
    xs
      .foldLeft(IO(Vector.empty[Fiber[IO, B]])) { (acc, io) =>
        Applicative[IO].map2(acc, io.flatMap(f).start)(_ :+ _)
      }
      .flatMap { xs =>
        xs
          .map(_.join).foldLeft(IO(Vector.empty[B])) { (acc, io) =>
            Applicative[IO].map2(acc, io)(_ :+ _)
          }
          .map(_.toList)
      }
}
