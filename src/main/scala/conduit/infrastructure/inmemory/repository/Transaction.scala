package conduit.infrastructure.inmemory.repository

import zio.ZIO

class Transaction(val state: State)

object Transaction:
  def execute[A](logic: State => ZIO[Transaction, Nothing, A]): ZIO[Transaction, Nothing, A] =
    ZIO.serviceWithZIO[Transaction](transaction => logic(transaction.state))
