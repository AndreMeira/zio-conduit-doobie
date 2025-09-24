package conduit.infrastructure.inmemory.monitor

import conduit.domain.logic.monitoring.Monitor
import conduit.infrastructure.inmemory.monitor.InMemoryMonitor.makeSpanId
import zio.{ Clock, FiberRef, Ref, Scope, ZIO, ZLayer }

import java.time.Instant
import java.util.UUID

class InMemoryMonitor(current: FiberRef[Span], traces: Ref[List[Span]]) extends Monitor {

  def getTraces: ZIO[Any, Nothing, List[Span]] = traces.get

  override def start[R, E, A](name: String)(effect: => ZIO[R, E, A]): ZIO[R, E, A] =
    for {
      start              <- Clock.instant
      _                  <- logStart(start, name)
      spanId             <- makeSpanId
      _                  <- current.set(Span.make(spanId, name, start))
      (duration, either) <- effect.either.timed
      _                  <- ZIO.when(either.isLeft)(ZIO.logError(either.toString))
      span               <- current.get
      handled             = either.fold(span.withError(_), _ => span).withDuration(duration).correctedTimeline
      _                  <- traces.update(stack => (handled :: stack).take(10))
      result             <- ZIO.fromEither(either)
    } yield result

  override def track[R, E, A](name: String, tags: (String, String)*)(effect: => ZIO[R, E, A]): ZIO[R, E, A] =
    for {
      start              <- Clock.instant
      _                  <- logStart(start, name, tags*)
      parent             <- current.get
      spanId             <- makeSpanId
      span                = Span.make(spanId, name, start).withTags(tags*)
      _                  <- current.set(span)
      (duration, either) <- effect.either.timed
      _                  <- ZIO.when(either.isLeft)(ZIO.log(either.toString))
      updated            <- current.get
      handled             = updated.withDuration(duration).correctedTimeline
      _                  <- current.set(parent.addChild(handled))
      result             <- ZIO.fromEither(either)
    } yield result

  private def logStart(now: Instant, name: String, tags: (String, String)*): ZIO[Any, Nothing, Unit] =
    ZIO.log:
      val tagPrint = tags.map((key, value) => s"$key=$value")
      s"Span started $name at $now [${tagPrint.mkString(", ")}]"
}

object InMemoryMonitor:

  val layer: ZLayer[Any, Nothing, Monitor] = ZLayer.scoped(make)

  private def makeSpanId: ZIO[Any, Nothing, String] =
    ZIO.succeed(UUID.randomUUID().toString)

  private def make: ZIO[Scope, Nothing, InMemoryMonitor] =
    for {
      id      <- makeSpanId
      now     <- Clock.instant
      current <- FiberRef.make(Span.make(id, "*", now), forkSpan, mergeSpan)
      traces  <- Ref.make(List.empty)
    } yield InMemoryMonitor(current, traces)

  private def forkSpan(span: Span): Span =
    // @todo use a sum type to represent a forked span
    Span.make(s"fork-${span.id.value}", "fork", span.timeline.start).withTag(":forked", "true")

  private def mergeSpan(child: Span, parent: Span): Span =
    if child.isEmpty then parent
    else parent.copy(children = parent.children ++ child.children)
