package sample.random

import sample.util._


case class Credentials(username: String, password: String)

object Credentials {
  def apply(name: Name): Credentials = Credentials(username = name.first + name.last, password = (name.first + name.last).reverse + "1")
}


case class User(
               name: Name,
               credentials: Credentials,
               sex: Sex,
               email: String,
               creditCard: CreditCard,
               height: Int,
               weight: Int
)

object User {

  // See: https://signup.weightwatchers.com/util/sig/healthy_weight_ranges_pop.aspx
  // Inches -> Pounds
  val weightRanges = Map[Int, Range](
    56 -> (89 to 112),
    57 -> (92 to 116),
    58 -> (96 to 120),
    59 -> (99 to 124),
    60 -> (102 to 128),
    61 -> (106 to 132),
    62 -> (109 to 137),
    63 -> (113 to 141),
    64 -> (117 to 146),
    65 -> (120 to 150),
    66 -> (124 to 155),
    67 -> (128 to 160),
    68 -> (132 to 164),
    69 -> (135 to 169),
    70 -> (139 to 174),
    71 -> (143 to 179),
    72 -> (147 to 184),
    73 -> (152 to 189),
    74 -> (156 to 195),
    75 -> (160 to 200),
    76 -> (164 to 205),
    77 -> (169 to 211),
    78 -> (173 to 216),
    79 -> (178 to 222)
  )

  val heights = weightRanges.keySet.toIndexedSeq.sorted

  lazy val domains = readCsvResource("/random/user/domains.csv").map(_("domain"))

  protected def randomEmail(name: Name) = s"${name.first}.${name.last}@${domains.random}".toLowerCase

  def random: User = {
    val sex = Sex.random
    val name = Name.random(sex)
    val height = heights.random
    new User(
      name = name,
      credentials = Credentials(name),
      sex = sex,
      email = randomEmail(name),
      creditCard = CreditCard.random,
      height = height,
      weight = weightRanges(height).random
    )
  }

}