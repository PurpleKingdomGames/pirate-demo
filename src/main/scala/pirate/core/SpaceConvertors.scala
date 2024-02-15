package pirate.core

import indigo.Vertex
import indigo.Point
import indigo.Rectangle
import indigo.BoundingBox

final case class SpaceConvertors(pixelsPerUnit: Point):

  object ScreenToWorld:

    def convert(i: Int): Double =
      i.toDouble / pixelsPerUnit.x.toDouble

    def convert(p: Point): Vertex =
      p.toVertex / pixelsPerUnit.toVertex

    def convert(r: Rectangle): BoundingBox =
      BoundingBox(
        convert(r.position),
        convert(r.size.toPoint)
      )

  object WorldToScreen:

    def convert(d: Double): Int =
      (d * pixelsPerUnit.x.toDouble).toInt

    def convert(v: Vertex): Point =
      (v * pixelsPerUnit.toVertex).toPoint

    def convert(b: BoundingBox): Rectangle =
      Rectangle(
        convert(b.position),
        convert(b.size).toSize
      )
