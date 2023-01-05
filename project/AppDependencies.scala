import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-28" % "7.12.0",
    "uk.gov.hmrc"             %% "play-frontend-hmrc"         % "5.5.0-play-28",
    "uk.gov.hmrc"             %% "play-language"              % "6.1.0-play-28"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % "7.12.0" % "test, it",
    "org.scalatest"           %% "scalatest"                % "3.1.2"                 % "test, it",
    "org.jsoup"               %  "jsoup"                    % "1.10.2"                % "test, it",
    "com.typesafe.play"       %% "play-test"                % current                 % "test, it",
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.35.10"               % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "5.0.0"                 % "test, it"
  )
}
