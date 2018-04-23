package maxwell

import scala.concurrent.duration.DurationInt
import org.scalatest._

import cats.syntax.apply._
import cats.effect.{IO, Timer}
import fs2.async.refOf


class EffectSpec extends FlatSpec with Matchers { self =>
  import effect.EC

  "`later`" should "not evaluate side effects without being called" in {
    var gotCalled = false
    lazy val x = {
      gotCalled = true
      1
    }

    effect.later(x)
    assert(!gotCalled, "side effects were evaluated")
  }

  it should "evaluate effects when called" in {
    var gotCalled = false
    lazy val x = {
      gotCalled = true
      1
    }

    effect.later(x).unsafeRunSync()
    assert(gotCalled, "side effects were not evaluated")
  }

  "`safeIO`" should "catch exceptions" in {
    effect.safeIO(
      effect.later(throw new Exception)).unsafeRunSync()
  }

  "`recover`" should "catch exceptions" in {
    val t =
      effect.recover(effect.later[Int](throw new Exception))(effect.later(1))

    t.unsafeRunSync() should be (1)
  }

  it should "not evaluate the first or second task" in {
    var didRun = false
    val t1 = effect.later[Int](throw new Exception)
    val t2 = effect.later {
      didRun = true
      1
    }

    effect.recover(t1)(t2)
    assert(!didRun, "did evaluate second task in `recover`")
  }

  "`ensure`" should "not evaluate task without it being run" in {
    effect.ensure(effect.later(1))(_ != 1, new Exception)
  }

  it should "should throw when the predicate is true" in {
    assertThrows[Exception] {
      effect.ensure(effect.later(1))(_ != 1, new Exception).unsafeRunSync()
    }
  }

  it should "should not throw when the predicate is false" in {
    effect.ensure(effect.later(1))(_ == 1, new Exception).unsafeRunSync()
  }

  "`product`" should "not execute the Effect first" in {
    var didRun = false
    val t1 = effect.later {
      didRun = true
      1
    }

    val t2 = effect.later {
      didRun = true
      "two"
    }

    effect.product(t1, t2)
    assert(!didRun, "product executed effect before producing tuple")
  }

  "`traverse`" should "not execute immediately" in {
    effect.flatten {
      List(
        effect.later(1),
        effect.later(throw new Exception),
      )
    }
  }

  it should "not catch exceptions when run" in {
    assertThrows[Exception] {
      effect.flatten {
        List(
          effect.later(1),
          effect.later(throw new Exception),
        )
      }
      .unsafeRunSync()
    }
  }

  it should "evaluate effect in parallel" in {
    val ref = refOf[IO, List[Int]](List.empty).unsafeRunSync()
    val t1  = Timer[IO].sleep(50.millis) *> IO.unit <* ref.modify(_ :+ 1)
    val t2  = Timer[IO].sleep(2.millis)  *> IO.unit <* ref.modify(_ :+ 2)

    effect.flatten(List(t1, t2)).unsafeRunSync()
    assert(ref.get.unsafeRunSync() == List(2, 1), "Threads run sequentially despite one thread being delayed")
  }

  "`traverse`" should "produce the correct result" in {
    val xs = List(effect.later(1), effect.later(2), effect.later(3))

    effect.traverse(xs)(a => effect.later(a + 1))
      .unsafeRunSync() shouldEqual List(2, 3, 4)
  }

  it should "not evaluate effect before being run" in {
    val xs = List(effect.later(1), effect.later(throw new Exception), effect.later(3))
    effect.traverse(xs)(a => effect.later(a + 1))
  }
}
