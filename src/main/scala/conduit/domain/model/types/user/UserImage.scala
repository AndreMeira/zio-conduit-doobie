package conduit.domain.model.types.user

import com.sun.jndi.toolkit.url.Uri
import conduit.domain.model.error.ApplicationError
import zio.prelude.{ Subtype, Validation }

import scala.util.chaining.scalaUtilChainingOps

type UserImage = UserImage.Type
object UserImage extends Subtype[Uri] {

  def fromString(uri: String): Validation[UserImage.Error, UserImage] =
    validateURI(uri.trim).map(UserImage(_))

  def validateURI(uri: String): Validation[UserImage.Error, Uri] =
    try Uri(uri).pipe(Validation.succeed)
    catch case _: Exception => Validation.fail(Error.InvalidUri(uri))

  enum Error extends ApplicationError.ValidationError:
    case InvalidUri(value: String)

    override def key: String = "user image"

    override def message: String = this match
      case InvalidUri(value) => s"$value is not a valid URI"

}
