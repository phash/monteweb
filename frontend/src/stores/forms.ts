import { defineStore } from 'pinia'
import { ref } from 'vue'
import { formsApi } from '@/api/forms.api'
import type {
  FormInfo,
  FormDetailInfo,
  CreateFormRequest,
  UpdateFormRequest,
  SubmitResponseRequest,
  FormResultsSummary,
  IndividualResponse,
} from '@/types/forms'

export const useFormsStore = defineStore('forms', () => {
  const availableForms = ref<FormInfo[]>([])
  const myForms = ref<FormInfo[]>([])
  const currentForm = ref<FormDetailInfo | null>(null)
  const currentResults = ref<FormResultsSummary | null>(null)
  const individualResponses = ref<IndividualResponse[]>([])
  const loading = ref(false)
  const totalAvailable = ref(0)
  const totalMine = ref(0)
  const hasMore = ref(true)

  async function fetchAvailableForms(page = 0, reset = true) {
    loading.value = true
    try {
      const res = await formsApi.getAvailableForms(page)
      if (reset) {
        availableForms.value = res.data.data.content
      } else {
        availableForms.value = [...availableForms.value, ...res.data.data.content]
      }
      totalAvailable.value = res.data.data.totalElements
      hasMore.value = !res.data.data.last
    } finally {
      loading.value = false
    }
  }

  async function fetchMyForms(page = 0, reset = true) {
    loading.value = true
    try {
      const res = await formsApi.getMyForms(page)
      if (reset) {
        myForms.value = res.data.data.content
      } else {
        myForms.value = [...myForms.value, ...res.data.data.content]
      }
      totalMine.value = res.data.data.totalElements
      hasMore.value = !res.data.data.last
    } finally {
      loading.value = false
    }
  }

  async function fetchForm(id: string) {
    const res = await formsApi.getForm(id)
    currentForm.value = res.data.data
  }

  async function createForm(data: CreateFormRequest) {
    const res = await formsApi.createForm(data)
    myForms.value.unshift(res.data.data.form)
    return res.data.data
  }

  async function updateForm(id: string, data: UpdateFormRequest) {
    const res = await formsApi.updateForm(id, data)
    currentForm.value = res.data.data
    const idx = myForms.value.findIndex(f => f.id === id)
    if (idx !== -1) myForms.value[idx] = res.data.data.form
    return res.data.data
  }

  async function deleteForm(id: string) {
    await formsApi.deleteForm(id)
    myForms.value = myForms.value.filter(f => f.id !== id)
    if (currentForm.value?.form.id === id) currentForm.value = null
  }

  async function publishForm(id: string) {
    const res = await formsApi.publishForm(id)
    const updated = res.data.data
    const idx = myForms.value.findIndex(f => f.id === id)
    if (idx !== -1) myForms.value[idx] = updated
    if (currentForm.value?.form.id === id) {
      currentForm.value = { ...currentForm.value, form: updated }
    }
    return updated
  }

  async function closeForm(id: string) {
    const res = await formsApi.closeForm(id)
    const updated = res.data.data
    const idx = myForms.value.findIndex(f => f.id === id)
    if (idx !== -1) myForms.value[idx] = updated
    if (currentForm.value?.form.id === id) {
      currentForm.value = { ...currentForm.value, form: updated }
    }
    return updated
  }

  async function submitResponse(id: string, data: SubmitResponseRequest) {
    await formsApi.submitResponse(id, data)
    // Update the responded flag in available list
    const idx = availableForms.value.findIndex(f => f.id === id)
    if (idx !== -1) {
      const existing = availableForms.value[idx]!
      availableForms.value[idx] = {
        ...existing,
        hasUserResponded: true,
        responseCount: existing.responseCount + 1,
      }
    }
    if (currentForm.value?.form.id === id) {
      currentForm.value = {
        ...currentForm.value,
        form: {
          ...currentForm.value.form,
          hasUserResponded: true,
          responseCount: currentForm.value.form.responseCount + 1,
        },
      }
    }
  }

  async function fetchResults(id: string) {
    const res = await formsApi.getResults(id)
    currentResults.value = res.data.data
  }

  async function fetchIndividualResponses(id: string) {
    const res = await formsApi.getIndividualResponses(id)
    individualResponses.value = res.data.data
  }

  async function downloadCsv(id: string) {
    const res = await formsApi.exportCsv(id)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = 'form-results.csv'
    a.click()
    window.URL.revokeObjectURL(url)
  }

  async function downloadPdf(id: string) {
    const res = await formsApi.exportPdf(id)
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
    const a = document.createElement('a')
    a.href = url
    a.download = 'form-results.pdf'
    a.click()
    window.URL.revokeObjectURL(url)
  }

  return {
    availableForms,
    myForms,
    currentForm,
    currentResults,
    individualResponses,
    loading,
    totalAvailable,
    totalMine,
    hasMore,
    fetchAvailableForms,
    fetchMyForms,
    fetchForm,
    createForm,
    updateForm,
    deleteForm,
    publishForm,
    closeForm,
    submitResponse,
    fetchResults,
    fetchIndividualResponses,
    downloadCsv,
    downloadPdf,
  }
})
