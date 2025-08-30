package conduit.domain.logic.validation

import conduit.domain.model.entity.User
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.request.user.{RegistrationRequest, UpdateUserRequest}
import zio.ZIO

trait UserValidator[Tx] {
  def validateRegistration(request: RegistrationRequest): ZIO[Tx, ApplicationError, User.Registration]
  def validateUpdate(request: UpdateUserRequest): ZIO[Tx, ApplicationError, User.Data]
}
