/**
 * Module-level cache for terms acceptance status.
 * Extracted to avoid circular dependencies between router and auth store.
 */
let termsChecked = false
let termsAcceptedCache = true

/** Mark terms as accepted in the cache (call after successful acceptance). */
export function markTermsAccepted() {
  termsChecked = true
  termsAcceptedCache = true
}

/** Reset terms cache (call on logout so the next user gets a fresh check). */
export function resetTermsCache() {
  termsChecked = false
  termsAcceptedCache = true
}

/** Check if terms status has been fetched this session. */
export function isTermsChecked() {
  return termsChecked
}

/** Get cached terms acceptance status. */
export function getTermsAccepted() {
  return termsAcceptedCache
}

/** Set terms check result (called by router guard after API fetch). */
export function setTermsStatus(checked: boolean, accepted: boolean) {
  termsChecked = checked
  termsAcceptedCache = accepted
}
