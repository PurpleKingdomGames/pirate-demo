package pirate.scenes.loading

import indigo.*
import indigo.scenes.*
import indigoextras.subsystems.*

import pirate.core.Assets
import pirate.core.StartupData
import indigo.scenes.SceneEvent.JumpTo
import pirate.core.{Model, ViewModel}
import pirate.scenes.level.LevelScene

/*
The first scene of the game uses the preloaded assets to show
the progress of all the other assets we need to load.

Most of what happens here is that the scene uses the `AssetBundleLoader`
subsystem to dynamically load assets, which it interfaces with using events.

As progress events come in, they are translated into some visual feedback
for the player.
 */
final case class LoadingScene(assetPath: String, screenDimensions: Rectangle)
    extends Scene[StartupData, Model, ViewModel]:

  type SceneModel     = LoadingState
  type SceneViewModel = Unit

  val name: SceneName =
    SceneName("loading")

  val modelLens: Lens[Model, LoadingState] =
    Lens(
      m => m.loadingScene,
      (m, sm) => m.copy(loadingScene = sm)
    )

  val viewModelLens: Lens[ViewModel, Unit] =
    Lens(_ => (), (vm, _) => vm)

  val eventFilters: EventFilters =
    EventFilters.Restricted

  val subSystems: Set[SubSystem] =
    Set(AssetBundleLoader)

  def updateModel(
      context: SceneContext[StartupData],
      loadingState: LoadingState
  ): GlobalEvent => Outcome[LoadingState] =
    case FrameTick =>
      loadingState match
        case LoadingState.NotStarted =>
          Outcome(LoadingState.InProgress(0))
            .addGlobalEvents(
              AssetBundleLoaderEvent.Load(BindingKey("Loading"), Assets.remainingAssets(assetPath))
            )

        case _ =>
          Outcome(loadingState)

    case AssetBundleLoaderEvent.LoadProgress(_, percent, _, _) =>
      Outcome(LoadingState.InProgress(percent))

    case AssetBundleLoaderEvent.Success(_) =>
      Outcome(LoadingState.Complete)
        .addGlobalEvents(JumpTo(LevelScene.name))

    case AssetBundleLoaderEvent.Failure(_, _) =>
      Outcome(LoadingState.Error)

    case _ =>
      Outcome(loadingState)

  def updateViewModel(
      context: SceneContext[StartupData],
      loadingState: LoadingState,
      viewModel: Unit
  ): GlobalEvent => Outcome[Unit] =
    _ => Outcome(viewModel)

  def present(
      context: SceneContext[StartupData],
      loadingState: LoadingState,
      viewModel: Unit
  ): Outcome[SceneUpdateFragment] =
    val x = screenDimensions.horizontalCenter
    val y = screenDimensions.verticalCenter

    val message: String =
      loadingState match
        case LoadingState.NotStarted =>
          "Loading..."

        case LoadingState.InProgress(percent) =>
          s"Loading...${percent.toString()}%"

        case LoadingState.Complete =>
          "Loading...100%"

        case LoadingState.Error =>
          "Uh oh, loading failed..."

    Outcome(
      SceneUpdateFragment(
        Text(
          message,
          x,
          y + 10,
          1,
          Assets.Fonts.fontKey,
          Assets.Fonts.fontMaterial
        ).alignCenter,
        context.startUpData.captain
          .modifyMaterial(_.withOverlay(Fill.Color(RGBA.White)))
          .moveTo(x, y)
          .changeCycle(CycleLabel("Run"))
          .play()
      )
    )

// An ADT representing the states we can be in during loading.
enum LoadingState:
  case NotStarted               extends LoadingState
  case InProgress(percent: Int) extends LoadingState
  case Complete                 extends LoadingState
  case Error                    extends LoadingState
