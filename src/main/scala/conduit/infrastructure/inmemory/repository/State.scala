package conduit.infrastructure.inmemory.repository

import conduit.domain.model.entity.{ Article, Comment, Credentials, UserProfile }
import conduit.domain.model.types.article.{ ArticleId, ArticleSlug, ArticleTag }
import conduit.domain.model.types.comment.CommentId
import conduit.domain.model.types.user.UserId
import zio.{ Ref, ZIO }

class State(
  val idSeq: Ref[Long],
  val articles: Ref[Map[ArticleId, Article]],
  val slugs: Ref[Map[ArticleSlug, Int]],
  val favorites: Ref[Map[ArticleId, Set[UserId]]],
  val followers: Ref[Map[UserId, Set[UserId]]],
  val comments: Ref[Map[CommentId, Comment]],
  val permalinks: Ref[Map[ArticleSlug, ArticleId]],
  val tags: Ref[Map[ArticleId, List[ArticleTag]]],
  val users: Ref[Map[UserId, Credentials.Hashed]],
  val profiles: Ref[Map[UserId, UserProfile]],
) {
  def nextId: ZIO[Any, Nothing, Long] =
    idSeq.updateAndGet(_ + 1L)

  def duplicate: ZIO[Any, Nothing, State] =
    for {
      articles   <- articles.get.flatMap(Ref.make)
      slugs      <- slugs.get.flatMap(Ref.make)
      favorites  <- favorites.get.flatMap(Ref.make)
      followers  <- followers.get.flatMap(Ref.make)
      comments   <- comments.get.flatMap(Ref.make)
      permalinks <- permalinks.get.flatMap(Ref.make)
      tags       <- tags.get.flatMap(Ref.make)
      users      <- users.get.flatMap(Ref.make)
      profiles   <- profiles.get.flatMap(Ref.make)
    } yield State(idSeq, articles, slugs, favorites, followers, comments, permalinks, tags, users, profiles)

  def merge(other: State): ZIO[Any, Nothing, Unit] =
    for {
      _ <- other.articles.get.flatMap(other => articles.update(_ ++ other))
      _ <- other.slugs.get.flatMap(other => slugs.update(_ ++ other))
      _ <- other.favorites.get.flatMap(other => favorites.update(_ ++ other))
      _ <- other.followers.get.flatMap(other => followers.update(_ ++ other))
      _ <- other.comments.get.flatMap(other => comments.update(_ ++ other))
      _ <- other.permalinks.get.flatMap(other => permalinks.update(_ ++ other))
      _ <- other.tags.get.flatMap(other => tags.update(_ ++ other))
      _ <- other.users.get.flatMap(other => users.update(_ ++ other))
      _ <- other.profiles.get.flatMap(other => profiles.update(_ ++ other))
    } yield ()
}

object State:
  def empty: ZIO[Any, Nothing, State] =
    for {
      idSeq      <- Ref.make(0L)
      articles   <- Ref.make(Map.empty[ArticleId, Article])
      slugs      <- Ref.make(Map.empty[ArticleSlug, Int])
      favorites  <- Ref.make(Map.empty[ArticleId, Set[UserId]])
      followers  <- Ref.make(Map.empty[UserId, Set[UserId]])
      comments   <- Ref.make(Map.empty[CommentId, Comment])
      permalinks <- Ref.make(Map.empty[ArticleSlug, ArticleId])
      tags       <- Ref.make(Map.empty[ArticleId, List[ArticleTag]])
      users      <- Ref.make(Map.empty[UserId, Credentials.Hashed])
      profiles   <- Ref.make(Map.empty[UserId, UserProfile])
    } yield State(idSeq, articles, slugs, favorites, followers, comments, permalinks, tags, users, profiles)
