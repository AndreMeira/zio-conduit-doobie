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

// @see https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/

trait ArticleValidator[Tx] {
  type Error <: ApplicationError
  protected type Validated[A] = Validation[ValidationError, A]
  protected type Result[A]    = ZIO[Tx, Error, Validated[A]]

  protected type PatchWithSlug   = (slug: ArticleSlug, patches: List[ArticlePatch])                   // for readability purpose only
  protected type ArticleWithTags = (article: Article.Data, tags: List[ArticleTag])                    // for readability purpose only
  protected type SearchQuery     = (filters: List[ArticleRepository.Search], limit: Int, offset: Int) // for readability purpose only
  protected type FeedQuery       = (userId: UserId, limit: Int, offset: Int)                          // for readability purpose only

  def parse(request: UpdateArticleRequest): Result[PatchWithSlug]
  def parse(request: CreateArticleRequest): Result[ArticleWithTags]
  def parse(request: GetArticleRequest): Result[ArticleSlug]
  def parse(request: DeleteArticleRequest): Result[ArticleSlug]
  def parse(request: ListArticlesRequest): Result[SearchQuery]
  def parse(request: ArticleFeedRequest): Result[FeedQuery]
  def parse(request: AddFavoriteArticleRequest): Result[ArticleSlug]
  def parse(request: RemoveFavoriteArticleRequest): Result[ArticleSlug]
  def parse(request: ListTagsRequest): Result[Unit]
}
