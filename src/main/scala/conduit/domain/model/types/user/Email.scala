package conduit.domain.model.types.user

import conduit.domain.model.error.ApplicationError.ValidationError
import zio.prelude.{ Subtype, Validation }

/**
 * Type-safe wrapper for email addresses with validation.
 *
 * Ensures email addresses are properly formatted and normalized before use.
 * Uses ZIO Prelude's Subtype for compile-time type safety and validation support.
 */
type Email = Email.Type

/**
 * Companion object providing validation and construction for Email addresses.
 */
object Email extends Subtype[String] {

  /**
   * Creates an Email from a string with trimming and validation.
   *
   * @param email Input email string
   * @return Validated Email or validation error
   */
  def fromString(email: String): Validation[Email.Error, Email] =
    validated(email.trim)

  /**
   * Validates and normalizes an email address.
   *
   * Currently performs basic validation by checking for '@' symbol.
   * The email is converted to lowercase for normalization.
   *
   * @param email Email string to validate
   * @return Validated Email instance or validation error
   */
  // @todo improve email validation
  def validated(email: String): Validation[Email.Error, Email] =
    if email.contains("@")
    then Validation.succeed(Email(email.toLowerCase))
    else Validation.fail(Error.InvalidEmail(email))

  /**
   * Enumeration of email validation errors.
   */
  enum Error extends ValidationError:
    /** Error when email format is invalid */
    case InvalidEmail(value: String)

    override def key: String     = "email"
    override def message: String = this match
      case InvalidEmail(value) => s"$value is not a valid email address"
}
