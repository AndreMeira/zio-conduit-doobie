package conduit.infrastructure.inmemory.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.TagRepository
import conduit.domain.model.entity.Tag
import conduit.domain.model.types.article.{ ArticleId, ArticleTag }
import zio.ZLayer

class InMemoryTagRepository(monitor: Monitor) extends TagRepository[Transaction] {
  override type Error = Nothing

  override def distinct: Result[List[ArticleTag]] =
    monitor.track("InMemoryTagRepository.distinct", "resource" -> "db") {
      Transaction.execute: state =>
        state.tags.get.map { tags =>
          tags.values.flatten.toList.distinct
        }
    }

  override def save(tag: List[Tag]): Result[List[Tag]] =
    monitor.track("InMemoryTagRepository.save", "resource" -> "db") {
      Transaction.execute: state =>
        state.tags.modify { tags =>
          val articleTags = tag.groupMap(_.article)(_.tag)
          tag -> (tags ++ articleTags)
        }
    }

  override def deleteByArticle(articleId: ArticleId): Result[Int] =
    monitor.track("InMemoryTagRepository.deleteByArticle", "resource" -> "db") {
      Transaction.execute: state =>
        state.tags.modify { tags =>
          val (deleted, remaining) = tags.partition { case (id, _) => id == articleId }
          deleted.values.flatten.size -> remaining
        }
    }
}

object InMemoryTagRepository:
  val layer: ZLayer[Monitor, Nothing, TagRepository[Transaction]] = ZLayer {
    for monitor <- zio.ZIO.service[Monitor]
    yield InMemoryTagRepository(monitor)
  }
