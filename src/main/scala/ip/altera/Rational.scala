package sifive.fpgashells.ip.altera

import java.lang

// A rational number x / y is represented by 2 integers x and y
// x is called the numerator and y is called the denominator

class Rational(x: Int, y: Int) {

  // require is used to enforce a precondition on the caller
  //require(y != 0, "denominator must be non-zero")

  // define a greatest common divisor method we can use to simplify rationals
  private def gcd(a: Int, b: Int): Int = Math.abs(if (b == 0) a else gcd(b, a % b))

  val g = gcd(x, y)

  val num = x / g
  val den = y / g

  // define a second constructor
  def this(x: Int) = this(x, 1)

  // define methods on this class
  def add(r: Rational): Rational =
    new Rational(num * r.den + r.num * den, den * r.den)

  def +(r: Rational): Rational = add(r)

  // negation
  def neg = new Rational(-num, den)
  def unary_- : Rational = neg

  def sub(r: Rational): Rational = add(r.neg)

  def -(r: Rational): Rational = sub(r)

  def mult(r: Rational) =
    new Rational(num * r.num, den * r.den)

  def *(r: Rational): Rational = mult(r)

  def div(r: Rational) =
    new Rational(num * r.den, den * r.num)

  def /(r: Rational): Rational = div(r)

  def less(r: Rational): Boolean = num * r.den < r.num * den

  def <(r: Rational): Boolean = less(r)

  def more(r: Rational): Boolean = num * r.den > r.num * den

  def >(r: Rational): Boolean = more(r)

  def max(r: Rational): Rational = if (less(r)) r else this

  def min(r: Rational): Rational = if (more(r)) r else this

  def inv: Rational = new Rational(den, num)
  def unary_/ : Rational = inv

  override def toString: String = num + "/" + den

  def toDouble: Double = num.toDouble / den.toDouble
}

object Rational {
  def apply(x: Int, y: Int): Rational = new Rational(x,y)
  def mediant(r: Rational, s: Rational) = new Rational(r.num + s.num, r.den + s.den)
}

object RationalApprox {
  def toRational(x: Double): Rational = {
    val epsilon = 1E-6
    var left = new Rational(0, 1)
    var right = new Rational(1, 0)
    var best = left
    var bestError = Math.abs(x)

    // do Stern-Brocot binary search
    while ( bestError > epsilon) { // compute next possible rational approximation
      val mediant = Rational.mediant(left, right)
      if (x < mediant.toDouble) right = mediant // go left
      else left = mediant // go right
      // check if better and update champion
      val error = Math.abs(mediant.toDouble - x)
      if (error < bestError) {
        best = mediant
        bestError = error
      }
    }
    best
  }
}