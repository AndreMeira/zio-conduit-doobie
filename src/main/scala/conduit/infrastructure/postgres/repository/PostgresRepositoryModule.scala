package conduit.infrastructure.postgres.repository

object PostgresRepositoryModule:
  val layer = PostgresUserRepository.layer ++ PostgresArticleRepository.layer ++ PostgresCommentRepository.layer ++ PostgresTagRepository.layer
    ++ PostgresFollowerRepository.layer ++ PostgresArticleSlugRepository.layer ++ PostgresFavoriteArticleRepository.layer
    ++ PostgresUserProfileRepository.layer ++ PostgresPermalinkRepository.layer
