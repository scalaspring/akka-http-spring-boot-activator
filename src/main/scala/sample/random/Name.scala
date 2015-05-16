package sample.random

import sample.util._


case class Name(first: String, last: String)

object Name {

  lazy val firstNames: Map[Sex, Seq[String]] = readCsvResource("/random/user/first_names.csv")
    .partition({case r if r("sex") == "M" => true; case _ => false }) match {
      case (m, f) => Map(
        Male -> m.map(_("name").toLowerCase.capitalize),
        Female -> f.map(_("name").toLowerCase.capitalize)
      )
    }
  lazy val lastNames = readCsvResource("/random/user/last_names.csv").map(_("name").toLowerCase.capitalize)

  def random: Name = random(Sex.random)
  def random(sex: Sex) = new Name(first = firstNames(sex).random, last = lastNames.random)

}
