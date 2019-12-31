package morphe

import org.scalatest.FlatSpec
import java.net.InetAddress

case class TestStruct(a: String, b: Int)

sealed trait TestUnion
case class MemberA(a: String) extends TestUnion
case class MemberB(int: Int) extends TestUnion

class SchemaForTest extends FlatSpec {
  "SchemaFor" should "be able to derive a primivite" in {
    implicitly[SchemaFor[String]]
    succeed
  }

  it should "be able to derive schema for case class" in {
    val schemaForStruct = SchemaFor.derive[TestStruct]
    println(DataTypeF.stringify(schemaForStruct.schema))
    succeed
  }

  it should "be able to derive schema for a sealed trait" in {
    val schemaForUnion = SchemaFor.derive[TestUnion]
    println(DataTypeF.stringify(schemaForUnion.schema))
    succeed
  }

  it should "be able to derive schema for a UDT" in {
    implicit val inj = new Injection[InetAddress, String] {
      def to(inet:InetAddress):String = ???
      def from(str:String):Either[Throwable, InetAddress] = ???
    }
    implicit val schemaForUDT = SchemaFor.udf[InetAddress, String]
    implicitly[SchemaFor[InetAddress]].schema
    succeed
  }
}
