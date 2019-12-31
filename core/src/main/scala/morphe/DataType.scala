package morphe

import cats._
import cats.implicits._
import cats.syntax._

import higherkindness.droste._
import higherkindness.droste.data.Fix
import higherkindness.droste.util.DefaultTraverse

import scala.Ordering
import scala.reflect.ClassTag

import java.time.Instant
import java.util.UUID

sealed trait DataTypeF[+A] extends Serializable {
  val name = toString.replace("TypeF", "").toLowerCase
}

sealed abstract class PrimitiveTypeF[T: ClassTag /*: Ordering: Codec*/ ] extends DataTypeF[Nothing] {
  @transient lazy val tag: ClassTag[T] = implicitly[ClassTag[T]] //make spark happy
  //@transient lazy val codec: Codec[T] = implicitly[Codec[T]] //make spark happy
  //val ordering: Ordering[T] = implicitly[Ordering[T]]
}

case object ShortTypeF extends PrimitiveTypeF[Short]
case object IntTypeF extends PrimitiveTypeF[Int]
case object LongTypeF extends PrimitiveTypeF[Long]
case object DoubleTypeF extends PrimitiveTypeF[Double]
case object StringTypeF extends PrimitiveTypeF[String]
case object BoolTypeF extends PrimitiveTypeF[Boolean]
case object ByteTypeF extends PrimitiveTypeF[Byte]
case object UUIDTypeF extends PrimitiveTypeF[UUID]
case object TimestampTypeF extends PrimitiveTypeF[Instant]
case object BinaryTypeF extends PrimitiveTypeF[Array[Byte]]

object PrimitiveTypeF {
  implicit val stringPrimitive = StringTypeF
  implicit val intPrimitive = IntTypeF
}

final case class StructF[A](
  fields: List[FieldF[A]]
) extends DataTypeF[A]

final case class ArrayTypeF[A](valueType: A) extends DataTypeF[A]
final case class MapTypeF[A](keyType: A, valueType: A) extends DataTypeF[A]
final case class OptionTypeF[A](valueType: A) extends DataTypeF[A]

object DataTypeF extends DataTypeDsl {
  implicit val fieldTraverse: Traverse[DataTypeF] = new DefaultTraverse[DataTypeF] {
    def traverse[G[_]: Applicative, A, B](fa: DataTypeF[A])(f: A => G[B]): G[DataTypeF[B]] =
      fa match {
        case StructF(fields) =>
          fields
            .map(
              field =>
                f(field.dataType).map { a =>
                  field.copy(dataType = a)
                }
            )
            .traverse(a => a)
            .map { a =>
              StructF(a)
            }
        case ArrayTypeF(valueType) => Applicative[G].map(f(valueType))(ArrayTypeF.apply)
        case OptionTypeF(valueType) => Applicative[G].map(f(valueType))(OptionTypeF.apply)
        case MapTypeF(keyType, valueType) =>
          (f(keyType), f(valueType)).mapN(MapTypeF(_, _))
        case t: PrimitiveTypeF[_] => Applicative[G].pure(t)
      }
  }

  val toStringAlgebra:Algebra[DataTypeF, String] = Algebra {
    case ArrayTypeF(value) => s"list($value)"
    case MapTypeF(key, value) => s"map($key,$value)"
    case OptionTypeF(value) => s"optional($value)"
    case StructF(fields) =>
      s"struct(${fields.mkString(",")})"
    case prim: PrimitiveTypeF[_] =>
      prim.name
  }

  val stringify: Fix[DataTypeF] => String = scheme.cata(toStringAlgebra)

