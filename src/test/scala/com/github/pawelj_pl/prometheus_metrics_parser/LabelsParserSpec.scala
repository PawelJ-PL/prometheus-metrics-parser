package com.github.pawelj_pl.prometheus_metrics_parser

import com.github.pawelj_pl.prometheus_metrics_parser.Metric.Untyped
import com.github.pawelj_pl.prometheus_metrics_parser.parser.Parser
import org.scalatest.{EitherValues, Matchers, WordSpec}

class LabelsParserSpec extends WordSpec with Matchers with EitherValues {
  final val parser = Parser()

  "Labels" should {
    "Not be present if missing" in {
      val input = "some_metric 123"
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
    "Not be present if only brackets and spaces found" in {
      val input = "some_metric{   } 123"
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
    "Be present" in {
      val input = """some_metric{ x = "y", a="b", } 123"""
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map("x" -> "y", "a" -> "b"), 123D, None, None)))
      )
    }
    "Be present without labels with incorrect name" in {
      val input = """some_metric{x="y", "o@o"="aaa", a="b", vvv, q="w"} 123"""
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map("x" -> "y", "a" -> "b", "q" -> "w"), 123D, None, None)))
      )
    }
    "Be present with quotation marks in value if escaped" in {
      val input = """some_metric{x = "aa \"bb\" cc"} 123"""
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map("x" -> """aa "bb" cc"""), 123D, None, None)))
      )
    }
    "Be present with unescaped backslashes in value" in {
      val input = """some_metric{x="aa\\bb"} 123"""
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map("x" -> """aa\bb"""), 123D, None, None)))
      )
    }
    "Be present with unescaped new lines in value" in {
      val input = """some_metric{x="aa\nbb"} 123"""
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map("x" -> "aa\nbb"), 123D, None, None)))
      )
    }
  }
}
