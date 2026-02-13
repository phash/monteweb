import { ref } from 'vue'

export function useConfirmDialog() {
  const visible = ref(false)
  const header = ref('')
  const message = ref('')

  let _resolve: ((value: boolean) => void) | null = null

  function confirm(opts: { header: string; message: string }): Promise<boolean> {
    header.value = opts.header
    message.value = opts.message
    visible.value = true
    return new Promise<boolean>((resolve) => {
      _resolve = resolve
    })
  }

  function onConfirm() {
    visible.value = false
    _resolve?.(true)
    _resolve = null
  }

  function onCancel() {
    visible.value = false
    _resolve?.(false)
    _resolve = null
  }

  return { visible, header, message, confirm, onConfirm, onCancel }
}
