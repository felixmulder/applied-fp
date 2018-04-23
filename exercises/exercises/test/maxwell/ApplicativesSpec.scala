package maxwell

import cats.instances.OptionInstances
import org.scalatest._
import cats.tests.CatsSuite
import cats.syntax.functor._
import cats.laws.discipline.ApplicativeTests

import org.scalacheck.{Arbitrary, Gen}
import maxwell.applicative._

class ApplicativesSpec extends CatsSuite {

  test("List Applicative - identity") {
    // The identity law says that when applying the `identity` function to an
    // applicative, it should return the original value:
    //
    // Implement the test here!
    fail("implement the identity law plz!")
  }

  test("List Applicative - homomorphism") {
    // The homomorphism law says that applying a `pure` function to a `pure`
    // value is the same as applying the function to the value in the normal
    // way and then using pure on the result. In a sense, that means pure
    // preserves function application.

    fail("implement the homomorphism law plz!")
  }

  test("List Applicative - interchange") {
    // The interchange law says that applying a morphism to a `pure` value
    // `y.pure[F]`, where `F` is an `Applicative`, is the same as applying
    // `(_ => y).pure[F]` to the morphism.

    fail("implement the interchange law plz!")
  }

  test("List Applicative - composition") {
    // The composition law says that `compose.pure[F]` composes morphisms similarly to
    // how `compose` composes functions: applying the composed morphism
    // `compose.pure[F] <*> u <*> v <*> w` gives the same result as applying u
    // to the result of applying v to w.
    //
    // Note: `<*>` is an alias of `ap`
    fail("implement the composition law plz!")
  }

  checkAll("List.ApplicativeLaws", ApplicativeTests[List](ListApplicative).applicative[Int, Int, String])

  checkAll(
    "Either[E, ?].ApplicativeLaws",
    ApplicativeTests[Either[Int, ?]](EitherApplicative[Int]).applicative[Int, Int, String])

  test("cartesian should return all possible combinations") {
    val xs  = List("yes", "no", "maybe")
    val ys  = List("?", "!", ".")
    val exp = List("yes?", "yes!", "yes.", "no?", "no!", "no.", "maybe?", "maybe!", "maybe.")
    cartesian(xs, ys) shouldEqual (exp)
  }

  test("combineWith should be able to combine Options") {
    combineWith(Option(1), Option(2), (i: Int, j: Int) => Some(i + j)) should be (Some(3))
  }
}
