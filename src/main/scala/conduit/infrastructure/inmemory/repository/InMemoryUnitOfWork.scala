package conduit.infrastructure.inmemory.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.UnitOfWork
import conduit.domain.model.error.ApplicationError
import zio.{ Exit, ZEnvironment, ZIO, ZLayer }

class InMemoryUnitOfWork(monitor: Monitor, appState: State) extends UnitOfWork[Transaction] {

  override def execute[R, A](effect: ZIO[R & Transaction, ApplicationError, A]): ZIO[R, ApplicationError, A] =
    ZIO.acquireReleaseExitWith(transaction)(commit) { transaction =>
      monitor.track("InMemoryUnitOfWork.execute") {
        effect.provideSomeEnvironment(_.union(ZEnvironment(transaction)))
      }
    }

  private def transaction: ZIO[Any, Nothing, Transaction] =
    monitor.track("InMemoryUnitOfWork.transaction", "resource" -> "db") {
      appState.duplicate.map(Transaction(_))
    }

  private def commit: (Transaction, Exit[Any, Any]) => ZIO[Any, Nothing, Unit] = (tx, exit) =>
    monitor.track("InMemoryUnitOfWork.commit", "resource" -> "db") {
      tx -> exit match {
        case (transaction, Exit.Success(_)) => appState.merge(transaction.state)
        case (_, Exit.Failure(e))           => ZIO.logError(s"Transaction failed, rolling back: $e")
      }
    }
}

object InMemoryUnitOfWork:
  val layer: ZLayer[Monitor, Nothing, UnitOfWork[Transaction]] = ZLayer {
    for {
      appState <- State.empty
      monitor  <- ZIO.service[Monitor]
    } yield InMemoryUnitOfWork(monitor, appState)
  }
