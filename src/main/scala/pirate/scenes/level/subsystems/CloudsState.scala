package pirate.scenes.level.subsystems

import indigo.*

final case class CloudsState(bigCloudPosition: Double, lastSpawn: Seconds)

object CloudsState:
  val initial: CloudsState =
    CloudsState(0, Seconds.zero)
