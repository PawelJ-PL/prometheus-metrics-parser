package com.github.pawelj_pl.prometheus_metrics_parser

import com.github.pawelj_pl.prometheus_metrics_parser.Metric.Untyped
import com.github.pawelj_pl.prometheus_metrics_parser.parser.{ParseError, ParseException, Parser}
import org.scalatest.{EitherValues, Matchers, OptionValues, WordSpec}

class BaseResponseSpec extends WordSpec with Matchers with EitherValues with OptionValues {
  final val parser = Parser()

  "parseE" should {
    "Return Right with metrics" in {
      val input = "some_metric 123"
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
    "Return Left with error" in {
      val input =
        """
          |some_metric 123
          |some_metric 987
        """.stripMargin
      val result = parser.parseE(input)
      result.left.value shouldBe ParseError.DuplicatedLabels("some_metric", List(Map.empty))
    }
  }

  "parseOpt" should {
    "Return Right with metrics" in {
      val input = "some_metric 123"
      val result = parser.parseOpt(input)
      result.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
    "Return Left with error" in {
      val input =
        """
          |some_metric 123
          |some_metric 987
        """.stripMargin
      val result = parser.parseOpt(input)
      result shouldBe None
    }
  }

  "unsafeParse" should {
    "Return Right with metrics" in {
      val input = "some_metric 123"
      val result = parser.unsafeParse(input)
      result shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
    "Return Left with error" in {
      val input =
        """
          |some_metric 123
          |some_metric 987
        """.stripMargin
      the [ParseException] thrownBy parser.unsafeParse(input) shouldBe ParseException(ParseError.DuplicatedLabels("some_metric", List(Map.empty)))

    }
  }
}
