package io.github.alexstaeding.rangefinder.network

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import io.github.alexstaeding.rangefinder.meta.{PartialKey, PartialKeyUniverse}
import org.apache.logging.log4j.Logger

sealed trait IndexEntry[+V, +P]

object IndexEntry {
  final case class Funnel[+V](targetId: NodeId, searchKey: PartialKey[V]) extends IndexEntry[V, Nothing]

  final case class Value[+V, +P](owner: NodeId, value: V, payload: P) extends IndexEntry[V, P]

  given codec[V: JsonValueCodec, P: JsonValueCodec]: JsonValueCodec[IndexEntry[V, P]] = JsonCodecMaker.make

  object Value {
    given ordering[V: Ordering, P]: Ordering[IndexEntry.Value[V, P]] =
      (x: IndexEntry.Value[V, P], y: IndexEntry.Value[V, P]) => summon[Ordering[V]].compare(x.value, y.value)
  }
}

extension [V](entry: IndexEntry[V, ?]) {
  def getIndexKeys(using universe: PartialKeyUniverse[V]): Set[PartialKey[V]] =
    entry match
      case IndexEntry.Funnel(_, search)  => universe.getIndexKeys(search)
      case IndexEntry.Value(_, value, _) => universe.getIndexKeys(value)

  def getIndexKeysOption(using universe: PartialKeyUniverse[V], logger: Logger): Option[Set[PartialKey[V]]] =
    try Some(getIndexKeys)
    catch {
      case e: Exception =>
        logger.error(s"Failed to get root keys for entry $entry", e)
        None
    }
}
