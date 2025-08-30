package conduit.domain.model.types.article

import conduit.domain.model.error.ApplicationError
import zio.prelude.{Subtype, Validation}

type ArticleTitle = ArticleTitle.Type
object ArticleTitle extends Subtype[String] {
  private val MinLength = 1
  private val MaxLength = 100
  
  def fromString(value: String): Validation[ArticleTitle.Error, ArticleTitle] =
    validated(value.trim.replaceAll("\\s+", " "))
  
  def validated(value: String): Validation[ArticleTitle.Error, ArticleTitle] =
    Validation.validate(
      validateIsNotEmpty(value),
      validateIsNotTooShort(value),
      validateIsNotTooLong(value)
    ).map(_ => ArticleTitle(value))
  
  def validateIsNotEmpty(value: String): Validation[ArticleTitle.Error, Unit] =
    if value.isEmpty
    then Validation.fail(Error.ArticleTitleEmpty)
    else Validation.succeed(())

  def validateIsNotTooLong(value: String): Validation[ArticleTitle.Error, Unit] =
    if value.length > MaxLength
    then Validation.fail(Error.ArticleTitleTooLong(MaxLength))
    else Validation.succeed(())
    
  def validateIsNotTooShort(value: String): Validation[ArticleTitle.Error, Unit] =
    if value.length < MinLength
    then Validation.fail(Error.ArticleTitleTooShort(MinLength))
    else Validation.succeed(())
  
  enum Error extends ApplicationError.ValidationError:
    case ArticleTitleEmpty
    case ArticleTitleTooLong(maxLength: Int)
    case ArticleTitleTooShort(minLength: Int)
    
    override def key: String = "article title"
    
    override def message: String = this match
      case ArticleTitleEmpty               => "Article title cannot be empty"
      case ArticleTitleTooLong(maxLength)  => s"Article title cannot be longer than $maxLength characters"
      case ArticleTitleTooShort(minLength) => s"Article title cannot be shorter than $minLength characters"

}
