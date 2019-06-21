package com.github.pawelj_pl.prometheus_metrics_parser

import com.github.pawelj_pl.prometheus_metrics_parser.Metric.Counter
import com.github.pawelj_pl.prometheus_metrics_parser.parser.{ParseError, Parser}
import org.scalatest.{EitherValues, Matchers, WordSpec}

class ParseErrorSpec extends WordSpec with Matchers with EitherValues {
  final val parser = Parser()

  "MultipleHelpLines" should {
    "Be present when multiple help lines exists for single metric" in {
      val input =
        """
          |# HELP some_metric some help
          |# HELP some_metric different help
          |some_metric 123
        """.stripMargin
      val result = parser.parseE(input)
      result.left.value shouldBe ParseError.MultipleHelpLines("different help")
    }
  }

  "MultipleTypeLines" should {
    "Be present when multiple type lines exists for single metric" in {
      val input =
        """
          |# TYPE some_metric counter
          |# TYPE some_metric gauge
          |some_metric 123
        """.stripMargin
      val result = parser.parseE(input)
      result.left.value shouldBe ParseError.MultipleTypeLines("gauge")
    }
  }

  "TypeNotAtTheBeginning" should {
    "Be present when metric value recorded before type line" in {
      val input =
        """
          |some_metric 123
          |# TYPE some_metric counter
        """.stripMargin
      val result = parser.parseE(input)
      result.left.value shouldBe ParseError.TypeNotAtTheBeginning
    }
    "Not be present when only help and comment recorded before type line" in {
      val input =
        """
          |# HELP some_metric some help
          |# some comment
          |# TYPE some_metric counter
          |some_metric 123
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Counter("some_metric", Some("some help"), List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
  }

  "MultipleGroupsOfMetrics" should {
    "Be present if metric exists in multiple groups" in {
      val input =
        """
          |some_metric 123
          |other_metric 234
          |some_metric 345
        """.stripMargin
      val result = parser.parseE(input)
      result.left.value shouldBe ParseError.MultipleGroupsOfMetrics(Seq("some_metric"))
    }
  }

  "DuplicatedLabels" should {
    "Be present on multiple occurrences of metric with the same labels" in {
      val input =
        """
          |some_metric{a="b", x="y"} 123
          |some_metric{a="c", x="z"} 999
          |some_metric{a="b", x="y"} 765
        """.stripMargin
      val result = parser.parseE(input)
      result.left.value shouldBe ParseError.DuplicatedLabels("some_metric", Seq(Map("a" -> "b", "x" -> "y")))
    }
  }
}
