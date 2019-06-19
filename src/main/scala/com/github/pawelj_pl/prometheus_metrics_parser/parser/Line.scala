package com.github.pawelj_pl.prometheus_metrics_parser.parser

private[parser] sealed trait Line extends Product with Serializable

private[parser] object Line {
  case class Invalid(line: String) extends Line
  case object Empty extends Line
  case class Comment(content: String) extends Line
  case class Help(name: String, content: String) extends Line
  case class Type(name: String, metricsType: MetricsType) extends Line
  case class Metric(name: String, labels: Map[String, String], value: Double, timestamp: Option[Long], modifier: Option[Modifier]) extends Line
}

private[parser] sealed trait MetricsType extends Product with Serializable

private[parser] object MetricsType {
  case object Counter extends MetricsType
  case object Gauge extends MetricsType
  case object Histogram extends MetricsType
  case object Summary extends MetricsType
  case object Untyped extends MetricsType

  def of(stringType: String): MetricsType = stringType.toLowerCase match {
    case "counter"   => Counter
    case "gauge"     => Gauge
    case "histogram" => Histogram
    case "summary"   => Summary
    case _           => Untyped
  }
}

private[parser] sealed trait Modifier extends Product with Serializable

private[parser] object Modifier {
  case object Sum extends Modifier
  case object Count extends Modifier
  case object Bucket extends Modifier
}