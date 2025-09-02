package conduit.domain.logic.persistence

import conduit.domain.model.entity.UserProfile
import conduit.domain.model.error.ApplicationError.TransientError
import conduit.domain.model.types.article.{ ArticleId, ArticleSlug }
import conduit.domain.model.types.user.{ Email, UserId, UserName }
import zio.ZIO

trait UserProfileRepository[Tx] {
  protected type Result[A] = ZIO[Tx, TransientError, A]

  def exists(username: UserName): Result[Boolean]
  def findById(id: UserId): Result[Option[UserProfile]]
  def findByEmail(email: Email): Result[Option[UserProfile]]
  def emailExists(email: Email): Result[Boolean]
  def userNameExists(username: UserName): Result[Boolean]
  def findAuthorOf(articleId: ArticleId): Result[Option[UserProfile]]
  def findAuthorOf(articleSlug: ArticleSlug): Result[Option[UserProfile]]
  def findByUserName(username: UserName): Result[Option[UserProfile]]
  def findByIds(userIds: List[UserId]): Result[List[UserProfile]]
  def save(userId: UserId, user: UserProfile.Data): Result[UserProfile]
  def save(user: UserProfile): Result[UserProfile]
}
