package conduit.domain.model.types.article

import conduit.domain.model.error.ApplicationError
import zio.prelude.{ Subtype, Validation }

type ArticleDescription = ArticleDescription.Type
object ArticleDescription extends Subtype[String] {

  def fromString(value: String): Validation[ArticleDescription.Error, ArticleDescription] =
    validated(value.trim)

  def validated(value: String): Validation[ArticleDescription.Error, ArticleDescription] =
    validateIsNotEmpty(value).map(_ => ArticleDescription(value))

  def validateIsNotEmpty(value: String): Validation[ArticleDescription.Error, Unit] =
    if value.isEmpty
    then Validation.fail(Error.ArticleDescriptionEmpty)
    else Validation.succeed(())

  enum Error extends ApplicationError.ValidationError:
    case ArticleDescriptionEmpty
    override def key: String     = "article description"
    override def message: String = "Article description cannot be empty"
}
