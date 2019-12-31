package morphe

import magnolia._
import higherkindness.droste.data.Fix

import language.experimental.macros
import scala.language.implicitConversions

trait SchemaFor[T] {
  def schema: DataType
}

object SchemaFor {
  import DataTypeF._
  type Typeclass[T] = SchemaFor[T]

  private def primitive[T](prim: PrimitiveTypeF[T]) = new SchemaFor[T] { def schema: DataType = Fix[DataTypeF](prim) }

  implicit val stringSchema = primitive(StringTypeF)

  implicit val shortSchema = primitive(ShortTypeF)

  implicit val intSchema = primitive(IntTypeF)

  def combine[T](ctx: CaseClass[SchemaFor, T]): SchemaFor[T] = new SchemaFor[T] {
    def schema: DataType = {
      val fields = ctx
        .parameters
        .map { p =>
          FieldF(p.label, p.typeclass.schema)
        }
      struct(fields: _*)
    }
  }

  def dispatch[T](ctx: SealedTrait[SchemaFor, T]): SchemaFor[T] =
    new SchemaFor[T] {
      def schema: DataType = {
        val fields = ctx.subtypes.map { t =>
          FieldF(t.typeName.short, t.typeclass.schema, true)
        }
        struct(fields: _*)
      }
    }

  implicit def derive[T]: Typeclass[T] = macro Magnolia.gen[T]
}
