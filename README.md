# Prmoetheus metrics parser

[![Maven Central](https://img.shields.io/maven-central/v/com.github.pawelj-pl/prometheus_metrics_parser_2.13.svg)](https://img.shields.io/maven-central/v/com.github.pawelj-pl/prometheus_metrics_parser_2.13.svg)

Prometheus metrics parser is small, dependency-free Scala library for parsing [Prometheus](https://prometheus.io/) metrics base on [exposition formats](https://prometheus.io/docs/instrumenting/exposition_formats/).

### Getting started

#### Parse metrics
You can start using library by adding  following dependency to `build.sbt`:
```scala
libraryDependencies += "com.github.pawelj-pl" %% "prometheus_metrics_parser" % "<libraryVersion>"
``` 

Now you can create parser Instance:

```scala
import com.github.pawelj_pl.prometheus_metrics_parser.parser.Parser

val parser = Parser()
```

and parse provided metrics with one of available methods:

```scala
val input: String =
    """
      |# HELP some_metric First metric
      |# TYPE some_metric counter
      |some_metric{foo = "bar"} 123 999
    """.stripMargin

val eitherResult: Either[ParseError, List[Metric]] = parser.parseE(input) // Right(List(Counter(some_metric,Some(First metric),List(MetricValue(Map(foo -> bar),123.0,Some(999),None)))))
val optionResult: Option[List[Metric]] = parser.parseOpt(input) // Some(List(Counter(some_metric,Some(First metric),List(MetricValue(Map(foo -> bar),123.0,Some(999),None)))))
val result: List[Metric] = parser.unsafeParse(input) //List(Counter(some_metric,Some(First metric),List(MetricValue(Map(foo -> bar),123.0,Some(999),None)))) 
```

The last one is marked as unsafe, because it throws exception (`com.github.pawelj_pl.prometheus_metrics_parser.parser.ParseException`) on error.

#### Rendering metrics
Each instance of `com.github.pawelj_pl.prometheus_metrics_parser.Metric` can be rendered to string representing set of Prometheus metric lines. For example:

```scala
import com.github.pawelj_pl.prometheus_metrics_parser.Metric
import com.github.pawelj_pl.prometheus_metrics_parser.Metric.Counter
import com.github.pawelj_pl.prometheus_metrics_parser.MetricValue

 val input: Metric = Counter(
        "some_metric",
        Some("Help content"),
        List(
          MetricValue(Map("foo" -> "bar"), 123D, Some(111L), None),
          MetricValue(Map("foo" -> "baz"), Double.NaN, Some(999L), None)
        )
      )
val result: String = input.render
```

`result` will contain following string:

```
# HELP some_metric Help content
# TYPE some_metric counter
some_metric{foo="bar"} 123 111
some_metric{foo="baz"} Nan 999
```

It can be useful for transforming existing metrics. Some examples:

##### Filtering metrics

```scala
import com.github.pawelj_pl.prometheus_metrics_parser.Metric
import com.github.pawelj_pl.prometheus_metrics_parser.Metric.Counter
import com.github.pawelj_pl.prometheus_metrics_parser.MetricValue
import com.github.pawelj_pl.prometheus_metrics_parser.parser.Parser

val input =
  """# TYPE metric1 counter
    |metric1{foo="bar"} 123
    |metric1{foo="baz"} 222
    |# TYPE metric2 counter
    |metric2 333
    |# TYPE other_metric counter
    |other_metric 999
  """.stripMargin

val parser = Parser()
val parsed = parser.parseE(input)

val updated = parsed
  .map(_
    .filter(_.name.startsWith("metric"))
    .map(_.render)
    .mkString("\n\n"))
    
val result = updated.getOrElse("")
```

`result` will contain following string:

```
# TYPE metric1 counter
metric1{foo="bar"} 123
metric1{foo="baz"} 222

# TYPE metric2 counter
metric2 333
```

##### Updating labels

```scala
import com.github.pawelj_pl.prometheus_metrics_parser.Metric.{Counter, Gauge, Histogram, Summary, Untyped}
import com.github.pawelj_pl.prometheus_metrics_parser.parser.Parser

val input =
        """# TYPE metric1 counter
          |metric1 123
          |metric1{foo="bar"} 222
        """.stripMargin
        
val parser = new Parser()
val parsed = parser.parseE(input).getOrElse(List.empty)

val updated = parsed.map {
  case Counter(name, help, values)                 => Counter(name, help, values.map(v => v.copy(labels = v.labels + ("x" -> "y"))))
  case Gauge(name, help, values)                   => Gauge(name, help, values.map(v => v.copy(labels = v.labels + ("x" -> "y"))))
  case Untyped(name, help, values)                 => Untyped(name, help, values.map(v => v.copy(labels = v.labels + ("x" -> "y"))))
  case Summary(name, help, values, sums, counts)   => Summary(name, help, values.map(v => v.copy(labels = v.labels + ("x" -> "y"))), sums, counts)
  case Histogram(name, help, values, sums, counts) => Histogram(name, help, values.map(v => v.copy(labels = v.labels + ("x" -> "y"))), sums, counts)
}
val result = updated.map(_.render).mkString("\n\n")
```

`result` should contain following string:

```
# TYPE metric1 counter
metric1{x="y"} 123
metric1{foo="bar", x="y"} 222
```
