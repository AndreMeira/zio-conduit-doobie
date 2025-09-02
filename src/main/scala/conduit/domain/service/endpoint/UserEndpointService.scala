package conduit.domain.service.endpoint

import conduit.domain.logic.authentication.{ CredentialsAuthenticator, TokenAuthenticator }
import conduit.domain.logic.authorisation.UserAuthorisation
import conduit.domain.logic.endpoint.UserEndpoint
import conduit.domain.logic.monitoring.Monitor
import conduit.domain.logic.persistence.{ FollowerRepository, UnitOfWork, UserProfileRepository, UserRepository }
import conduit.domain.logic.validation.UserValidator
import conduit.domain.model.entity.Follower
import conduit.domain.model.error.NotFound
import conduit.domain.model.error.InconsistentState
import conduit.domain.model.patching.UserProfilePatch
import conduit.domain.model.request.user.*
import conduit.domain.model.response.user.*
import conduit.domain.model.types.article.AuthorId
import conduit.domain.service.endpoint.dsl.EndpointDsl
import zio.ZIO

class UserEndpointService[Tx](
  monitor: Monitor,
  unitOfWork: UnitOfWork[Tx],
  authorisation: UserAuthorisation[Tx],
  validation: UserValidator[Tx],
  credsAuth: CredentialsAuthenticator[Tx],
  tokenAuth: TokenAuthenticator[Tx],
  users: UserRepository[Tx],
  followers: FollowerRepository[Tx],
  profiles: UserProfileRepository[Tx],
) extends UserEndpoint[Tx], EndpointDsl {

  override def follow(request: FollowUserRequest): Result[ProfileResponse] =
    monitor.track("UserEndpointService.follow") {
      unitOfWork.execute:
        for {
          _         <- authorisation.authorise(request).allowedOrFail
          authorId  <- validation.validate(request).validOrFailWith(NotFound.profile(request.username))
          profile   <- profiles.findById(authorId).someOrFail(NotFound.profile(request.username))
          follower   = Follower(request.requester.userId, authorId)
          following <- followers.exists(follower)
          _         <- ZIO.when(!following)(followers.save(follower))
        } yield ProfileResponse.make(profile, following = true)
    }

  override def unfollow(request: UnfollowUserRequest): Result[ProfileResponse] =
    monitor.track("UserEndpointService.unfollow") {
      unitOfWork.execute:
        for {
          _         <- authorisation.authorise(request).allowedOrFail
          authorId  <- validation.validate(request).validOrFailWith(NotFound.profile(request.username))
          profile   <- profiles.findById(authorId).someOrFail(NotFound.profile(request.username))
          follower   = Follower(request.requester.userId, authorId)
          following <- followers.exists(follower)
          _         <- ZIO.when(following)(followers.delete(follower))
        } yield ProfileResponse.make(profile, following = false)
    }

  override def getProfile(request: GetProfileRequest): Result[ProfileResponse] =
    monitor.track("UserEndpointService.getProfile") {
      unitOfWork.execute:
        for {
          _         <- authorisation.authorise(request).allowedOrFail
          profileId <- validation.validate(request).validOrFailWith(NotFound.profile(request.username))
          profile   <- profiles.findByUserName(profileId).someOrFail(NotFound.profile(request.username))
          follower   = request.requester.option.map(user => Follower(user.userId, AuthorId(profile.id)))
          following <- ZIO.foreach(follower)(followers.exists).map(_.getOrElse(false))
        } yield ProfileResponse.make(profile, following)
    }

  override def update(request: UpdateUserRequest): Result[AuthenticationResponse] =
    monitor.track("UserEndpointService.update") {
      unitOfWork.execute:
        for {
          _           <- authorisation.authorise(request).allowedOrFail
          patches     <- validation.validate(request).validOrFail
          userId       = request.requester.userId
          currentUser <- profiles.findById(userId).someOrFail(NotFound.user(userId))
          patched      = UserProfilePatch.apply(currentUser, patches)
          updated     <- profiles.save(patched)
          token       <- tokenAuth.generateToken(userId)
        } yield AuthenticationResponse.make(updated, token)
    }

  override def login(request: AuthenticateRequest): Result[AuthenticationResponse] =
    monitor.track("UserEndpointService.login") {
      unitOfWork.execute:
        for {
          _           <- authorisation.authorise(request).allowedOrFail
          credentials <- validation.validate(request).validOrFail
          user        <- credsAuth.authenticate(credentials)
          profile     <- profiles.findById(user.userId).someOrFail(InconsistentState.noProfile(user.userId))
          token       <- tokenAuth.generateToken(user.userId)
        } yield AuthenticationResponse.make(profile, token)
    }

  override def getCurrent(request: GetUserRequest): Result[AuthenticationResponse] =
    monitor.track("UserEndpointService.getCurrent") {
      unitOfWork.execute:
        val userId = request.requester.userId
        for {
          _       <- authorisation.authorise(request).allowedOrFail
          userId  <- validation.validate(request).validOrFailWith(NotFound.user(userId))
          profile <- profiles.findById(userId).someOrFail(NotFound.user(userId))
          token   <- tokenAuth.generateToken(userId)
        } yield AuthenticationResponse.make(profile, token)
    }

  override def register(request: RegistrationRequest): Result[AuthenticationResponse] =
    monitor.track("UserEndpointService.register") {
      unitOfWork.execute:
        for {
          _                      <- authorisation.authorise(request).allowedOrFail
          (profile, credentials) <- validation.validate(request).validOrFail
          hashedCredentials      <- credsAuth.hash(credentials)
          user                   <- users.save(hashedCredentials)
          profile                <- profiles.save(user, profile)
          token                  <- tokenAuth.generateToken(user)
        } yield AuthenticationResponse.make(profile, token)
    }
}
