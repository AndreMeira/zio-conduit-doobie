package conduit.domain.service.validation

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.UserProfileRepository
import conduit.domain.logic.validation.UserValidator
import conduit.domain.model.entity.{ Credentials, UserProfile }
import conduit.domain.model.error.ApplicationError.ValidationError
import conduit.domain.model.error.NotFound
import conduit.domain.model.patching.UserProfilePatch
import conduit.domain.model.request.Patchable
import conduit.domain.model.request.user.{
  AuthenticateRequest,
  FollowUserRequest,
  GetProfileRequest,
  GetUserRequest,
  RegistrationRequest,
  UnfollowUserRequest,
  UpdateUserRequest,
}
import conduit.domain.model.types.article.AuthorId
import conduit.domain.model.types.user.{ Email, Password, UserId, UserName }
import conduit.domain.service.validation.UserValidationService.Failure
import zio.ZIO
import zio.prelude.Validation

class UserValidationService[Tx](
  monitor: Monitor,
  repository: UserProfileRepository[Tx],
) extends UserValidator[Tx] {

  override def validate(request: AuthenticateRequest): Result[Credentials.Clear] =
    monitor.track("UserValidationService.validateAuthentication") {
      ZIO.succeed {
        Validation
          .validate(
            Email.fromString(request.payload.user.email),
            Password.validated(request.payload.user.password),
          )
          .map(Credentials.Clear(_, _))
      }
    }

  override def validate(request: RegistrationRequest): Result[Registration] =
    monitor.track("UserValidationService.validateRegistration") {
      for {
        uniqueEmail <- validateEmailIsAvailable(Email(request.payload.user.email))
        uniqueName  <- validateUserNameIsAvailable(UserName(request.payload.user.username))
        email        = Email.fromString(request.payload.user.email)
        username     = UserName.fromString(request.payload.user.username)
        password     = Password.validated(request.payload.user.password)
      } yield Validation
        .validate(email, username, password, uniqueEmail, uniqueName)
        .map { (email, username, password, _, _) =>
          (UserProfile.Data(username, email, None, None), Credentials.Clear(email, password))
        }
    }

  override def validate(request: UpdateUserRequest): Result[List[UserProfilePatch]] =
    monitor.track("UserValidationService.validateUpdate") {
      val maybeEmail = request.payload.user.email.option
      val maybeName  = request.payload.user.username.option
      for {
        uniqueEmail <- ZIO.foreach(maybeEmail)(email => validateEmailIsAvailable(Email(email)))
        uniqueName  <- ZIO.foreach(maybeName)(username => validateUserNameIsAvailable(UserName(username)))
        email        = uniqueEmail.getOrElse(Validation.succeed(()))
        username     = uniqueName.getOrElse(Validation.succeed(()))
        patches     <- validatePatchFields(request)
      } yield Validation
        .validate(patches, email, username)
        .map((patches, _, _) => patches)
    }

  override def validate(request: GetUserRequest): Result[UserId] =
    monitor.track("UserValidationService.validateGetUser") {
      ZIO.succeed(Validation.succeed(request.requester.userId))
    }

  override def validate(request: FollowUserRequest): Result[AuthorId] =
    monitor.track("UserValidationService.validateFollow") {
      validateExists(request.username).map: validated =>
        validated.flatMap: authorId =>
          if authorId == AuthorId(request.requester.userId)
          then Validation.fail(Failure.FollowSelf)
          else Validation.succeed(authorId)
    }

  override def validate(request: UnfollowUserRequest): Result[AuthorId] =
    monitor.track("UserValidationService.validateUnfollow") {
      validateExists(request.username).map: validated =>
        validated.flatMap: authorId =>
          if authorId == AuthorId(request.requester.userId)
          then Validation.fail(Failure.UnfollowSelf)
          else Validation.succeed(authorId)
    }

  override def validate(request: GetProfileRequest): Result[UserName] =
    monitor.track("UserValidationService.validateGetProfile") {
      validateExists(request.username).map: validated =>
        validated.map(_ => UserName(request.username))
    }

  private def validateExists(username: String): Result[AuthorId] =
    repository
      .findByUserName(UserName(username))
      .map:
        case Some(profile) => Validation.succeed(AuthorId(profile.id))
        case None          => Validation.fail(NotFound.user(UserName(username)))

  private def validatePatchFields(request: UpdateUserRequest): Result[List[UserProfilePatch]] =
    ZIO.succeed {
      List(
        validateEmailPatch(request.payload.user.email),
        validateUserNamePatch(request.payload.user.username),
        validateBioPatch(request.payload.user.bio),
        validateImagePatch(request.payload.user.image),
      ).flatten match
        case Nil     => Validation.succeed(Nil)
        case patches => Validation.validateAll(patches)
    }

  private def validateEmailPatch(email: Patchable[String]): Option[Validated[UserProfilePatch.Email]] =
    email match {
      case Patchable.Present(value) => Some(UserProfilePatch.email(value))
      case _                        => None
    }

  private def validateUserNamePatch(username: Patchable[String]): Option[Validated[UserProfilePatch.Name]] =
    username match {
      case Patchable.Present(value) => Some(UserProfilePatch.name(value))
      case _                        => None
    }

  private def validateBioPatch(bio: Patchable[String]): Option[Validated[UserProfilePatch.Bio]] =
    bio match {
      case Patchable.Present(value) => Some(UserProfilePatch.bio(Some(value)))
      case Patchable.Empty          => Some(UserProfilePatch.bio(None))
      case Patchable.Absent         => None
    }

  private def validateImagePatch(image: Patchable[String]): Option[Validated[UserProfilePatch.Image]] =
    image match {
      case Patchable.Present(value) => Some(UserProfilePatch.image(Some(value)))
      case Patchable.Empty          => Some(UserProfilePatch.image(None))
      case Patchable.Absent         => None
    }

  private def validateUserNameIsAvailable(username: UserName): Result[Unit] =
    repository
      .userNameExists(username)
      .map:
        case true  => Validation.fail(Failure.UserNameAlreadyInUse(username))
        case false => Validation.succeed(())

  private def validateEmailIsAvailable(email: Email): Result[Unit] =
    repository
      .emailExists(email)
      .map:
        case true  => Validation.fail(Failure.EmailAlreadyInUse(email))
        case false => Validation.succeed(())
}

object UserValidationService:
  enum Failure extends ValidationError:
    case FollowSelf
    case UnfollowSelf
    case AuthorNotFound(username: String)
    case EmailAlreadyInUse(email: String)
    case UserNameAlreadyInUse(username: String)
    case UserNotFound(userId: Long)

    override def key: String     = "user"
    override def message: String = this match
      case AuthorNotFound(username)       => s"User with username '$username' not found"
      case EmailAlreadyInUse(email)       => s"Email '$email' is already in use"
      case UserNameAlreadyInUse(username) => s"Username '$username' is already in use"
      case UserNotFound(userId)           => s"User with id '$userId' not found"
      case FollowSelf                     => s"Cannot follow yourself"
