package applied

//import cats.syntax.apply._
//import cats.syntax.applicative._
import cats.implicits._
import cats.Applicative

object applicative {

  // 1) Implement an applicative instance for list
  implicit val ListApplicative = new Applicative[List] {
    def ap[A, B](ff: List[A => B])(fa: List[A]): List[B] =
      ???

    def pure[A](a: A): List[A] =
      ???
  }

  // 2) Implement the right hand of Either as Applicative
  implicit def EitherApplicative[L] = new Applicative[Either[L, ?]] {
    def ap[A, B](ff: Either[L, A => B])(fa: Either[L, A]): Either[L, B] =
      ???

    def pure[A](a: A): Either[L, A] =
      ???
  }

  // 3) Implement `cartesian` product in terms of Applicative:
  def cartesian(xs: List[String], ys: List[String]): List[String] =
    ???

  // 4) Implement validate in terms of applicative:
  def combineWith[F[_]: Applicative, A, B](a1: F[A], a2: F[A], p: (A, A) => B): F[B] =
    ???
}
