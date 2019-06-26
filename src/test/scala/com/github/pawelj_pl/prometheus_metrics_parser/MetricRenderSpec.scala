package com.github.pawelj_pl.prometheus_metrics_parser

import com.github.pawelj_pl.prometheus_metrics_parser.Metric.{Counter, Gauge, Histogram, Summary, Untyped}
import com.github.pawelj_pl.prometheus_metrics_parser.parser.Parser
import org.scalatest.{EitherValues, Matchers, WordSpec}

class MetricRenderSpec extends WordSpec with Matchers with EitherValues {
  "Metric" should {
    "Be rendered" in {
      val input = Counter("some_metric", None, List(MetricValue(Map.empty, 123D, None, None)))
      val result = input.render
      result shouldBe
        """# TYPE some_metric counter
          |some_metric 123""".stripMargin
    }
    "Be rendered with help" in {
      val input = Counter("some_metric", Some("Some help"), List(MetricValue(Map.empty, 123D, None, None)))
      val result = input.render
      result shouldBe
        """# HELP some_metric Some help
          |# TYPE some_metric counter
          |some_metric 123""".stripMargin
    }
    "Be rendered with timestamp" in {
      val input = Counter("some_metric", None, List(MetricValue(Map.empty, 123D, Some(999L), None)))
      val result = input.render
      result shouldBe
        """# TYPE some_metric counter
          |some_metric 123 999""".stripMargin
    }
    "Be rendered with decimal part" in {
      val input = Counter("some_metric", None, List(MetricValue(Map.empty, 123.765D, None, None)))
      val result = input.render
      result shouldBe
        """# TYPE some_metric counter
          |some_metric 123.765""".stripMargin
    }
    "Be rendered with Nan" in {
      val input = Counter("some_metric", None, List(MetricValue(Map.empty, Double.NaN, None, None)))
      val result = input.render
      result shouldBe
        """# TYPE some_metric counter
          |some_metric Nan""".stripMargin
    }
    "Be rendered with +Inf" in {
      val input = Counter("some_metric", None, List(MetricValue(Map.empty, Double.PositiveInfinity, None, None)))
      val result = input.render
      result shouldBe
        """# TYPE some_metric counter
          |some_metric +Inf""".stripMargin
    }
    "Be rendered with -Inf" in {
      val input = Counter("some_metric", None, List(MetricValue(Map.empty, Double.NegativeInfinity, None, None)))
      val result = input.render
      result shouldBe
        """# TYPE some_metric counter
          |some_metric -Inf""".stripMargin
    }
    "Be rendered with labels" in {
      val input = Counter("some_metric", None, List(MetricValue(Map("foo" -> "bar", "baz" -> "qux", "quux" -> "quuz"), 123D, None, None)))
      val result = input.render
      result shouldBe
        """# TYPE some_metric counter
          |some_metric{foo="bar", baz="qux", quux="quuz"} 123""".stripMargin
    }
    "Be rendered with multiple values" in {
      val input = Counter("some_metric", None, List(
        MetricValue(Map.empty, 123D, None, None),
        MetricValue(Map("foo" -> "bar"), 21D, Some(123), None),
        MetricValue(Map("baz" -> "qux"), 0D, None, None)
      ))
      val result = input.render
      result shouldBe
        """# TYPE some_metric counter
          |some_metric 123
          |some_metric{foo="bar"} 21 123
          |some_metric{baz="qux"} 0""".stripMargin
    }
    "Be rendered as summary with sum and count" in {
      val input = Summary("some_metric", None,
        List(
          MetricValue(Map("quantile" -> "0.01", "foo" -> "bar"), 123D, None, None)
        ),
        List(
          MetricValue(Map("foo" -> "bar"), 999D, None, Some(Modifier.Sum))
        ),
        List(
          MetricValue(Map("foo" -> "bar"), 222D, None, Some(Modifier.Count))
        ))
      val result = input.render
      result shouldBe
        """# TYPE some_metric summary
          |some_metric{quantile="0.01", foo="bar"} 123
          |some_metric_sum{foo="bar"} 999
          |some_metric_count{foo="bar"} 222""".stripMargin
    }
    "Be rendered as histogram with sum, count and bucket" in {
      val input = Histogram("some_metric", None,
        List(
          MetricValue(Map("le" -> "0.05", "foo" -> "bar"), 123D, None, Some(Modifier.Bucket)),
          MetricValue(Map("le" -> "+Inf", "foo" -> "bar"), 222D, None, Some(Modifier.Bucket))
        ),
        List(
          MetricValue(Map("foo" -> "bar"), 999D, None, Some(Modifier.Sum))
        ),
        List(
          MetricValue(Map("foo" -> "bar"), 222D, None, Some(Modifier.Count))
        ))
      val result = input.render
      result shouldBe
        """# TYPE some_metric histogram
          |some_metric_bucket{le="0.05", foo="bar"} 123
          |some_metric_bucket{le="+Inf", foo="bar"} 222
          |some_metric_sum{foo="bar"} 999
          |some_metric_count{foo="bar"} 222""".stripMargin
    }
    "Be rendered with escaped help" in {
      val input = Counter("some_metric", Some("some \n help \\"), List(MetricValue(Map.empty, 123D, None, None)))
      val result = input.render
      result shouldBe
        """# HELP some_metric some \n help \\
          |# TYPE some_metric counter
          |some_metric 123""".stripMargin
    }
    "Be rendered with escaped label" in {
      val input = Counter("some_metric", None, List(MetricValue(Map("foo" -> "some \n value \\ to \" escape"), 123D, None, None)))
      val result = input.render
      result shouldBe
        """# TYPE some_metric counter
          |some_metric{foo="some \n value \\ to \" escape"} 123""".stripMargin
    }
  }

  "Doc example" should {
    "Be rendered" in {
      val input =
        """# TYPE metric1 counter
          |metric1 123
          |metric1{foo="bar"} 222
          |# TYPE metric2 gauge
          |metric2 999
        """.stripMargin
      val parser = new Parser()
      val parsed = parser.parseE(input).right.value
      val updated = parsed.filter(_.name == "metric1").map {
        case Counter(name, help, values)                 => Counter(name, help, values.map(v => v.copy(labels = v.labels + ("x" -> "y"))))
        case Gauge(name, help, values)                   => Gauge(name, help, values.map(v => v.copy(labels = v.labels + ("x" -> "y"))))
        case Untyped(name, help, values)                 => Untyped(name, help, values.map(v => v.copy(labels = v.labels + ("x" -> "y"))))
        case Summary(name, help, values, sums, counts)   => Summary(name, help, values.map(v => v.copy(labels = v.labels + ("x" -> "y"))), sums, counts)
        case Histogram(name, help, values, sums, counts) => Histogram(name, help, values.map(v => v.copy(labels = v.labels + ("x" -> "y"))), sums, counts)
      }
      val result = updated.map(_.render).mkString("\n\n")
      result shouldBe
        """# TYPE metric1 counter
          |metric1{x="y"} 123
          |metric1{foo="bar", x="y"} 222""".stripMargin
    }
  }
}
