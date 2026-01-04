package conduit.infrastructure.postgres.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.UserRepository
import conduit.domain.model.entity.Credentials
import conduit.domain.model.types.user.{ Email, UserId }
import conduit.infrastructure.postgres.Transaction
import conduit.infrastructure.postgres.Transaction.Transactional
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.syntax.*
import zio.*

import conduit.infrastructure.postgres.meta.FieldMeta.given

class PostgresUserRepository(monitor: Monitor) extends UserRepository[Transaction] {
  override type Error = Transaction.Error

  override def exists(userId: UserId): Result[Boolean] =
    monitor.track("PostgresUserRepository.exists", "resource" -> "db") {
      Transactional:
        sql"""SELECT 1 FROM users WHERE id = $userId LIMIT 1"""
          .query[Boolean]
          .option
          .map(_.isDefined)
    }

  override def save(credential: Credentials.Hashed): Result[UserId] =
    monitor.track("PostgresUserRepository.save", "resource" -> "db") {
      Transactional.withTime: now =>
        sql"""INSERT INTO users (email, password, created_at, updated_at)
             |VALUES (${credential.email}, ${credential.password}, $now, $now)
             |RETURNING id"""
          .stripMargin
          .query[UserId]
          .unique
    }

  override def save(userId: UserId, credential: Credentials.Hashed): Result[Unit] =
    monitor.track("PostgresUserRepository.save", "resource" -> "db") {
      Transactional.withTime: now =>
        sql"""UPDATE users 
             |SET email = ${credential.email}, password = ${credential.password}, updated_at = $now
             |WHERE id = $userId"""
          .stripMargin
          .update
          .run
          .map(_ => ())
    }

  override def find(credential: Credentials.Hashed): Result[Option[UserId]] =
    monitor.track("PostgresUserRepository.find", "resource" -> "db") {
      Transactional:
        sql"""SELECT id FROM users WHERE email = ${credential.email} AND password = ${credential.password}"""
          .query[UserId]
          .option
    }

  override def findEmail(userId: UserId): Result[Option[Email]] =
    monitor.track("PostgresUserRepository.findEmail", "resource" -> "db") {
      Transactional:
        sql"""SELECT email FROM users WHERE id = $userId"""
          .query[Email]
          .option
    }

  override def findCredentials(userId: UserId): Result[Option[Credentials.Hashed]] =
    monitor.track("PostgresUserRepository.findCredentials", "resource" -> "db") {
      Transactional:
        sql"""SELECT email, password FROM users WHERE id = $userId"""
          .query[Credentials.Hashed]
          .option
    }

  override def emailExists(email: Email): Result[Boolean] =
    monitor.track("PostgresUserRepository.emailExists", "resource" -> "db") {
      Transactional:
        sql"""SELECT 1 FROM users WHERE email = $email LIMIT 1"""
          .query[Boolean]
          .option
          .map(_.isDefined)
    }
}

object PostgresUserRepository:
  val layer: ZLayer[Monitor, Nothing, UserRepository[Transaction]] =
    ZLayer {
      for monitor <- ZIO.service[Monitor]
      yield PostgresUserRepository(monitor)
    }
