lazy val commonSettings = Defaults.defaultSettings ++ Seq(
    organization := "Sparklinedata",
    version := "0.0.1",
    scalaVersion := "2.10.4",
    crossScalaVersions := Seq("2.10.2", "2.10.3", "2.10.4", "2.11.0", "2.11.1", "2.11.2", "2.11.3", "2.11.4", "2.11.5"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    resolvers += "JitPack.IO" at "https://jitpack.io",
    scalacOptions ++= Seq("-feature")
  )

val sparkVersion = "1.4.0"


lazy val root: Project = Project(
    "spark-druid-indexer",
    file("."),
    settings = commonSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
        "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
        "org.apache.spark" %% "spark-catalyst" % sparkVersion % "provided",
        "org.apache.spark" %% "spark-hive" % sparkVersion % "provided"
      ),
     assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
      test in assembly := {}
  )
)

