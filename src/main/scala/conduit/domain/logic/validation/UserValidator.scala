package conduit.domain.logic.validation

import conduit.domain.model.entity.{ Credentials, UserProfile }
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.request.user.{ AuthenticateRequest, RegistrationRequest, UpdateUserRequest }
import zio.ZIO

trait UserValidator[Tx] {
  private type Registration = (user: UserProfile.Data, creds: Credentials.Clear) // for aliasing purpose only
  def validate(request: AuthenticateRequest): ZIO[Tx, ApplicationError, Credentials.Clear]
  def validate(request: RegistrationRequest): ZIO[Tx, ApplicationError, Registration]
  def validate(request: UpdateUserRequest): ZIO[Tx, ApplicationError, UserProfile.Data]
}
