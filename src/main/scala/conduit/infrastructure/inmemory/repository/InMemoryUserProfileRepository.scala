package conduit.infrastructure.inmemory.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.UserProfileRepository
import conduit.domain.model.entity.UserProfile
import conduit.domain.model.types.article.{ ArticleId, ArticleSlug }
import conduit.domain.model.types.user.{ Email, UserId, UserName }
import zio.{ Clock, ZIO, ZLayer }

class InMemoryUserProfileRepository(monitor: Monitor) extends UserProfileRepository[Transaction] {
  override type Error = Nothing

  override def exists(username: UserName): Result[Boolean] =
    monitor.track("InMemoryUserProfileRepository.exists(username)", "resource" -> "db") {
      Transaction.execute: state =>
        state.profiles.get.map { profiles =>
          profiles.values.exists(_.data.name == username)
        }
    }

  override def exists(user: UserId, userName: UserName): Result[Boolean] =
    monitor.track("InMemoryUserProfileRepository.exists(user, username)", "resource" -> "db") {
      Transaction.execute: state =>
        state.profiles.get.map { profiles =>
          profiles.get(user).exists(_.data.name == userName)
        }
    }

  override def findById(id: UserId): Result[Option[UserProfile]] =
    monitor.track("InMemoryUserProfileRepository.findById", "resource" -> "db") {
      Transaction.execute: state =>
        state.profiles.get.map(_.get(id))
    }

  override def findAuthorOf(articleId: ArticleId): Result[Option[UserProfile]] =
    monitor.track("InMemoryUserProfileRepository.findAuthorOfArticleId", "resource" -> "db") {
      Transaction.execute: state =>
        for {
          articles <- state.articles.get
          profiles <- state.profiles.get
          article   = articles.get(articleId)
          author    = article.flatMap(article => profiles.get(article.data.author))
        } yield author
    }

  override def findByUserName(username: UserName): Result[Option[UserProfile]] =
    monitor.track("InMemoryUserProfileRepository.findByUserName", "resource" -> "db") {
      Transaction.execute: state =>
        state.profiles.get.map { profiles =>
          profiles.values.find(_.data.name == username)
        }
    }

  override def findByIds(userIds: List[UserId]): Result[List[UserProfile]] =
    monitor.track("InMemoryUserProfileRepository.findByIds", "resource" -> "db") {
      Transaction.execute: state =>
        state.profiles.get.map { profiles =>
          userIds.flatMap(profiles.get)
        }
    }

  override def save(user: UserProfile): Result[UserProfile] =
    monitor.track("InMemoryUserProfileRepository.save", "resource" -> "db") {
      Transaction.execute: state =>
        Clock.instant.flatMap { now =>
          val metadata = user.metadata.copy(updatedAt = now)
          val updated  = user.copy(metadata = metadata)
          state.profiles.modify { profiles =>
            updated -> (profiles + (user.id -> updated))
          }
        }
    }

  override def create(userId: UserId, user: UserProfile.Data): Result[UserProfile] =
    monitor.track("InMemoryUserProfileRepository.create", "resource" -> "db") {
      Transaction.execute: state =>
        Clock.instant.flatMap { now =>
          val metadata = UserProfile.Metadata(now, now)
          val profile  = UserProfile(userId, user, metadata)
          state.profiles.modify { profiles =>
            profile -> (profiles + (userId -> profile))
          }
        }
    }
}

object InMemoryUserProfileRepository:
  val layer: ZLayer[Monitor, Nothing, InMemoryUserProfileRepository] = ZLayer {
    for monitor <- ZIO.service[Monitor]
    yield InMemoryUserProfileRepository(monitor)
  }
