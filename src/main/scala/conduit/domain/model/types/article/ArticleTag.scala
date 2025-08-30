package conduit.domain.model.types.article

import conduit.domain.model.error.ApplicationError
import zio.prelude.{Subtype, Validation}

type ArticleTag = ArticleTag.Type
object ArticleTag extends Subtype[String] {

  def fromString(value: String): Validation[ArticleTag.Error, ArticleTag] =
    validated(value.trim)

  def validated(value: String): Validation[ArticleTag.Error, ArticleTag] =
    validateIsNotEmpty(value).map(_ => ArticleTag(value))

  def validateIsNotEmpty(value: String): Validation[Error, Unit] =
    if value.isEmpty
    then Validation.fail(Error.ArticleTagEmpty)
    else Validation.succeed(())

  enum Error extends ApplicationError.ValidationError:
    case ArticleTagEmpty
    override def key: String = "article tag"
    override def message: String = "Article tag cannot be empty"
}
