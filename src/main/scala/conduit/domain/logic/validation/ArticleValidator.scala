package conduit.domain.logic.validation

import conduit.domain.model.entity.Article
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.request.article.{ CreateArticleRequest, UpdateArticleRequest }
import zio.ZIO

trait ArticleValidator[Tx] {
  def validateUpdate(request: UpdateArticleRequest): ZIO[Tx, ApplicationError, Article.Data]
  def validateCreate(request: CreateArticleRequest): ZIO[Tx, ApplicationError, Article.Data]
}
