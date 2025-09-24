package conduit.infrastructure.postgres.repository

import cats.syntax.list.*
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.FollowerRepository
import conduit.domain.model.entity.Follower
import conduit.domain.model.types.article.AuthorId
import conduit.domain.model.types.user.UserId
import conduit.infrastructure.postgres.Transaction
import conduit.infrastructure.postgres.Transaction.Transactional
import conduit.infrastructure.postgres.meta.FieldMeta.given
import doobie.Fragments
import doobie.implicits.*
import doobie.postgres.implicits.*
import zio.*

class PostgresFollowerRepository(monitor: Monitor) extends FollowerRepository[Transaction] {
  override type Error = Transaction.Error

  override def save(follower: Follower): Result[Follower] =
    monitor.track("PostgresFollowerRepository.save", "resource" -> "db") {
      Transactional.withTime: now =>
        sql"""INSERT INTO followers (follower_id, followee_id, created_at)
             |VALUES (${follower.by}, ${follower.author}, $now)
             |ON CONFLICT (follower_id, followee_id) DO NOTHING
             |RETURNING follower_id, followee_id, created_at"""
          .stripMargin
          .query[Follower]
          .option
          .map:
            case Some(follower) => follower
            case None           => follower // If already exists, return the input follower
    }

  override def delete(follower: Follower): Result[Option[Follower]] =
    monitor.track("PostgresFollowerRepository.delete", "resource" -> "db") {
      Transactional:
        sql"""DELETE FROM followers
             |WHERE follower_id = ${follower.by} AND followee_id = ${follower.author}
             |RETURNING follower_id, followee_id, created_at"""
          .stripMargin
          .query[Follower]
          .option
    }

  override def exists(follower: Follower): Result[Boolean] =
    monitor.track("PostgresFollowerRepository.exists", "resource" -> "db") {
      Transactional:
        sql"""SELECT 1 FROM followers
             |WHERE follower_id = ${follower.by} AND followee_id = ${follower.author} LIMIT 1"""
          .stripMargin
          .query[Boolean]
          .option
          .map(_.isDefined)
    }

  override def list(by: UserId, authors: List[AuthorId]): Result[List[AuthorId]] =
    monitor.track("PostgresFollowerRepository.list", "resource" -> "db") {
      ZIO
        .foreach(authors.toNel): authors =>
          Transactional {
            sql"""SELECT followee_id FROM followers
                 |WHERE follower_id = $by AND ${Fragments.in(fr"followee_id", authors)}"""
              .stripMargin
              .query[AuthorId]
              .to[List]
          }
        .someOrElse(List.empty)
    }
}

object PostgresFollowerRepository:
  val layer: ZLayer[Monitor, Nothing, FollowerRepository[Transaction]] = ZLayer {
    for monitor <- ZIO.service[Monitor]
    yield PostgresFollowerRepository(monitor)
  }
