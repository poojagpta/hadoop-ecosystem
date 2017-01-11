import sbt.Keys._

name := "kafkasparkdemo"

version := "1.0"

scalaVersion := "2.11.8"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

val sparkVersion = "1.6.2"

libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-core_2.11" % sparkVersion % "provided",
  "org.apache.spark" % "spark-streaming_2.11" % sparkVersion % "provided",
  "org.apache.spark" % "spark-streaming-kafka-0-8_2.11" % "2.1.0"  withSources()
)

resolvers ++= Seq(
  "Cloudera Repository" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
  "Akka Repository" at "http://repo.akka.io/releases/",
  "Spray Repository" at "http://repo.spray.cc/",
  "bintray-spark-packages" at "https://dl.bintray.com/spark-packages/maven/")

assemblyJarName in assembly := "kafkasparkdemo.jar"

mergeStrategy in assembly := {
  case PathList("org", "apache", "spark", "unused", "UnusedStubClass.class") => MergeStrategy.first
  case x => (mergeStrategy in assembly).value(x)
}
