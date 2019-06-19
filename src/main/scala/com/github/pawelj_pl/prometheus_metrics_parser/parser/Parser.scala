package com.github.pawelj_pl.prometheus_metrics_parser.parser

import com.github.pawelj_pl.prometheus_metrics_parser.{Metric, MetricValue}

import scala.annotation.tailrec
import scala.util.Try

class Parser {
  def parseE(rawMetrics: String): Either[ParseError, List[Metric]] = {
    parseRest(rawMetrics.split("\n").toList)
  }

  @tailrec
  private def parseRest(restLines: List[String],
                parsed: List[Option[Metric]] = List.empty)
    : Either[ParseError, List[Metric]] = restLines match {
    case Nil => Right(parsed.flatten)
    case _ =>
      parseBlock(restLines) match {
        case Left(err) => Left(err)
        case Right((maybeMetric, rest)) =>
          parseRest(rest, parsed :+ maybeMetric)
      }
  }

  @tailrec
  private def parseBlock(blockLines: List[String],
                         parsedLines: List[Line] = List.empty,
                         currentMetric: Option[String] = None)
    : Either[ParseError, (Option[Metric], List[String])] = blockLines match {
    case Nil => Right(buildMetric(parsedLines), List.empty)
    case head :: tail =>
      parseLine(head) match {
        case _ @(_: Line.Invalid | _: Line.Comment | _: Line.Empty.type) =>
          parseBlock(tail, parsedLines, currentMetric) //Ignore invalid or empty lines and comments
        case line @ Line.Help(name, content) =>
          if (currentMetric.exists(_ != name))
            Right(buildMetric(parsedLines), head :: tail) // Finish block if new metric found
          else if (parsedLines.exists(l => l.isInstanceOf[Line.Comment]))
            Left(ParseError.MultipleHelpLines(content)) // Fail on duplicate help
          else parseBlock(tail, parsedLines :+ line, Some(name))
        case line @ Line.Type(name, metricsType) =>
          if (currentMetric.exists(_ != name))
            Right(buildMetric(parsedLines), head :: tail) // Finish block if new metric found
          else if (parsedLines.exists(l => l.isInstanceOf[Line.Type]))
            Left(ParseError.MultipleTypeLines(metricsType)) // Fail on duplicate type
          else if (parsedLines.exists(l => l.isInstanceOf[Line.Metric]))
            Left(ParseError.TypeNotInTheBeginning) // Fail if type not in the beginning of block
          else parseBlock(tail, parsedLines :+ line, Some(name))
        case line @ Line.Metric(name, labels, value, timestamp, _) =>
          val currentMetricType: Option[MetricsType] = parsedLines.find(_.isInstanceOf[Line.Type]).map(_.asInstanceOf[Line.Type].metricsType)
          if (currentMetricType.exists(t => t == MetricsType.Histogram || t == MetricsType.Summary) && currentMetric.exists(_ + "_sum" == name)) { // Handle Histogram and Summary _sum
            val updatedName = removeSuffix(name, "_sum")
            val updatedLine = Line.Metric(updatedName, labels, value, timestamp, Some(Modifier.Sum))
            parseBlock(tail, parsedLines :+ updatedLine, Some(updatedName))
          }
          else if (currentMetricType.exists(t => t == MetricsType.Histogram || t == MetricsType.Summary) && currentMetric.exists(_ + "_count" == name)) { // Handle Histogram and Summary _count
            val updatedName = removeSuffix(name, "_count")
            val updatedLine = Line.Metric(updatedName, labels, value, timestamp, Some(Modifier.Count))
            parseBlock(tail, parsedLines :+ updatedLine, Some(updatedName))
          }
          else if (currentMetricType.contains(MetricsType.Histogram) && currentMetric.exists(_ + "_bucket" == name)) { // Handle Histogram _bucket
            val updatedName = removeSuffix(name, "_bucket")
            val updatedLine = Line.Metric(updatedName, labels, value, timestamp, Some(Modifier.Bucket))
            parseBlock(tail, parsedLines :+ updatedLine, Some(updatedName))
          }
          else if (currentMetric.exists(_ != name))
            Right(buildMetric(parsedLines), head :: tail) // Finish block if new metric found
          else parseBlock(tail, parsedLines :+ line, Some(name))
      }
  }

