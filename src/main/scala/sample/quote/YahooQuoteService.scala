package sample.quote

import java.io.IOException
import java.net.URL
import java.time.{LocalDate, Period}

import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.stream.scaladsl._
import akka.util.ByteString
import com.github.scalaspring.akka.http.{AkkaStreamsAutowiredImplicits, HttpClient, AkkaHttpClient}
import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Component
import sample.quote.stage.ParseRecord

import scala.concurrent.Future


/**
 * Retrieves historical stock quotes from Yahoo Finance.
 */
@Component
class YahooQuoteService extends QuoteService with AkkaStreamsAutowiredImplicits with StrictLogging {

  @Autowired val client: HttpClient = null

  val baseUri = Uri("http://real-chart.finance.yahoo.com/table.csv")

  protected def params(symbol: String, begin: LocalDate, end: LocalDate) =
    Map[String, String](
      "s" -> symbol, "g" -> "d",
      "a" -> (begin.getMonthValue - 1).toString, "b" -> begin.getDayOfMonth.toString, "c" -> begin.getYear.toString,
      "d" -> (end.getMonthValue - 1).toString, "e" -> end.getDayOfMonth.toString, "f" -> end.getYear.toString
    )

  // Converts a ByteString stream into a Quote stream
  val parseResponse = Flow() { implicit b =>
    val records = b.add(Flow[ByteString].transform[String](() => ParseRecord()).map(_.trim.split(',')))
    val zipHeader = b.add(Flow[Array[String]].prefixAndTail(1).map(pt => pt._2.map((pt._1.head, _))).flatten(FlattenStrategy.concat))
    val quote = b.add(Flow[(Array[String], Array[String])].map(t => t._1.zip(t._2).foldLeft(Quote())((q, t) => q += t)))

    records ~> zipHeader ~> quote

    (records.inlet, quote.outlet)
  }

  override def history(symbol: String, period: Period): Future[Option[Source[Quote, _]]] = history(symbol, LocalDate.now.minus(period), LocalDate.now)

  override def history(symbol: String, begin: LocalDate, end: LocalDate): Future[Option[Source[Quote, _]]] = {
    require(end.isAfter(begin) || end.isEqual(begin), "invalid date range - end date must be on or after begin date")

    val uri = baseUri.withQuery(params(symbol, begin, end))

    logger.info(s"Sending request for $uri")

    client.request(RequestBuilding.Get(uri)).flatMap { response =>
      logger.info(s"Received response with status ${response.status} from $uri")
      response.status match {
        case OK => Future.successful(Some(response.entity.dataBytes.via(parseResponse)))
        case NotFound => Future.successful(None)
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"Request to $uri failed with status code ${response.status}"
          logger.error(error)
          Future.failed(new IOException(error))
        }
      }
    }
  }

}
