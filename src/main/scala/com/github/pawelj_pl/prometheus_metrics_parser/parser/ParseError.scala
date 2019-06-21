package com.github.pawelj_pl.prometheus_metrics_parser.parser

case class ParseException(error: ParseError) extends Exception(error.toString)

sealed trait ParseError extends Product with Serializable

object ParseError {
  case class MultipleHelpLines(content: String) extends ParseError
  case class MultipleTypeLines(metricsType: String) extends ParseError
  case object TypeNotAtTheBeginning extends ParseError
  case class MultipleGroupsOfMetrics(names: Seq[String]) extends ParseError
  case class DuplicatedLabels(metricName: String, labels: Seq[Map[String, String]]) extends ParseError
}
