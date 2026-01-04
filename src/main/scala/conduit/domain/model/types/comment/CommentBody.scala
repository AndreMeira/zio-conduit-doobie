package conduit.domain.model.types.comment

import conduit.domain.model.error.ApplicationError
import zio.prelude.{ Subtype, Validation }

type CommentBody = CommentBody.Type
object CommentBody extends Subtype[String] {
  private val minLength = 3
  private val maxLength = 500

  def fromString(body: String): Validation[Error, CommentBody] =
    validated(body.trim)

  def validated(body: String): Validation[Error, CommentBody] =
    Validation
      .validate(
        validateIsNotTooShort(body),
        validateIsNotTooLong(body),
      )
      .map(_ => CommentBody(body))

  def validateIsNotTooShort(body: String): Validation[CommentBody.Error, Unit] =
    if body.length >= minLength
    then Validation.succeed(())
    else Validation.fail(Error.CommentBodyTooShort(minLength))

  def validateIsNotTooLong(body: String): Validation[CommentBody.Error, Unit] =
    if body.length <= maxLength
    then Validation.succeed(())
    else Validation.fail(Error.CommentBodyTooLong(maxLength))

  enum Error extends ApplicationError.ValidationError:
    case CommentBodyTooShort(min: Int)
    case CommentBodyTooLong(max: Int)

    override def key: String = "comment body"

    override def message: String = this match
      case CommentBodyTooShort(min) => s"must be at least $min characters long"
      case CommentBodyTooLong(max)  => s"must be at most $max characters long"

}
