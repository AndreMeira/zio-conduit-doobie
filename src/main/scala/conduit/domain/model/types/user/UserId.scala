package conduit.domain.model.types.user

import zio.prelude.Subtype

/**
 * Type-safe wrapper for user identifiers.
 *
 * Uses ZIO Prelude's Subtype to provide compile-time safety around user IDs,
 * preventing accidental mixing of user IDs with other Long values like article IDs.
 */
type UserId = UserId.Type

/**
 * Companion object providing construction and validation for UserId.
 */
object UserId extends Subtype[Long]
