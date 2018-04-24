---
title:       Pure Functional Programming
subtitle:    Local Reasoning and Controlled Effects
author:      Felix Mulder
date:        April 2018
classoption: "aspectratio=169"
---

## Who am I?
• Scala 2.12 Docs Compiler

• Scala 3 Compiler Engineer @ EPFL w/ Martin Odersky

• Software Engineer @ Klarna Bank

# Pure Functional Programming

## What is pure FP?
*"Programming using only functions"*

or

*"Programming without \say{side-effects}"*

## Referential Transparency
>  An expression is said to be referentially transparent if it can be replaced
>  with its corresponding value without changing the program's behavior.

## Equational Reasoning
```none
x = 5
y = x + x
z = 2 * y + x
```

## Equational Reasoning
```none
x = 5
y = 5 + 5
z = 2 * y + x
```

## Equational Reasoning
```none
x = 5
y = 10
z = 2 * y + x
```

## Equational Reasoning
```none
x = 5
y = 10
z = 2 * 10 + 5
```

## Equational Reasoning
```none
x = 5
y = 10
z = 25
```

## Equational Reasoning
```tut:invisible
import cats._
import cats.implicits._
import cats.effect.IO
```
```tut:silent
val const5 = 5
```
```tut:book
const5 + const5
```

## Equational Reasoning
```tut:invisible
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
```
```tut:book
val const5  = Future(5)
val const10 = const5.flatMap(x => const5.map(x + _))

Await.result(const10, 1.second)
```

## Equational Reasoning
```tut:book
val read = Future(io.StdIn.readInt())
// > 10

val read2 = read.flatMap(x => read.map(x + _))

Await.result(read2, 5.seconds)
```

## Equational Reasoning
```tut:book
val read2 = Future(io.StdIn.readInt()).flatMap {
  x => Future(io.StdIn.readInt()).map(x + _)
}

// > 10

// > 20

Await.result(read2, 5.seconds)
```
## Side Effects
- Future is not pure
- Neither is `StdIn.readInt`

# The shapes themselves are.

## Shapes
```tut:silent
trait Future[A] {
  def result: A
}
```

## Category Theory
> A Monad is just a Monoid in the category of Endo-Functors, so what's the
> problem?

## Category Theory
The study of how these shapes behave and how they relate to each other

## Category Theory
You already know a lot of these shapes and relations.

# `def map[B](f: A => B): List[B]`

# `def map[B](f: A => B): F[B]`

# Functor

## Type Classes
- Ad-hoc polymorphism
- Provided via implicits

# Let's implement this

## Functor Laws
- Identity
- Composition

# Cats

## Cats
```tut:book
import cats._, cats.implicits._

Functor[List].map(List(1, 2, 3))(_ - 1)
```

## Cats
```tut:invisible
import cats.data._
implicit val showNFE = new Show[NonEmptyList[NumberFormatException]] {
  def show(xs: NonEmptyList[NumberFormatException]) =
    xs.map("  " +_.toString)
      .toList
      .mkString("NEL(\n", "\n", "\n)")
}
```
```tut:silent
def parseInt(s: String) =
  Validated.catchOnly[NumberFormatException](s.toInt).toValidatedNel
```
```tut:book
List("1", "2", "a", "b").traverse(parseInt).show
```

# Applicative

## Applicative
Adds two functions:

• `pure`

• `ap`

## Applicative - `pure`
```tut:silent
trait Applicative[F[_]] {

  def pure[A](a: A): F[A]

  // ...
}
```

•  `List.apply`

•  `Option.apply`

## Applicative - `ap`
```tut:silent
trait Applicative[F[_]] {
  // ...

  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]
}
```

## ap in action

```tut:silent
import cats._, cats.implicits._

val fa = List(1,2,3)
val ff: List[Int => String] = List(_.toString, x => (x * 2).toString)
```

```tut:book
ff.ap(fa)

// or simply:

ff <*> fa
```

# Cartesian

## Applicative Laws
• Identity

• Composition

• Homomorphism

• Interchange

## Identity
```tut:book
((x: Int) => x).pure[List] <*> List(1, 2, 3) == List(1, 2, 3)
```

## Composition
```tut:invisible
case class A()
case class B()
case class C()
case class D()

val u: List[B => C] = List(_ => C())
val v: List[A => B] = List(_ => B())
val w: List[A] = List(A())
```
```tut:silent
val compose: (B => C) => (A => B) => (A => C) =
  bc => ab => bc compose ab
```
```tut:book
compose.pure[List] <*> u <*> v <*> w == u <*> (v <*> w)
```

## Homomorphism
```tut:book
val f: Int => String = _.toString

f.pure[List] <*> 1.pure[List] == f(1).pure[List]
```

## Interchange
```tut:book
val y: Int = 1
val u: List[Int => String] = List(_.toString)

u <*> y.pure[List] == ((f: Int => String) => f(y)).pure[List] <*> u
```

# Exercise

## Two things to remember about Applicative
• "Disjoint" - i.e. parallel

• `pure` - lift a value into `F[_]`

