package conduit.domain.service.entrypoint

import conduit.domain.logic.authentication.{ CredentialsAuthenticator, TokenAuthenticator }
import conduit.domain.logic.authorisation.UserAuthorisation
import conduit.domain.logic.entrypoint.UserEntrypoint
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.{ FollowerRepository, UnitOfWork, UserProfileRepository, UserRepository }
import conduit.domain.logic.validation.UserValidator
import conduit.domain.model.entity.{ Credentials, Follower, UserProfile }
import conduit.domain.model.error.{ ApplicationError, InconsistentState, NotFound }
import conduit.domain.model.patching.{ UserProfilePatch, CredentialsPatch as CredsPatch }
import conduit.domain.model.request.user.*
import conduit.domain.model.response.user.*
import conduit.domain.model.types.article.AuthorId
import conduit.domain.model.types.user.UserId
import conduit.domain.service.entrypoint.dsl.EntrypointDsl
import izumi.reflect.Tag as ReflectionTag
import zio.{ ZIO, ZLayer }

class UserEntrypointService[Tx](
  monitor: Monitor,
  unitOfWork: UnitOfWork[Tx],
  authorisation: UserAuthorisation[Tx],
  validation: UserValidator[Tx],
  credsAuth: CredentialsAuthenticator[Tx],
  tokenAuth: TokenAuthenticator[Tx],
  users: UserRepository[Tx],
  followers: FollowerRepository[Tx],
  profiles: UserProfileRepository[Tx],
) extends UserEntrypoint, EntrypointDsl(unitOfWork, authorisation) {

  override def follow(request: FollowUserRequest): Result[GetProfileResponse] =
    monitor.track("UserEndpointService.follow") {
      authorise(request):
        for {
          authorName <- validation.parse(request).validOrFail
          profile    <- profiles.findByUserName(authorName) ?! NotFound.profile(authorName)
          follower    = Follower(request.requester.userId, AuthorId(profile.id))
          _          <- followers.save(follower)
        } yield GetProfileResponse.make(profile, following = true)
    }

  override def unfollow(request: UnfollowUserRequest): Result[GetProfileResponse] =
    monitor.track("UserEndpointService.unfollow") {
      authorise(request):
        for {
          authorName <- validation.parse(request).validOrFail
          profile    <- profiles.findByUserName(authorName) ?! NotFound.profile(authorName)
          follower    = Follower(request.requester.userId, AuthorId(profile.id))
          _          <- followers.delete(follower)
        } yield GetProfileResponse.make(profile, following = false)
    }

  override def getProfile(request: GetProfileRequest): Result[GetProfileResponse] =
    monitor.track("UserEndpointService.getProfile") {
      authorise(request):
        for {
          authorName <- validation.parse(request).validOrFail
          profile    <- profiles.findByUserName(authorName) ?! NotFound.profile(authorName)
          follower    = request.requester.option.map(user => Follower(user.userId, AuthorId(profile.id)))
          following  <- ZIO.foreach(follower)(followers.exists).map(_.getOrElse(false))
        } yield GetProfileResponse.make(profile, following)
    }

  override def login(request: AuthenticateRequest): Result[AuthenticationResponse] =
    monitor.track("UserEndpointService.login") {
      authorise(request):
        for {
          credentials <- validation.parse(request).validOrFail
          user        <- credsAuth.authenticate(credentials)
          profile     <- profiles.findById(user.userId) ?! InconsistentState.noProfile(user.userId)
          email       <- users.findEmail(user.userId) ?! InconsistentState.noEmail(user.userId)
          token       <- tokenAuth.generateToken(user.userId)
        } yield AuthenticationResponse.make(email, profile, token)
    }

  override def getCurrent(request: GetUserRequest): Result[AuthenticationResponse] =
    monitor.track("UserEndpointService.getCurrent") {
      authorise(request):
        val userId = request.requester.userId
        for {
          userId  <- validation.parse(request).validOrFail
          profile <- profiles.findById(userId) ?! NotFound.user(userId)
          email   <- users.findEmail(userId) ?! InconsistentState.noEmail(userId)
          token   <- tokenAuth.generateToken(userId)
        } yield AuthenticationResponse.make(email, profile, token)
    }

  override def register(request: RegistrationRequest): Result[AuthenticationResponse] =
    monitor.track("UserEndpointService.register") {
      authorise(request):
        for {
          (profile, credentials) <- validation.parse(request).validOrFail
          hashedCredentials      <- credsAuth.hashCredentials(credentials)
          userId                 <- users.save(hashedCredentials)
          profile                <- profiles.create(userId, profile)
          token                  <- tokenAuth.generateToken(userId)
        } yield AuthenticationResponse.make(credentials.email, profile, token)
    }

  override def update(request: UpdateUserRequest): Result[AuthenticationResponse] =
    monitor.track("UserEndpointService.update") {
      authorise(request):
        for {
          patches <- validation.parse(request).validOrFail
          userId   = request.requester.userId
          profile <- updateProfile(userId, patches.profile)
          creds   <- updateCredentials(userId, patches.creds)
          token   <- tokenAuth.generateToken(userId)
        } yield AuthenticationResponse.make(creds.email, profile, token)
    }

  private def updateProfile(userId: UserId, patches: List[UserProfilePatch]): ZIO[Tx, ApplicationError, UserProfile] =
    for {
      profile <- profiles.findById(userId) ?! NotFound.user(userId)
      updated  = UserProfilePatch.apply(profile, patches)
      saved   <- profiles.save(updated)
    } yield saved

  private def updateCredentials(userId: UserId, patches: List[CredsPatch]): ZIO[Tx, ApplicationError, Credentials.Hashed] =
    for {
      safePatches <- ZIO.foreach(patches):
                       // transform password patch into a hashed password patch
                       case CredsPatch.Password(pwd) => credsAuth.hash(pwd).map(CredsPatch.HashedPwd(_))
                       case other                    => ZIO.succeed(other)
      creds       <- users.findCredentials(userId) ?! InconsistentState.noCredentials(userId)
      updated      = CredsPatch.apply(creds, safePatches)
      _           <- ZIO.when(creds != updated)(users.save(userId, updated))
    } yield updated

}

object UserEntrypointService:
  type Dependency[Tx] = UserProfileRepository[Tx] & FollowerRepository[Tx] & UserRepository[Tx] & TokenAuthenticator[Tx] &
    CredentialsAuthenticator[Tx] & UserValidator[Tx] & UserAuthorisation[Tx] & UnitOfWork[Tx] & Monitor

  def layer[Tx: ReflectionTag]: ZLayer[Dependency[Tx], Nothing, UserEntrypointService[Tx]] =
    ZLayer {
      for {
        monitor       <- ZIO.service[Monitor]
        unitOfWork    <- ZIO.service[UnitOfWork[Tx]]
        authorisation <- ZIO.service[UserAuthorisation[Tx]]
        validation    <- ZIO.service[UserValidator[Tx]]
        credsAuth     <- ZIO.service[CredentialsAuthenticator[Tx]]
        tokenAuth     <- ZIO.service[TokenAuthenticator[Tx]]
        users         <- ZIO.service[UserRepository[Tx]]
        followers     <- ZIO.service[FollowerRepository[Tx]]
        profiles      <- ZIO.service[UserProfileRepository[Tx]]
      } yield UserEntrypointService(
        monitor,
        unitOfWork,
        authorisation,
        validation,
        credsAuth,
        tokenAuth,
        users,
        followers,
        profiles,
      )
    }
