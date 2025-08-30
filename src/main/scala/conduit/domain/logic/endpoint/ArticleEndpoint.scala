package conduit.domain.logic.endpoint

import conduit.domain.model.error.ApplicationError
import conduit.domain.model.request.article.*
import conduit.domain.model.response.article.*
import zio.ZIO

trait ArticleEndpoint[Tx] {
  def delete(request: DeleteArticleRequest): ZIO[Tx, ApplicationError, DeleteArticleRequest]
  def create(request: CreateArticleRequest): ZIO[Tx, ApplicationError, GetArticleResponse]
  def update(request: UpdateArticleRequest): ZIO[Tx, ApplicationError, GetArticleResponse]
  def addFavorite(request: AddFavoriteArticleRequest): ZIO[Tx, ApplicationError, GetArticleResponse]
  def removeFavorite(request: RemoveFavoriteArticleRequest): ZIO[Tx, ApplicationError, GetArticleResponse]
  def recent(request: ListArticlesRequest): ZIO[Tx, ApplicationError, ArticleListResponse]
  def feed(request: ArticleFeedRequest): ZIO[Tx, ApplicationError, ArticleListResponse]
}
