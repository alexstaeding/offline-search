package io.github.alexstaeding.offlinesearch.network

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

import java.util.UUID

/** Represents a message sent by or received from a [[NetworkAdapter]].
  */
sealed trait NetworkEvent[V] {

  /** Persistent ID for all messages in this communication
    */
  val requestId: UUID
}

sealed trait AnswerEvent[V] extends NetworkEvent[V] {
  type Content
  val content: Content
}

sealed trait OptionAnswerEvent[V] extends AnswerEvent[V] {
  override type Content = Option[V]
}

sealed trait BooleanAnswerEvent[V] extends AnswerEvent[V] {
  override type Content = Boolean
}

sealed trait RequestEvent[V] extends NetworkEvent[V] {

  type Answer <: AnswerEvent[V]

  /** The ID of the node that sent this message
    */
  val sourceInfo: NodeInfo

  /** The ID of the node that being searched
    */
  val targetId: NodeId

  def createRedirect(closerTargetInfo: NodeInfo): Left[RedirectEvent[V], Answer] = Left(RedirectEvent(requestId, closerTargetInfo))
}

object NetworkEvent {
  given codec[V: JsonValueCodec]: JsonValueCodec[NetworkEvent[V]] = JsonCodecMaker.make
}

object AnswerEvent {
  given codec[V: JsonValueCodec]: JsonValueCodec[AnswerEvent[V]] = JsonCodecMaker.make
}

object RequestEvent {
  given codec[V: JsonValueCodec]: JsonValueCodec[RequestEvent[V]] = JsonCodecMaker.make

  def createPing[V](localNodeInfo: NodeInfo, targetId: NodeId): PingEvent[V] =
    PingEvent[V](UUID.randomUUID(), localNodeInfo, targetId)

  def createFindNode[V](localNodeInfo: NodeInfo, targetId: NodeId): FindNodeEvent[V] =
    FindNodeEvent[V](UUID.randomUUID(), localNodeInfo, targetId)

  def createFindValue[V](localNodeInfo: NodeInfo, targetId: NodeId): FindValueEvent[V] =
    FindValueEvent[V](UUID.randomUUID(), localNodeInfo, targetId)

  def createStoreValue[V](localNodeInfo: NodeInfo, targetId: NodeId, value: V): StoreValueEvent[V] =
    StoreValueEvent[V](UUID.randomUUID(), localNodeInfo, targetId, value)
}

case class PingEvent[V](
    override val requestId: UUID,
    override val sourceInfo: NodeInfo,
    override val targetId: NodeId,
) extends RequestEvent[V] {
  override type Answer = PingAnswerEvent[V]
  def createAnswer(content: Boolean): Right[RedirectEvent[V], PingAnswerEvent[V]] =
    Right(PingAnswerEvent(requestId, content))
}

case class FindNodeEvent[V](
    override val requestId: UUID,
    override val sourceInfo: NodeInfo,
    override val targetId: NodeId,
) extends RequestEvent[V] {
  override type Answer = FindNodeAnswerEvent[V]
  def createAnswer(content: Boolean): Right[RedirectEvent[V], FindNodeAnswerEvent[V]] =
    Right(FindNodeAnswerEvent(requestId, content))
}

case class FindValueEvent[V](
    override val requestId: UUID,
    override val sourceInfo: NodeInfo,
    override val targetId: NodeId,
) extends RequestEvent[V] {
  override type Answer = FindValueAnswerEvent[V]
  def createAnswer(content: Option[V]): Right[RedirectEvent[V], FindValueAnswerEvent[V]] =
    Right(FindValueAnswerEvent(requestId, content))
}

case class StoreValueEvent[V](
    override val requestId: UUID,
    override val sourceInfo: NodeInfo,
    override val targetId: NodeId,
    value: V,
) extends RequestEvent[V] {
  override type Answer = StoreValueAnswerEvent[V]
  def createAnswer(content: Boolean): Right[RedirectEvent[V], StoreValueAnswerEvent[V]] =
    Right(StoreValueAnswerEvent(requestId, content))
}

case class PingAnswerEvent[V](override val requestId: UUID, override val content: Boolean) extends BooleanAnswerEvent[V]
case class FindNodeAnswerEvent[V](override val requestId: UUID, override val content: Boolean) extends BooleanAnswerEvent[V]
case class FindValueAnswerEvent[V](override val requestId: UUID, override val content: Option[V]) extends OptionAnswerEvent[V]
case class StoreValueAnswerEvent[V](override val requestId: UUID, override val content: Boolean) extends BooleanAnswerEvent[V]

case class RedirectEvent[V](override val requestId: UUID, closerTargetInfo: NodeInfo) extends AnswerEvent[V] {
  override type Content = Nothing
  override val content: Nothing = throw new NoSuchElementException
}
