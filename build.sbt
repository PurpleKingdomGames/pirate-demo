import scala.sys.process._
import scala.language.postfixOps

import indigoplugin.IndigoGenerators
import indigoplugin.IndigoOptions

import sbtwelcome._

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"

val scala3Version = "3.3.1"

lazy val pirateOptions: IndigoOptions =
  IndigoOptions.defaults
    .withTitle("The Cursed Pirate")
    .withWindowWidth(1280)
    .withWindowHeight(720)
    .withBackgroundColor("black")
    .excludeAssetPaths {
      case p if p.contains("unused")                       => true
      case p if p.contains("Captain Clown Nose Data.json") => true
    }

lazy val pirate =
  (project in file("."))
    .enablePlugins(
      ScalaJSPlugin,
      SbtIndigo
    )
    .settings(
      name         := "pirate",
      version      := "0.0.1",
      scalaVersion := scala3Version,
      organization := "pirate",
      libraryDependencies ++= Seq(
        "org.scalameta"  %%% "munit"      % "0.7.29" % Test,
        "org.scalacheck" %%% "scalacheck" % "1.15.3" % "test"
      ),
      Test / scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
    )
    .settings(
      indigoOptions := pirateOptions,
      libraryDependencies ++= Seq(
        "io.indigoengine" %%% "indigo-json-circe" % "0.15.0",
        "io.indigoengine" %%% "indigo"            % "0.15.0",
        "io.indigoengine" %%% "indigo-extras"     % "0.15.0"
      ),
      Compile / sourceGenerators += Def.task {
        val cachedFun = FileFunction.cached(
          streams.value.cacheDirectory / "pirate-gen"
        ) { _ =>
          IndigoGenerators
            .sbt((Compile / sourceManaged).value, "pirate.generated")
            .listAssets("Assets", pirateOptions.assets)
            .generateConfig("Config", pirateOptions)
            .embedAseprite("CaptainAnim", baseDirectory.value / "assets" / "captain" / "Captain Clown Nose Data.json")
            .toSourceFiles
            .toSet
        }
        cachedFun(IO.listFiles(baseDirectory.value / "assets").toSet).toSeq
      }
    )
    .enablePlugins(GhpagesPlugin)
    .settings(
      siteSourceDirectory      := target.value / "indigoBuildFull",
      makeSite / includeFilter := "*",
      makeSite / excludeFilter := ".DS_Store",
      git.remoteRepo           := "git@github.com:davesmith00000/pong.git",
      ghpagesNoJekyll          := true
    )
    .settings(
      code := {
        val command = Seq("code", ".")
        val run = sys.props("os.name").toLowerCase match {
          case x if x contains "windows" => Seq("cmd", "/C") ++ command
          case _                         => command
        }
        run.!
      }
    )
    .settings(
      logo := rawLogo + "(v" + version.value.toString + ")",
      usefulTasks := Seq(
        UsefulTask("runGame", "Run the game").noAlias,
        UsefulTask("buildGame", "Build web version").noAlias,
        UsefulTask("runGameFull", "Run the fully optimised game").noAlias,
        UsefulTask(
          "buildGameFull",
          "Build the fully optimised web version"
        ).noAlias,
        UsefulTask("publishGame", "Publish the game to ghpages").noAlias,
        UsefulTask("code", "Launch VSCode").noAlias
      ),
      logoColor        := scala.Console.MAGENTA,
      aliasColor       := scala.Console.YELLOW,
      commandColor     := scala.Console.CYAN,
      descriptionColor := scala.Console.WHITE
    )

addCommandAlias("buildGame", ";compile;fastLinkJS;indigoBuild")
addCommandAlias("buildGameFull", ";compile;fullLinkJS;indigoBuildFull")
addCommandAlias("runGame", ";compile;fastLinkJS;indigoRun")
addCommandAlias("runGameFull", ";compile;fullLinkJS;indigoRunFull")

// TODO: Set up GHPages + Clean Branch.
// addCommandAlias(
//   "publishGame",
//   List(
//     "buildGameFull",
//     "makeSite",
//     "ghpagesPushSite"
//   ).mkString(";", ";", "")
// )

lazy val code =
  taskKey[Unit]("Launch VSCode in the current directory")

lazy val rawLogo =
  """
 _______ .-. .-.,---.     ,--,  .-. .-.,---.    .---. ,---.   ,'|"\   
|__   __|| | | || .-'   .' .')  | | | || .-.\  ( .-._)| .-'   | |\ \  
  )| |   | `-' || `-.   |  |(_) | | | || `-'/ (_) \   | `-.   | | \ \ 
 (_) |   | .-. || .-'   \  \    | | | ||   (  _  \ \  | .-'   | |  \ \
   | |   | | |)||  `--.  \  `-. | `-')|| |\ \( `-'  ) |  `--. /(|`-' /
   `-'   /(  (_)/( __.'   \____\`---(_)|_| \)\`----'  /( __.'(__)`--' 
        (__)   (__)                        (__)      (__)             
 ,---.  ,-.,---.    .--.  _______ ,---.                               
 | .-.\ |(|| .-.\  / /\ \|__   __|| .-'                               
 | |-' )(_)| `-'/ / /__\ \ )| |   | `-.                               
 | |--' | ||   (  |  __  |(_) |   | .-'                               
 | |    | || |\ \ | |  |)|  | |   |  `--.                             
 /(     `-'|_| \)\|_|  (_)  `-'   /( __.'                             
(__)           (__)              (__)                                 

  """
