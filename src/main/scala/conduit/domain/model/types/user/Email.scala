package conduit.domain.model.types.user

import conduit.domain.model.error.ApplicationError.ValidationError
import zio.prelude.{ Subtype, Validation }

type Email = Email.Type
object Email extends Subtype[String] {

  def fromString(email: String): Validation[Email.Error, Email] =
    validated(email.trim)

  // @todo improve email validation
  def validated(email: String): Validation[Email.Error, Email] =
    if email.contains("@")
    then Validation.succeed(Email(email.toLowerCase))
    else Validation.fail(Error.InvalidEmail(email))

  enum Error extends ValidationError:
    case InvalidEmail(value: String)
    override def key: String     = "email"
    override def message: String = this match
      case InvalidEmail(value) => s"$value is not a valid email address"
}
