/**
 * Common selectors used across tests.
 * PrimeVue components render specific class names we can rely on.
 */
export const selectors = {
  // Toast messages
  toast: '.p-toast-message',
  toastSuccess: '.p-toast-message-success',
  toastError: '.p-toast-message-error',
  toastContent: '.p-toast-message-text',

  // PrimeVue components
  dialog: '.p-dialog',
  dialogHeader: '.p-dialog-title',
  dialogContent: '.p-dialog-content',
  confirmDialog: '.p-confirmdialog',

  // Buttons
  submitButton: 'button[type="submit"]',

  // Data display
  dataTable: '.p-datatable',
  dataTableRow: '.p-datatable-row-expansion, tr',

  // Forms
  inputText: '.p-inputtext',
  dropdown: '.p-select',
  checkbox: '.p-checkbox',

  // Navigation
  sidebar: '.sidebar, .layout-sidebar, nav',
  menuItem: '.menu-item, .nav-item',

  // Common
  loading: '.loading-spinner, .p-progress-spinner',
  emptyState: '.empty-state',
  card: '.card',
  pageTitle: '.page-title',
}

/**
 * Get text content matcher for PrimeVue toast.
 */
export function toastWithText(text: string) {
  return `.p-toast-message:has-text("${text}")`
}
