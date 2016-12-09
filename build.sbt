name := "EZPassAnalytics"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.4"



libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "2.0.2"
libraryDependencies += "org.apache.spark" % "spark-streaming_2.11" % "2.0.2"
libraryDependencies += "org.apache.spark" % "spark-sql_2.11" % "2.0.2"
libraryDependencies += "org.json4s" % "json4s-native_2.11" % "3.5.0"
libraryDependencies += "org.json4s" % "json4s-jackson_2.11" % "3.5.0"
libraryDependencies += "net.liftweb" % "lift-json_2.11" % "3.0"
libraryDependencies += "org.scalatra" % "scalatra_2.11" % "2.4.1"
libraryDependencies += "org.scalatra" % "scalatra_2.11" % "2.4.1"
libraryDependencies += "org.apache.activemq" % "activemq-core" % "5.5.0"



mergeStrategy in assembly <<= (mergeStrategy in assembly) ((old) => {
  case x if Assembly.isConfigFile(x) =>
    MergeStrategy.concat
  case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
    MergeStrategy.rename
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.first // Changed deduplicate to first
    }
  case PathList(_*) => MergeStrategy.first // added this line
})