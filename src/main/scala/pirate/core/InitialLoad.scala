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
        .toSpriteAndAnimations(dice, assets.captain.CaptainClownNose)
        .map(s => s.copy(sprite = s.sprite.withDepth(Depth(2))))
        .map { captain =>
          makeStartupData(
            captain,
            levelDataStore(screenDimensions, assetCollection, dice)
          )
        } match {
        case None =>
          Startup.Failure("Failed to start The Cursed Pirate")

        case Some(success) =>
          success
      }
    )

  def levelDataStore(
      screenDimensions: Rectangle,
      assetCollection: AssetCollection,
      dice: Dice
  ): Option[LevelDataStore] =
    val singleClipLoader: (AssetName, AssetName, String) => Either[String, Clip[Material.Bitmap]] =
      loadSingleClip(assetCollection, dice)

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
        helm <- singleClipLoader(
          assets.helm.ShipHelmData,
          assets.helm.ShipHelm,
          "Idle"
        ).toOption
        palm <- singleClipLoader(
          assets.trees.PalmTreeData,
          assets.trees.PalmTree,
          "P Front"
        ).toOption
        backPalm <- singleClipLoader(
          assets.trees.PalmTreeData,
          assets.trees.PalmTree,
          "P Back"
        ).toOption
        reflections <- singleClipLoader(
          assets.water.WaterReflectData,
          assets.water.WaterReflect,
          "Big"
        ).toOption
        flag <- singleClipLoader(
          assets.flag.FlagData,
          assets.flag.Flag,
          "Flapping"
        ).toOption
        terrain <- terrainData
      } yield makeAdditionalAssets(
        screenDimensions,
        helm,
        palm,
        backPalm,
        reflections,
        flag,
        terrain._1,
        terrain._2,
        terrain._3
      )
    else None

  // Helper function that loads Aseprite animations.
  def loadAnimation(
      assetCollection: AssetCollection,
      dice: Dice
  )(jsonRef: AssetName, name: AssetName, depth: Depth): Either[String, SpriteAndAnimations] =
    val res = for {
      json                <- assetCollection.findTextDataByName(jsonRef)
      aseprite            <- Json.asepriteFromJson(json)
      spriteAndAnimations <- aseprite.toSpriteAndAnimations(dice, name)
    } yield spriteAndAnimations.copy(sprite = spriteAndAnimations.sprite.withDepth(depth))

    res match
      case Some(spriteAndAnimations) =>
        Right(spriteAndAnimations)

      case None =>
        Left("Failed to load " + name)

  // Helper function that loads Aseprite clips.
  def loadClip(
      assetCollection: AssetCollection,
      dice: Dice
  )(jsonRef: AssetName, name: AssetName): Either[String, Map[CycleLabel, Clip[Material.Bitmap]]] =
    val res = for {
      json     <- assetCollection.findTextDataByName(jsonRef)
      aseprite <- Json.asepriteFromJson(json)
      clips    <- aseprite.toClips(name)
    } yield clips

    res match
      case Some(clips) =>
        Right(clips)

      case None =>
        Left(s"Failed to load $name")

  // Helper function that loads a single Aseprite clip.
  def loadSingleClip(
      assetCollection: AssetCollection,
      dice: Dice
  )(jsonRef: AssetName, name: AssetName, cycleName: String): Either[String, Clip[Material.Bitmap]] =
    loadClip(assetCollection, dice)(jsonRef, name).flatMap {
      _.get(CycleLabel(cycleName)) match
        case Some(clip) =>
          Right(clip)

        case None =>
          Left(s"No cycle named: $cycleName for asset: $name")
    }

  def makeAdditionalAssets(
      screenDimensions: Rectangle,
      helm: Clip[Material.Bitmap],
      palm: Clip[Material.Bitmap],
      backPalm: Clip[Material.Bitmap],
      waterReflections: Clip[Material.Bitmap],
      flag: Clip[Material.Bitmap],
      tileSize: Point,
      terrainMap: TiledGridMap[TileType],
      terrain: Group
  ): LevelDataStore =
    LevelDataStore(
      waterReflections
        .withDepth(Depth(20))
        .withRef(85, 0)
        .moveTo(screenDimensions.horizontalCenter, screenDimensions.verticalCenter + 5),
      flag.withDepth(Depth(9)).withRef(22, 105).moveTo(200, 288),
      helm.withRef(31, 49).moveTo(605, 160),
      palm.withDepth(Depth(1)),
      backPalm.withDepth(Depth(10)),
      tileSize,
      terrainMap,
      terrain
    )

  def makeStartupData(
      captain: SpriteAndAnimations,
      levelDataStore: Option[LevelDataStore]
  ): Startup.Success[StartupData] =
    Startup
      .Success(
        StartupData(
          captain.sprite
            .modifyMaterial(m => Material.ImageEffects(m.diffuse))
            .withRef(37, 64)
            .moveTo(300, 271),
          levelDataStore
        )
      )
      .addAnimations(captain.animations)

final case class StartupData(
    captain: Sprite[Material.ImageEffects],
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
