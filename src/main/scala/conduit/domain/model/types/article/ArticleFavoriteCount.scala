package conduit.domain.model.types.article

import conduit.domain.model.error.ApplicationError
import zio.prelude.{ Subtype, Validation }

/**
 * Type-safe wrapper for article favorite counts with validation.
 *
 * Represents the number of users who have favorited an article.
 * Must be a non-negative integer to ensure data consistency.
 */
type ArticleFavoriteCount = ArticleFavoriteCount.Type

/**
 * Companion object providing validation and construction for ArticleFavoriteCount.
 */
object ArticleFavoriteCount extends Subtype[Int] {

  /**
   * Creates an ArticleFavoriteCount from an integer with validation.
   *
   * @param value Input favorite count
   * @return Validated ArticleFavoriteCount or validation error
   */
  def fromInt(value: Int): Validation[ArticleFavoriteCount.Error, ArticleFavoriteCount] =
    validated(value)

  /**
   * Validates a favorite count for non-negativity.
   *
   * @param value Count value to validate
   * @return Validated ArticleFavoriteCount or validation error
   */
  def validated(value: Int): Validation[ArticleFavoriteCount.Error, ArticleFavoriteCount] =
    validatedIsNotNegative(value).map(_ => ArticleFavoriteCount(value))

  /**
   * Validates that a count value is not negative.
   *
   * @param value Count to validate
   * @return Success if non-negative, error otherwise
   */
  def validatedIsNotNegative(value: Int): Validation[ArticleFavoriteCount.Error, Unit] =
    if value < 0
    then Validation.fail(Error.NegativeFavoriteCount)
    else Validation.succeed(())

  /**
   * Enumeration of favorite count validation errors.
   */
  enum Error extends ApplicationError.ValidationError:
    /** Error when favorite count is negative */
    case NegativeFavoriteCount

    override def key: String     = "favorite count"
    override def message: String = "Favorite count cannot be negative"
}
