package com.github.pawelj_pl.prometheus_metrics_parser

import com.github.pawelj_pl.prometheus_metrics_parser.parser.Parser

import scala.io.Source

object Main extends App {
  val file = Source.fromFile("/tmp/metrics2")

  val result = Parser().parseE(file.mkString)
  result match {
    case Left(err)      => println(err)
    case Right(metrics) =>
      println(metrics.mkString("\n"))
  }
}
