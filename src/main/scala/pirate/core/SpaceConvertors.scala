package pirate.core

import indigo.Vertex
import indigo.Point

final case class SpaceConvertors(pixelsPerUnit: Point):

  object ScreenToWorld:

    def convert(i: Int): Double =
      i.toDouble / pixelsPerUnit.x.toDouble

    def convert(p: Point): Vertex =
      p.toVertex / pixelsPerUnit.toVertex

  object WorldToScreen:

    def convert(d: Double): Int =
      (d * pixelsPerUnit.x.toDouble).toInt

    def convert(v: Vertex): Point =
      (v * pixelsPerUnit.toVertex).toPoint
