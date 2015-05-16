package sample.quote

import java.time.{LocalDate, Period}

import akka.stream.scaladsl.Source

import scala.concurrent.Future

trait QuoteService {
  /**
   * Returns daily historical quotes for a specified trailing period, e.g. the past month.
   */
  def history(symbol: String, period: Period): Future[Option[Source[Quote, _]]]

  /**
   * Returns daily historical quotes for a specified date range.
   */
  def history(symbol: String, begin: LocalDate, end: LocalDate): Future[Option[Source[Quote, _]]]
}
