package conduit.domain.model.types.user

import conduit.domain.model.error.ApplicationError
import zio.prelude.{ Subtype, Validation }

import java.net.URI
import scala.util.chaining.scalaUtilChainingOps

type UserImage = UserImage.Type
object UserImage extends Subtype[URI] {

  def fromString(url: String): Validation[UserImage.Error, UserImage] =
    validateURL(url.trim).map(UserImage(_))

  def validateURL(url: String): Validation[UserImage.Error, URI] =
    try URI(url).pipe(Validation.succeed)
    catch case _: Exception => Validation.fail(Error.InvalidURL(url))

  enum Error extends ApplicationError.ValidationError:
    case InvalidURL(value: String)

    override def key: String = "user image"

    override def message: String = this match
      case InvalidURL(value) => s"$value is not a valid URL"

}
