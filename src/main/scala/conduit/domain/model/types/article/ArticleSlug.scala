package conduit.domain.model.types.article

import conduit.domain.model.error.ApplicationError
import zio.prelude.{ Subtype, Validation }

import java.text.Normalizer
import scala.util.chaining.scalaUtilChainingOps

/**
 * Type-safe wrapper for article slugs with normalization and validation.
 *
 * Article slugs are URL-friendly identifiers derived from article titles.
 * They are normalized to contain only lowercase letters, numbers, and hyphens.
 */
type ArticleSlug = ArticleSlug.Type

/**
 * Companion object providing validation, normalization, and construction for ArticleSlug.
 */
object ArticleSlug extends Subtype[String] {

  extension (slug: ArticleSlug) {
    /**
     * Appends a numeric index to create a unique slug variant.
     *
     * Used when the base slug already exists and a unique variant is needed.
     * Index 1 returns the original slug unchanged.
     *
     * @param index Numeric suffix to append (e.g., 2 becomes "slug_2")
     * @return Modified slug with index suffix if index > 1
     */
    def appendIndex(index: Int): ArticleSlug =
      if index <= 1 then slug
      else ArticleSlug(s"${slug}_$index")
  }

  /**
   * Creates an ArticleSlug from a string with normalization and validation.
   *
   * @param value Input string (typically from article title)
   * @return Validated and normalized ArticleSlug or validation error
   */
  def fromString(value: String): Validation[ArticleSlug.Error, ArticleSlug] =
    normalize(value).pipe(validated)

  /**
   * Validates a pre-normalized string as an ArticleSlug.
   *
   * @param value Normalized string to validate
   * @return Validated ArticleSlug or validation error
   */
  def validated(value: String): Validation[ArticleSlug.Error, ArticleSlug] =
    validateIsNotEmpty(value).map(_ => ArticleSlug(normalize(value)))

  /**
   * Normalizes a string to create a URL-friendly slug.
   *
   * The normalization process:
   * 1. Trims whitespace and converts to lowercase
   * 2. Applies Unicode normalization (NFD)
   * 3. Removes diacritical marks
   * 4. Replaces non-alphanumeric characters with hyphens
   * 5. Removes leading/trailing hyphens
   *
   * @param slug Input string to normalize
   * @return Normalized slug string
   */
  def normalize(slug: String): String =
    Normalizer
      .normalize(slug.trim.toLowerCase, Normalizer.Form.NFD)
      .replaceAll("\\p{M}", "")
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("(^-|-$)", "") // thanks copilot

  /**
   * Validates that the slug is not empty after normalization.
   *
   * @param value Slug string to validate
   * @return Success if non-empty, error otherwise
   */
  def validateIsNotEmpty(value: String): Validation[ArticleSlug.Error, Unit] =
    if value.isEmpty
    then Validation.fail(Error.ArticleSlugEmpty)
    else Validation.succeed(())

  /**
   * Enumeration of article slug validation errors.
   */
  enum Error extends ApplicationError.ValidationError:
    /** Error when the slug is empty after normalization */
    case ArticleSlugEmpty

    override def key: String     = "article slug"
    override def message: String = "Article slug cannot be empty"

}
