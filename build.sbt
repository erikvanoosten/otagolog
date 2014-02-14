name := "otagolog"

description <<= (crossVersion) { v => "Otago Log for Scala " + v }

organization := "nl.grons"

version := "0.1.0"

scalaVersion := "2.10.3"

// crossScalaVersions := Seq("2.10.1")

crossVersion := CrossVersion.binary

resolvers ++= Seq(
  "Local Maven repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "SpringSource milestonr repository" at "http://repo.springsource.org/libs-milestone"
)

libraryDependencies ++= Seq(
  "io.netty" % "netty-all" % "4.0.15.Final",
  "com.lmax" % "disruptor" % "3.2.0",
  // "net.openhft" % "chronicle" % "2.0.1",
  // "com.yammer.metrics" % "metrics-core" % "3.0.0",
  // Logging
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  // Test
  "junit" % "junit" % "4.11" % "test",
  "org.scalatest" %% "scalatest" % "2.0" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test"
)

javacOptions ++= Seq("-Xmx512m", "-Xms128m", "-Xss10m")

javaOptions += "-Xmx512m"

scalacOptions ++= Seq("-deprecation", "-unchecked")

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
  else                             Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

credentials += Credentials(Path.userHome / ".sbt" / "sonatype.credentials")

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

pomExtra := (
  <url>http://otagolog.grons.nl/</url>
  <scm>
    <url>git@github.com:erikvanoosten/otagolog.git</url>
    <connection>scm:git:git@github.com:erikvanoosten/otagolog.git</connection>
  </scm>
  <developers>
    <developer>
      <name>Erik van Oosten</name>
      <url>http://day-to-day-stuff.blogspot.com/</url>
      <timezone>+1</timezone>
    </developer>
  </developers>
)
