name := "prometheus_metrics_parser"
organization := "com.github.pawelj-pl"

scalaVersion := "2.12.8"

useJGit
enablePlugins(GitVersioning)

crossScalaVersions := Seq("2.12.8", "2.13.0")

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
