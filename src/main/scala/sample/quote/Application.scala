package sample.quote

import java.time.Period

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.stream.scaladsl._
import akka.util.ByteString
import com.github.scalaspring.akka.http.{AkkaHttpServer, AkkaHttpServerAutoConfiguration, AkkaHttpService}
import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import
import sample.quote.Bollinger._
import sample.quote.BollingerQuoteService._
import sample.quote.stage._

import scala.collection.immutable

/**
 * Defines the Bollinger quote service route used to handle requests.
 */
trait BollingerQuoteService extends AkkaHttpService with StrictLogging {

  @Autowired val quoteService: QuoteService = null

  abstract override def route = {
    get {
      path("quote" / Segment) { symbol =>
        parameters('months.as[Int] ? 6) { months =>
          val period = Period.ofMonths(months)
          complete {
            quoteService.history(symbol, period).map[ToResponseMarshallable] {
              case Some(s) => HttpEntity.Chunked.fromData(ContentTypes.`text/plain`, s.via(responseFlow))
              case None => NotFound -> s"No data found for the given symbol '$symbol' or period $period"
            }
          }
        }
      }
    }
  } ~ super.route

}

object BollingerQuoteService {

  val defaultWindow = 14

  // The composite flow that adds bollinger points, renders CSV output, and chunks rows
  def responseFlow = Flow[Quote].via(bollinger()).via(csv).via(chunkRows())

  // Calculates and adds Bollinger points to a Quote stream
  // Note: Assumes quotes are in descending date order, as provided by Yahoo Finance
  // Note: The number of quotes emitted will be reduced by the window size - 1 (dropped from the tail)
  def bollinger(window: Int = defaultWindow) = Flow() { implicit b =>

    val in = b.add(Broadcast[Quote](2))
    val extract = b.add(Flow[Quote].map(_("Close").toDouble))
    val statistics = b.add(Flow[Double].slidingStatistics(window))
    val bollinger = b.add(Flow[Statistics[Double]].map(Bollinger(_)))
    // Need buffer to avoid deadlock. See: https://github.com/akka/akka/issues/17435
    val buffer = b.add(Flow[Quote].buffer(window, OverflowStrategy.backpressure))
    val zip = b.add(Zip[Quote, Bollinger])
    val merge = b.add(Flow[(Quote, Bollinger)].map(t => t._1 ++ t._2))

    in ~> buffer                             ~> zip.in0
    in ~> extract ~> statistics ~> bollinger ~> zip.in1
    zip.out ~> merge

    (in.in, merge.outlet)
  }

  // Converts quotes to CSV with header
  val csv = Flow() { implicit b =>
    def format(q: Quote, header: Boolean): String = if (header) q.keys.mkString(",") else "\n" + q.values.mkString(",")
    val rows = b.add(
      Flow[Quote].prefixAndTail(1).mapConcat[Source[String, Unit]]{ pt =>
        val (prefix, tail) = pt
        immutable.Seq(
          Source(prefix).map(format(_, true)),
          Source(prefix).map(format(_, false)),
          tail.map(format(_, false)))
      }.flatten(FlattenStrategy.concat))

    (rows.inlet, rows.outlet)
  }

  // Chunks output into batches of rows
  def chunkRows(count: Int = 100) = Flow[String].grouped(count).map(_.foldLeft(ByteString())((bs, s) => bs ++ ByteString(s)))

}

@SpringBootApplication
@Import(Array(classOf[AkkaHttpServerAutoConfiguration]))
class Application extends AkkaHttpServer with BollingerQuoteService

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
