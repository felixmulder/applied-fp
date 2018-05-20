package applied

import cats.implicits._
import cats.Applicative

object applicative {

  // 1) Implement an applicative instance for list
  implicit val ListApplicative = new Applicative[List] {
    def ap[A, B](ff: List[A => B])(fa: List[A]): List[B] =
      ff.flatMap(f => fa.map(f))

    def pure[A](a: A): List[A] =
      List(a)
  }

  // 2) Implement the right hand of Either as Applicative
  implicit def EitherApplicative[L] = new Applicative[Either[L, ?]] {
    def ap[A, B](ff: Either[L, A => B])(fa: Either[L, A]): Either[L, B] =
      ff.flatMap(f => fa.map(f))

    def pure[A](a: A): Either[L, A] =
      Right(a)
  }

  // 3) Implement `cartesian` product in terms of Applicative:
  def cartesian(xs: List[String], ys: List[String]): List[String] = {
    val f: ((String, String)) => String = { case (x, y) => x ++ y }

    f.pure[List] <*> Applicative[List].product(xs, ys)
  }

  // 4) Implement validate in terms of applicative:
  def combineWith[F[_]: Applicative, A, B](a1: F[A], a2: F[A], p: (A, A) => B): F[B] =
    (a1, a2).mapN { case (a1, a2) => p(a1, a2) }
}
