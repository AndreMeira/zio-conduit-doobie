package conduit.domain.model.request

enum Patchable[+A]:
  case Present(value: A)
  case Empty
  case Absent

  def map[B](f: A => B): Patchable[B] = this match
    case Present(value) => Present(f(value))
    case Empty          => Empty
    case Absent         => Absent

  def asOption: Option[A] = this match
    case Present(value) => Some(value)
    case Empty          => None
    case Absent         => None