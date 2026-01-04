package conduit.domain.service

import conduit.domain.service.authentication.AuthenticationModule
import conduit.domain.service.authorisation.AuthorisationModule
import conduit.domain.service.entrypoint.EntrypointModule
import conduit.domain.service.validation.ValidationModule
import zio.Tag

object DomainServiceModule {
  def layer[Tx: Tag] =
    (AuthenticationModule.layer[Tx] ++ AuthorisationModule.layer[Tx] ++ ValidationModule.layer[Tx]) >>> EntrypointModule.layer[Tx]
      ++ AuthenticationModule.layer[Tx] ++ AuthorisationModule.layer[Tx] ++ ValidationModule.layer[Tx]
}
