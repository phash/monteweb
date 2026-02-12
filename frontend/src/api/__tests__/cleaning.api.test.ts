import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../client', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: { data: {} } }),
    post: vi.fn().mockResolvedValue({ data: { data: {} } }),
    put: vi.fn().mockResolvedValue({ data: { data: {} } }),
    delete: vi.fn().mockResolvedValue({ data: { data: null } }),
  },
}))

import client from '../client'
import {
  getUpcomingSlots,
  getMySlots,
  getSlotById,
  registerForSlot,
  unregisterFromSlot,
  checkIn,
  checkOut,
  getConfigs,
  createConfig,
  updateConfig,
  generateSlots,
  updateSlot,
  cancelSlot,
  getQrToken,
  exportQrCodesPdf,
  getDashboard,
  offerSwap,
  getSwapOffers,
} from '../cleaning.api'

describe('cleaning.api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('slot functions', () => {
    it('getUpcomingSlots should GET /cleaning/slots', async () => {
      await getUpcomingSlots()
      expect(client.get).toHaveBeenCalledWith('/cleaning/slots', { params: { page: 0, size: 20 } })
    })

    it('getUpcomingSlots should pass custom pagination', async () => {
      await getUpcomingSlots(2, 10)
      expect(client.get).toHaveBeenCalledWith('/cleaning/slots', { params: { page: 2, size: 10 } })
    })

    it('getMySlots should GET /cleaning/slots/mine', async () => {
      await getMySlots()
      expect(client.get).toHaveBeenCalledWith('/cleaning/slots/mine')
    })

    it('getSlotById should GET /cleaning/slots/{id}', async () => {
      await getSlotById('slot-1')
      expect(client.get).toHaveBeenCalledWith('/cleaning/slots/slot-1')
    })

    it('registerForSlot should POST /cleaning/slots/{id}/register', async () => {
      await registerForSlot('slot-1')
      expect(client.post).toHaveBeenCalledWith('/cleaning/slots/slot-1/register')
    })

    it('unregisterFromSlot should DELETE /cleaning/slots/{id}/register', async () => {
      await unregisterFromSlot('slot-1')
      expect(client.delete).toHaveBeenCalledWith('/cleaning/slots/slot-1/register')
    })

    it('checkIn should POST /cleaning/slots/{id}/checkin with qrToken', async () => {
      await checkIn('slot-1', 'qr-token-123')
      expect(client.post).toHaveBeenCalledWith('/cleaning/slots/slot-1/checkin', { qrToken: 'qr-token-123' })
    })

    it('checkOut should POST /cleaning/slots/{id}/checkout', async () => {
      await checkOut('slot-1')
      expect(client.post).toHaveBeenCalledWith('/cleaning/slots/slot-1/checkout')
    })
  })

  describe('swap functions', () => {
    it('offerSwap should POST /cleaning/slots/{id}/swap', async () => {
      await offerSwap('slot-1')
      expect(client.post).toHaveBeenCalledWith('/cleaning/slots/slot-1/swap')
    })

    it('getSwapOffers should GET /cleaning/slots/{id}/swaps', async () => {
      await getSwapOffers('slot-1')
      expect(client.get).toHaveBeenCalledWith('/cleaning/slots/slot-1/swaps')
    })
  })

  describe('config functions', () => {
    it('getConfigs should GET /cleaning/configs', async () => {
      await getConfigs()
      expect(client.get).toHaveBeenCalledWith('/cleaning/configs', { params: {} })
    })

    it('getConfigs should pass sectionId', async () => {
      await getConfigs('sect-1')
      expect(client.get).toHaveBeenCalledWith('/cleaning/configs', { params: { sectionId: 'sect-1' } })
    })

    it('createConfig should POST /cleaning/configs', async () => {
      const request = { name: 'Weekly Clean', sectionId: 's-1' }
      await createConfig(request as any)
      expect(client.post).toHaveBeenCalledWith('/cleaning/configs', request)
    })

    it('updateConfig should PUT /cleaning/configs/{id}', async () => {
      const request = { active: false }
      await updateConfig('cfg-1', request)
      expect(client.put).toHaveBeenCalledWith('/cleaning/configs/cfg-1', request)
    })
  })

  describe('admin functions', () => {
    it('generateSlots should POST /cleaning/configs/{id}/generate', async () => {
      const request = { from: '2026-01-01', to: '2026-03-31' }
      await generateSlots('cfg-1', request as any)
      expect(client.post).toHaveBeenCalledWith('/cleaning/configs/cfg-1/generate', request)
    })

    it('updateSlot should PUT /cleaning/slots/{id}', async () => {
      await updateSlot('slot-1', { status: 'CANCELLED' })
      expect(client.put).toHaveBeenCalledWith('/cleaning/slots/slot-1', { status: 'CANCELLED' })
    })

    it('cancelSlot should DELETE /cleaning/slots/{id}', async () => {
      await cancelSlot('slot-1')
      expect(client.delete).toHaveBeenCalledWith('/cleaning/slots/slot-1')
    })

    it('getQrToken should GET /cleaning/slots/{id}/qr', async () => {
      await getQrToken('slot-1')
      expect(client.get).toHaveBeenCalledWith('/cleaning/slots/slot-1/qr')
    })

    it('exportQrCodesPdf should GET as blob', async () => {
      await exportQrCodesPdf('cfg-1', '2026-01-01', '2026-03-31')
      expect(client.get).toHaveBeenCalledWith('/cleaning/configs/cfg-1/qr-codes', {
        params: { from: '2026-01-01', to: '2026-03-31' },
        responseType: 'blob',
      })
    })

    it('getDashboard should GET /cleaning/dashboard', async () => {
      await getDashboard('sect-1', '2026-01-01', '2026-03-31')
      expect(client.get).toHaveBeenCalledWith('/cleaning/dashboard', {
        params: { sectionId: 'sect-1', from: '2026-01-01', to: '2026-03-31' },
      })
    })
  })
})
