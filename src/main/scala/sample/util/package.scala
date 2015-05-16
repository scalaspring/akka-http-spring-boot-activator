package sample

import java.io.StringReader

import com.github.tototoshi.csv.CSVReader

import scala.util.Random
import resource._

package object util {

  implicit class SeqOperations[T](seq: Seq[T]) {
    /** Selects a random element from a sequence. */
    def random: T = seq(Random.nextInt(seq.size))
  }

  def readCsvResource(resource: String): List[Map[String, String]] =
    managed(CSVReader.open(scala.io.Source.fromURL(getClass.getResource(resource)).bufferedReader()))
      .map(_.allWithHeaders).opt.get

  def parseCsvData(data: String): List[Map[String, String]] =
    managed(CSVReader.open(new StringReader(data)))
      .map(_.allWithHeaders).opt.get
}