# Monads

## Monads
```tut:invisible
import scala.concurrent.Future
```
```tut:silent
val c1 =
  Future { /** let's assume we're making an async call here */ 1 }

val c2: Int => Future[Int] =
  x => Future { /** let's assume we're making another async call here */ x * 2 }
```

Now there's a dependency between `c2` and some value `x`. What if we want to
chain calls to `c1` with a call to `c2`?

## Monads
```tut:book
c1.map(c2)
```

## Monads
```tut:book
c1.map(c2).flatten
```

## Monads
```tut:book
c1.flatMap(c2)
```

## Monads
Monads are applicative functors with a continuation function referred to as
"bind" and in scala `flatMap`

They can be thought of as "*composable* computation descriptions"

## Monads
"Monads are things with `flatMap`" <- this isn't completely wrong - ergo it's
not completely right.

## Monad Laws
• Left identity

  `a.pure[M].flatMap(f) == f(a)`

• Right identity

  `m.flatMap(pure) == m`

• Associativity

  `m.flatMap(f).flatMap(g) == m.flatMap(x => f(x).flatMap(g))`

## Monad Laws - Left identity
```tut:silent
val f: Int => List[Int] = x => List(x)
val g: Int => List[Int] = x => List(x * 2)
```
```tut:book
1.pure[List].flatMap(f)

f(1)

1.pure[List].flatMap(f) == f(1)
```

## Monad Laws - Right identity
```tut:book
List(1).flatMap(_.pure[List])

List(1).flatMap(_.pure[List]) == List(1)
```

## Monad Laws - Associativity
```tut:book
List(1, 2).flatMap(f).flatMap(g)

List(1, 2).flatMap(x => f(x).flatMap(g))

List(1, 2).flatMap(f).flatMap(g) == List(1, 2).flatMap(x => f(x).flatMap(g))
```

# IO

## IO
```tut:book
import cats.effect.IO

val read = IO { io.StdIn.readInt() }

(read, read).mapN(_ + _)
```

## IO
```tut:book
(read, read).mapN(_ + _).unsafeRunSync() // > 1, > 2
```

# Exercise

# Traverse

## Traverse
```tut:silent
trait Traverse[F[_]] {
  def traverse[G[_]: Applicative, A, B](fa: F[G[A]])(f: A => G[B]): G[F[B]]
}
```

## Traverse
```tut:book
List(Option(1), None, Option(3)).traverse(identity)

List(Option(1), Option(2), Option(3)).traverse(identity)
```

## Traverse
```tut:silent
def traverse[G[_]: Applicative, A, B](fa: List[A])(f: A => G[B]): G[List[B]] = {
  fa.foldRight(List.empty[B].pure[G]){ case (a, acc) =>
    Applicative[G].map2(f(a), acc)(_ ::_)
  }
}
```
```tut:book
traverse(List(Option(1), Option(2), Option(3)))(identity)
```

## Traverse
```tut:book
List(Option(1), Option(2), Option(3)).traverse(identity)

List(Option(1), Option(2), Option(3)).sequence
```

# Stream

## Stream
### `fs2.Stream[F[_], A]`

# Why do we need another stream?

# Purity & Control Flow

## `Stream[F[_], A]`
• Emits `n` values of the type `A` where `n = 0, 1, ...`

• `A` is evaluated in the context of `F[_]`

# Akka-Streams

## `Stream[F[_], A]`
```tut:book
import fs2.Stream

Stream.emit(1, 2, 3, 4, 5).toList
```
## `Stream[F[_], A]`
```tut:silent
val concat = Stream.eval(IO(1)) ++ Stream.eval(IO(2))

concat.repeat
```

## Concatenating for Effect
```tut:book:nofail
Stream.eval(IO(println("hello!"))) ++ concat
```

## Concatenating for Effect
```tut:book
val hello = Stream.eval(IO(println("hello!"))).drain

hello ++ concat
```

## Concurrency Primitives
• `Ref[F[_], A]`

• `Signal[F[_], A]`

• `Scheduler`

# Principled vs Unprincipled

# Control Flow

# Exercise

# Putting it all together - http4s

## Kleisli Tripple
```tut:silent
case class Kleisli[F[_], A, B](val run: A => F[B])
```

# Kleisli is a Monad

## http4s
```tut:silent:reset
import cats.data.Kleisli
import cats.effect.IO
import org.http4s.{Request, Response}

type HttpService[F[_]] = Kleisli[F, Request[F], Response[F]]
```

## http4s
```tut:silent
type HttpService[F[_]] = Kleisli[F, Request[F], Option[Response[F]]]
```

## http4s
```tut:silent
import cats.data.OptionT

type HttpService[F[_]] = Kleisli[OptionT[F, ?], Request[F], Response[F]]
```

## http4s
```tut:silent
import org.http4s._
import org.http4s.dsl.io._

val service = HttpService[IO] {
  case req => Ok("hello, whatever!")
}
```
```tut:book
val resp = service(Request[IO]())

resp.value.unsafeRunSync()
```
