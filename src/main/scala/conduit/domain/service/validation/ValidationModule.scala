package conduit.domain.service.validation

import izumi.reflect.Tag

object ValidationModule {
  def layer[Tx: Tag] = UserValidationService.layer[Tx] ++ ArticleValidationService.layer[Tx] ++ CommentValidationService.layer[Tx]
}
