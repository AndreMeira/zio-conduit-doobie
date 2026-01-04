package conduit.infrastructure.inmemory.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.PermalinkRepository
import conduit.domain.model.types.article.{ ArticleId, ArticleSlug, AuthorId }
import zio.ZLayer

class InMemoryPermalinkRepository(monitor: Monitor) extends PermalinkRepository[Transaction] {
  override type Error = Nothing

  override def save(articleId: ArticleId, slug: ArticleSlug): Result[Unit] =
    monitor.track("InMemoryPermalinkRepository.save", "resource" -> "db") {
      Transaction.execute: state =>
        state.permalinks.update { permalinks =>
          permalinks.updated(slug, articleId)
        }
    }

  override def exists(articleId: ArticleId, slug: ArticleSlug): Result[Boolean] =
    monitor.track("InMemoryPermalinkRepository.exists", "resource" -> "db") {
      Transaction.execute: state =>
        state.permalinks.get.map { permalinks =>
          permalinks.get(slug).contains(articleId)
        }
    }

  override def exists(slug: ArticleSlug, authorId: AuthorId): Result[Boolean] =
    monitor.track("InMemoryArticleRepository.existsByAuthor", "resource" -> "db") {
      Transaction.execute: state =>
        for {
          permalinks <- state.permalinks.get
          articles   <- state.articles.get

          articleId  = permalinks.get(slug)
          article    = articleId.flatMap(id => articles.get(id))
          realAuthor = article.map(_.data.author)
        } yield realAuthor.contains(authorId)
    }

  override def resolve(slug: ArticleSlug): Result[Option[ArticleId]] =
    monitor.track("InMemoryPermalinkRepository.resolve", "resource" -> "db") {
      Transaction.execute: state =>
        state.permalinks.get.map { permalinks =>
          permalinks.get(slug)
        }
    }

  override def delete(articleId: ArticleId): Result[Unit] =
    monitor.track("InMemoryPermalinkRepository.delete", "resource" -> "db") {
      Transaction.execute: state =>
        state.permalinks.update { permalinks =>
          permalinks.filter { case (_, id) => id != articleId }
        }
    }
}

object InMemoryPermalinkRepository:
  val layer: ZLayer[Monitor, Nothing, PermalinkRepository[Transaction]] =
    ZLayer {
      for monitor <- zio.ZIO.service[Monitor]
      yield InMemoryPermalinkRepository(monitor)
    }
