package conduit.domain.logic.persistence

import conduit.domain.model.entity.UserProfile
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.error.ApplicationError.TransientError
import conduit.domain.model.types.article.{ ArticleId, ArticleSlug }
import conduit.domain.model.types.user.{ Email, UserId, UserName }
import zio.ZIO

trait UserProfileRepository[Tx] {
  type Error <: ApplicationError
  protected type Result[A] = ZIO[Tx, Error, A]

  def exists(username: UserName): Result[Boolean]
  def exists(user: UserId, userName: UserName): Result[Boolean]
  def findById(id: UserId): Result[Option[UserProfile]]
  def findAuthorOf(articleId: ArticleId): Result[Option[UserProfile]]
  def findByUserName(username: UserName): Result[Option[UserProfile]]
  def findByIds(userIds: List[UserId]): Result[List[UserProfile]]
  def save(user: UserProfile): Result[UserProfile]
  def create(userId: UserId, user: UserProfile.Data): Result[UserProfile]
}
