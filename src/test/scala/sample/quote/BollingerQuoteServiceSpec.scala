package sample.quote

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.scalaspring.akka.http.{AkkaHttpClient, AkkaStreamsAutowiredImplicits, AkkaStreamsAutoConfiguration, HttpClient}
import com.github.scalaspring.scalatest.TestContextManagement
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.context.annotation.{Bean, ComponentScan, Configuration, Import}
import org.springframework.test.context.ContextConfiguration
import sample.util._

import scala.concurrent.Future
import scala.concurrent.duration._

@Configuration
@ContextConfiguration(
  loader = classOf[SpringApplicationContextLoader],
  classes = Array(classOf[BollingerQuoteServiceSpec.Configuration])
)
class BollingerQuoteServiceSpec extends FlatSpec with TestContextManagement with BollingerQuoteService with ScalatestRouteTest with Matchers with ScalaFutures with StrictLogging {

  import BollingerQuoteServiceSpec._

  // Yahoo takes more than a second to respond
  implicit val patience = PatienceConfig((10.seconds))

  "Bollinger quote service" should "add bollinger points to quote data" in {
    Get(s"/quote/yhoo") ~> route ~> check {
      status shouldBe OK
      val quotes = parseCsvData(entityAs[String])
      quotes should have size testData.size - BollingerQuoteService.defaultWindow
      quotes.zip(testData).foreach{ t =>
        val (actual, expected) = t

        actual.keySet shouldBe expected.keySet
        actual.keySet.foreach { k =>
          try {
            actual(k).toDouble shouldBe expected(k).toDouble +- 0.01
          } catch {
            case e: NumberFormatException => actual(k) shouldBe expected(k)
          }
        }
      }
    }
  }

  it should "return NotFound for bad symbol" in {
    Get(s"/quote/notfound") ~> route ~> check {
      status shouldBe NotFound
    }
  }

}

object BollingerQuoteServiceSpec {

  // Read test data and map columns
  val testData = readCsvResource("/bollinger_test_data.csv").map(_.collect {
      case ("BB Upper(14)", v) => ("BB Upper", v)
      case ("SMA(14)", v) =>      ("BB Middle", v)
      case ("BB Lower(14)", v) => ("BB Lower", v)
      case (k, v) if k != "Std Dev(14)" => (k, v)
    })

  val testDataCsv = (Seq(testData.head.keys.mkString(",")) ++ testData.map(_.values.mkString(","))).mkString("\n")

  @Configuration
  @Import(Array(classOf[AkkaStreamsAutoConfiguration]))
  class Configuration extends AkkaStreamsAutowiredImplicits with StrictLogging {

    // Provide HTTP client that returns known test data
    @Bean
    def httpClient: HttpClient = new HttpClient {
      override def request(request: HttpRequest): Future[HttpResponse] = {
        logger.info(s"Received request $request")
        val symbol = request.uri.query.toMap("s").toUpperCase
        symbol match {
          case "YHOO" => Future.successful(HttpResponse(status = OK, entity = testDataCsv))
          case "NOTFOUND" => Future.successful(HttpResponse(status = NotFound))
          case _ => Future.successful(HttpResponse(status = BadRequest))
        }
      }
    }

    @Bean
    def quoteService = new YahooQuoteService()

  }
}
