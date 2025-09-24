package conduit.domain.model.patching

import conduit.domain.model.entity.Credentials
import conduit.domain.model.types.user.{ Email as UserEmail, Password as UserPassword, HashedPassword as UserHashedPassword }
import zio.prelude.Validation

enum CredentialsPatch:
  case Email(value: UserEmail)
  case Password(value: UserPassword)
  case HashedPwd(value: UserHashedPassword)

  def apply(creds: Credentials.Hashed): Credentials.Hashed = this match
    case CredentialsPatch.HashedPwd(value) => creds.copy(password = value)
    case CredentialsPatch.Email(value)     => creds.copy(email = value)
    case CredentialsPatch.Password(_)      => creds

object CredentialsPatch:
  def apply(creds: Credentials.Hashed, patches: List[CredentialsPatch]): Credentials.Hashed =
    patches.foldLeft(creds)((c, p) => p.apply(c))

  def email(value: String): Validation[UserEmail.Error, CredentialsPatch.Email] =
    UserEmail.fromString(value).map(CredentialsPatch.Email.apply)

  def password(value: String): Validation[UserPassword.Error, CredentialsPatch.Password] =
    UserPassword.fromString(value).map(CredentialsPatch.Password.apply)