  private def parseLine(line: String): Line = {
    val emptyPattern = "^$".r
    val typePattern = "^# TYPE (.+?) (.+?)$".r
    val helpPattern = "^# HELP (.+?) (.+?)$".r
    val commentPattern = "^# (?!HELP|TYPE.*$)(.*)".r
    val metricPattern = "^(.*?)((?:\\{.*?\\})?)?\\s+(.*?)(\\s.+?)?$".r

    line.trim match {
      case emptyPattern() => Line.Empty
      case typePattern(name, metricType) =>
        Line.Type(name, MetricsType.of(metricType))
      case helpPattern(name, help) => Line.Help(name, help)
      case commentPattern(content) => Line.Comment(content)
      case line @ metricPattern(name, labels, value, timestamp) =>
        val maybeMetric = for {
          l <- extractLabels(labels)
          v <- parseValue(value)
        } yield Line.Metric(name, l, v, parseTimestamp(timestamp), None)
        maybeMetric.getOrElse(Line.Invalid(line))
      case l => Line.Invalid(l)
    }
  }

  private def extractLabels(fragment: String): Option[Map[String, String]] =
    Some(Map.empty) //FIXME

  private def parseValue(fragment: String): Option[Double] =
    fragment.trim match {
      case "Nan"  => Some(Double.NaN)
      case "+Inf" => Some(Double.PositiveInfinity)
      case "-Inf" => Some(Double.NegativeInfinity)
      case value  => Try(value.toDouble).toOption
    }

  private def parseTimestamp(fragment: String): Option[Long] = {
    Try(fragment.trim.toLong).toOption
  }

  private def buildMetric(parsedLines: List[Line]): Option[Metric] = {
    parsedLines
      .find(_.isInstanceOf[Line.Metric])
      .map(_.asInstanceOf[Line.Metric].name)
      .map { name =>
        {
          val metricType: MetricsType = parsedLines
            .find(_.isInstanceOf[Line.Type])
            .map(_.asInstanceOf[Line.Type].metricsType)
            .getOrElse(MetricsType.Untyped)

          val maybeHelp: Option[String] = parsedLines
            .find(_.isInstanceOf[Line.Help])
            .map(_.asInstanceOf[Line.Help].content)

          metricType match {
            case MetricsType.Counter =>
              Metric.Counter(name,
                             maybeHelp,
                             extractValuesFromLines(parsedLines))
            case MetricsType.Gauge =>
              Metric.Gauge(name, maybeHelp, extractValuesFromLines(parsedLines))
            case MetricsType.Summary =>
              Metric.Summary(name,
                             maybeHelp,
                             extractValuesFromLines(parsedLines, Some(l => l.modifier.isEmpty)),
                             extractValuesFromLines(parsedLines, Some(l => l.modifier.contains(Modifier.Sum))),
                             extractValuesFromLines(parsedLines, Some(l => l.modifier.contains(Modifier.Count))))
            case MetricsType.Histogram =>
              Metric.Histogram(name,
                               maybeHelp,
                               extractValuesFromLines(parsedLines, Some(l => l.modifier.isEmpty)),
                               extractValuesFromLines(parsedLines, Some(l => l.modifier.contains(Modifier.Sum))),
                               extractValuesFromLines(parsedLines, Some(l => l.modifier.contains(Modifier.Count))))
            case MetricsType.Untyped =>
              Metric.Untyped(name,
                             maybeHelp,
                             extractValuesFromLines(parsedLines))
          }
        }
      }
  }

  private def extractValuesFromLines(lines: List[Line], predicate: Option[Line.Metric => Boolean] = None): List[MetricValue] =
    lines
      .filter(_.isInstanceOf[Line.Metric])
      .map(_.asInstanceOf[Line.Metric])
      .filter(predicate.getOrElse(_ => true))
      .map(l => MetricValue(l.labels, l.value, l.timestamp))

  private def removeSuffix(base: String, suffix: String) = base.dropRight(suffix.length)

  def parseOpt(rawMetrics: String): Option[List[Metric]] =
    parseE(rawMetrics) match {
      case Left(_)       => None
      case Right(result) => Some(result)
    }

  def unsafeParse(rawMetrics: String): List[Metric] =
    parseE(rawMetrics) match {
      case Left(err)     => throw ParseException(err)
      case Right(result) => result
    }
}

object Parser {
  def apply(): Parser = new Parser
}