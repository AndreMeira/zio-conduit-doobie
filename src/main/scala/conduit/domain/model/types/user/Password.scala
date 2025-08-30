package conduit.domain.model.types.user

import zio.prelude.{Subtype, Validation}

type Password = Password.Type
object Password extends Subtype[String] {
  private val minLength = 8
  private val maxLength = 64
  private val specialCharacters = "!@#$%^&*()-_=+[]{}|;:'\",.<>?/`~"
  
  def validated(value: String): Validation[Password.Error, Password] =
    Validation.validate(
      validateIsNotTooShort(value),
      validateIsNotTooLong(value),
      validateNoWhitespace(value),
      validateHasUppercase(value),
      validateHasLowercase(value),
      validateHasDigit(value),
      validateHasSpecialCharacter(value)
    ).map(_ => Password(value))
  
  def validateIsNotTooShort(value: String): Validation[Password.Error, Unit] =
    if value.length < minLength
    then Validation.fail(Error.PasswordTooShort(minLength))
    else Validation.succeed(())
    
  def validateIsNotTooLong(value: String): Validation[Password.Error, Unit] =
    if value.length > maxLength
    then Validation.fail(Error.PasswordTooLong(maxLength))
    else Validation.succeed(())
    
  def validateNoWhitespace(value: String): Validation[Password.Error, Unit] =
    if value.exists(_.isWhitespace)
    then Validation.fail(Error.PasswordContainsWhitespace)
    else Validation.succeed(())
    
  def validateHasUppercase(value: String): Validation[Password.Error, Unit] =
    if !value.exists(_.isUpper)
    then Validation.fail(Error.PasswordMissingUppercase)
    else Validation.succeed(())
    
  def validateHasLowercase(value: String): Validation[Password.Error, Unit] =
    if !value.exists(_.isLower)
    then Validation.fail(Error.PasswordMissingLowercase)
    else Validation.succeed(())
    
  def validateHasDigit(value: String): Validation[Password.Error, Unit] =
    if !value.exists(_.isDigit)
    then Validation.fail(Error.PasswordMissingDigit)
    else Validation.succeed(())
    
  def validateHasSpecialCharacter(value: String): Validation[Password.Error, Unit] = 
    if !value.exists(specialCharacters.contains(_))
    then Validation.fail(Error.PasswordMissingSpecialCharacter(specialCharacters))
    else Validation.succeed(())
    
  enum Error:
    case PasswordTooShort(minLength: Int)
    case PasswordTooLong(maxLength: Int)
    case PasswordContainsWhitespace
    case PasswordMissingUppercase
    case PasswordMissingLowercase
    case PasswordMissingDigit
    case PasswordMissingSpecialCharacter(required: String)

    def key: String = "password"
    
    def message: String = this match
      case PasswordTooShort(minLength)               => s"Password must be at least $minLength characters long"
      case PasswordTooLong(maxLength)                => s"Password cannot be longer than $maxLength characters"
      case PasswordContainsWhitespace                => "Password cannot contain whitespace"
      case PasswordMissingUppercase                  => "Password must contain at least one uppercase letter"
      case PasswordMissingLowercase                  => "Password must contain at least one lowercase letter"
      case PasswordMissingDigit                      => "Password must contain at least one digit"
      case PasswordMissingSpecialCharacter(required) => s"Password must contain at least one special character: $required"
}
