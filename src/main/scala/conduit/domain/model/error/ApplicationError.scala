package conduit.domain.model.error

trait ApplicationError:
  def message: String
  def kind: String              = this.getClass.getSimpleName
  override def toString: String = s"{kind: $kind, message: $message}"

object ApplicationError:
  trait DomainError       extends ApplicationError
  trait ConflictError     extends ApplicationError
  trait TransientError    extends ApplicationError
  trait UnauthorisedError extends ApplicationError
  trait IllegalStateError extends ApplicationError
  trait NotFoundError     extends ApplicationError

  trait FromException(reason: Throwable) extends ApplicationError:
    override def message: String = reason.getMessage

  trait ValidationError extends DomainError:
    def key: String
