package conduit.domain.service.endpoint

import conduit.domain.logic.authorisation.{ Authorisation, CommentAuthorisation }
import conduit.domain.logic.endpoint.CommentEndpoint
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.*
import conduit.domain.logic.validation.CommentValidator
import conduit.domain.model.entity.{ Comment, Follower }
import conduit.domain.model.error.*
import conduit.domain.model.request.comment.*
import conduit.domain.model.response.comment.*
import conduit.domain.model.types.article.AuthorId
import conduit.domain.model.types.comment.CommentAuthorId
import conduit.domain.model.types.user.UserId
import conduit.domain.service.endpoint.dsl.EndpointDsl
import zio.ZIO

class CommentEndpointService[Tx](
  monitor: Monitor,
  unitOfWork: UnitOfWork[Tx],
  authorisation: CommentAuthorisation[Tx],
  validator: CommentValidator[Tx],
  followers: FollowerRepository[Tx],
  comments: CommentRepository[Tx],
  articles: ArticleRepository[Tx],
  profiles: UserProfileRepository[Tx],
) extends CommentEndpoint[Tx], EndpointDsl {

  override def addComment(request: AddCommentRequest): Result[GetCommentResponse] =
    monitor.track("CommentEndpointService.addComment") {
      unitOfWork.execute:
        for {
          _          <- authorisation.authorise(request).allowedOrFail
          validated  <- validator.validate(request).validOrFail
          slug        = validated.slug
          currentUser = request.requester.userId
          article    <- articles.findBySlug(slug).someOrFail(NotFound.article(slug))
          author     <- profiles.findAuthorOf(slug).someOrFail(NotFound.authorOf(slug))
          following  <- followers.exists(Follower(currentUser, AuthorId(author.id)))
          commentData = Comment.Data(article.id, validated.body, validated.author)
          comment    <- comments.save(commentData)
        } yield GetCommentResponse.make(comment, author, following)
    }

  override def deleteComment(request: DeleteCommentRequest): Result[GetCommentResponse] =
    monitor.track("CommentEndpointService.deleteComment") {
      unitOfWork.execute:
        for {
          _          <- authorisation.authorise(request).allowedOrFail
          commentId  <- validator.validate(request).validOrFail
          comment    <- comments.find(commentId).someOrFail(NotFound.comment(commentId))
          articleId   = comment.data.article
          currentUser = request.requester.userId
          author     <- profiles.findAuthorOf(articleId).someOrFail(NotFound.authorOf(articleId))
          following  <- followers.exists(Follower(currentUser, AuthorId(author.id)))
          _          <- comments.delete(comment.id)
        } yield GetCommentResponse.make(comment, author, following = following)
    }

  override def listComments(request: ListCommentsRequest): Result[CommentListResponse] =
    monitor.track("CommentEndpointService.listComments") {
      unitOfWork.execute:
        for {
          _            <- authorisation.authorise(request).allowedOrFail
          articleSlug  <- validator.validate(request).validOrFail
          commentsList <- comments.findByArticle(articleSlug)
          authorIds     = commentsList.map(_.data.author).map(AuthorId(_))
          userId        = request.requester.option.map(_.userId)
          followed     <- ZIO.foreach(userId)(followers.list(_, authorIds)).map(_.getOrElse(Nil))
          userProfiles <- profiles.findByIds(authorIds.distinct)
        } yield CommentListResponse.make(commentsList, userProfiles, followed.toSet)
    }
}
