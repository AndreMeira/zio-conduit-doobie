package conduit.domain.model.error

trait ApplicationError:
  def message: String
  def kind: String = this.getClass.getSimpleName

object ApplicationError:
  trait DomainError extends ApplicationError
  trait TransientError extends ApplicationError
  trait UnauthorisedError extends ApplicationError
  trait NotFoundError extends DomainError

  trait ValidationError extends DomainError:
    def key: String

  case class InvalidInput(errors: List[ValidationError]) extends DomainError:
    override def message: String = errors.map(e => s"${e.key}: ${e.message}").mkString(", ")
