package conduit.domain.model.types.user

import conduit.domain.model.error.ApplicationError
import zio.prelude.{ Subtype, Validation }

type Biography = Biography.Type
object Biography extends Subtype[String] {
  private val minLength = 10
  private val maxLength = 160

  def fromString(value: String): Validation[Biography.Error, Biography] =
    validated(value.trim)

  def validated(value: String): Validation[Biography.Error, Biography] =
    Validation
      .validate(
        validateIsNotTooShort(value),
        validateIsNotTooLong(value),
      )
      .map(_ => Biography(value))

  def validateIsNotTooShort(value: String): Validation[Biography.Error, Unit] =
    if value.length < minLength
    then Validation.fail(Error.BiographyTooShort(minLength))
    else Validation.succeed(())

  def validateIsNotTooLong(value: String): Validation[Biography.Error, Unit] =
    if value.length > maxLength
    then Validation.fail(Error.BiographyTooLong(maxLength))
    else Validation.succeed(())

  enum Error extends ApplicationError.ValidationError:
    case BiographyTooShort(minLength: Int)
    case BiographyTooLong(maxLength: Int)

    override def key: String = "biography"

    override def message: String = this match
      case BiographyTooShort(minLength) => s"Biography must be at least $minLength characters long"
      case BiographyTooLong(maxLength)  => s"Biography must be at most $maxLength characters long"
}
