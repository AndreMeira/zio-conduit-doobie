package conduit.domain.model.types.user

import conduit.domain.model.error.ApplicationError
import zio.prelude.{ Subtype, Validation }

type UserName = UserName.Type
object UserName extends Subtype[String] {
  private val MinLength = 3
  private val MaxLength = 20

  def fromString(value: String): Validation[UserName.Error, UserName] =
    validated(value.trim.replaceAll("\\s+", " "))

  def validated(value: String): Validation[UserName.Error, UserName] =
    Validation
      .validate(
        validateIsNotEmpty(value),
        validateMinLength(value),
        validateMaxLength(value),
      )
      .map(_ => UserName(value))

  private def validateIsNotEmpty(value: String): Validation[Error, Unit] =
    if value.isEmpty
    then Validation.fail(Error.UserNameEmpty)
    else Validation.succeed(())

  private def validateMinLength(value: String): Validation[Error, Unit] =
    if value.length < MinLength
    then Validation.fail(Error.UserNameTooShort(MinLength))
    else Validation.succeed(())

  private def validateMaxLength(value: String): Validation[Error, Unit] =
    if value.length > MaxLength
    then Validation.fail(Error.UserNameTooLong(MaxLength))
    else Validation.succeed(())

  enum Error extends ApplicationError.ValidationError:
    case UserNameEmpty
    case UserNameTooLong(maxLength: Int)
    case UserNameTooShort(minLength: Int)

    override def key: String = "username"

    override def message: String = this match
      case UserNameEmpty               => "Username cannot be empty"
      case UserNameTooLong(maxLength)  => s"Username cannot be longer than $maxLength characters"
      case UserNameTooShort(minLength) => s"Username must be at least $minLength characters long"
}
