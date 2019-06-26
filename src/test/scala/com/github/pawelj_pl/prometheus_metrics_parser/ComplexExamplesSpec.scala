package com.github.pawelj_pl.prometheus_metrics_parser

import com.github.pawelj_pl.prometheus_metrics_parser.Metric.{Counter, Histogram, Summary, Untyped}
import com.github.pawelj_pl.prometheus_metrics_parser.parser.Parser
import org.scalatest.{EitherValues, Matchers, WordSpec}

import scala.io.Source

class ComplexExamplesSpec extends WordSpec with Matchers with EitherValues {
  final val parser = Parser()

  "Parser" should {
    "parse example from Prometheus doc" in {
      val src = Source.fromResource("metrics/prometheus_example.txt")
      val result = parser.parseE(src.mkString)
      result.right.value shouldBe List(
        Counter("http_requests_total", Some("The total number of HTTP requests."), List(
          MetricValue(Map("method" -> "post", "code" -> "200"), 1027D, Some(1395066363000L), None),
          MetricValue(Map("method" -> "post", "code" -> "400"), 3D, Some(1395066363000L), None)
        )),
        Untyped("msdos_file_access_time_seconds", None, List(
          MetricValue(Map("path" -> """C:\DIR\FILE.TXT""", "error" -> "Cannot find file:\n\"FILE.TXT\""), 1.458255915e9D, None, None)
        )),
        Untyped("metric_without_timestamp_and_labels", None, List(
          MetricValue(Map.empty, 12.47D, None, None)
        )),
        Untyped("something_weird", None, List(
          MetricValue(Map("problem" -> "division by zero"), Double.PositiveInfinity, Some(-3982045L), None)
        )),
        Histogram(
          "http_request_duration_seconds",
          Some("A histogram of the request duration."),
          List(
            MetricValue(Map("le" -> "0.05"), 24054D, None, Some(Modifier.Bucket)),
            MetricValue(Map("le" -> "0.1"), 33444D, None, Some(Modifier.Bucket)),
            MetricValue(Map("le" -> "0.2"), 100392D, None, Some(Modifier.Bucket)),
            MetricValue(Map("le" -> "0.5"), 129389D, None, Some(Modifier.Bucket)),
            MetricValue(Map("le" -> "1"), 133988D, None, Some(Modifier.Bucket)),
            MetricValue(Map("le" -> "+Inf"), 144320D, None, Some(Modifier.Bucket))
          ),
          List(
            MetricValue(Map.empty, 53423D, None, Some(Modifier.Sum))
          ),
          List(
            MetricValue(Map.empty, 144320D, None, Some(Modifier.Count))
          )
        ),
        Summary(
          "rpc_duration_seconds",
          Some("A summary of the RPC duration in seconds."),
          List(
            MetricValue(Map("quantile" -> "0.01"), 3102D, None, None),
            MetricValue(Map("quantile" -> "0.05"), 3272D, None, None),
            MetricValue(Map("quantile" -> "0.5"), 4773D, None, None),
            MetricValue(Map("quantile" -> "0.9"), 9001D, None, None),
            MetricValue(Map("quantile" -> "0.99"), 76656D, None, None)
          ),
          List(
            MetricValue(Map.empty, 1.7560473e+07D, None, Some(Modifier.Sum))
          ),
          List(
            MetricValue(Map.empty, 2693, None, Some(Modifier.Count))
          )
        )
      )
      src.close()
    }
  }
}
