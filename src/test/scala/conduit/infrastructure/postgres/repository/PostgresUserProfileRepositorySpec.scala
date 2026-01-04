package conduit.infrastructure.postgres.repository

import conduit.domain.model.entity.{ Generator, UserProfile }
import conduit.domain.model.types.article.{ ArticleId, AuthorId }
import conduit.domain.model.types.user.{ UserId, UserName }
import conduit.infrastructure.inmemory.monitor.NoopMonitor
import conduit.infrastructure.postgres.PostgresTestAspect.{ transaction, withMigration }
import conduit.infrastructure.postgres.{ PostgresMigration, PostgresSpecLayers, PostgresUnitOfWork }
import zio.ZIO
import zio.test.{ TestAspect, ZIOSpec, assertTrue }

object PostgresUserProfileRepositorySpec extends ZIOSpec[PostgresUnitOfWork & PostgresMigration] {
  override val bootstrap = PostgresSpecLayers.layer
  val users              = PostgresUserRepository(new NoopMonitor)
  val profiles           = PostgresUserProfileRepository(new NoopMonitor)
  val articles           = PostgresArticleRepository(new NoopMonitor)

  def spec = suiteAll("PostgresUserProfileRepository") {

    test("Return None when retrieving a non-existing profile") {
      transaction:
        for {
          creds  <- Generator.credentials.runHead.map(_.get)
          userId <- users.save(creds)
          found  <- profiles.findById(UserId(userId + 1))
        } yield assertTrue(found.isEmpty)
    } @@ withMigration

    test("Insert a profile and retrieve it") {
      transaction:
        for {
          creds   <- Generator.credentials.runHead.map(_.get)
          profile <- Generator.profileData.runHead.map(_.get)
          userId  <- users.save(creds)
          _       <- profiles.create(userId, profile)
          found   <- profiles.findById(userId)
        } yield assertTrue(found.map(_.data).contains(profile))
    } @@ withMigration

    test("Insert a profile and check existence by user name") {
      transaction:
        for {
          creds     <- Generator.credentials.runHead.map(_.get)
          profile   <- Generator.profileData.runHead.map(_.get)
          other     <- Generator.userName.filter(_ != profile.name).runHead.map(_.get)
          userId    <- users.save(creds)
          _         <- profiles.create(userId, profile)
          exists    <- profiles.exists(profile.name)
          notExists <- profiles.exists(other)
        } yield assertTrue(exists && !notExists)
    } @@ withMigration

    test("Insert a profile and check existence by user id and user name") {
      transaction:
        for {
          creds      <- Generator.credentials.runHead.map(_.get)
          profile    <- Generator.profileData.runHead.map(_.get)
          othername  <- Generator.userName.filter(_ != profile.name).runHead.map(_.get)
          userId     <- users.save(creds)
          _          <- profiles.create(userId, profile)
          exists     <- profiles.exists(userId, profile.name)
          notExists1 <- profiles.exists(userId, othername)
          notExists2 <- profiles.exists(UserId(userId + 1), profile.name)
        } yield assertTrue(exists && !notExists1 && !notExists2)
    } @@ withMigration

    test("Insert a profile and retrieve it by user name") {
      transaction:
        for {
          creds    <- Generator.credentials.runHead.map(_.get)
          profile  <- Generator.profileData.runHead.map(_.get)
          userId   <- users.save(creds)
          _        <- profiles.create(userId, profile)
          found    <- profiles.findByUserName(profile.name)
          notFound <- profiles.findByUserName(UserName(profile.name + "1"))
        } yield assertTrue(found.map(_.data).contains(profile)) &&
        assertTrue(notFound.isEmpty)
    } @@ withMigration

    test("Insert a profile and retrieve it by user name") {
      transaction:
        for {
          creds    <- Generator.credentials.runHead.map(_.get)
          profile  <- Generator.profileData.runHead.map(_.get)
          userId   <- users.save(creds)
          _        <- profiles.create(userId, profile)
          found    <- profiles.findByUserName(profile.name)
          notFound <- profiles.findByUserName(UserName(profile.name + "1"))
        } yield assertTrue(found.map(_.data).contains(profile)) &&
        assertTrue(notFound.isEmpty)
    } @@ withMigration

    test("Insert multiple profiles and retrieve them by user IDs") {
      transaction:
        for {
          sample        <- Generator.credentials.zip(Generator.profileData).runCollectN(10)
          (creds, profs) = sample.distinctBy(_._1.email).distinctBy(_._2.name).unzip
          userIds       <- ZIO.foreach(creds)(users.save)
          _             <- ZIO.foreach(userIds.zip(profs))(profiles.create)
          found         <- profiles.findByIds(userIds)
        } yield assertTrue(userIds.size == creds.size) &&
        assertTrue(found.map(_.data).toSet == profs.toSet)
    } @@ withMigration

    test("Insert a profile and update it") {
      transaction:
        for {
          creds   <- Generator.credentials.runHead.map(_.get)
          profile <- Generator.profileData.runHead.map(_.get)
          updated <- Generator.profileData.filter(_.name != profile.name).runHead.map(_.get)
          userId  <- users.save(creds)
          created <- profiles.create(userId, profile)
          _       <- profiles.save(created.copy(data = updated))
          found   <- profiles.findById(userId)
        } yield assertTrue(found.map(_.data).contains(updated))
    } @@ withMigration

    test("Insert a article and find the author's profile") {
      transaction:
        for {
          creds    <- Generator.credentials.runHead.map(_.get)
          profile  <- Generator.profileData.runHead.map(_.get)
          article  <- Generator.articleData.runHead.map(_.get)
          userId   <- users.save(creds)
          _        <- profiles.create(userId, profile)
          article  <- articles.save(article.copy(author = AuthorId(userId)))
          found    <- ZIO.foreach(article)(article => profiles.findAuthorOf(article.id)).someOrElse(None)
          notFound <- ZIO.foreach(article)(article => profiles.findAuthorOf(ArticleId(article.id + 1))).someOrElse(None)
        } yield assertTrue(found.map(_.data).contains(profile))
        && assertTrue(notFound.isEmpty)
    } @@ withMigration
  } @@ TestAspect.sequential
}
