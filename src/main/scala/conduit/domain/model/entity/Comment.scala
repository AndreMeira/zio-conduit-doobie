package conduit.domain.model.entity

import conduit.domain.model.types.article.ArticleId
import conduit.domain.model.types.comment.{ CommentAuthorId, CommentBody, CommentId }

import java.time.Instant

case class Comment(
  id: CommentId, 
  data: Comment.Data, 
  metadata: Comment.Metadata
)

object Comment:
  case class Metadata(
    createdAt: Instant, 
    updatedAt: Instant
  )
  
  case class Data(
    article: ArticleId, 
    body: CommentBody, 
    author: CommentAuthorId
  )
