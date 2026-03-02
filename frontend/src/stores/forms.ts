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
  MyResponseInfo,
} from '@/types/forms'

export const useFormsStore = defineStore('forms', () => {
  const availableForms = ref<FormInfo[]>([])
  const myForms = ref<FormInfo[]>([])
  const currentForm = ref<FormDetailInfo | null>(null)
  const currentResults = ref<FormResultsSummary | null>(null)
  const individualResponses = ref<IndividualResponse[]>([])
  const myResponse = ref<MyResponseInfo | null>(null)
  const loading = ref(false)
  const totalAvailable = ref(0)
  const totalMine = ref(0)
  const hasMoreAvailable = ref(true)
  const hasMoreMy = ref(true)

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
      hasMoreAvailable.value = !res.data.data.last
    } catch (e) {
      console.error('Failed to fetch available forms:', e)
      throw e
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
      hasMoreMy.value = !res.data.data.last
    } catch (e) {
      console.error('Failed to fetch my forms:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchForm(id: string) {
    try {
      const res = await formsApi.getForm(id)
      currentForm.value = res.data.data
    } catch (e) {
      console.error('Failed to fetch form:', e)
      throw e
    }
  }

  async function createForm(data: CreateFormRequest) {
    try {
      const res = await formsApi.createForm(data)
      myForms.value.unshift(res.data.data.form)
      return res.data.data
    } catch (e) {
      console.error('Failed to create form:', e)
      throw e
    }
  }

  async function updateForm(id: string, data: UpdateFormRequest) {
    try {
      const res = await formsApi.updateForm(id, data)
      currentForm.value = res.data.data
      const idx = myForms.value.findIndex(f => f.id === id)
      if (idx !== -1) myForms.value[idx] = res.data.data.form
      return res.data.data
    } catch (e) {
      console.error('Failed to update form:', e)
      throw e
    }
  }

  async function deleteForm(id: string) {
    try {
      await formsApi.deleteForm(id)
      myForms.value = myForms.value.filter(f => f.id !== id)
      if (currentForm.value?.form.id === id) currentForm.value = null
    } catch (e) {
      console.error('Failed to delete form:', e)
      throw e
    }
  }

  async function publishForm(id: string) {
    try {
      const res = await formsApi.publishForm(id)
      const updated = res.data.data
      const idx = myForms.value.findIndex(f => f.id === id)
      if (idx !== -1) myForms.value[idx] = updated
      if (currentForm.value?.form.id === id) {
        currentForm.value = { ...currentForm.value, form: updated }
      }
      return updated
    } catch (e) {
      console.error('Failed to publish form:', e)
      throw e
    }
  }

  async function closeForm(id: string) {
    try {
      const res = await formsApi.closeForm(id)
      const updated = res.data.data
      const idx = myForms.value.findIndex(f => f.id === id)
      if (idx !== -1) myForms.value[idx] = updated
      if (currentForm.value?.form.id === id) {
        currentForm.value = { ...currentForm.value, form: updated }
      }
      return updated
    } catch (e) {
      console.error('Failed to close form:', e)
      throw e
    }
  }

  async function submitResponse(id: string, data: SubmitResponseRequest) {
    try {
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
    } catch (e) {
      console.error('Failed to submit response:', e)
      throw e
    }
  }

  async function fetchMyResponse(id: string) {
    try {
      const res = await formsApi.getMyResponse(id)
      myResponse.value = res.data.data
    } catch (e) {
      console.error('Failed to fetch my response:', e)
      throw e
    }
  }

  async function updateResponse(id: string, data: SubmitResponseRequest) {
    try {
      await formsApi.updateResponse(id, data)
      if (currentForm.value?.form.id === id) {
        currentForm.value = {
          ...currentForm.value,
          form: {
            ...currentForm.value.form,
            hasUserResponded: true,
          },
        }
      }
      myResponse.value = null
    } catch (e) {
      console.error('Failed to update response:', e)
      throw e
    }
  }

  async function archiveForm(id: string) {
    try {
      const res = await formsApi.archiveForm(id)
      const updated = res.data.data
      const idx = myForms.value.findIndex(f => f.id === id)
      if (idx !== -1) myForms.value[idx] = updated
      if (currentForm.value?.form.id === id) {
        currentForm.value = { ...currentForm.value, form: updated }
      }
      return updated
    } catch (e) {
      console.error('Failed to archive form:', e)
      throw e
    }
  }

  async function fetchResults(id: string) {
    try {
      const res = await formsApi.getResults(id)
      currentResults.value = res.data.data
    } catch (e) {
      console.error('Failed to fetch results:', e)
      throw e
    }
  }

  async function fetchIndividualResponses(id: string) {
    try {
      const res = await formsApi.getIndividualResponses(id)
      individualResponses.value = res.data.data
    } catch (e) {
      console.error('Failed to fetch individual responses:', e)
      throw e
    }
  }

  async function downloadCsv(id: string) {
    try {
      const res = await formsApi.exportCsv(id)
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = url
      a.download = 'form-results.csv'
      a.click()
      window.URL.revokeObjectURL(url)
    } catch (e) {
      console.error('Failed to download CSV:', e)
      throw e
    }
  }

  async function downloadPdf(id: string) {
    try {
      const res = await formsApi.exportPdf(id)
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
      const a = document.createElement('a')
      a.href = url
      a.download = 'form-results.pdf'
      a.click()
      window.URL.revokeObjectURL(url)
    } catch (e) {
      console.error('Failed to download PDF:', e)
      throw e
    }
  }

  return {
    availableForms,
    myForms,
    currentForm,
    currentResults,
    individualResponses,
    myResponse,
    loading,
    totalAvailable,
    totalMine,
    hasMoreAvailable,
    hasMoreMy,
    fetchAvailableForms,
    fetchMyForms,
    fetchForm,
    createForm,
    updateForm,
    deleteForm,
    publishForm,
    closeForm,
    submitResponse,
    fetchMyResponse,
    updateResponse,
    archiveForm,
    fetchResults,
    fetchIndividualResponses,
    downloadCsv,
    downloadPdf,
  }
})
