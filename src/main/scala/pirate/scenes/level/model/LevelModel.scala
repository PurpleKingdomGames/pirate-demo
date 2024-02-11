package pirate.scenes.level.model

import indigo.*
import indigo.physics.World
import pirate.generated.Assets.*
import indigo.physics.*
import pirate.core.SpaceConvertors

/*
The model cannot be initialised at game start up, because we want to load
some data during the loading screen, parse it, and use it to generate part
of the model. We _could_ represent that with an Option, but that could get
messy.
 */
enum LevelModel:
  case NotReady
  case Ready(pirate: Pirate, mapHeight: Int, world: World[String])

  def notReady: Boolean =
    this match
      case NotReady       => true
      case Ready(_, _, _) => false

  def update(gameTime: GameTime, inputState: InputState): Outcome[LevelModel] =
    this match
      case NotReady =>
        Outcome(this)

      case Ready(pirate, mapHeight, world) =>
        val inputForce =
          inputState.mapInputs(Pirate.inputMappings(pirate.state.inMidAir), Vector2.zero)

        val currentY = world.findByTag("pirate").map(_.position.y)

        world
          .modifyByTag("pirate") { p =>
            p.withVelocity(Vector2(inputForce.x, p.velocity.y + inputForce.y))
          }
          .update(gameTime.delta)
          .map {
            _.modifyByTag("chest") {
              case c: Collider.Box[_] =>
                if c.position.y > mapHeight.toDouble + 1 then c.withPosition(Vertex(5, -2))
                else c

              case c =>
                c
            }
          }
          .flatMap { w =>
            w.findByTag("pirate").headOption match
              case None =>
                Outcome(Ready(pirate, mapHeight, w))

              case Some(p) =>
                val yDiff =
                  Math.abs(p.position.y - currentY.headOption.getOrElse(0.0))
                val nextState =
                  Pirate.decideNextState(pirate.state, p.velocity, inputForce, yDiff)

                // Respawn if the pirate is below the bottom of the map.
                val nextPirate =
                  if p.position.y > mapHeight.toDouble + 1 then
                    Outcome(Pirate(nextState, gameTime.running))
                      .addGlobalEvents(
                        assets.sounds.respawnPlay,
                        PirateRespawn(Pirate.respawnPoint)
                      )
                  else
                    val maybeJumpSound =
                      if (!pirate.state.inMidAir && nextState.isJumping)
                        Batch(assets.sounds.jumpPlay)
                      else Batch.empty

                    Outcome(Pirate(nextState, pirate.lastRespawn))
                      .addGlobalEvents(maybeJumpSound)

                nextPirate.map(p => Ready(p, mapHeight, w))
          }

object LevelModel:

  def makeReady(pirate: Pirate, mapHeight: Int, spaceConvertors: SpaceConvertors): LevelModel.Ready =
    val platforms =
      Batch(
        BoundingBox(Vertex(0, 3), Vertex(2, 1)),
        BoundingBox(Vertex(17, 5), Vertex(3, 1)),
        BoundingBox(Vertex(4, 9), Vertex(11, 1)),
        BoundingBox(Vertex(16, 10), Vertex(2, 1))
      ).map { b =>
        Collider.Box("platform", b).makeStatic.withFriction(Friction(0.5))
      }

    LevelModel.Ready(
      pirate,
      mapHeight,
      World
        .empty[String](SimulationSettings(BoundingBox(0, 0, 1280, 720)))
        .withResistance(Resistance(0.01))
        .withForces(Vector2(0, 30))
        .withColliders(platforms)
        .addColliders(
          Collider
            .Box("pirate", Pirate.initialBounds(spaceConvertors))
        )
        .addColliders(
          Collider
            .Box("chest", BoundingBox(Vertex(5, 6), spaceConvertors.ScreenToWorld.convert(Point(30, 25))))
            .withRestitution(Restitution(0))
        )
    )
