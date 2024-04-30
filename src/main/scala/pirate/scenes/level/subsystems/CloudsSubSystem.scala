package pirate.scenes.level.subsystems

import indigo.*
import indigoextras.subsystems.*
import pirate.core.Assets
import pirate.core.LayerKeys
import pirate.core.Model

/*
The `CloudsSubSystem` does two things:
1. Directly manages the constantly rolling big clouds on the horizon;
2. Emits periodic events telling the cloud automata system to spawn
   a new small cloud.
 */
final case class CloudsSubSystem(screenWidth: Int) extends SubSystem[Model]:

  val verticalCenter: Int = 181

  type EventType      = FrameTick
  type SubSystemModel = CloudsState
  type ReferenceData  = Unit

  def id: SubSystemId = SubSystemId("clouds")

  lazy val eventFilter: GlobalEvent => Option[FrameTick] = {
    case FrameTick => Some(FrameTick)
    case _         => None
  }

  def reference(model: Model): ReferenceData =
    ()

  def initialModel: Outcome[CloudsState] =
    Outcome(CloudsState.initial)

  def update(context: SubSystemFrameContext[Unit], model: SubSystemModel): EventType => Outcome[SubSystemModel] =
    case FrameTick if context.gameTime.running - model.lastSpawn > Seconds(3.0) =>
      Outcome(
        CloudsState(
          bigCloudPosition = CloudsSubSystem.nextBigCloudPosition(
            context.gameTime,
            model.bigCloudPosition,
            Assets.Clouds.bigCloudsWidth
          ),
          lastSpawn = context.gameTime.running
        )
      ).addGlobalEvents(
        AutomataEvent.Spawn(
          CloudsAutomata.poolKey,
          CloudsSubSystem.generateSmallCloudStartPoint(screenWidth, context.dice),
          CloudsSubSystem.generateSmallCloudLifeSpan(context.dice),
          None
        )
      )

    case FrameTick =>
      Outcome(
        model.copy(
          bigCloudPosition = CloudsSubSystem.nextBigCloudPosition(
            context.gameTime,
            model.bigCloudPosition,
            Assets.Clouds.bigCloudsWidth
          )
        )
      )

  def present(context: SubSystemFrameContext[Unit], model: SubSystemModel): Outcome[SceneUpdateFragment] =
    Outcome(
      SceneUpdateFragment.empty
        .addLayer(
          LayerKeys.bigClouds ->
            Layer(
              Assets.Clouds.bigCloudsGraphic
                .moveTo(
                  model.bigCloudPosition.toInt - Assets.Clouds.bigCloudsWidth,
                  verticalCenter
                ),
              Assets.Clouds.bigCloudsGraphic
                .moveTo(
                  model.bigCloudPosition.toInt,
                  verticalCenter
                ),
              Assets.Clouds.bigCloudsGraphic
                .moveTo(
                  model.bigCloudPosition.toInt + Assets.Clouds.bigCloudsWidth,
                  verticalCenter
                )
            )
        )
    )

object CloudsSubSystem:

  val scrollSpeed: Double = 3.0d

  def generateSmallCloudStartPoint(screenWidth: Int, dice: Dice): Point =
    Point(screenWidth + dice.roll(30), dice.roll(100) + 10)

  def generateSmallCloudLifeSpan(dice: Dice): Option[Seconds] =
    Some(Millis(((dice.roll(10) + 10) * 1000).toLong).toSeconds)

  def nextBigCloudPosition(gameTime: GameTime, bigCloudPosition: Double, assetWidth: Int): Double =
    if bigCloudPosition <= 0.0d then assetWidth.toDouble
    else bigCloudPosition - (scrollSpeed * gameTime.delta.toDouble)
