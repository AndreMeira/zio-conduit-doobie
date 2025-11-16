package conduit.infrastructure.postgres

import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.UnitOfWork
import conduit.domain.model.error.ApplicationError
import zio.{ Exit, ZEnvironment, ZIO, ZLayer }

class PostgresUnitOfWork(
  monitor: Monitor,
  dataSource: HikariDataSource,
) extends UnitOfWork[Transaction] {

  override def execute[R, A](effect: ZIO[R & Transaction, ApplicationError, A]): ZIO[R, ApplicationError, A] =
    ZIO.acquireReleaseExitWith(transaction)(commit) { transaction =>
      monitor.track("PostgresUnitOfWork.execute") {
        effect.provideSomeEnvironment(_.union(ZEnvironment(transaction)))
      }
    }

  private def transaction: ZIO[Any, ApplicationError, Transaction] =
    monitor.track("PostgresUnitOfWork.transaction", "resource" -> "db") {
      ZIO
        .attemptBlocking(dataSource.getConnection)
        .map(Transaction.make)
        .mapError(Transaction.Error(_))
    }

  private def commit(transaction: Transaction, exit: Exit[Any, Any]): ZIO[Any, Nothing, Unit] =
    monitor
      .track("PostgresUnitOfWork.commit", "resource" -> "db") {
        exit match {
          case Exit.Success(_) =>
            ZIO.attemptBlocking {
              transaction.connection.commit()
              transaction.connection.close()
            }
          case Exit.Failure(e) =>
            ZIO.attemptBlocking {
              transaction.connection.rollback()
              transaction.connection.close()
            } *> ZIO.logError(s"Transaction failed, rolling back: $e")
        }
      }
      .tapError(err => ZIO.logError(s"$err"))
      .ignore
}

object PostgresUnitOfWork {
  val layer = ZLayer.scoped {
    for {
      datasource <- ZIO.service[HikariDataSource]
      monitor    <- ZIO.service[Monitor]
    } yield PostgresUnitOfWork(monitor, datasource)
  }
}
