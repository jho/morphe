package morphe.circe

import morphe._

import higherkindness.droste._
import org.scalatest.FlatSpec
import io.circe.Encoder
import io.circe.Json
import org.scalatest.Matchers

case class Test(a: String, b: Int)

class DataTypeTest extends FlatSpec with Matchers {
  def toEncoderAlgebra: Algebra[DataTypeF, Encoder[_]] = Algebra {
    //case StructF(fields) =>
    case StringTypeF => Encoder.encodeString
  }

  val makeEncoder: DataType => Encoder[_] = scheme.cata(toEncoderAlgebra)

  implicit val encoder = new Encoder[Test] {
    val schema = SchemaFor.derive[Test]

    def apply(a: Test) = {
      ???
    }
  }

  "Circe" should "be able to create an encoder" in {
    val encoder = makeEncoder(DataTypeF.stringType)

    encoder.asInstanceOf[Encoder[String]]("foobar") shouldBe Json.fromString("foobar")
    succeed
  }
}
