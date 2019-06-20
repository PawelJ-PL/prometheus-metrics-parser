package com.github.pawelj_pl.prometheus_metrics_parser

final case class MetricValue(labels: Map[String, String], value: Double, timestamp: Option[Long], modifier: Option[Modifier])

sealed trait Modifier extends Product with Serializable

object Modifier {
  case object Sum extends Modifier
  case object Count extends Modifier
  case object Bucket extends Modifier
}

sealed trait Metric extends Product with Serializable {
  def name: String
  def help: Option[String]
  def values: List[MetricValue]
}

object Metric {
  final case class Counter(name: String, help: Option[String], values: List[MetricValue]) extends Metric
  final case class Gauge(name: String, help: Option[String], values: List[MetricValue]) extends Metric
  final case class Histogram(name: String, help: Option[String], values: List[MetricValue], sums: List[MetricValue], counts: List[MetricValue]) extends Metric
  final case class Summary(name: String, help: Option[String], values: List[MetricValue], sums: List[MetricValue], counts: List[MetricValue]) extends Metric
  final case class Untyped(name: String, help: Option[String], values: List[MetricValue]) extends Metric
}
