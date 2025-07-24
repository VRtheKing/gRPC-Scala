ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.6"

val scala3Version = "3.3.6"
val http4sVersion = "0.23.30"
val weaverVersion = "0.8.4"

lazy val protobuf =
  project
    .in(file("protobuf"))
    .settings(
      name := "protobuf",
      scalaVersion := scala3Version
    )
    .enablePlugins(Fs2Grpc)

lazy val root =
  project
    .in(file("."))
    .settings(
      name := "gRPC-Scala",
      scalaVersion := scala3Version,
      libraryDependencies ++= Seq(
        "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion,
        "org.http4s" %% "http4s-ember-server" % http4sVersion,
        "org.http4s" %% "http4s-dsl" % http4sVersion,
        "org.http4s" %% "http4s-circe" % http4sVersion,
        "com.disneystreaming" %% "weaver-cats" % weaverVersion % Test,
),
      testFrameworks += new TestFramework("weaver.framework.CatsEffect")
    )
    .dependsOn(protobuf)