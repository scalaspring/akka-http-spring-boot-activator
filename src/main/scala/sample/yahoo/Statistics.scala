package sample.yahoo

case class Statistics[T] private (population: Boolean, M: Double, S: Double, k: Int)(implicit num: Numeric[T]) {

  def apply(values: Iterator[T]): Statistics[T] = values.foldLeft(this)((s, x) => s(x))

  // See: http://stackoverflow.com/questions/895929/how-do-i-determine-the-standard-deviation-stddev-of-a-set-of-values
  def apply(value: T): Statistics[T] = {
    val x = num.toDouble(value)
    val newM = M + (x - M) / k
    val newS = S + (x - newM) * (x - M)
    val newK = k + 1

    copy(M = newM, S = newS, k = newK)
  }

  def mean: Double = M
  def variance: Double = if (k <= 2) 0 else if (population) (S / (k-1)) else (S / (k-2))
  def stddev: Double = Math.sqrt(variance)

}

object Statistics {
  def apply[T](values: Iterator[T] = Iterator.empty)(implicit num: Numeric[T]) =
    (new Statistics[T](false, 0, 0, 1)(num))(values)
  def apply[T](values: Iterable[T])(implicit num: Numeric[T]) =
    (new Statistics[T](false, 0, 0, 1)(num))(values.iterator)
}