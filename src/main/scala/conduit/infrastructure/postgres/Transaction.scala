package conduit.infrastructure.postgres

import cats.effect.Resource
import conduit.domain.model.error.ApplicationError
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Strategy
import doobie.{ KleisliInterpreter, LogHandler, Transactor }
import zio.interop.catz.*
import zio.{ Clock, Task, ZIO }

import java.sql.Connection
import java.time.Instant

trait Transaction {
  def transactor: Transactor[Task]
  def connection: Connection
}

object Transaction:
  case class Live(transactor: Transactor[Task], connection: Connection) extends Transaction

  // @todo refine error cases
  case class Error(reason: Throwable) extends ApplicationError.FromException(reason)

  def make(connection: Connection): Transaction = {
    val connect = (c: Connection) => Resource.pure[Task, Connection](c)
    val interp  = KleisliInterpreter[Task](LogHandler.noop).ConnectionInterpreter
    Live(Transactor(connection, connect, interp, Strategy.void), connection)
  }

  object Transactional:
    def apply[A](action: ConnectionIO[A]): ZIO[Transaction, Error, A] =
      execute(action)

    def withTime[A](action: Instant => ConnectionIO[A]): ZIO[Transaction, Error, A] =
      for {
        now    <- Clock.instant
        result <- execute(action(now))
      } yield result

    def execute[A](action: ConnectionIO[A]): ZIO[Transaction, Error, A] =
      for {
        tx     <- ZIO.service[Transaction].map(_.transactor.trans)
        result <- tx.apply(action).mapError(Error(_))
      } yield result
