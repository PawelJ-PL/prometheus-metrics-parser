package com.github.pawelj_pl.prometheus_metrics_parser

final case class MetricValue(labels: Map[String, String], value: Double, timestamp: Option[Long], modifier: Option[Modifier])

sealed trait Modifier extends Product with Serializable {
  def suffix: String
}

object Modifier {
  final case object Sum extends Modifier {
    override def suffix: String = "_sum"
  }
  final case object Count extends Modifier {
    override def suffix: String = "_count"
  }
  final case object Bucket extends Modifier {
    override def suffix: String = "_bucket"
  }
}

sealed abstract class Metric extends Product with Serializable {
  def name: String
  def help: Option[String]
  def values: List[MetricValue]
  def metricType: String
  def render: String =
    s"$renderHelpWithNewLine$renderType\n$renderValues"

  private def renderHelpWithNewLine: String = help.map(h => s"# HELP $name ${escapeChars(h, includeQuotationMarks = false)}\n").getOrElse("")
  private def renderType: String = s"# TYPE $name $metricType"
  private def escapeChars(input: String, includeQuotationMarks: Boolean): String = {
    val base = input
      .replace("\\", "\\\\") //escape backslashes
      .replace("\n", "\\n") // escape new lines
    if (includeQuotationMarks) base.replace("\"", "\\\"")
    else base
  }
  private def labelsAsString(labels: Map[String, String]): String = {
    if (labels.isEmpty) ""
    else s"{${labels.map(pair => s"""${pair._1}="${escapeChars(pair._2, includeQuotationMarks = true)}"""").mkString(", ")}}"
  }
  private def renderDoubleValue(value: Double): String = value match {
    case Double.NegativeInfinity => "-Inf"
    case Double.PositiveInfinity => "+Inf"
    case v if v.isNaN            => "Nan"
    case v                       => BigDecimal(v).bigDecimal.stripTrailingZeros().toString
  }
  private def renderSingleValue(metricValue: MetricValue): String = {
    val suffix: String = metricValue.modifier.map(_.suffix).getOrElse("")
    s"$name$suffix${labelsAsString(metricValue.labels)} ${renderDoubleValue(metricValue.value)}${metricValue.timestamp.map(t => s" $t").getOrElse("")}"
  }
  protected  def valuesToRender: List[MetricValue] = values
  private def renderValues: String =
    s"${valuesToRender.map(renderSingleValue).mkString("\n")}"
}

object Metric {
  final case class Counter(name: String, help: Option[String], values: List[MetricValue]) extends Metric {
    override def metricType: String = "counter"
  }
  final case class Gauge(name: String, help: Option[String], values: List[MetricValue]) extends Metric {
    override def metricType: String = "gauge"
  }
  final case class Histogram(name: String, help: Option[String], values: List[MetricValue], sums: List[MetricValue], counts: List[MetricValue]) extends Metric {
    override def metricType: String = "histogram"
    override def valuesToRender: List[MetricValue] = values ++ sums ++ counts
  }
  final case class Summary(name: String, help: Option[String], values: List[MetricValue], sums: List[MetricValue], counts: List[MetricValue]) extends Metric {
    override def metricType: String = "summary"
    override def valuesToRender: List[MetricValue] = values ++ sums ++ counts
  }
  final case class Untyped(name: String, help: Option[String], values: List[MetricValue]) extends Metric {
    override def metricType: String = "untyped"
  }
}
