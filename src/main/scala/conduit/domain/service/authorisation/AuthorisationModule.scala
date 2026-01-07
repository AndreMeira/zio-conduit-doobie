package conduit.domain.service.authorisation

import izumi.reflect.Tag

object AuthorisationModule {
  def layer[Tx: Tag] =
    UserAuthorisationService.layer[Tx] ++
      ArticleAuthorisationService.layer[Tx] ++
      CommentAuthorisationService.layer[Tx]
}
