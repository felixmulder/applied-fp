package applied

import cats.implicits._
import cats.Applicative
import cats.effect.IO
import scala.concurrent.ExecutionContext

object effect {

  final implicit val EC: ExecutionContext = ExecutionContext.global

  /** Creates an IO that is not evaluated immediately */
  def later[A](a: => A): IO[A] =
    ???

  /** Creates a task that lifts the computation into an either - catching
    * exceptions in the left part of the disjunction.
    */
  def safeIO[A](a: IO[A]): IO[Either[Throwable, A]] =
    ???

  /** If the first IO `t1` throws an exception, the IO `t2` is returned in
    * its stead
    */
  def recover[A, B >: A](t1: IO[A])(t2: IO[B]): IO[B] =
    ???

  /** Ensures the result of the effect conforms to the predicate and otherwise
   *  fails the IO operation
    */
  def ensure[A](t: IO[A])(p: A => Boolean, ex: => Throwable): IO[A] =
    ???

  /** Combines two IOs in a tuple */
  def product[A, B](t1: IO[A], t2: IO[B]): IO[(A, B)] =
    ???

  /* Flattens a list of IO to an IO of a list */
  def flatten[A](xs: List[IO[A]]): IO[List[A]] =
    ???

  /** Traverse a list of IO into an IO of list where the function has been
    * applied to each element
    */
  def traverse[A, B](xs: List[IO[A]])(f: A => IO[B]): IO[List[B]] =
    ???
}
