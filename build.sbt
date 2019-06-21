name := "prometheus_metrics_parser"
organization := "com.github.pawelj-pl"

scalaVersion := "2.12.8"

useJGit
enablePlugins(GitVersioning)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
