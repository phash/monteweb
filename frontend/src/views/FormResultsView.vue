<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { useLocaleDate } from '@/composables/useLocaleDate'
import { useFormsStore } from '@/stores/forms'
import PageTitle from '@/components/common/PageTitle.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import Button from 'primevue/button'
import ProgressBar from 'primevue/progressbar'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

const { t } = useI18n()
const { formatCompactDateTime } = useLocaleDate()
const router = useRouter()
const route = useRoute()
const forms = useFormsStore()

const formId = computed(() => route.params.id as string)

const responseRate = computed(() => {
  if (!forms.currentResults) return 0
  const f = forms.currentResults.form
  if (f.targetCount === 0) return 0
  return Math.round((f.responseCount / f.targetCount) * 100)
})

onMounted(async () => {
  await forms.fetchResults(formId.value)
  if (forms.currentResults && !forms.currentResults.form.anonymous) {
    await forms.fetchIndividualResponses(formId.value)
  }
})

function maxOptionCount(optionCounts: Record<string, number>): number {
  return Math.max(...Object.values(optionCounts), 1)
}

function barWidth(count: number, max: number): string {
  return `${Math.round((count / max) * 100)}%`
}
</script>

<template>
  <div>
    <Button
      icon="pi pi-arrow-left"
      :label="t('common.back')"
      severity="secondary"
      text
      @click="router.back()"
      class="mb-1"
    />

    <LoadingSpinner v-if="!forms.currentResults" />

    <template v-else>
      <PageTitle :title="t('forms.resultsSummary') + ': ' + forms.currentResults.form.title" />

      <!-- Response rate -->
      <div class="response-rate card">
        <div class="rate-header">
          <span class="rate-label">{{ t('forms.responseRate') }}</span>
          <span class="rate-value">{{ forms.currentResults.form.responseCount }}
            <span v-if="forms.currentResults.form.targetCount > 0"> / {{ forms.currentResults.form.targetCount }}</span>
          </span>
        </div>
        <ProgressBar :value="responseRate" :showValue="true" />
      </div>

      <!-- Export buttons -->
      <div class="export-actions card">
        <Button
          :label="t('forms.exportCsv')"
          icon="pi pi-file"
          severity="secondary"
          size="small"
          @click="forms.downloadCsv(formId)"
        />
        <Button
          :label="t('forms.exportPdf')"
          icon="pi pi-file-pdf"
          severity="secondary"
          size="small"
          @click="forms.downloadPdf(formId)"
        />
      </div>

      <!-- Question results -->
      <div
        v-for="result in forms.currentResults.results"
        :key="result.questionId"
        class="result-card card"
      >
        <h2 class="result-label">{{ result.label }}</h2>
        <span class="result-total">{{ result.totalAnswers }} {{ t('forms.responsesCount') }}</span>

        <!-- Choice bar chart -->
        <div v-if="(result.type === 'SINGLE_CHOICE' || result.type === 'MULTIPLE_CHOICE') && result.optionCounts" class="bar-chart">
          <div v-for="(count, option) in result.optionCounts" :key="option" class="bar-row">
            <span class="bar-label">{{ option }}</span>
            <div class="bar-container">
              <div class="bar" :style="{ width: barWidth(count, maxOptionCount(result.optionCounts!)) }" />
            </div>
            <span class="bar-count">{{ count }}</span>
          </div>
        </div>

        <!-- Rating -->
        <div v-if="result.type === 'RATING'" class="rating-result">
          <div class="average-display">
            <span class="average-number">{{ result.averageRating?.toFixed(1) || '-' }}</span>
            <span class="average-label">{{ t('forms.average') }}</span>
          </div>
          <div v-if="result.ratingDistribution" class="rating-bars">
            <div v-for="(count, stars) in result.ratingDistribution" :key="stars" class="bar-row">
              <span class="bar-label">{{ stars }} &#9733;</span>
              <div class="bar-container">
                <div class="bar" :style="{ width: barWidth(count, Math.max(...Object.values(result.ratingDistribution!), 1)) }" />
              </div>
              <span class="bar-count">{{ count }}</span>
            </div>
          </div>
        </div>

        <!-- Yes/No -->
        <div v-if="result.type === 'YES_NO'" class="yesno-result">
          <div class="yesno-item yes">
            <span class="yesno-label">{{ t('forms.yes') }}</span>
            <span class="yesno-count">{{ result.yesCount }}</span>
          </div>
          <div class="yesno-item no">
            <span class="yesno-label">{{ t('forms.no') }}</span>
            <span class="yesno-count">{{ result.noCount }}</span>
          </div>
        </div>

        <!-- Text answers -->
        <div v-if="result.type === 'TEXT' && result.textAnswers" class="text-answers">
          <div v-for="(answer, idx) in result.textAnswers" :key="idx" class="text-answer">
            {{ answer }}
          </div>
        </div>
      </div>

      <!-- Individual responses table (non-anonymous only) -->
      <div v-if="!forms.currentResults.form.anonymous && forms.individualResponses.length" class="individual-section">
        <h2 class="section-heading">{{ t('forms.individualResponses') }}</h2>
        <DataTable :value="forms.individualResponses" stripedRows size="small">
          <Column field="userName" :header="t('forms.user')" />
          <Column :header="t('forms.submittedAt')">
            <template #body="{ data }">
              {{ formatCompactDateTime(data.submittedAt) }}
            </template>
          </Column>
          <Column v-for="result in forms.currentResults.results" :key="result.questionId" :header="result.label">
            <template #body="{ data }">
              <template v-for="answer in data.answers" :key="answer.questionId">
                <span v-if="answer.questionId === result.questionId">
                  <span v-if="answer.text">{{ answer.text }}</span>
                  <span v-else-if="answer.selectedOptions">{{ answer.selectedOptions.join(', ') }}</span>
                  <span v-else-if="answer.rating != null">{{ answer.rating }} &#9733;</span>
                </span>
              </template>
            </template>
          </Column>
        </DataTable>
      </div>
    </template>
  </div>
