package conduit.domain.logic.validation

import conduit.domain.logic.persistence.ArticleRepository
import conduit.domain.model.entity.Article
import conduit.domain.model.error.ApplicationError.{ TransientError, ValidationError }
import conduit.domain.model.error.{ ApplicationError, InvalidInput }
import conduit.domain.model.patching.ArticlePatch
import conduit.domain.model.request.article.*
import conduit.domain.model.types.article.{ ArticleSlug, ArticleTag }
import conduit.domain.model.types.user.UserId
import zio.ZIO
import zio.prelude.Validation

trait ArticleValidator[Tx] {
  protected type Validated[A] = Validation[ValidationError, A]
  protected type Result[A]    = ZIO[Tx, TransientError, Validated[A]]

  protected type ArticleWithTags = (article: Article.Data, tags: List[ArticleTag])                    // for aliasing purpose only
  protected type SearchQuery     = (filters: List[ArticleRepository.Search], limit: Int, offset: Int) // for aliasing purpose only
  protected type FeedQuery       = (userId: UserId, limit: Int, offset: Int)                          // for aliasing purpose only

  def validate(request: UpdateArticleRequest): Result[List[ArticlePatch]]
  def validate(request: CreateArticleRequest): Result[ArticleWithTags]
  def validate(request: GetArticleRequest): Result[ArticleSlug]
  def validate(request: DeleteArticleRequest): Result[ArticleSlug]
  def validate(request: ListArticlesRequest): Result[SearchQuery]
  def validate(request: ArticleFeedRequest): Result[FeedQuery]
  def validate(request: AddFavoriteArticleRequest): Result[ArticleSlug]
  def validate(request: RemoveFavoriteArticleRequest): Result[ArticleSlug]
  def validate(request: ListTagsRequest): Result[Unit]
}
