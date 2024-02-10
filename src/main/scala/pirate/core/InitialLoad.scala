package pirate.core

import indigo.*
import indigo.json.Json
import indigo.shared.formats.TiledGridMap
import pirate.generated.Assets.*
import pirate.generated.CaptainAnim

object InitialLoad:

  /*
  In a nutshell, the setup function here takes the boot data (screen dimensions),
  the asset collection, and a dice object, and produces "start up data", which is
  totally user defined and you can do that however you like, you just need to return
  a success or failure object.

  What's really important to understand is that this function is run _more than once!_

  The first time it runs, we only have available the assets we told indigo we needed
  for the loading screen. We find this out by simply checking which assets are available
  at the moment.

  The second run is triggered by the completion of a dynamic asset load - you see the
  progress of which on the loading screen. This can theoretically happen as many times
  as you decide to load assets. So it's only on the second run that we do all the work
  in `makeAdditionalAssets`.
   */
  def setup(
      screenDimensions: Rectangle,
      assetCollection: AssetCollection,
      dice: Dice
  ): Outcome[Startup[StartupData]] =
    Outcome(
      CaptainAnim.aseprite
        .toClips(assets.captain.CaptainClownNose)
        .map { captainClips =>
          makeStartupData(
            captainClips,
            levelDataStore(screenDimensions, assetCollection, dice)
          )
        } match
        case None =>
          Startup.Failure("Failed to start The Cursed Pirate")

        case Some(success) =>
          success
    )

  def makeStartupData(
      captainClips: Map[CycleLabel, Clip[Material.Bitmap]],
      levelDataStore: Option[LevelDataStore]
  ): Startup[StartupData] =
    val captainClipsPrepared =
      captainClips.map { case (label, clip) =>
        label ->
          clip
            .withDepth(Depth(2))
            .modifyMaterial(m => Material.ImageEffects(m.diffuse))
            .withRef(37, 64)
            .moveTo(300, 271)
      }

    val pirateClips =
      for {
        idle <- captainClipsPrepared.get(CycleLabel("Idle"))
        run  <- captainClipsPrepared.get(CycleLabel("Run"))
        fall <- captainClipsPrepared.get(CycleLabel("Fall"))
        jump <- captainClipsPrepared.get(CycleLabel("Jump"))
      } yield PirateClips(
        idleLeft = idle.flipHorizontal(true).withRef(idle.ref.moveBy(20, 0)),
        idleRight = idle,
        moveLeft = run.flipHorizontal(true).withRef(run.ref.moveBy(20, 0)),
        moveRight = run,
        fallLeft = fall.flipHorizontal(true).withRef(fall.ref.moveBy(20, 0)),
        fallRight = fall,
        jumpLeft = jump.flipHorizontal(true).withRef(jump.ref.moveBy(20, 0)),
        jumpRight = jump
      )

    pirateClips match
      case None =>
        Startup.Failure("Pirate captain animations failed to load")

      case Some(pcs) =>
        Startup
          .Success(
            StartupData(
              pcs.moveRight.modifyMaterial(_.withOverlay(Fill.Color(RGBA.White))),
              pcs,
              levelDataStore
            )
          )

  def levelDataStore(
      screenDimensions: Rectangle,
      assetCollection: AssetCollection,
      dice: Dice
  ): Option[LevelDataStore] =
    // If these assets haven't been loaded yet, we're not going to try and process anything.
    if assetCollection.findTextDataByName(assets.helm.ShipHelmData).isDefined &&
      assetCollection.findTextDataByName(assets.trees.PalmTreeData).isDefined &&
      assetCollection.findTextDataByName(assets.water.WaterReflectData).isDefined &&
      assetCollection.findTextDataByName(assets.flag.FlagData).isDefined &&
      assetCollection.findTextDataByName(assets.static.terrainData).isDefined
    then
      val tileMapper: Int => TileType =
        case 0 => TileType.Empty
        case _ => TileType.Solid

      // Here we read the Tiled level description and manufacture a triple of:
      // (the tile size, a `TiledGridMap` of data, and a renderable verison of the map)
      val terrainData: Option[(Point, TiledGridMap[TileType], Group)] =
        for {
          json         <- assetCollection.findTextDataByName(assets.static.terrainData)
          tileMap      <- Json.tiledMapFromJson(json)
          terrainGroup <- tileMap.toGroup(assets.static.terrain)
          grid         <- tileMap.toGrid(tileMapper)
        } yield (Point(tileMap.tilewidth, tileMap.tileheight), grid, terrainGroup.withDepth(Depth(4)))

      for {

        helm <- loadSingleClip(assetCollection, assets.helm.ShipHelmData, assets.helm.ShipHelm, "Idle")

        palm <- loadSingleClip(
          assetCollection,
          assets.trees.PalmTreeData,
          assets.trees.PalmTree,
          "P Front"
        )

        backPalm <- loadSingleClip(
          assetCollection,
          assets.trees.PalmTreeData,
          assets.trees.PalmTree,
          "P Back"
        )

        reflections <- loadSingleClip(
          assetCollection,
          assets.water.WaterReflectData,
          assets.water.WaterReflect,
          "Big"
        )

        flag <- loadSingleClip(assetCollection, assets.flag.FlagData, assets.flag.Flag, "Flapping")

        terrain <- terrainData

      } yield LevelDataStore(
        reflections
          .withDepth(Depth(20))
          .withRef(85, 0)
          .moveTo(screenDimensions.horizontalCenter, screenDimensions.verticalCenter + 5),
        flag.withDepth(Depth(9)).withRef(22, 105).moveTo(200, 288),
        helm.withRef(31, 49).moveTo(605, 160),
        palm.withDepth(Depth(1)),
        backPalm.withDepth(Depth(10)),
        terrain._1,
        terrain._2,
        terrain._3
      )
    else None

  private def loadSingleClip(
      assetCollection: AssetCollection,
      jsonRef: AssetName,
      name: AssetName,
      cycleName: String
  ): Option[Clip[Material.Bitmap]] =
    for {
      json     <- assetCollection.findTextDataByName(jsonRef)
      aseprite <- Json.asepriteFromJson(json)
      clips    <- aseprite.toClips(name)
      clip     <- clips.get(CycleLabel(cycleName))
    } yield clip

final case class StartupData(
    captainLoading: Clip[Material.ImageEffects],
    captainClips: PirateClips,
    levelDataStore: Option[LevelDataStore]
)
final case class LevelDataStore(
    waterReflections: Clip[Material.Bitmap],
    flag: Clip[Material.Bitmap],
    helm: Clip[Material.Bitmap],
    palm: Clip[Material.Bitmap],
    backTallPalm: Clip[Material.Bitmap],
    tileSize: Point,
    terrainMap: TiledGridMap[TileType],
    terrain: Group
)

enum TileType:
  case Empty, Solid

final case class PirateClips(
    idleLeft: Clip[Material.ImageEffects],
    idleRight: Clip[Material.ImageEffects],
    moveLeft: Clip[Material.ImageEffects],
    moveRight: Clip[Material.ImageEffects],
    fallLeft: Clip[Material.ImageEffects],
    fallRight: Clip[Material.ImageEffects],
    jumpLeft: Clip[Material.ImageEffects],
    jumpRight: Clip[Material.ImageEffects]
)
