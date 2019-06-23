git.gitTagToVersionNumber := { tag: String =>
  if(tag matches "[0-9]+\\..*") Some(tag)
  else None
}

useGpg := false

credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )
  ).toSeq

pomIncludeRepository := { _ => false }
licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
homepage := Some(url("https://github.com/PawelJ-PL/prometheus-metrics-parser"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/PawelJ-PL/prometheus-metrics-parser"),
    "scm:https://github.com/PawelJ-PL/prometheus-metrics-parser.git"
  )
)

developers := List(
  Developer(
    id    = "Pawelj-PL",
    name  = "Pawel",
    email = "inne.poczta@gmail.com",
    url   = url("https://github.com/PawelJ-PL")
  )
)

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
