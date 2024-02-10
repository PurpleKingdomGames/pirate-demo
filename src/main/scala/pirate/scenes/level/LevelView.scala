package pirate.scenes.level

import indigo.*
import indigo.physics.*

import pirate.core.Assets
import pirate.core.LevelDataStore
import pirate.core.PirateClips
import pirate.scenes.level.model.PirateState
import pirate.scenes.level.model.Pirate
import pirate.scenes.level.model.LevelModel
import pirate.scenes.level.viewmodel.LevelViewModel
import pirate.scenes.level.viewmodel.PirateViewState
import pirate.generated.Assets.*
import pirate.core.LayerKeys

object LevelView:

  def draw(
      gameTime: GameTime,
      model: LevelModel.Ready,
      viewModel: LevelViewModel.Ready,
      captainClips: PirateClips,
      levelDataStore: Option[LevelDataStore]
  ): SceneUpdateFragment =
    Level.draw(levelDataStore) |+| {
      model.world.findByTag("pirate").headOption match
        case Some(collider: Collider.Box[_]) =>
          PirateCaptain.draw(
            gameTime,
            model.pirate,
            collider,
            viewModel.pirateViewState,
            captainClips,
            viewModel.worldToScreenSpace
          )

        case _ =>
          // Shouldn't happen.
          SceneUpdateFragment.empty
    } // |+| showColliderDebug(model.world, viewModel.worldToScreenSpace)

  def showColliderDebug(world: World[String], toScreenSpace: Vertex => Vertex): SceneUpdateFragment =
    SceneUpdateFragment(
      world.present {
        case Collider.Circle(_, bounds, _, _, _, _, _, _, _, _) =>
          // Won't get here, no circles.
          Shape.Circle(
            toScreenSpace(bounds.position).toPoint,
            bounds.radius.toInt,
            Fill.None,
            Stroke(1, RGBA.Green)
          )

        case Collider.Box(_, bounds, _, _, _, _, _, _, _, _) =>
          Shape.Box(
            BoundingBox(
              toScreenSpace(bounds.position),
              toScreenSpace(bounds.size)
            ).toRectangle,
            Fill.None,
            Stroke(1, RGBA.Green)
          )
      }
    )

  object Level:

    def draw(levelDataStore: Option[LevelDataStore]): SceneUpdateFragment =
      levelDataStore
        .map { levelAssets =>
          SceneUpdateFragment.empty
            .addLayer(
              Layer(
                LayerKeys.background,
                Batch(Graphic(Rectangle(0, 0, 640, 360), 50, assets.static.bgMaterial)) ++
                  drawWater(levelAssets.waterReflections)
              )
            )
            .addLayer(
              Layer(LayerKeys.game, drawForeground(levelAssets))
            )
            .withAudio(
              assets.sounds.bgmusicSceneAudio
            )
        }
        .getOrElse(SceneUpdateFragment.empty)

    def drawWater(waterReflections: Clip[Material.Bitmap]): Batch[SceneNode] =
      Batch(
        waterReflections,
        waterReflections.moveBy(150, 30),
        waterReflections.moveBy(-100, 60)
      )

    def drawForeground(assets: LevelDataStore): Batch[SceneNode] =
      Batch(
        assets.flag,
        assets.helm,
        Assets.Trees.tallTrunkGraphic.moveTo(420, 236),
        Assets.Trees.leftLeaningTrunkGraphic.moveTo(100, 286),
        Assets.Trees.rightLeaningTrunkGraphic.moveTo(25, 166),
        assets.backTallPalm.moveTo(420, 226),
        assets.palm.moveTo(397, 204),
        assets.palm.moveTo(77, 251),
        assets.palm.moveTo(37, 120),
        Assets.Static.chestGraphic.moveTo(380, 288),
        assets.terrain
      )

  object PirateCaptain:

    def draw(
        gameTime: GameTime,
        pirate: Pirate,
        collider: Collider.Box[String],
        pirateViewState: PirateViewState,
        captainClips: PirateClips,
        toScreenSpace: Vertex => Vertex
    ): SceneUpdateFragment =
      SceneUpdateFragment.empty
        .addLayer(
          Layer(
            LayerKeys.game,
            respawnEffect(
              gameTime,
              pirate.lastRespawn,
              updatedCaptain(pirate, collider, pirateViewState, captainClips, toScreenSpace)
            )
          )
        )

    val respawnFlashSignal: Seconds => Signal[(Boolean, Boolean)] =
      lastRespawn => Signal(_ < lastRespawn + Seconds(1.2)) |*| Signal.Pulse(Seconds(0.1))

    val captainWithAlpha
        : Clip[Material.ImageEffects] => SignalFunction[(Boolean, Boolean), Clip[Material.ImageEffects]] =
      captain =>
        SignalFunction {
          case (false, _) =>
            captain

          case (true, true) =>
            captain
              .modifyMaterial(_.withAlpha(1))

          case (true, false) =>
            captain
              .modifyMaterial(_.withAlpha(0))
        }

    def respawnEffect(
        gameTime: GameTime,
        lastRespawn: Seconds,
        captain: Clip[Material.ImageEffects]
    ): Clip[Material.ImageEffects] =
      (respawnFlashSignal(lastRespawn) |> captainWithAlpha(captain)).at(gameTime.running)

    def updatedCaptain(
        pirate: Pirate,
        collider: Collider.Box[String],
        pirateViewState: PirateViewState,
        captainClips: PirateClips,
        toScreenSpace: Vertex => Vertex
    ): Clip[Material.ImageEffects] =
      val onScreenBounds =
        BoundingBox(
          toScreenSpace(collider.position),
          toScreenSpace(collider.bounds.size)
        )

      val position =
        Vertex(onScreenBounds.center.x, onScreenBounds.bottom).toPoint

      pirate.state match
        case PirateState.Idle if pirateViewState.facingRight =>
          captainClips.idleRight
            .moveTo(position)

        case PirateState.Idle =>
          captainClips.idleLeft
            .moveTo(position)

        case PirateState.MoveLeft =>
          captainClips.moveLeft
            .moveTo(position)

        case PirateState.MoveRight =>
          captainClips.moveRight
            .moveTo(position)

        case PirateState.FallingRight =>
          captainClips.fallRight
            .moveTo(position)

        case PirateState.FallingLeft =>
          captainClips.fallLeft
            .moveTo(position)

        case PirateState.JumpingRight =>
          captainClips.jumpRight
            .moveTo(position)

        case PirateState.JumpingLeft =>
          captainClips.jumpLeft
            .moveTo(position)
