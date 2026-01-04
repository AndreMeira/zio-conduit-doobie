package conduit.infrastructure.inmemory.monitor

import conduit.infrastructure.inmemory.monitor.Span.Timeline
import scala.math.Ordering.Implicits.infixOrderingOps

import java.time.{ Duration, Instant }

case class Span(id: Span.Id, data: Span.Data, timeline: Span.Timeline, children: List[Span]) {
  def isEmpty: Boolean =
    children.isEmpty || children.forall(_.isEmpty)

  def addChild(child: Span): Span =
    copy(children = children :+ child)

  def withDuration(duration: Duration): Span =
    copy(timeline = timeline.copy(duration = duration))

  def withTag(key: String, value: String): Span =
    copy(data = data.withTag(key, value))

  def withTags(tags: (String, String)*): Span =
    copy(data = data.withTags(tags*))

  def withError(error: Any): Span =
    withTags(
      "error"         -> "true",
      "error.message" -> error.toString,
      "error.type"    -> error.getClass.getSimpleName,
    )

  def correctedTimeline: Span = copy(
    timeline = timeline.copy(duration = correctedDuration),
    children = children.map(_.correctedTimeline),
  )

  private def correctedDuration: Duration =
    if children.isEmpty then timeline.duration
    else
      val end      = children.map(_.timeline.end).max
      val duration = Duration.between(timeline.start, end)
      timeline.duration max duration
}

object Span:
  case class Id(value: String)
  case class Timeline(start: Instant, duration: Duration)  {
    def finish(instant: Instant): Timeline = copy(duration = Duration.between(start, instant))
    def end: Instant                       = start.plus(duration)
  }
  case class Data(name: String, tags: Map[String, String]) {
    def withTag(key: String, value: String): Data = copy(tags = tags + (key -> value))
    def withTags(tags: (String, String)*): Data   = copy(tags = this.tags ++ tags.toMap)
  }

  def make(id: String, name: String, start: Instant): Span =
    Span(Id(id), Data(name, Map.empty), Timeline(start, Duration.ZERO), List.empty)
