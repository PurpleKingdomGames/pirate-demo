package pirate

import indigo.*
import indigo.scenes.*
import indigoextras.subsystems.FPSCounter
import pirate.scenes.loading.LoadingScene
import pirate.scenes.level.LevelScene
import pirate.core.{Model, ViewModel}
import pirate.core.BootInformation
import pirate.core.LayerKeys
import pirate.generated.Config

import pirate.core.{Assets, InitialLoad, StartupData}

import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("IndigoGame")
object CursedPirateDemo extends IndigoGame[BootInformation, StartupData, Model, ViewModel]:

  def initialScene(bootInfo: BootInformation): Option[SceneName] =
    None

  def scenes(bootInfo: BootInformation): NonEmptyList[Scene[StartupData, Model, ViewModel]] =
    NonEmptyList(
      LoadingScene(bootInfo.assetPath, bootInfo.screenDimensions),
      LevelScene(bootInfo.screenDimensions.width)
    )

  val eventFilters: EventFilters =
    EventFilters.BlockAll

  def boot(flags: Map[String, String]): Outcome[BootResult[BootInformation]] =
    Outcome {
      val assetPath: String =
        flags.getOrElse("baseUrl", "")

      val config =
        Config.config
          .withMagnification(2)
          .noResize

      BootResult(
        config,
        BootInformation(assetPath, config.screenDimensions)
      ).withAssets(Assets.initialAssets(assetPath))
        .withFonts(Assets.Fonts.fontInfo)
        .withSubSystems(
          FPSCounter(Point(10, 10), LayerKeys.fps)
        )
    }

  def setup(
      bootInfo: BootInformation,
      assetCollection: AssetCollection,
      dice: Dice
  ): Outcome[Startup[StartupData]] =
    InitialLoad.setup(bootInfo.screenDimensions, assetCollection, dice)

  def initialModel(startupData: StartupData): Outcome[Model] =
    Outcome(Model.initial)

  def initialViewModel(startupData: StartupData, model: Model): Outcome[ViewModel] =
    Outcome(ViewModel.initial)

  def updateModel(context: FrameContext[StartupData], model: Model): GlobalEvent => Outcome[Model] =
    _ => Outcome(model)

  def updateViewModel(
      context: FrameContext[StartupData],
      model: Model,
      viewModel: ViewModel
  ): GlobalEvent => Outcome[ViewModel] =
    _ => Outcome(viewModel)

  def present(context: FrameContext[StartupData], model: Model, viewModel: ViewModel): Outcome[SceneUpdateFragment] =
    Outcome(
      SceneUpdateFragment.empty
        .addLayer(Layer(LayerKeys.background))
        .addLayer(Layer(LayerKeys.bigClouds))
        .addLayer(Layer(LayerKeys.smallClouds))
        .addLayer(Layer(LayerKeys.game))
        .addLayer(Layer(LayerKeys.fps))
    )
