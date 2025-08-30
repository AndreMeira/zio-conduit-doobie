package conduit.domain.model.request.user

import conduit.domain.model.entity.Requester

case class AuthenticateRequest(requester: Requester, payload: AuthenticateRequest.Payload)

object AuthenticateRequest:
  case class Payload(user: Data) // wrapping due to api spec
  case class Data(email: String, password: String) // wrapping due to api spec
