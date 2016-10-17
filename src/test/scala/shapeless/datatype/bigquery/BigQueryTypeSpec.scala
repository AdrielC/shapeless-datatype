package shapeless.datatype.bigquery

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.google.protobuf.ByteString
import org.joda.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import org.scalacheck.Prop.forAll
import org.scalacheck.Shapeless._
import org.scalacheck._
import shapeless._
import shapeless.datatype.SerializableUtils
import shapeless.datatype.record._

class BigQueryTypeSpec extends Properties("BigQueryType") {

  import shapeless.datatype.Records._

  val mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)

  implicit val arbByteString = Arbitrary(Gen.alphaStr.map(ByteString.copyFromUtf8))
  implicit val arbInstant = Arbitrary(Gen.const(Instant.now()))
  implicit val compareByteArrays = (x: Array[Byte], y: Array[Byte]) => java.util.Arrays.equals(x, y)
  implicit val compareIntArrays = (x: Array[Int], y: Array[Int]) => java.util.Arrays.equals(x, y)

  def roundTrip[A, L <: HList](m: A, t: BigQueryType[A] = BigQueryType[A])
                              (implicit
                               gen: LabelledGeneric.Aux[A, L],
                               fromL: FromTableRow[L],
                               toL: ToTableRow[L],
                               rm: RecordMatcher[L]): Boolean = {
    val tr1 = t.toTableRow(m)
    val tr2 = mapper.readValue(mapper.writeValueAsString(tr1), classOf[TableRow])
    val rmt = RecordMatcherType[A]
    t.fromTableRow(tr2).exists(rmt(_, m))
  }

  property("required") = forAll { m: Required => roundTrip(m) }
  property("optional") = forAll { m: Optional => roundTrip(m) }
  property("repeated") = forAll { m: Repeated => roundTrip(m) }
  property("mixed") = forAll { m: Mixed => roundTrip(m) }
  property("nested") = forAll { m: Nested => roundTrip(m) }
  property("seq types") = forAll { m: SeqTypes => roundTrip(m) }

  val t = SerializableUtils.ensureSerializable(BigQueryType[Nested])
  property("serializable") = forAll { m: Nested => roundTrip(m, t) }

  implicit val arbDate = Arbitrary(Gen.const(LocalDate.now()))
  implicit val arbTime = Arbitrary(Gen.const(LocalTime.now()))
  implicit val arbDateTime = Arbitrary(Gen.const(LocalDateTime.now()))

  case class DateTimeTypes(instant: Instant,
                           date: LocalDate,
                           time: LocalTime,
                           dateTime: LocalDateTime)
  property("date time types") = forAll { m: DateTimeTypes => roundTrip(m) }

}
