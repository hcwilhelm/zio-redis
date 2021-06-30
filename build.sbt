import BuildHelper._

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  List(
    organization := "dev.zio",
    homepage := Some(url("https://github.com/zio/zio-redis/")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer("jdegoes", "John De Goes", "john@degoes.net", url("https://degoes.net")),
      Developer("mijicd", "Dejan Mijic", "dmijic@acm.org", url("https://github.com/mijicd"))
    )
  )
)

addCommandAlias("prepare", "fix; fmt")
addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll")
addCommandAlias("fix", "scalafixAll")
addCommandAlias("fixCheck", "scalafixAll --check")

lazy val root =
  project
    .in(file("."))
    .settings(publish / skip := true)
    .aggregate(redis, benchmarks, example)

lazy val redis =
  project
    .in(file("redis"))
    .enablePlugins(BuildInfoPlugin)
    .settings(stdSettings("zio-redis"))
    .settings(buildInfoSettings("zio.redis"))
    .settings(
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio-streams"  % Zio,
        "dev.zio" %% "zio-logging"  % "0.5.11",
        "dev.zio" %% "zio-schema"   % "0.0.5",
        "dev.zio" %% "zio-test"     % Zio % Test,
        "dev.zio" %% "zio-test-sbt" % Zio % Test
      ),
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
    )

lazy val benchmarks =
  project
    .in(file("benchmarks"))
    .settings(stdSettings("benchmarks"))
    .dependsOn(redis)
    .enablePlugins(JmhPlugin)
    .settings(
      publish / skip := true,
      libraryDependencies ++= Seq(
        "dev.profunktor"    %% "redis4cats-effects" % "0.14.0",
        "io.chrisdavenport" %% "rediculous"         % "0.0.12",
        "io.laserdisc"      %% "laserdisc-fs2"      % "0.4.1"
      )
    )

lazy val example =
  project
    .in(file("example"))
    .settings(stdSettings("example"))
    .dependsOn(redis)
    .settings(
      publish / skip := true,
      libraryDependencies ++= Seq(
        "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % "2.2.9",
        "com.softwaremill.sttp.client" %% "circe"                         % "2.2.9",
        "de.heikoseeberger"            %% "akka-http-circe"               % "1.36.0",
        "dev.zio"                      %% "zio-streams"                   % Zio,
        "dev.zio"                      %% "zio-config-magnolia"           % "1.0.6",
        "dev.zio"                      %% "zio-config-typesafe"           % "1.0.6",
        "dev.zio"                      %% "zio-prelude"                   % "1.0.0-RC5",
        "io.circe"                     %% "circe-core"                    % "0.14.1",
        "io.circe"                     %% "circe-generic"                 % "0.14.1",
        "io.scalac"                    %% "zio-akka-http-interop"         % "0.4.0"
      )
    )
