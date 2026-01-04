package conduit.domain.service.validation

import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.{ UserProfileRepository, UserRepository }
import conduit.domain.logic.validation.UserValidator
import conduit.domain.model.entity.{ Credentials, UserProfile }
import conduit.domain.model.error.ApplicationError.ValidationError
import conduit.domain.model.patching.{ CredentialsPatch, UserProfilePatch }
import conduit.domain.model.request.Patchable
import conduit.domain.model.request.user.*
import conduit.domain.model.types.user.{ Email, Password, UserId, UserName }
import conduit.domain.service.validation.UserValidationService.Failure
import izumi.reflect.Tag as ReflectionTag
import zio.prelude.Validation
import zio.{ ZIO, ZLayer }

class UserValidationService[Tx](
  monitor: Monitor,
  val users: UserRepository[Tx],
  val profiles: UserProfileRepository[Tx],
) extends UserValidator[Tx] {

  override type Error = profiles.Error | users.Error // can only fail with the same errors as the injected repositories

  override def parse(request: GetProfileRequest): Result[UserName] =
    monitor.track("UserValidationService.validateGetProfile") {
      ZIO.succeed {
        Validation.succeed(UserName(request.username))
      }
    }

  override def parse(request: GetUserRequest): Result[UserId] =
    monitor.track("UserValidationService.validateGetUser") {
      ZIO.succeed {
        Validation.succeed(request.requester.userId)
      }
    }

  override def parse(request: FollowUserRequest): Result[UserName] =
    monitor.track("UserValidationService.validateFollow") {
      // Is the user to follow the same as the requester?
      profiles
        .exists(request.requester.userId, UserName(request.username))
        .map:
          case true  => Validation.fail(Failure.FollowSelf)
          case false => Validation.succeed(UserName(request.username))
    }

  override def parse(request: UnfollowUserRequest): Result[UserName] =
    monitor.track("UserValidationService.validateUnfollow") {
      // Is the user to unfollow the same as the requester?
      profiles
        .exists(request.requester.userId, UserName(request.username))
        .map:
          case true  => Validation.fail(Failure.UnfollowSelf)
          case false => Validation.succeed(UserName(request.username))
    }

  override def parse(request: AuthenticateRequest): Result[Credentials.Clear] =
    monitor.track("UserValidationService.validateAuthentication") {
      ZIO.succeed {
        Validation
          .validate(
            Email.fromString(request.payload.user.email),
            Password.fromString(request.payload.user.password),
          )
          .map(Credentials.Clear(_, _))
      }
    }

  override def parse(request: RegistrationRequest): Result[Registration] =
    monitor.track("UserValidationService.validateRegistration") {
      for {
        uniqueEmail <- validateEmailIsAvailable(Email(request.payload.user.email))
        uniqueName  <- validateUserNameIsAvailable(UserName(request.payload.user.username))
        email        = Email.fromString(request.payload.user.email)
        username     = UserName.fromString(request.payload.user.username)
        password     = Password.fromString(request.payload.user.password)
      } yield Validation
        .validate(email, username, password, uniqueEmail, uniqueName)
        .map { (email, username, password, _, _) =>
          (UserProfile.Data(username, None, None), Credentials.Clear(email, password))
        }
    }

  override def parse(request: UpdateUserRequest): Result[Patches] =
    monitor.track("UserValidationService.validateUpdate") {
      val maybeEmail = request.payload.user.email.option
      val maybeName  = request.payload.user.username.option
      for {
        uniqueEmail        <- ZIO.foreach(maybeEmail)(email => validateEmailIsAvailable(Email(email)))
        uniqueName         <- ZIO.foreach(maybeName)(username => validateUserNameIsAvailable(UserName(username)))
        email               = uniqueEmail.getOrElse(Validation.succeed(()))
        username            = uniqueName.getOrElse(Validation.succeed(()))
        profilePatches     <- validateProfilePatches(request)
        credentialsPatches <- validateCredentialsPatches(request)
      } yield Validation
        .validate(profilePatches, credentialsPatches, email, username)
        .map((profilePatches, credsPatches, _, _) => (profile = profilePatches, creds = credsPatches))
    }

  private def validateProfilePatches(request: UpdateUserRequest): Result[List[UserProfilePatch]] =
    ZIO.succeed {
      List(
        validateUserNamePatch(request.payload.user.username),
        validateBioPatch(request.payload.user.bio),
        validateImagePatch(request.payload.user.image),
      ).flatten match
        case Nil     => Validation.succeed(Nil)
        case patches => Validation.validateAll(patches)
    }

  private def validateCredentialsPatches(request: UpdateUserRequest): Result[List[CredentialsPatch]] =
    ZIO.succeed {
      List(
        validateEmailPatch(request.payload.user.email),
        validatePasswordPatch(request.payload.user.password),
      ).flatten match
        case Nil     => Validation.succeed(Nil)
        case patches => Validation.validateAll(patches)
    }

  private def validateEmailPatch(email: Patchable[String]): Option[Validated[CredentialsPatch]] =
    email match {
      case Patchable.Present(value) => Some(CredentialsPatch.email(value))
      case _                        => None
    }

  private def validatePasswordPatch(password: Patchable[String]): Option[Validated[CredentialsPatch]] =
    password match {
      case Patchable.Present(value) => Some(CredentialsPatch.password(value))
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
    profiles
      .exists(username)
      .map:
        case true  => Validation.fail(Failure.UserNameAlreadyInUse(username))
        case false => Validation.succeed(())

  private def validateEmailIsAvailable(email: Email): Result[Unit] =
    users
      .emailExists(email)
      .map:
        case true  => Validation.fail(Failure.EmailAlreadyInUse(email))
        case false => Validation.succeed(())
}

object UserValidationService:
  enum Failure extends ValidationError {
    case FollowSelf
    case UnfollowSelf
    case AuthorNotFound(username: String)
    case EmailAlreadyInUse(email: String)
    case UserNameAlreadyInUse(username: String)
    case UserNotFound(userId: Long)

    override def key: String = "user"

    override def kind: String = this match
      case FollowSelf              => "FollowSelf"
      case UnfollowSelf            => "UnfollowSelf"
      case AuthorNotFound(_)       => "AuthorNotFound"
      case EmailAlreadyInUse(_)    => "EmailAlreadyInUse"
      case UserNameAlreadyInUse(_) => "UserNameAlreadyInUse"
      case UserNotFound(_)         => "UserNotFound"

    override def message: String = this match
      case AuthorNotFound(username)       => s"User with username '$username' not found"
      case EmailAlreadyInUse(email)       => s"Email '$email' is already in use"
      case UserNameAlreadyInUse(username) => s"Username '$username' is already in use"
      case UserNotFound(userId)           => s"User with id '$userId' not found"
      case FollowSelf                     => s"Cannot follow yourself"
      case UnfollowSelf                   => s"Cannot unfollow yourself"
  }

  def layer[Tx: ReflectionTag]: ZLayer[
    UserProfileRepository[Tx] & UserRepository[Tx] & Monitor,
    Nothing,
    UserValidator[Tx],
  ] = ZLayer {
    for {
      monitor  <- ZIO.service[Monitor]
      users    <- ZIO.service[UserRepository[Tx]]
      profiles <- ZIO.service[UserProfileRepository[Tx]]
    } yield new UserValidationService(monitor, users, profiles)
  }
