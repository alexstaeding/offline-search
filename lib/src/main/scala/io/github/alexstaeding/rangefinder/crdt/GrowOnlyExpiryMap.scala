package io.github.alexstaeding.rangefinder.crdt

import java.time.OffsetDateTime
import scala.collection.SortedMap
import scala.math.Ordering.Implicits.*

type GrowOnlyExpiryMap[E] = Map[E, OffsetDateTime]

object GrowOnlyExpiryMap {
  def ofOne[E](
      element: E,
      expiration: OffsetDateTime = OffsetDateTime.now().plusHours(1),
  ): GrowOnlyExpiryMap[E] =
    Map(element -> expiration)

  given lattice[E]: Lattice[GrowOnlyExpiryMap[E]] =
    Lattice.mapLattice(using (left: OffsetDateTime, right: OffsetDateTime) => left max right)
}