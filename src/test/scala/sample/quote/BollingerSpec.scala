package sample.quote

import akka.stream.scaladsl._
import com.github.scalaspring.akka.http.{AkkaStreamsAutowiredImplicits, AkkaStreamsAutoConfiguration}
import com.github.scalaspring.scalatest.TestContextManagement
import com.typesafe.scalalogging.StrictLogging
import org.scalactic.Tolerance._
import org.scalactic.TripleEqualsSupport.Spread
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.context.annotation.{Configuration, Import}
import org.springframework.test.context.ContextConfiguration
import sample.util
import sample.quote.BollingerSpec.Expected
import sample.quote.stage._

@Configuration
@ContextConfiguration(classes = Array(classOf[BollingerSpec]))
@Import(Array(classOf[AkkaStreamsAutoConfiguration]))
class BollingerSpec extends FlatSpec with TestContextManagement with AkkaStreamsAutowiredImplicits with Matchers with ScalaFutures with StrictLogging {

  "Bollinger stage" should "properly calculate bands" in {
    val window = 14
    val input: List[Double] = util.readCsvResource("/bollinger_test_data.csv").map(_("Close").toDouble)
    val expected: List[Expected] = util.readCsvResource("/bollinger_test_data.csv").toList.dropRight(window - 1).map(r => Expected(r("BB Lower(14)").toDouble, r("SMA(14)").toDouble, r("BB Upper(14)").toDouble))

    val bollingerFlow = Flow[Double].slidingStatistics(window).map(Bollinger(_))

    val future = Source(input).via(bollingerFlow).runWith(Sink.fold(List[Bollinger]())(_ :+ _))
    whenReady(future) { result =>

      result should have size expected.size

      result.zip(expected).foreach { pair =>
        val (bollinger, expected) = pair

        bollinger.lower shouldBe expected.lower
        bollinger.middle shouldBe expected.middle
        bollinger.upper shouldBe expected.upper

      }
    }
  }

}

object BollingerSpec {
  case class Expected(lower: Spread[Double], middle: Spread[Double], upper: Spread[Double])
  object Expected {
    def apply(lower: Double, middle: Double, upper: Double, tolerance: Double = 0.001) =
      new Expected(lower +- tolerance, middle +- tolerance, upper +- tolerance)
  }
}