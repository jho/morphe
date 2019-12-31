package morphe

import magnolia._
import higherkindness.droste.data.Fix

import language.experimental.macros
import scala.language.implicitConversions

/**
  * Used to conjure up a schema for a type.  This trait is sealed because it
  * is not meant for extention.  SchemaFor instances exist for all necessary types, or an Injection can be used to
  * create a Schema for a User Defined Type
  */
sealed trait SchemaFor[T] {
  def schema: DataType
}

abstract class Injection[T, P] {
  def to(t: T): P
  def from(p: P): Either[Throwable, T]
}

object SchemaFor {
  import DataTypeF._
  type Typeclass[T] = SchemaFor[T]

  implicit def primitive[T: PrimitiveTypeF]: SchemaFor[T] = new SchemaFor[T] {
    def schema: DataType = Fix[DataTypeF](implicitly[PrimitiveTypeF[T]])
  }

  implicit def udf[U, P: PrimitiveTypeF](implicit inj: Injection[U, P]) = new SchemaFor[U] {
    def schema: DataType = primitive[P].schema
  }

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
