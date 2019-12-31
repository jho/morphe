package morphe

import cats._
import cats.implicits._
import cats.syntax._

import higherkindness.droste._
import higherkindness.droste.util.DefaultTraverse

final case class FieldF[+A](
  name: String,
  dataType: A,
  nullable:Boolean = false
)

object FieldF {
  implicit val fieldTraverse: Traverse[FieldF] = new DefaultTraverse[FieldF] {
    def traverse[G[_]: Applicative, A, B](fa: FieldF[A])(
        f: A => G[B]): G[FieldF[B]] = f(fa.dataType).map(b => FieldF(fa.name, b))
  }

  implicit val equal: Delay[Eq, FieldF] = new Delay[Eq, FieldF] {
    def apply[B](eq: Eq[B]) = Eq.instance(
      (a, b) => {
        implicit val ieq = eq
        a.name == b.name && a.dataType === b.dataType
      }
    )
  }

  /*
  implicit val show: Show, FieldF] = new Delay[Show, FieldF] {
    def apply[B](s: Show[B]) = {
      Show.show { c =>
        s"${c.name}: ${s.show(c.dataType)}"
      }
    }
  }

  def stringify(col: FieldF[String]): String = */
}
