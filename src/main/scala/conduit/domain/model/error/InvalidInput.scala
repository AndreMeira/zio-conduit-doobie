package conduit.domain.model.error

import conduit.domain.model.error.ApplicationError.{ DomainError, ValidationError }
import zio.prelude.Validation

/**
 * Represents a collection of input validation errors.
 *
 * This error type is used when user input fails validation rules.
 * It can contain multiple validation errors that occurred simultaneously,
 * allowing the application to report all validation issues at once
 * rather than stopping at the first error.
 *
 * @param errors List of individual validation errors
 */
case class InvalidInput(errors: List[ValidationError]) extends DomainError:
  /** Combined error message listing all validation failures */
  override def message: String = errors.map(e => s"${e.key}: ${e.message}").mkString(", ")

  /**
   * Checks if any of the validation errors are "not found" errors.
   *
   * This is useful for determining if the validation failed because
   * referenced entities don't exist, which might require different
   * handling than other validation failures.
   *
   * @return true if at least one error is a NotFoundError
   */
  def containsNotFound: Boolean = errors.exists {
    case _: ApplicationError.NotFoundError => true
    case _                                 => false
  }

  /**
   * Checks if all validation errors are "not found" errors.
   *
   * This can be used to determine if the entire validation failure
   * was due to missing entities rather than invalid input format.
   *
   * @return true if all errors are NotFoundError instances
   */
  def containsOnlyNotFound: Boolean = errors.forall {
    case _: ApplicationError.NotFoundError => true
    case _                                 => false
  }
