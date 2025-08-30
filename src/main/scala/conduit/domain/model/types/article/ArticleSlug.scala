package conduit.domain.model.types.article

import conduit.domain.model.error.ApplicationError
import zio.prelude.{Subtype, Validation}

import java.text.Normalizer
import scala.util.chaining.scalaUtilChainingOps

type ArticleSlug = ArticleSlug.Type
object ArticleSlug extends Subtype[String] {

  def withIndex(slug: ArticleSlug, index: ArticleSlugIndex): ArticleSlug =
    index match
      case 0     => ArticleSlug(s"$slug")
      case index => ArticleSlug(s"$slug-$index")

  def fromTitle(title: ArticleTitle): Validation[ArticleSlug.Error, ArticleSlug] =
    fromString(title)

  def fromString(value: String): Validation[ArticleSlug.Error, ArticleSlug] =
    normalize(value).pipe(validated)

  def validated(value: String): Validation[ArticleSlug.Error, ArticleSlug] =
    validateIsNotEmpty(value).map(_ => ArticleSlug(normalize(value)))

  def normalize(slug: String): String =
    Normalizer.normalize(slug.trim.toLowerCase, Normalizer.Form.NFD)
      .replaceAll("\\p{M}", "")
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("(^-|-$)", "") // thanks copilot


  def validateIsNotEmpty(value: String): Validation[ArticleSlug.Error, Unit] =
    if value.isEmpty
    then Validation.fail(Error.ArticleSlugEmpty)
    else Validation.succeed(())

  enum Error extends ApplicationError.ValidationError:
    case ArticleSlugEmpty
    override def key: String = "article slug"
    override def message: String = "Article slug cannot be empty"


}
