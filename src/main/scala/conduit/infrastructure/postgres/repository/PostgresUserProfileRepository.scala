package conduit.infrastructure.postgres.repository

import cats.syntax.list.*
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.UserProfileRepository
import conduit.domain.model.entity.UserProfile
import conduit.domain.model.types.article.ArticleId
import conduit.domain.model.types.user.{ Email, UserId, UserName }
import conduit.infrastructure.postgres.Transaction
import conduit.infrastructure.postgres.Transaction.Transactional
import conduit.infrastructure.postgres.meta.FieldMeta.given
import doobie.Fragments
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.syntax.*
import zio.*

class PostgresUserProfileRepository(monitor: Monitor) extends UserProfileRepository[Transaction] {
  override type Error = Transaction.Error

  override def exists(username: UserName): Result[Boolean] =
    monitor.track("PostgresUserProfileRepository.exists", "resource" -> "db") {
      Transactional:
        sql"""SELECT 1 FROM profiles WHERE name = $username LIMIT 1"""
          .query[Boolean]
          .option
          .map(_.isDefined)
    }

  override def exists(user: UserId, userName: UserName): Result[Boolean] =
    monitor.track("PostgresUserProfileRepository.exists", "resource" -> "db") {
      Transactional:
        sql"""SELECT 1 FROM profiles WHERE user_id = $user AND name = $userName LIMIT 1"""
          .query[Boolean]
          .option
          .map(_.isDefined)
    }

  override def findById(id: UserId): Result[Option[UserProfile]] =
    monitor.track("PostgresUserProfileRepository.findById", "resource" -> "db") {
      Transactional:
        sql"""SELECT user_id, name, bio, image, created_at, updated_at
             |FROM profiles
             |WHERE user_id = $id"""
          .stripMargin
          .query[UserProfile]
          .option
    }

  override def findAuthorOf(articleId: ArticleId): Result[Option[UserProfile]] =
    monitor.track("PostgresUserProfileRepository.findAuthorOf", "resource" -> "db") {
      Transactional:
        sql"""SELECT
             |p.user_id, p.name, p.bio, p.image, p.created_at, p.updated_at
             |FROM articles a
             |JOIN profiles p ON a.author_id = p.user_id
             |WHERE a.id = $articleId"""
          .stripMargin
          .query[UserProfile]
          .option
    }

  override def findByUserName(username: UserName): Result[Option[UserProfile]] =
    monitor.track("PostgresUserProfileRepository.findByUserName", "resource" -> "db") {
      Transactional:
        sql"""SELECT user_id, name, bio, image, created_at, updated_at
             |FROM profiles
             |WHERE name = $username"""
          .stripMargin
          .query[UserProfile]
          .option
    }

  override def findByIds(userIds: List[UserId]): Result[List[UserProfile]] =
    monitor.track("PostgresUserProfileRepository.findByIds", "resource" -> "db") {
      ZIO
        .foreach(userIds.toNel): userIds =>
          Transactional {
            sql"""SELECT user_id, name, bio, image, created_at, updated_at
                 |FROM profiles
                 |WHERE ${Fragments.in(fr"user_id", userIds)}"""
              .stripMargin
              .query[UserProfile]
              .to[List]
          }
        .someOrElse(List.empty)
    }

  override def save(user: UserProfile): Result[UserProfile] =
    monitor.track("PostgresUserProfileRepository.save", "resource" -> "db") {
      Transactional.withTime: now =>
        sql"""UPDATE profiles
             |SET name = ${user.data.name},
             |    bio = ${user.data.bio},
             |    image = ${user.data.image},
             |    updated_at = $now
             |WHERE user_id = ${user.id} 
             |RETURNING user_id, name, bio, image, created_at, updated_at"""
          .stripMargin
          .query[UserProfile]
          .unique
    }

  override def create(userId: UserId, user: UserProfile.Data): Result[UserProfile] =
    monitor.track("PostgresUserProfileRepository.create", "resource" -> "db") {
      Transactional.withTime: now =>
        sql"""INSERT INTO profiles (user_id, name, bio, image, created_at, updated_at)
             |VALUES ($userId, ${user.name}, ${user.bio}, ${user.image}, $now, $now)
             |RETURNING user_id, name, bio, image, created_at, updated_at"""
          .stripMargin
          .query[UserProfile]
          .unique
    }
}

object PostgresUserProfileRepository:
  val layer: ZLayer[Monitor, Nothing, UserProfileRepository[Transaction]] = ZLayer {
    for monitor <- ZIO.service[Monitor]
    yield new PostgresUserProfileRepository(monitor)
  }
