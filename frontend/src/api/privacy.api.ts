import client from './client'

export const privacyApi = {
  getPrivacyPolicy() {
    return client.get('/privacy/policy')
  },

  getTerms() {
    return client.get('/privacy/terms')
  },

  getTermsStatus() {
    return client.get('/privacy/terms/status')
  },

  acceptTerms() {
    return client.post('/privacy/terms/accept')
  },

  getConsents() {
    return client.get('/privacy/consents')
  },

  updateConsent(data: { consentType: string; granted: boolean; targetUserId?: string; notes?: string }) {
    return client.put('/privacy/consents', data)
  },
}
