package conduit.domain.model.request

/**
 * A type that represents a field value that can be:
 * <ul>
 * <li>Present with a value (to be updated)</li>
 * <li>Present but empty (to be cleared)</li>
 * <li>Absent (no change)</li>
 * </ul>
 * This is useful to distinguish between null and missing fields when decoding a data structure that accepts nullable.
 * For example, in a JSON PATCH request.
 */
enum Patchable[+A]:
  case Present(value: A)
  case Empty
  case Absent

  def map[B](f: A => B): Patchable[B] = this match
    case Present(value) => Present(f(value))
    case Empty          => Empty
    case Absent         => Absent

  def option: Option[A] = this match
    case Present(value) => Some(value)
    case Empty          => None
    case Absent         => None
