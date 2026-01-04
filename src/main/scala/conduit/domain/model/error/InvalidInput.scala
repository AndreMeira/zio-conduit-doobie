package conduit.domain.model.error

import conduit.domain.model.error.ApplicationError.{ DomainError, ValidationError }
import zio.prelude.Validation

case class InvalidInput(errors: List[ValidationError]) extends DomainError:
  override def message: String = errors.map(e => s"${e.key}: ${e.message}").mkString(", ")

  def containsNotFound: Boolean = errors.exists {
    case _: ApplicationError.NotFoundError => true
    case _                                 => false
  }

  def containsOnlyNotFound: Boolean = errors.forall {
    case _: ApplicationError.NotFoundError => true
    case _                                 => false
  }
