package conduit.domain.service.entrypoint

import conduit.domain.logic.authorisation.CommentAuthorisation
import conduit.domain.logic.entrypoint.CommentEntrypoint
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.*
import conduit.domain.logic.validation.CommentValidator
import conduit.domain.model.entity.{ Comment, Follower }
import conduit.domain.model.error.*
import conduit.domain.model.request.comment.*
import conduit.domain.model.response.comment.*
import conduit.domain.model.types.article.AuthorId
import conduit.domain.model.types.user.UserId
import conduit.domain.service.entrypoint.dsl.EntrypointDsl
import zio.ZIO

import zio.ZLayer
import izumi.reflect.Tag as ReflectionTag

class CommentEntrypointService[Tx](
  monitor: Monitor,
  unitOfWork: UnitOfWork[Tx],
  authorisation: CommentAuthorisation[Tx],
  validation: CommentValidator[Tx],
  followers: FollowerRepository[Tx],
  comments: CommentRepository[Tx],
  profiles: UserProfileRepository[Tx],
  permalinks: PermalinkRepository[Tx],
) extends CommentEntrypoint, EntrypointDsl(unitOfWork, authorisation) {

  override def addComment(request: AddCommentRequest): Result[GetCommentResponse] =
    monitor.track("CommentEndpointService.addComment") {
      authorise(request):
        for {
          validated  <- validation.parse(request).validOrFail
          slug        = validated.slug
          currentUser = request.requester.userId
          articleId  <- permalinks.resolve(slug) ?! NotFound.article(slug)
          author     <- profiles.findById(currentUser) ?! NotFound.user(currentUser)
          following  <- followers.exists(Follower(currentUser, AuthorId(author.id)))
          commentData = Comment.Data(articleId, validated.body, validated.author)
          comment    <- comments.save(commentData)
        } yield GetCommentResponse.make(comment, author, following)
    }

  override def deleteComment(request: DeleteCommentRequest): Result[GetCommentResponse] =
    monitor.track("CommentEndpointService.deleteComment") {
      authorise(request):
        for {
          commentId  <- validation.parse(request).validOrFail
          comment    <- comments.find(commentId) ?! NotFound.comment(commentId)
          articleId   = comment.data.article
          currentUser = request.requester.userId
          author     <- profiles.findAuthorOf(articleId) ?! NotFound.authorOf(articleId)
          following  <- followers.exists(Follower(currentUser, AuthorId(author.id)))
          _          <- comments.delete(comment.id)
        } yield GetCommentResponse.make(comment, author, following = following)
    }

  override def listComments(request: ListCommentsRequest): Result[CommentListResponse] =
    monitor.track("CommentEndpointService.listComments") {
      authorise(request):
        for {
          articleSlug  <- validation.parse(request).validOrFail
          article      <- permalinks.resolve(articleSlug) ?! NotFound.article(articleSlug)
          commentsList <- comments.findByArticle(article)
          authorIds     = commentsList.map(_.data.author).map(AuthorId(_))
          userId        = request.requester.option.map(_.userId)
          followed     <- ZIO.foreach(userId)(followers.list(_, authorIds)).map(_.getOrElse(Nil))
          userProfiles <- profiles.findByIds(authorIds.distinct)
        } yield CommentListResponse.make(commentsList, userProfiles, followed.toSet)
    }
}

object CommentEntrypointService:
  type Dependency[Tx] =
    PermalinkRepository[Tx] & UserProfileRepository[Tx] & CommentRepository[Tx] & FollowerRepository[Tx] & CommentValidator[Tx] &
      CommentAuthorisation[Tx] & UnitOfWork[Tx] & Monitor

  def layer[Tx: ReflectionTag]: ZLayer[Dependency[Tx], Nothing, CommentEntrypointService[Tx]] =
    ZLayer {
      for {
        monitor       <- ZIO.service[Monitor]
        unitOfWork    <- ZIO.service[UnitOfWork[Tx]]
        authorisation <- ZIO.service[CommentAuthorisation[Tx]]
        validator     <- ZIO.service[CommentValidator[Tx]]
        followers     <- ZIO.service[FollowerRepository[Tx]]
        comments      <- ZIO.service[CommentRepository[Tx]]
        profiles      <- ZIO.service[UserProfileRepository[Tx]]
        permalinks    <- ZIO.service[PermalinkRepository[Tx]]
      } yield CommentEntrypointService(
        monitor,
        unitOfWork,
        authorisation,
        validator,
        followers,
        comments,
        profiles,
        permalinks,
      )
    }
