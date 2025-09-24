package conduit.infrastructure.inmemory.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.FollowerRepository
import conduit.domain.model.entity.Follower
import conduit.domain.model.types.article.AuthorId
import conduit.domain.model.types.user.UserId
import zio.ZLayer

import scala.util.chaining.scalaUtilChainingOps

class InMemoryFollowerRepository(monitor: Monitor) extends FollowerRepository[Transaction] {
  override type Error = Nothing

  override def save(follower: Follower): Result[Follower] =
    monitor.track("InMemoryFollowerRepository.save", "resource" -> "db") {
      Transaction.execute: state =>
        state.followers.modify { followers =>
          val authors = followers.getOrElse(follower.by, Set.empty) + follower.author
          follower -> followers.updated(follower.by, authors)
        }
    }

  override def delete(follower: Follower): Result[Option[Follower]] =
    monitor.track("InMemoryFollowerRepository.delete", "resource" -> "db") {
      Transaction.execute: state =>
        state.followers.modify { followers =>
          followers
            .getOrElse(follower.by, Set.empty)
            .pipe(authors => authors - follower.author)
            .pipe: authors =>
              if authors.isEmpty then None -> followers.removed(follower.by)
              else Some(follower)          -> followers.updated(follower.by, authors)
        }
    }

  override def exists(follower: Follower): Result[Boolean] =
    monitor.track("InMemoryFollowerRepository.exists", "resource" -> "db") {
      Transaction.execute: state =>
        state.followers.get.map { followers =>
          followers.get(follower.by).exists(_.contains(follower.author))
        }
    }

  override def list(by: UserId, authors: List[AuthorId]): Result[List[AuthorId]] =
    monitor.track("InMemoryFollowerRepository.list", "resource" -> "db") {
      Transaction.execute: state =>
        state.followers.get.map { followers =>
          val followed = followers.getOrElse(by, Set.empty)
          authors.filter(followed.contains)
        }
    }
}

object InMemoryFollowerRepository:
  val layer: ZLayer[Monitor, Nothing, FollowerRepository[Transaction]] =
    ZLayer {
      for monitor <- zio.ZIO.service[Monitor]
      yield InMemoryFollowerRepository(monitor)
    }
