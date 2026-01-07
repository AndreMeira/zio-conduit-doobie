package conduit.domain.model.entity

import conduit.domain.model.types.article.ArticleId
import conduit.domain.model.types.comment.{ CommentAuthorId, CommentBody, CommentId }

import java.time.Instant

/**
 * Represents a comment on an article in the Conduit system.
 *
 * Comments allow users to engage in discussions about articles. Each comment
 * is associated with a specific article and authored by a registered user.
 *
 * @param id       Unique identifier for the comment
 * @param data     Core comment content and associations
 * @param metadata Timestamps for creation and last update
 */
case class Comment(
  id: CommentId,
  data: Comment.Data,
  metadata: Comment.Metadata,
)

object Comment:
  /**
   * Metadata associated with a comment, containing audit information.
   *
   * @param createdAt Timestamp when the comment was first created
   * @param updatedAt Timestamp when the comment was last modified
   */
  case class Metadata(
    createdAt: Instant,
    updatedAt: Instant,
  )

  /**
   * Core comment data containing the content and essential associations.
   *
   * @param article The article this comment is associated with
   * @param body    The text content of the comment
   * @param author  ID of the user who authored this comment
   */
  case class Data(
    article: ArticleId,
    body: CommentBody,
    author: CommentAuthorId,
  )