</template>

<style scoped>
.response-rate {
  padding: 1rem;
  margin-bottom: 1rem;
}

.rate-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.rate-label {
  font-weight: 600;
}

.rate-value {
  font-weight: 600;
  color: var(--mw-primary);
}

.export-actions {
  display: flex;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
}

.result-card {
  padding: 1rem;
  margin-bottom: 0.75rem;
}

.result-label,
.section-heading {
  margin: 0 0 0.25rem 0;
  font-size: var(--mw-font-size-md);
}

.result-total {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
  display: block;
  margin-bottom: 0.75rem;
}

.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.bar-label {
  min-width: 100px;
  font-size: var(--mw-font-size-sm);
  text-align: right;
}

.bar-container {
  flex: 1;
  background: var(--mw-bg-hover);
  height: 24px;
  border-radius: 4px;
  overflow: hidden;
}

.bar {
  background: var(--mw-primary);
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s;
  min-width: 2px;
}

.bar-count {
  min-width: 30px;
  font-weight: 600;
  font-size: var(--mw-font-size-sm);
}

.rating-result {
  display: flex;
  gap: 2rem;
  align-items: flex-start;
}

.average-display {
  text-align: center;
}

.average-number {
  display: block;
  font-size: 2rem;
  font-weight: 700;
  color: var(--mw-primary);
}

.average-label {
  font-size: var(--mw-font-size-xs);
  color: var(--mw-text-muted);
}

.rating-bars {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.yesno-result {
  display: flex;
  gap: 1rem;
}

.yesno-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1rem 2rem;
  border-radius: 8px;
}

.yesno-item.yes {
  background: var(--p-green-50);
  color: var(--p-green-700);
}

.yesno-item.no {
  background: var(--p-red-50);
  color: var(--p-red-700);
}

.yesno-count {
  font-size: 1.5rem;
  font-weight: 700;
}

.yesno-label {
  font-weight: 600;
}

.text-answers {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.text-answer {
  padding: 0.5rem 0.75rem;
  background: var(--mw-bg-hover);
  border-radius: 6px;
  font-size: var(--mw-font-size-sm);
}

.individual-section {
  margin-top: 1.5rem;
}

.individual-section h3 {
  margin-bottom: 0.75rem;
}

@media (max-width: 600px) {
  .bar-label {
    min-width: 60px;
  }

  .rating-result {
    flex-direction: column;
  }
}
</style>
