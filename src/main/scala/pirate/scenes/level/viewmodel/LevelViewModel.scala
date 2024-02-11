package pirate.scenes.level.viewmodel

import indigo.*
import pirate.scenes.level.model.Pirate
import pirate.core.SpaceConvertors

/*
The view model cannot be initialised at game start up, because we want to load
some data during the loading screen, parse it, and use it to build the
`worldToScreenSpace` function, which relies on knowing the size of the tiles
which is stored in the Tiled data.
 */
enum LevelViewModel:
  case NotReady
  case Ready(
      spaceConvertors: SpaceConvertors,
      pirateViewState: PirateViewState
  )

  def notReady: Boolean =
    this match
      case NotReady    => true
      case Ready(_, _) => false

object LevelViewModel:

  extension (lvm: LevelViewModel)
    def update(gameTime: GameTime, pirate: Pirate): Outcome[LevelViewModel] =
      lvm match
        case NotReady =>
          Outcome(lvm)

        case r @ Ready(_, pirateViewState) =>
          pirateViewState
            .update(gameTime, pirate)
            .map(ps => r.copy(pirateViewState = ps))
