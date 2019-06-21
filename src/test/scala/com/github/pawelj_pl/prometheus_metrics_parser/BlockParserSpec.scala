package com.github.pawelj_pl.prometheus_metrics_parser

import com.github.pawelj_pl.prometheus_metrics_parser.Metric.{Counter, Histogram, Summary, Untyped}
import com.github.pawelj_pl.prometheus_metrics_parser.parser.Parser
import org.scalatest.{EitherValues, Matchers, WordSpec}

class BlockParserSpec extends WordSpec with Matchers with EitherValues {
  final val parser = Parser()

  "Invalid or empty lines and comments" should {
    "Be ignored" in {
      val input =
        """
          |invalid line
          |
          |# some comment
          |some_metric 123
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric", None, List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
  }

  "_sum handler" should {
    "Should remove _sum suffix and add Sum modifier for Histogram" in {
      val input =
        """
          |# TYPE some_metric histogram
          |some_metric_sum 123
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Histogram("some_metric", None, values = List.empty, sums = List(MetricValue(Map.empty, 123D, None, Some(Modifier.Sum))), counts = List.empty)
      )
    }
    "Should remove _sum suffix and add Sum modifier for Summary" in {
      val input =
        """
          |# TYPE some_metric summary
          |some_metric_sum 123
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Summary("some_metric", None, values = List.empty, sums = List(MetricValue(Map.empty, 123D, None, Some(Modifier.Sum))), counts = List.empty)
      )
    }
    "Should treat sum as separate metric name for other types" in {
      val input =
        """
          |# TYPE some_metric counter
          |some_metric{quantile="0.01"} 123
          |some_metric_sum 999
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Counter("some_metric", None, List(MetricValue(Map("quantile" -> "0.01"), 123D, None, None))),
        Untyped("some_metric_sum", None, List(MetricValue(Map.empty, 999D, None, None)))
      )
    }
  }

  "_count handler" should {
    "Should remove _count suffix and add Count modifier for Histogram" in {
      val input =
        """
          |# TYPE some_metric histogram
          |some_metric_count 123
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Histogram("some_metric", None, values = List.empty, sums = List.empty, counts = List(MetricValue(Map.empty, 123D, None, Some(Modifier.Count))))
      )
    }
    "Should remove _count suffix and add Sum modifier for Summary" in {
      val input =
        """
          |# TYPE some_metric summary
          |some_metric_count 123
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Summary("some_metric", None, values = List.empty, sums = List.empty, counts = List(MetricValue(Map.empty, 123D, None, Some(Modifier.Count))))
      )
    }
    "Should treat count as separate metric name for other types" in {
      val input =
        """
          |# TYPE some_metric counter
          |some_metric{quantile="0.01"} 123
          |some_metric_count 999
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Counter("some_metric", None, List(MetricValue(Map("quantile" -> "0.01"), 123D, None, None))),
        Untyped("some_metric_count", None, List(MetricValue(Map.empty, 999D, None, None)))
      )
    }
  }

  "_bucket handler" should {
    "Remove _bucket suffix and add Bucket modifier for histogram" in {
      val input =
        """
          |# TYPE some_metric histogram
          |some_metric_bucket{le="0.1"} 123
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Histogram("some_metric", None, values = List(MetricValue(Map("le" -> "0.1"), 123D, None, Some(Modifier.Bucket))), sums = List.empty, counts = List.empty)
      )
    }
    "Treat _bucket as a part of metric for summary" in {
      val input =
        """
          |# TYPE some_metric_bucket summary
          |some_metric_bucket{quantile="0.05"} 123
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Summary("some_metric_bucket", None, List(MetricValue(Map("quantile" -> "0.05"), 123D, None, None)), List.empty, List.empty)
      )
    }
    "Treat _bucket as a part of metric for other types" in {
      val input =
        """
          |# TYPE some_metric counter
          |some_metric_bucket 123
        """.stripMargin
      val result = parser.parseE(input)
      result.right.value shouldBe List(
        Untyped("some_metric_bucket", None, List(MetricValue(Map.empty, 123D, None, None)))
      )
    }
  }
}
