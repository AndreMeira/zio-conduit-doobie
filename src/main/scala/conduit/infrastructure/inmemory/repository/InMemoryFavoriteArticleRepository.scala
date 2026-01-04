package conduit.infrastructure.inmemory.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.FavoriteArticleRepository
import conduit.domain.model.entity.FavoriteArticle
import conduit.domain.model.types.article.{ ArticleFavoriteCount, ArticleId }
import conduit.domain.model.types.user.UserId
import zio.{ ZIO, ZLayer }

import scala.util.chaining.scalaUtilChainingOps

class InMemoryFavoriteArticleRepository(monitor: Monitor) extends FavoriteArticleRepository[Transaction] {
  override type Error = Nothing

  override def add(favorite: FavoriteArticle): Result[FavoriteArticle] =
    monitor.track("InMemoryFavoriteArticleRepository.add", "resource" -> "db") {
      Transaction.execute: state =>
        state.favorites.modify { favorites =>
          val users = favorites.getOrElse(favorite.article, Set.empty) + favorite.by
          favorite -> favorites.updated(favorite.article, users)
        }
    }

  override def delete(favorite: FavoriteArticle): Result[Option[FavoriteArticle]] =
    monitor.track("InMemoryFavoriteArticleRepository.delete", "resource" -> "db") {
      Transaction.execute: state =>
        state.favorites.modify { favorites =>
          favorites
            .getOrElse(favorite.article, Set.empty)
            .pipe(users => users - favorite.by)
            .pipe: users =>
              if users.isEmpty then None -> favorites.removed(favorite.article)
              else Some(favorite)        -> favorites.updated(favorite.article, users)
        }
    }

  override def deleteByArticle(article: ArticleId): Result[Int] =
    monitor.track("InMemoryFavoriteArticleRepository.deleteByArticle", "resource" -> "db") {
      Transaction.execute: state =>
        state.favorites.modify { favorites =>
          val count = favorites.get(article).map(_.size).getOrElse(0)
          count -> favorites.removed(article)
        }
    }

  override def count(article: ArticleId): Result[ArticleFavoriteCount] =
    monitor.track("InMemoryFavoriteArticleRepository.count", "resource" -> "db") {
      Transaction.execute: state =>
        state.favorites.get.map { favorites =>
          val count = favorites.get(article).map(_.size).getOrElse(0)
          ArticleFavoriteCount(count)
        }
    }

  override def count(articles: List[ArticleId]): Result[Map[ArticleId, ArticleFavoriteCount]] =
    monitor.track("InMemoryFavoriteArticleRepository.countMultiple", "resource" -> "db") {
      Transaction.execute: state =>
        ZIO.foreach(articles)(count).map { counts =>
          articles.zip(counts).toMap
        }
    }

  override def exists(favorite: FavoriteArticle): Result[Boolean] =
    monitor.track("InMemoryFavoriteArticleRepository.exists", "resource" -> "db") {
      Transaction.execute: state =>
        state.favorites.get.map { favorites =>
          favorites.get(favorite.article).exists(_.contains(favorite.by))
        }
    }

  override def list(by: UserId, articles: List[ArticleId]): Result[List[ArticleId]] =
    monitor.track("InMemoryFavoriteArticleRepository.list", "resource" -> "db") {
      Transaction.execute: state =>
        state.favorites.get.map { favorites =>
          articles.filter(article => favorites.get(article).exists(_.contains(by)))
        }
    }
}

object InMemoryFavoriteArticleRepository:
  val layer: ZLayer[Monitor, Nothing, InMemoryFavoriteArticleRepository] =
    ZLayer {
      for monitor <- zio.ZIO.service[Monitor]
      yield InMemoryFavoriteArticleRepository(monitor)
    }
