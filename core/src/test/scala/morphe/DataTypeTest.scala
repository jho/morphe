package morphe

import org.scalatest.FlatSpec

class DataTypeTest extends FlatSpec {
  import DataTypeF._
  "DataType" should "be able to print a string" in {
    println(
      DataTypeF.stringify(
        struct(
          field("foo", shortType),
          field("bar", intType)
        )
      )
    )
    succeed
  }
}
