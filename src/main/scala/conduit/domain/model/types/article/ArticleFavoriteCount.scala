package conduit.domain.model.types.article

import conduit.domain.model.error.ApplicationError
import zio.prelude.{ Subtype, Validation }

type ArticleFavoriteCount = ArticleFavoriteCount.Type
object ArticleFavoriteCount extends Subtype[Int] {

  def fromInt(value: Int): Validation[Error, ArticleFavoriteCount] =
    validated(value)

  def validated(value: Int): Validation[ArticleFavoriteCount.Error, ArticleFavoriteCount] =
    validatedIsNotNegative(value).map(_ => ArticleFavoriteCount(value))

  def validatedIsNotNegative(value: Int): Validation[ArticleFavoriteCount.Error, Unit] =
    if value < 0
    then Validation.fail(Error.NegativeFavoriteCount)
    else Validation.succeed(())

  enum Error extends ApplicationError.ValidationError:
    case NegativeFavoriteCount
    override def key: String     = "favorite count"
    override def message: String = "Favorite count cannot be negative"
}
