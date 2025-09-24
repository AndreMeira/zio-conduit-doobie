package conduit.infrastructure.inmemory.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.UserRepository
import conduit.domain.model.entity.Credentials
import conduit.domain.model.types.user.UserId
import zio.{ ZIO, ZLayer }

class InMemoryUserRepository(monitor: Monitor) extends UserRepository[Transaction] {
  override type Error = Nothing

  override def exists(userId: UserId): Result[Boolean] =
    monitor.track("InMemoryUserRepository.exists", "resource" -> "db") {
      Transaction.execute: state =>
        state.users.get.map { users =>
          users.contains(userId)
        }
    }

  override def save(credential: Credentials.Hashed): Result[UserId] =
    monitor.track("InMemoryUserRepository.save", "resource" -> "db") {
      Transaction.execute: state =>
        for {
          id <- state.nextId.map(UserId(_))
          _  <- state.users.update(_ + (id -> credential))
        } yield id
    }

  override def save(userId: UserId, credential: Credentials.Hashed): Result[Unit] =
    monitor.track("InMemoryUserRepository.save", "resource" -> "db") {
      Transaction.execute: state =>
        state.users.update(_ + (userId -> credential))
    }

  override def find(credential: Credentials.Hashed): Result[Option[UserId]] =
    monitor.track("InMemoryUserRepository.find", "resource" -> "db") {
      Transaction.execute: state =>
        state.users.get.map { users =>
          users.collectFirst { case (id, cred) if cred == credential => id }
        }
    }

  override def findEmail(userId: UserId): Result[Option[conduit.domain.model.types.user.Email]] =
    monitor.track("InMemoryUserRepository.findEmail", "resource" -> "db") {
      Transaction.execute: state =>
        state.users.get.map { users =>
          users.get(userId).map(_.email)
        }
    }

  override def findCredentials(userId: UserId): Result[Option[Credentials.Hashed]] =
    monitor.track("InMemoryUserRepository.findCredentials", "resource" -> "db") {
      Transaction.execute: state =>
        state.users.get.map { users =>
          users.get(userId)
        }
    }

  override def emailExists(email: conduit.domain.model.types.user.Email): Result[Boolean] =
    monitor.track("InMemoryUserRepository.emailExists", "resource" -> "db") {
      Transaction.execute: state =>
        state.users.get.map { users =>
          users.values.exists(_.email == email)
        }
    }
}

object InMemoryUserRepository:
  val layer: ZLayer[Monitor, Nothing, UserRepository[Transaction]] =
    ZLayer {
      for monitor <- zio.ZIO.service[Monitor]
      yield InMemoryUserRepository(monitor)
    }
