package conduit.domain.model.error

/**
 * Base trait for all application-specific errors in the Conduit system.
 *
 * This trait establishes a common interface for error handling across the application,
 * providing structured error information that can be consistently processed by
 * middleware and presented to users or logs.
 */
trait ApplicationError:
  /** Human-readable error message describing what went wrong */
  def message: String

  /** Error category/type, defaults to the class name for debugging */
  def kind: String              = getClass.getSimpleName

  /** Structured string representation for logging and debugging */
  override def toString: String = s"{kind: $kind, message: $message}"

object ApplicationError:
  /** Domain-specific business logic errors */
  trait DomainError       extends ApplicationError

  /** Errors caused by conflicting operations or resource contention */
  trait ConflictError     extends ApplicationError

  /** Temporary errors that might succeed if retried */
  trait TransientError    extends ApplicationError

  /** Authentication or authorization failures */
  trait UnauthorisedError extends ApplicationError

  /** System state inconsistencies or invariant violations */
  trait IllegalStateError extends ApplicationError

  /** Errors when requested resources cannot be found */
  trait NotFoundError     extends ApplicationError

  /**
   * Base trait for errors that wrap underlying exceptions.
   *
   * This provides a way to convert checked exceptions or system errors
   * into the application's error model while preserving the original cause.
   *
   * @param reason The underlying exception that caused this error
   */
  trait FromException(reason: Throwable) extends ApplicationError:
    override def message: String = reason.getMessage

  /**
   * Base trait for input validation errors.
   *
   * Validation errors typically occur when user input doesn't meet
   * the system's requirements or constraints.
   */
  trait ValidationError extends DomainError:
    /** The field or key that failed validation */
    def key: String
