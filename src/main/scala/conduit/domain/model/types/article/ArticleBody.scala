package conduit.domain.model.types.article

import conduit.domain.model.error.ApplicationError
import zio.prelude.{ Subtype, Validation }

type ArticleBody = ArticleBody.Type
object ArticleBody extends Subtype[String] {

  def fromString(value: String): Validation[ArticleBody.Error, ArticleBody] =
    validated(value.trim)

  def validated(value: String): Validation[ArticleBody.Error, ArticleBody] =
    validateIsNotEmpty(value).map(_ => ArticleBody(value))

  private def validateIsNotEmpty(value: String): Validation[ArticleBody.Error, Unit] =
    if value.isEmpty
    then Validation.fail(Error.ArticleBodyEmpty)
    else Validation.succeed(())

  enum Error extends ApplicationError.ValidationError:
    case ArticleBodyEmpty
    override def key: String     = "article body"
    override def message: String = "Article body cannot be empty"
}
