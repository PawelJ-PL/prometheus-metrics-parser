# Prmoetheus metrics parser

[![Maven Central](https://img.shields.io/maven-central/v/com.github.pawelj-pl/prometheus_metrics_parser_2.13.svg)](https://img.shields.io/maven-central/v/com.github.pawelj-pl/prometheus_metrics_parser_2.13.svg)

Prometheus metrics parser is small, dependency-free Scala library for parsing [Prometheus](https://prometheus.io/) metrics according to [exposition formats](https://prometheus.io/docs/instrumenting/exposition_formats/).

### Getting started

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
