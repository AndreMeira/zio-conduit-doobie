package conduit.infrastructure.inmemory.repository

import conduit.domain.logic.persistence.*

object InMemoryRepositoryModule {

  val layer =
    InMemoryUnitOfWork.layer ++
      InMemoryUserRepository.layer ++
      InMemoryTagRepository.layer ++
      InMemoryCommentRepository.layer ++
      InMemoryArticleRepository.layer ++
      InMemoryFollowerRepository.layer ++
      InMemoryPermalinkRepository.layer ++
      InMemoryUserProfileRepository.layer ++
      InMemoryArticleSlugRepository.layer ++
      InMemoryFavoriteArticleRepository.layer
}
