package applied

import cats.data.ValidatedNel
import cats.effect.IO
import cats.implicits._

object traverse {

  // Traverse is a type class that at its core provides the following function:
  //
  //    def traverse[G[_]: Applicative, A, B](fa: F[A])(f: A => G[B]): G[F[B]]
  //
  // This function takes a function from `A => G[B]` and uses it to turn an
  // `F[A]` into a `G[F[B]]`.
  //
  // An example of this is taking a `List[Future[Int]]` and turning it into an
  // all-or-nothing result `Future[List[Int]]`.
  //
  // We could implement this in terms of:
  //
  //   def traverse[A, B](fa: List[Future[A]])(f: A => Future[B]): Future[List[B]] =
  //     fa.foldRight(Future.success(List.empty[B])) { (a, acc) =>
  //       f(a).flatMap { b =>
  //         acc.map(b :: _)
  //       }
  //     }
  //
  // With this, we can now do:
  //
  //   fa.traverse(identity)
  //
  // and get back a `Future[List[A]]`. Since this is a common pattern, there's
  // an alias for this called sequence:
  //
  //    def sequence[G[_]: Applicative, A](fa: F[A]): G[F[A]]
  //
  //
  // With the above implementation, however, we're not able to solve it
  // generically! Let's try to solve it if we didn't know that `G =:= Future`:
  //
  //   def traverse[G[_]: Applicative, A, B](fa: List[G[A]])(f: A => G[B]): G[List[B]] =
  //     fa.foldRight(Applicative[G].pure(List.empty[B])) { (a, acc) =>
  //       Applicative[G].map2(f(a), acc)(_ :: _)
  //     }
  //
  // Now the only known factor is `List`!
  //
  // Doing it in parallel
  // ====================
  // Since `Applicative` provides disjoint execution - we can actually do it in
  // parallel - if there's an instance of `Parallel` for the type we're
  // traversing on.
  //
  //   fa.parTraverse(f)


  // 1) Implement `allOrNothing`

  /** Take turn a list of `Either[E, A]` into a single either with the first
   *  error
   */
  def allOrNothing[E <: Throwable, A](xs: List[Either[E, A]]): Either[E, List[A]] =
    ???

  // Hmm, this isn't really a very good approach - we'd like to actually know
  // what all the errors were. Imagine this is validating a form - in that case
  // we'd like to give the user all errors at once. Otherwise, the user is
  // going to fix one error, resubmit, fix the next error and so on -- this
  // doesn't make for that great of a user experience.
  //
  // Let's introduce, `ValidatedNel`!
  //
  // `ValidatedNel` is similar to `Either` -- but it can gather errors in the
  // left part of the disjunction ("Nel" stands for non-empty list).
  //
  // We can turn an either into a validated by doing: `either.toValidatedNel`
  //
  // Since `ValidatedNel` has an `Applicative` instance, we can use it with
  // traverse!

  // 2) Implement `allOrErrors`!

  def allOrErrors[E <: Throwable, A](xs: List[Either[E, A]]): ValidatedNel[Throwable, List[A]] =
    ???


  // 3) Implement `requestsInPar`!

  case class Response(status: Int, body: String)

  /** Performs a list of requests in parallel and then gathers the result in a
   *  `ValidatedNel`
   */
  def requestsInPar[A](xs: List[IO[Response]]): IO[ValidatedNel[Throwable, List[Response]]] = {

    def validateRes(io: IO[Response]): IO[ValidatedNel[Throwable, Response]] =
      ???

    def flatten(xs: List[ValidatedNel[Throwable, Response]]): ValidatedNel[Throwable, List[Response]] =
      ???

    // User a combination of `performReq` and `flatten` with the `Parallel`
    // type class's `Traverse` ops to make the request in parallel and gather
    // the errors in a NEL:
    ???
  }

}
