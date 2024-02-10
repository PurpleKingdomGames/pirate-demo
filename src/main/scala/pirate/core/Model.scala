package pirate.core

import pirate.scenes.level.model.LevelModel
import pirate.scenes.loading.LoadingState

// A simple master model class to hold the sub-models for
// each scene.
final case class Model(
    loadingScene: LoadingState,
    gameScene: LevelModel
)
object Model:

  def initial: Model =
    Model(
      LoadingState.NotStarted,
      LevelModel.NotReady
    )
