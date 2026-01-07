package conduit.domain.model.types.article

import conduit.domain.model.error.ApplicationError
import zio.prelude.{ Subtype, Validation }

/**
 * Type-safe wrapper for article descriptions with validation.
 *
 * Article descriptions provide brief summaries of articles, used in
 * article listings and previews. They must be non-empty strings.
 */
type ArticleDescription = ArticleDescription.Type

/**
 * Companion object providing validation and construction for ArticleDescription.
 */
object ArticleDescription extends Subtype[String] {

  /**
   * Creates an ArticleDescription from a string with trimming and validation.
   *
   * @param value Input description string
   * @return Validated ArticleDescription or validation error
   */
  def fromString(value: String): Validation[ArticleDescription.Error, ArticleDescription] =
    validated(value.trim)

  /**
   * Validates a description string for non-emptiness.
   *
   * @param value Description string to validate
   * @return Validated ArticleDescription or validation error
   */
  def validated(value: String): Validation[ArticleDescription.Error, ArticleDescription] =
    if value.isEmpty
    then Validation.fail(Error.ArticleDescriptionEmpty)
    else Validation.succeed(ArticleDescription(value))

  /**
   * Enumeration of article description validation errors.
   */
  enum Error extends ApplicationError.ValidationError:
    /** Error when description is empty */
    case ArticleDescriptionEmpty

    override def key: String     = "article description"
    override def message: String = "Article description cannot be empty"
}
