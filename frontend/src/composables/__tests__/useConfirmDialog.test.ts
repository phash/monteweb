import { describe, it, expect } from 'vitest'
import { useConfirmDialog } from '../useConfirmDialog'

describe('useConfirmDialog', () => {
  it('should initialize with visible = false', () => {
    const { visible } = useConfirmDialog()
    expect(visible.value).toBe(false)
  })

  it('should initialize with empty header and message', () => {
    const { header, message } = useConfirmDialog()
    expect(header.value).toBe('')
    expect(message.value).toBe('')
  })

  it('should set dialog state when confirm is called', () => {
    const { visible, header, message, confirm } = useConfirmDialog()
    confirm({ header: 'Delete Item?', message: 'This action cannot be undone.' })
    expect(visible.value).toBe(true)
    expect(header.value).toBe('Delete Item?')
    expect(message.value).toBe('This action cannot be undone.')
  })

  it('should resolve with true when onConfirm is called', async () => {
    const { confirm, onConfirm } = useConfirmDialog()
    const promise = confirm({ header: 'Test', message: 'Sure?' })
    onConfirm()
    const result = await promise
    expect(result).toBe(true)
  })

  it('should resolve with false when onCancel is called', async () => {
    const { confirm, onCancel } = useConfirmDialog()
    const promise = confirm({ header: 'Test', message: 'Sure?' })
    onCancel()
    const result = await promise
    expect(result).toBe(false)
  })

  it('should set visible to false after onConfirm', async () => {
    const { visible, confirm, onConfirm } = useConfirmDialog()
    confirm({ header: 'Test', message: 'Sure?' })
    expect(visible.value).toBe(true)
    onConfirm()
    expect(visible.value).toBe(false)
  })

  it('should set visible to false after onCancel', async () => {
    const { visible, confirm, onCancel } = useConfirmDialog()
    confirm({ header: 'Test', message: 'Sure?' })
    expect(visible.value).toBe(true)
    onCancel()
    expect(visible.value).toBe(false)
  })

  it('should handle multiple confirm/cancel cycles', async () => {
    const { confirm, onConfirm, onCancel } = useConfirmDialog()

    const p1 = confirm({ header: 'First', message: 'One' })
    onConfirm()
    expect(await p1).toBe(true)

    const p2 = confirm({ header: 'Second', message: 'Two' })
    onCancel()
    expect(await p2).toBe(false)

    const p3 = confirm({ header: 'Third', message: 'Three' })
    onConfirm()
    expect(await p3).toBe(true)
  })

  it('should not throw when onConfirm is called without pending confirm', () => {
    const { onConfirm } = useConfirmDialog()
    expect(() => onConfirm()).not.toThrow()
  })

  it('should not throw when onCancel is called without pending confirm', () => {
    const { onCancel } = useConfirmDialog()
    expect(() => onCancel()).not.toThrow()
  })

  it('should update header and message on subsequent confirm calls', () => {
    const { header, message, confirm, onConfirm } = useConfirmDialog()

    confirm({ header: 'First Header', message: 'First Message' })
    expect(header.value).toBe('First Header')
    expect(message.value).toBe('First Message')
    onConfirm()

    confirm({ header: 'Second Header', message: 'Second Message' })
    expect(header.value).toBe('Second Header')
    expect(message.value).toBe('Second Message')
  })
})
