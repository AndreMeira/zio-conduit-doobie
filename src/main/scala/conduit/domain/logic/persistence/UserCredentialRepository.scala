package conduit.domain.logic.persistence

import conduit.domain.model.entity.Credential
import conduit.domain.model.error.ApplicationError
import zio.ZIO

trait UserCredentialRepository[Tx] {
  def save(credential: Credential): ZIO[Tx, UserCredentialRepository.Error, Unit]
  def find(credential: Credential): ZIO[Tx, UserCredentialRepository.Error, Option[Credential]]
}

object UserCredentialRepository:
  trait Error extends ApplicationError.TransientError