  /*
  implicit val equal: Delay[Equal, DataTypeF] = new Delay[Equal, DataTypeF] {
    def apply[B](eq: Equal[B]) = Equal.equal(
      (a, b) => {
        implicit val ieq = eq
        (a, b) match {
          case (l: PrimitiveTypeF[_], r: PrimitiveTypeF[_]) => l == r
          case (StructF(lfields), StructF(rfields)) =>
            val sameLength = lfields.length == rfields.length
            val sameFields = lfields == rfields
            sameLength && sameFields
          case (ArrayTypeF(l), ArrayTypeF(r)) =>
            l === r
          case (MapTypeF(lk, lv), MapTypeF(rk, rv)) =>
            lk === rk && lv === lv
          case (OptionTypeF(lv), OptionTypeF(rv)) =>
            lv === rv
          case (l @ _, r @ _) =>
            false
        }
      }
    )
  }

  implicit val show: Delay[Show, DataTypeF] = new Delay[Show, DataTypeF] {
    def apply[B](s: Show[B]) = {
      implicit val is = s
      Show.show {
        case ArrayTypeF(value) => Cord(s"list(${value.shows})")
        case MapTypeF(key, value) => Cord(s"map(${key.shows},${value.shows})")
        case OptionTypeF(value) => Cord(s"optional(${value.shows})")
        case StructF(fields) =>
          Cord(s"struct(${fields.map(f => s"${f.shows}").mkString(",")})")
        case prim: PrimitiveTypeF[_] =>
          Cord(prim.name)
        case NullTypeF =>
          Cord("null")
      }
    }
  }

  def stringify(dataType: DataType): String =
    dataType.shows

  type Annotation = (String, Int, DataType)
  type Annotated[A] = EnvT[Annotation, DataTypeF, A]
  def annotateSchema: Coalgebra[Annotated, (Annotation, Fix[DataTypeF])] = {
    case (anno @ (path, ordinal, subType), dt) =>
      dt.project match {
        case StructF(fields) =>
          val out = fields.zipWithIndex.map { case (v, i) => v.map(d => (v.name, i, v.dataType) -> d) }
          EnvT(anno -> StructF(out))
        case dt =>
          EnvT(anno -> dt.map(anno -> _))
      }
  }

  def treeString(dataType: DataType): String = {
    def schemaWithPath: Coalgebra[EnvT[(String, Boolean), DataTypeF, ?], ((String, Boolean), Fix[DataTypeF])] = {
      case (anno @ (path, nullable), dt) =>
        dt.project match {
          case StructF(fields) =>
            val out = fields.map(v => v.map(d => (v.name -> v.nullable) -> d))
            EnvT(anno -> StructF(out))
          case dt =>
            EnvT(anno -> dt.map(anno -> _))
        }
    }

    val toTree: Algebra[EnvT[(String, Boolean), DataTypeF, ?], Tree[String]] = {
      case EnvT(((path, nullable), children @ StructF(fields))) =>
        Tree.Node(path, fields.map(_.dataType).toStream)
      case EnvT(((path, nullable), MapTypeF(key, value))) =>
        Tree.Node(path, value.subForest)
      case EnvT(((path, nullable), ArrayTypeF(value))) =>
        Tree.Node(path, value.subForest)
      case EnvT(((path, nullable), OptionTypeF(value))) =>
        Tree.Node(path, value.subForest)
      case EnvT(((path, nullable), p: PrimitiveTypeF[_])) =>
        Tree.Leaf(s"$path: ${p.name} (nullable = $nullable)")
    }

    val res = (("root", false), dataType).hylo(toTree, schemaWithPath)

    res.drawTree
  }*/
}

trait DataTypeDsl {
  type DataType = Fix[DataTypeF]

  def struct(fields: FieldF[DataType]*): DataType = Fix[DataTypeF](StructF(fields.toList))

  def field(column: String, dataType: DataType): FieldF[DataType] = FieldF(column, dataType)

  def mapType(keyType: DataType, valueType: DataType) = Fix[DataTypeF](MapTypeF(keyType, valueType))

  def arrayType(valueType: DataType) = Fix[DataTypeF](ArrayTypeF(valueType))

  def optional(valueType: DataType) = Fix[DataTypeF](OptionTypeF(valueType))

  val shortType: DataType = Fix[DataTypeF](ShortTypeF)
  val intType: DataType = Fix[DataTypeF](IntTypeF)
  val longType: DataType = Fix[DataTypeF](LongTypeF)
  val stringType: DataType = Fix[DataTypeF](StringTypeF)
  val doubleType: DataType = Fix[DataTypeF](DoubleTypeF)
  val boolType: DataType = Fix[DataTypeF](BoolTypeF)
  val uuidType: DataType = Fix[DataTypeF](UUIDTypeF)
  val timestamp: DataType = Fix[DataTypeF](TimestampTypeF)
  val binary: DataType = Fix[DataTypeF](BinaryTypeF)
  val byte: DataType = Fix[DataTypeF](ByteTypeF)
}
