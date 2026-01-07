package conduit.domain.service.authentication

import izumi.reflect.Tag

object AuthenticationModule {
  def layer[Tx: Tag] =
    CredentialsAuthenticationService.layer[Tx] ++
      TokenAuthenticationService.layer[Tx] ++
      TokenAuthenticationService.layer[Any]
}
