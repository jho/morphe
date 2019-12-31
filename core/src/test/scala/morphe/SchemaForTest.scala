package morphe

import org.scalatest.FlatSpec

case class TestStruct(a: String, b: Int)

sealed trait TestUnion 
case class MemberA(a:String) extends TestUnion
case class MemberB(int:Int) extends TestUnion

class SchemaForTest extends FlatSpec {
  "SchemaFor" should "be able to derive a schema for" in {
    val schemaForStruct = SchemaFor.derive[TestStruct]
    println(DataTypeF.stringify(schemaForStruct.schema))
    val schemaForUnion = SchemaFor.derive[TestUnion]
    println(DataTypeF.stringify(schemaForUnion.schema))
    succeed
  }
}
