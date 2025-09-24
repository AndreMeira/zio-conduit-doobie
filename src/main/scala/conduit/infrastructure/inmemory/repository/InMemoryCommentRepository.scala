package conduit.infrastructure.inmemory.repository

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.CommentRepository
import conduit.domain.model.entity.Comment
import conduit.domain.model.types.article.ArticleId
import conduit.domain.model.types.comment.{ CommentAuthorId, CommentId }
import zio.{ Clock, ZLayer }

class InMemoryCommentRepository(monitor: Monitor) extends CommentRepository[Transaction] {
  override type Error = Nothing

  override def find(commentId: CommentId): Result[Option[Comment]] =
    monitor.track("InMemoryCommentRepository.find", "resource" -> "db") {
      Transaction.execute: state =>
        state.comments.get.map(_.get(commentId))
    }

  override def save(comment: Comment.Data): Result[Comment] =
    monitor.track("InMemoryCommentRepository.save", "resource" -> "db") {
      Transaction.execute: state =>
        for {
          now      <- Clock.instant
          id       <- state.nextId.map(CommentId(_))
          created   = Comment(id, comment, Comment.Metadata(now, now))
          inserted <- state.comments.modify(comments => created -> comments.updated(id, created))
        } yield inserted
    }

  override def delete(commentId: CommentId): Result[Option[Comment]] =
    monitor.track("InMemoryCommentRepository.delete", "resource" -> "db") {
      Transaction.execute: state =>
        state.comments.modify { comments =>
          comments.get(commentId) -> comments.removed(commentId)
        }
    }

  override def exists(commentId: CommentId, author: CommentAuthorId): Result[Boolean] =
    monitor.track("InMemoryCommentRepository.exists", "resource" -> "db") {
      Transaction.execute: state =>
        state.comments.get.map(_.get(commentId).exists(_.data.author == author))
    }

  override def findByArticle(article: ArticleId): Result[List[Comment]] =
    monitor.track("InMemoryCommentRepository.findByArticle", "resource" -> "db") {
      Transaction.execute: state =>
        state.comments.get.map(_.values.filter(_.data.article == article).toList)
    }
}

object InMemoryCommentRepository:
  val layer: ZLayer[Monitor, Nothing, CommentRepository[Transaction]] =
    ZLayer {
      for monitor <- zio.ZIO.service[Monitor]
      yield InMemoryCommentRepository(monitor)
    }
