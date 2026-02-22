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

const PIE_COLORS = [
  '#4CAF50', '#2196F3', '#FF9800', '#E91E63',
  '#9C27B0', '#00BCD4', '#FF5722', '#607D8B'
]

function getPieSegments(data: Record<string, number>): Array<{ label: string; count: number; percentage: number; color: string; startAngle: number; endAngle: number }> {
  const total = Object.values(data).reduce((a, b) => a + b, 0)
  if (total === 0) return []

  const entries = Object.entries(data)
  let currentAngle = 0
  return entries.map(([label, count], i) => {
    const percentage = (count / total) * 100
    const angle = (count / total) * 360
    const segment = {
      label,
      count,
      percentage,
      color: PIE_COLORS[i % PIE_COLORS.length]!,
      startAngle: currentAngle,
      endAngle: currentAngle + angle,
    }
    currentAngle += angle
    return segment
  })
}

function describeArc(cx: number, cy: number, r: number, startAngle: number, endAngle: number): string {
  const start = polarToCartesian(cx, cy, r, endAngle)
  const end = polarToCartesian(cx, cy, r, startAngle)
  const largeArcFlag = endAngle - startAngle > 180 ? 1 : 0
  return `M ${cx} ${cy} L ${start.x} ${start.y} A ${r} ${r} 0 ${largeArcFlag} 0 ${end.x} ${end.y} Z`
}

function polarToCartesian(cx: number, cy: number, r: number, angleDeg: number) {
  const angleRad = ((angleDeg - 90) * Math.PI) / 180
  return {
    x: cx + r * Math.cos(angleRad),
    y: cy + r * Math.sin(angleRad),
  }
}

function getYesNoPieData(yesCount: number, noCount: number): Record<string, number> {
  const data: Record<string, number> = {}
  if (yesCount > 0) data[t('forms.yes')] = yesCount
  if (noCount > 0) data[t('forms.no')] = noCount
  return data
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

        <!-- Pie chart for SINGLE_CHOICE -->
        <div v-if="result.type === 'SINGLE_CHOICE' && result.optionCounts" class="pie-chart-section">
          <div class="pie-chart-container">
            <svg viewBox="0 0 200 200" class="pie-chart">
              <template v-for="(seg, i) in getPieSegments(result.optionCounts)" :key="i">
                <path
                  v-if="seg.endAngle - seg.startAngle < 360"
                  :d="describeArc(100, 100, 90, seg.startAngle, seg.endAngle)"
                  :fill="seg.color"
                />
                <circle v-else cx="100" cy="100" r="90" :fill="seg.color" />
              </template>
            </svg>
          </div>
          <div class="pie-legend">
            <div v-for="(seg, i) in getPieSegments(result.optionCounts)" :key="i" class="legend-item">
              <span class="legend-color" :style="{ background: seg.color }" />
              <span class="legend-label">{{ seg.label }}</span>
              <span class="legend-count">{{ seg.count }} ({{ seg.percentage.toFixed(0) }}%)</span>
            </div>
          </div>
        </div>

        <!-- Bar chart for MULTIPLE_CHOICE -->
        <div v-if="result.type === 'MULTIPLE_CHOICE' && result.optionCounts" class="bar-chart">
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

        <!-- Pie chart for YES_NO -->
        <div v-if="result.type === 'YES_NO'" class="pie-chart-section">
          <div class="pie-chart-container">
            <svg viewBox="0 0 200 200" class="pie-chart">
              <template v-for="(seg, i) in getPieSegments(getYesNoPieData(result.yesCount, result.noCount))" :key="i">
                <path
                  v-if="seg.endAngle - seg.startAngle < 360"
                  :d="describeArc(100, 100, 90, seg.startAngle, seg.endAngle)"
                  :fill="seg.color"
                />
                <circle v-else cx="100" cy="100" r="90" :fill="seg.color" />
              </template>
            </svg>
          </div>
          <div class="pie-legend">
            <div v-for="(seg, i) in getPieSegments(getYesNoPieData(result.yesCount, result.noCount))" :key="i" class="legend-item">
              <span class="legend-color" :style="{ background: seg.color }" />
              <span class="legend-label">{{ seg.label }}</span>
              <span class="legend-count">{{ seg.count }} ({{ seg.percentage.toFixed(0) }}%)</span>
            </div>
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

.pie-chart-section {
  display: flex;
  gap: 2rem;
  align-items: center;
}

.pie-chart-container {
  width: 160px;
  height: 160px;
  flex-shrink: 0;
}

.pie-chart {
  width: 100%;
  height: 100%;
}

.pie-legend {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: var(--mw-font-size-sm);
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  flex-shrink: 0;
}

.legend-label {
  flex: 1;
}

.legend-count {
  font-weight: 600;
  white-space: nowrap;
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

@media (max-width: 767px) {
  .bar-label {
    min-width: 60px;
  }

  .rating-result {
    flex-direction: column;
  }

  .pie-chart-section {
    flex-direction: column;
    align-items: flex-start;
  }

  .pie-chart-container {
    width: 140px;
    height: 140px;
  }
}
</style>
