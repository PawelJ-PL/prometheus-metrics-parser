package com.github.pawelj_pl.prometheus_metrics_parser

import com.github.pawelj_pl.prometheus_metrics_parser.Metric.{Counter, Untyped}
import com.github.pawelj_pl.prometheus_metrics_parser.parser.Parser
import org.scalatest.{EitherValues, Matchers, OptionValues, WordSpec}

class LineParserSpec extends WordSpec with Matchers with EitherValues with OptionValues {
  final val parser = Parser()

  "Type line" should {
    "Be present in line with single spaces" in {
      val input =
        """
          |# TYPE some_metric counter
          |some_metric 123
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Counter("some_metric", None, List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
    "Be present in line with multiple spaces" in {
      val input =
        """
          |   # TYPE    some_metric   counter
          |some_metric 123
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Counter("some_metric", None, List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
  }

  "Help line" should {
    "Be present in line with single spaces" in {
      val input =
        """
          |# HELP some_metric Some help message
          |some_metric 123
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", Some("Some help message"), List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
    "Be present in line with multiple spaces" in {
      val input =
        """
          |   #    HELP    some_metric    Some help message
          |some_metric 123
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", Some("Some help message"), List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
    "Be present with unescaped backslashes" in {
      val input =
        """
          |# HELP some_metric Some \\help\\ message
          |some_metric 123
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", Some("""Some \help\ message"""), List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
    "Be present with unescaped new lines" in {
      val input =
        """
          |# HELP some_metric Some help\n message
          |some_metric 123
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", Some("Some help\n message"), List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
  }

  "Metric line" should {
    "Be present for simple metric" in {
      val input = "some_metric 123"
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
    "Be present for metric with empty labels" in {
      val input = "some_metric{} 123"
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
    "Be present for metric with labels" in {
      val input = """some_metric{foo="bar", baz="qux"} 123"""
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map("foo" -> "bar", "baz" -> "qux"), 123D, None, None)))
      )
    }
    "Be present for metric with timestamp" in {
      val input = "some_metric 123 999"
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map.empty, 123D, Some(999L), None)))
      )
    }
    "Be present for metric with labels and timestamp" in {
      val input = """some_metric{foo="bar", baz="qux"} 123 999"""
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map("foo" -> "bar", "baz" -> "qux"), 123D, Some(999L), None)))
      )
    }
    "Be present for metric with labels and timestamp with multiple spaces" in {
      val input = """some_metric{foo="bar", baz="qux"}   123    999"""
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map("foo" -> "bar", "baz" -> "qux"), 123D, Some(999L), None)))
      )
    }
    "Not be present for invalid metric name" in {
      val input = "some@_metric 123"
      val result = parser.parseE(input)
      result.right.value shouldBe List.empty
    }
    "Be present without timestamp if timestamp has invalid format" in {
      val input = "some_metric 123 12x"
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
    "Not be present for invalid value format" in {
      val input = "some_metric 123x"
      val result = parser.parseE(input)
      result.right.value shouldBe List.empty
    }
    "Be present for NaN value" in {
      val input = "some_metric Nan"
      val result = parser.parseE(input)
      result.right.value.headOption.value.values.headOption.value.value.isNaN shouldBe true
    }
    "Be present for +Inf value" in {
      val input = "some_metric +Inf"
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map.empty, Double.PositiveInfinity, None, None)))
      )
    }
    "Be present for -Inf value" in {
      val input = "some_metric -Inf"
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map.empty, Double.NegativeInfinity, None, None)))
      )
    }
  }
}
