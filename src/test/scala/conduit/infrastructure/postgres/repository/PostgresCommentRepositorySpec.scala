package conduit.infrastructure.postgres.repository

import conduit.domain.model.entity.Generator
import conduit.domain.model.error.ApplicationError
import conduit.domain.model.types.article.AuthorId
import conduit.domain.model.types.comment.CommentAuthorId
import conduit.infrastructure.inmemory.monitor.NoopMonitor
import conduit.infrastructure.postgres.PostgresTestAspect.{ transaction, withMigration }
import conduit.infrastructure.postgres.{ PostgresMigration, PostgresSpecLayers, PostgresUnitOfWork }
import zio.test.{ TestAspect, ZIOSpec, assertTrue }

object PostgresCommentRepositorySpec extends ZIOSpec[PostgresUnitOfWork & PostgresMigration] {
  override val bootstrap = PostgresSpecLayers.layer
  val users              = PostgresUserRepository(new NoopMonitor)
  val profiles           = PostgresUserProfileRepository(new NoopMonitor)
  val articles           = PostgresArticleRepository(new NoopMonitor)
  val comments           = PostgresCommentRepository(new NoopMonitor)

  case object ArticleNotCreated extends ApplicationError:
    def message = "Article was not created"

  def spec = suiteAll("PostgresCommentRepository") {
    test("Adds a comment and retrieves it") {
      transaction:
        for {
          creds   <- Generator.credentials.runHead.map(_.get)
          profile <- Generator.profileData.runHead.map(_.get)
          article <- Generator.articleData.runHead.map(_.get)
          userId  <- users.save(creds)
          _       <- profiles.create(userId, profile)
          created <- articles.save(article.copy(author = AuthorId(userId))).someOrFail(ArticleNotCreated)
          comment <- Generator.commentData.runHead.map(_.get)
          created <- comments.save(comment.copy(article = created.id, author = CommentAuthorId.fromLong(userId)))
          fetched <- comments.find(created.id)
        } yield assertTrue(fetched.contains(created))
    } @@ withMigration

    test("Deletes a comment") {
      transaction:
        for {
          creds   <- Generator.credentials.runHead.map(_.get)
          profile <- Generator.profileData.runHead.map(_.get)
          article <- Generator.articleData.runHead.map(_.get)
          userId  <- users.save(creds)
          _       <- profiles.create(userId, profile)
          created <- articles.save(article.copy(author = AuthorId(userId))).someOrFail(ArticleNotCreated)
          comment <- Generator.commentData.runHead.map(_.get)
          created <- comments.save(comment.copy(article = created.id, author = CommentAuthorId.fromLong(userId)))
          deleted <- comments.delete(created.id)
          fetched <- comments.find(created.id)
        } yield assertTrue(deleted.contains(created)) && assertTrue(fetched.isEmpty)
    } @@ withMigration

    test("Checks existence of a comment by id and author") {
      transaction:
        for {
          creds   <- Generator.credentials.runHead.map(_.get)
          profile <- Generator.profileData.runHead.map(_.get)
          article <- Generator.articleData.runHead.map(_.get)
          userId  <- users.save(creds)
          _       <- profiles.create(userId, profile)
          created <- articles.save(article.copy(author = AuthorId(userId))).someOrFail(ArticleNotCreated)
          comment <- Generator.commentData.runHead.map(_.get)
          created <- comments.save(comment.copy(article = created.id, author = CommentAuthorId.fromLong(userId)))
          exists1 <- comments.exists(created.id, created.data.author)
          exists2 <- comments.exists(created.id, CommentAuthorId.fromLong(created.data.author + 1))
        } yield assertTrue(exists1) && assertTrue(!exists2)
    } @@ withMigration

    test("Finds comments by article id") {
      transaction:
        for {
          creds   <- Generator.credentials.runHead.map(_.get)
          profile <- Generator.profileData.runHead.map(_.get)
          article <- Generator.articleData.runHead.map(_.get)
          userId  <- users.save(creds)
          _       <- profiles.create(userId, profile)
          created <- articles.save(article.copy(author = AuthorId(userId))).someOrFail(ArticleNotCreated)

          comment1 <- Generator.commentData.runHead.map(_.get)
          comment2 <- Generator.commentData.runHead.map(_.get)
          comment3 <- Generator.commentData.runHead.map(_.get)
          created1 <- comments.save(comment1.copy(article = created.id, author = CommentAuthorId.fromLong(userId)))
          created2 <- comments.save(comment2.copy(article = created.id, author = CommentAuthorId.fromLong(userId)))
          created3 <- comments.save(comment3.copy(article = created.id, author = CommentAuthorId.fromLong(userId)))
          fetched  <- comments.findByArticle(created.id)
        } yield assertTrue(fetched.toSet == Set(created1, created2, created3))
    } @@ withMigration
  } @@ TestAspect.sequential
}
