package conduit.domain.service.entrypoint

import izumi.reflect.Tag

object EntrypointModule {
  def layer[Tx: Tag] =
    ArticleEntrypointService.layer[Tx] ++
      CommentEntrypointService.layer[Tx] ++
      UserEntrypointService.layer[Tx]
}
