package conduit.domain.model.entity

import conduit.domain.model.types.user.*
import conduit.domain.model.types.article.*
import conduit.domain.model.types.comment.*
import conduit.domain.model.types.user

import java.net.URI
import zio.test.Gen

object Generator {
  def credentials: Gen[Any, Credentials.Hashed] = for {
    email    <- Gen.alphaChar.map(_ + "@example.com")
    password <- Gen.stringBounded(8, 16)(Gen.alphaChar)
  } yield Credentials.Hashed(Email(email), HashedPassword(password))

  def userId: Gen[Any, UserId] =
    Gen.long.map(UserId(_))

  def authorId: Gen[Any, AuthorId] =
    userId.map(AuthorId(_))

  def commentAuthorId: Gen[Any, CommentAuthorId] =
    userId.map(CommentAuthorId(_))

  def image: Gen[Any, UserImage] = Gen
    .stringBounded(10, 200)(Gen.alphaChar)
    .map(s => URI("http://example.com/" + s))
    .map(UserImage(_))

  def bio: Gen[Any, Biography] = Gen
    .stringBounded(0, 160)(Gen.alphaChar)
    .map(Biography(_))

  def userName: Gen[Any, UserName] = Gen
    .stringBounded(3, 20)(Gen.alphaChar)
    .map(UserName(_))

  def title: Gen[Any, ArticleTitle] = Gen
    .stringBounded(10, 100)(Gen.alphaChar)
    .map(ArticleTitle(_))

  def slug: Gen[Any, ArticleSlug] =
    title.map(ArticleSlug(_))

  def description: Gen[Any, ArticleDescription] = Gen
    .stringBounded(20, 200)(Gen.alphaChar)
    .map(ArticleDescription(_))

  def body: Gen[Any, ArticleBody] = Gen
    .stringBounded(50, 5000)(Gen.alphaChar)
    .map(ArticleBody(_))

  def commentBody: Gen[Any, CommentBody] = Gen
    .stringBounded(1, 1000)(Gen.alphaChar)
    .map(CommentBody(_))

  def profileData: Gen[Any, UserProfile.Data] = for {
    username <- userName
    bio      <- Gen.option(bio)
    image    <- Gen.option(image)
  } yield UserProfile.Data(UserName(username), bio, image)

  def articleData: Gen[Any, Article.Data] = for {
    title       <- title
    slug        <- slug
    description <- description
    authorId    <- authorId
    body        <- body
  } yield Article.Data(slug, title, description, authorId, body)

  def commentData: Gen[Any, Comment.Data] = for {
    authorId  <- commentAuthorId
    body      <- commentBody
    articleId <- Gen.long.map(ArticleId(_))
  } yield Comment.Data(articleId, body, authorId)
}
