import AssemblyKeys._
import sbt.Keys._
import sbtrelease._
import ReleaseStateTransformations._
import ReleasePlugin._
import ReleaseKeys._
import AssemblyKeys._

Statika.distributionProject

name := "ddwriter"

description := "compota dynamograph distributed writing"

organization := "bio4j"

scalaVersion:= "2.10.4"

conflictManager := ConflictManager.latestRevision

libraryDependencies ++= Seq(
  "ohnosequences" % "compota_2.10" % "0.9.4-SNAPSHOT" exclude("ohnosequences", "type-sets_2.10"),
  "ohnosequences" % "statika_2.10" % "1.0.0"  exclude("ohnosequences", "type-sets_2.10"),
  "bio4j" % "dynamograph_2.11" % "0.1.4-SNAPSHOT" exclude("com.chuusai", "shapeless_2.11")  exclude("ohnosequences", "aws-statika_2.11") exclude("ohnosequences", "statika_2.11") exclude("com.thinkaurelius.titan", "titan-berkeleyje") exclude("org.mockito", "mockito-all") exclude("com.thinkaurelius.titan", "titan-all")
)

resolvers +=  Resolver.url("era7" + " public ivy releases",  url("http://releases.era7.com.s3.amazonaws.com"))(Resolver.ivyStylePatterns)

resolvers +=  Resolver.url("era7" + " public ivy snapshots",  url("http://snapshots.era7.com.s3.amazonaws.com"))(Resolver.ivyStylePatterns)

resolvers += Resolver.sonatypeRepo("snapshots")

metadataObject := name.value

dependencyOverrides += "ohnosequences" % "aws-scala-tools_2.10" % "0.7.1-SNAPSHOT"

dependencyOverrides += "org.scalatest" % "scalatest_2.10" % "2.0"

dependencyOverrides += "ohnosequences" % "aws-statika_2.10" % "1.0.0"

dependencyOverrides += "ohnosequences" % "statika_2.10" % "1.0.0"

dependencyOverrides += "ohnosequences" % "amazon-linux-ami_2.10" % "0.14.1"

dependencyOverrides += "commons-codec" % "commons-codec" % "1.6"

dependencyOverrides += "com.chuusai" % "shapeless_2.10.2" % "2.0.0-M1"

dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.1.2"

dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.1.2"

dependencyOverrides += "jline" % "jline" % "2.6"

dependencyOverrides += "org.slf4j" % "slf4j-api" % "1.7.5"

dependencyOverrides += "com.thinkaurelius.titan" % "titan-core" % "0.4.1"

dependencyOverrides += "com.codahale.metrics" % "metrics-core" % "3.0.1"

dependencyOverrides += "commons-lang" % "commons-lang" % "2.5"

dependencyOverrides += "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.3.1"

dependencyOverrides += "org.apache.httpcomponents" % "httpclient" % "4.2"

dependencyOverrides += "joda-time" % "joda-time" % "2.3"

dependencyOverrides +=  "org.apache.commons" % "commons-math" % "2.2"

dependencyOverrides += "org.apache.lucene" % "lucene-core" % "4.4.0"

dependencyOverrides += "tomcat" % "jasper-compiler" % "5.5.23"

dependencyOverrides += "tomcat" % "jasper-runtime" % "5.5.23"

dependencyOverrides += "com.amazonaws" % "aws-java-sdk" % "1.8.0"

dependencyOverrides += "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.2"


mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) => {
  case "log4j.properties" => MergeStrategy.first
  case "about.html" => MergeStrategy.first
  case "mime.types" => MergeStrategy.first
  case "avsl.conf" => MergeStrategy.first
  case PathList("images", "ant_logo_large.gif") => MergeStrategy.first
  case PathList("javax", "servlet", _*) => MergeStrategy.first
  case PathList("org", "apache", "avro", "ipc",  _*) => MergeStrategy.first
  case PathList("javax", "xml", "stream", _*) => MergeStrategy.first
  case PathList("org", "apache", "jasper", _*) => MergeStrategy.first
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList("META-INF", _*) => MergeStrategy.first
  case PathList("org", "apache", "commons", "beanutils", _*) => MergeStrategy.first
  case PathList("org", "fusesource", "hawtjni", "runtime", "Library.class") => MergeStrategy.first
  case PathList("org", "fusesource", "jansi", _*) => MergeStrategy.first
  case PathList("org", "apache", "commons", "collections", _*) => MergeStrategy.first
  case x => MergeStrategy.first
  }
}

