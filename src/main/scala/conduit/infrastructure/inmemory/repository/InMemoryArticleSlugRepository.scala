package conduit.infrastructure.inmemory.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.ArticleSlugRepository
import conduit.domain.model.types.article.ArticleSlug
import zio.ZLayer

class InMemoryArticleSlugRepository(monitor: Monitor) extends ArticleSlugRepository[Transaction] {

  override type Error = Nothing

  override def nextAvailable(slug: ArticleSlug): Result[ArticleSlug] =
    monitor.track("InMemoryArticleSlugRepository.nextAvailable", "resource" -> "db") {
      Transaction.execute: state =>
        state
          .slugs
          .modify: slugs =>
            val version = slugs.getOrElse(slug, 0)
            val newSlug = slug.appendIndex(version)
            newSlug -> slugs.updated(slug, version + 1)
    }
}

object InMemoryArticleSlugRepository:
  val layer: ZLayer[Monitor, Nothing, InMemoryArticleSlugRepository] =
    ZLayer {
      for monitor <- zio.ZIO.service[Monitor]
      yield InMemoryArticleSlugRepository(monitor)
    }
