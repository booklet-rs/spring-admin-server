<template>
  <sba-instance-section :error="error" :loading="!hasLoaded">
    <div v-if="run" class="px-12">
      <div class="flex items-center justify-between py-4 border-b border-gray-400">
        <div>
          <h2 class="text-xl font-bold">ALIMS Import Runs</h2>
          <p class="text-sm text-gray-500 mt-1 font-mono">{{ run.runId }}</p>
        </div>
        <span class="px-3 py-1 rounded-full text-sm" :class="statusClass(run.status)">
          {{ run.status }}
        </span>
      </div>

      <div v-if="needsAttention(run.status)" class="mt-4 p-4 rounded-md bg-amber-50 border border-amber-200 text-amber-800">
        <div class="flex items-center justify-between gap-4">
          <p>This run needs operator attention. Later runs cannot advance until it is resolved.</p>
          <div class="flex shrink-0 gap-2">
            <button
              v-if="run.status === 'FAILED'"
              class="px-4 py-2 rounded-md text-white text-sm font-medium shadow-sm disabled:opacity-50"
              style="background-color: #ea580c"
              :disabled="isRetrying"
              @click="openRetryDialog"
            >
              ↻ Retry run
            </button>
            <button
              class="px-4 py-2 rounded-md bg-red-600 text-white text-sm font-medium hover:bg-red-700 disabled:opacity-50"
              :disabled="isSkipping"
              @click="openSkipDialog"
            >
              Skip run
            </button>
          </div>
        </div>
      </div>

      <section v-if="run.skippedBy || run.skipReason" class="py-4">
        <h3 class="text-lg font-bold mb-3">Skip Decision</h3>
        <div class="p-4 rounded-md bg-gray-50 border border-gray-200">
          <p class="text-sm"><span class="text-gray-500">Skipped by:</span> {{ run.skippedBy || '—' }}</p>
          <p class="text-sm mt-1"><span class="text-gray-500">Skipped at:</span> {{ formatInstant(run.skippedAt) }}</p>
          <p class="text-sm mt-1"><span class="text-gray-500">Reason:</span> {{ run.skipReason || '—' }}</p>
        </div>
      </section>

      <section v-if="run.failureCode" class="py-4">
        <h3 class="text-lg font-bold mb-3">Failure</h3>
        <div class="p-4 rounded-md bg-red-50 border border-red-200">
          <p class="font-mono text-sm text-red-900">{{ run.failureCode }}</p>
          <p class="text-sm text-red-800 mt-1">{{ run.failureSummary || '—' }}</p>
        </div>
      </section>

      <section class="py-4">
        <h3 class="text-lg font-bold mb-3">Timeline</h3>
        <div class="lg:grid lg:grid-cols-3 lg:gap-6">
          <div v-for="field in timelineFields" :key="field.label">
            <label class="text-sm text-gray-500">{{ field.label }}</label>
            <p>{{ formatInstant(field.value) }}</p>
          </div>
        </div>
      </section>

      <section class="py-4">
        <h3 class="text-lg font-bold mb-3">Counts</h3>
        <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-4">
          <div v-for="count in counts" :key="count.label" class="p-4 bg-white border border-gray-200 rounded-lg text-center">
            <p class="text-2xl font-bold">{{ count.value ?? '—' }}</p>
            <p class="text-xs text-gray-500 mt-1">{{ count.label }}</p>
          </div>
        </div>
      </section>

      <section class="py-4">
        <h3 class="text-lg font-bold mb-3">Hashes</h3>
        <div class="lg:grid lg:grid-cols-3 lg:gap-6">
          <div>
            <label class="text-sm text-gray-500">Baseline hash</label>
            <p class="font-mono text-sm break-all" :title="run.baselineHash">{{ shortHash(run.baselineHash) }}</p>
          </div>
          <div>
            <label class="text-sm text-gray-500">Diff hash</label>
            <p class="font-mono text-sm break-all" :title="run.diffHash">{{ shortHash(run.diffHash) }}</p>
          </div>
          <div>
            <label class="text-sm text-gray-500">Baseline run</label>
            <p class="font-mono text-sm break-all">{{ run.baselineImportRunId || '—' }}</p>
          </div>
        </div>
      </section>

      <section class="py-6">
        <router-link
          v-if="run.diffHash"
          :to="{ name: 'instances/alims-import-run', params: { runId: run.runId } }"
          class="inline-flex items-center gap-2 px-4 py-2 rounded-md bg-blue-600 text-white text-sm font-medium hover:bg-blue-700"
        >
          View diffs
        </router-link>
        <p v-else class="text-sm text-gray-500">
          No diff was generated for this run.
        </p>
      </section>
    </div>

    <div v-else-if="hasLoaded" class="py-12 text-center text-gray-500">
      No ALIMS import runs exist yet. The nightly import has not captured a snapshot.
    </div>

    <div v-if="showRetryDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @keydown.esc="closeRetryDialog">
      <div class="bg-white rounded-lg shadow-xl max-w-lg w-full mx-4 p-6">
        <h3 class="text-lg font-bold">Retry failed ALIMS import run</h3>
        <p class="text-sm text-gray-600 mt-2">
          The retry replays the exact saved ALIMS response with the currently deployed parser. It
          never fetches new source data and changes no serving catalog until the run passes review
          and is published.
        </p>
        <p class="text-sm font-mono mt-3 bg-gray-100 rounded p-2 break-all">{{ run?.runId }}</p>
        <p v-if="retryError" class="text-sm text-red-600 mt-2">{{ retryError }}</p>
        <div class="flex justify-end gap-2 mt-4">
          <button
            class="px-4 py-2 rounded-md border border-gray-300 text-sm font-medium hover:bg-gray-50"
            @click="closeRetryDialog"
          >
            Cancel
          </button>
          <button
            class="px-4 py-2 rounded-md text-white text-sm font-medium disabled:opacity-50"
            style="background-color: #ea580c"
            :disabled="isRetrying"
            @click="confirmRetry"
          >
            {{ isRetrying ? 'Retrying…' : 'Confirm retry' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showSkipDialog" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @keydown.esc="closeSkipDialog">
      <div class="bg-white rounded-lg shadow-xl max-w-lg w-full mx-4 p-6">
        <h3 class="text-lg font-bold">Skip ALIMS import run</h3>
        <p class="text-sm text-gray-600 mt-2">
          You are skipping the oldest unresolved run. This preserves all evidence, changes no serving
          product, and allows the next run to advance. A skipped run is terminal and can never be
          published.
        </p>
        <p class="text-sm font-mono mt-3 bg-gray-100 rounded p-2 break-all">{{ run?.runId }}</p>
        <label class="block text-sm font-medium text-gray-700 mt-4">Reason (required)</label>
        <textarea
          ref="skipReasonInput"
          v-model="skipReason"
          class="mt-1 w-full h-24 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-red-500"
          placeholder="Why is this run being skipped?"
        ></textarea>
        <p v-if="skipError" class="text-sm text-red-600 mt-2">{{ skipError }}</p>
        <div class="flex justify-end gap-2 mt-4">
          <button
            class="px-4 py-2 rounded-md border border-gray-300 text-sm font-medium hover:bg-gray-50"
            @click="closeSkipDialog"
          >
            Cancel
          </button>
          <button
            class="px-4 py-2 rounded-md bg-red-600 text-white text-sm font-medium hover:bg-red-700 disabled:opacity-50"
            :disabled="!skipReason.trim() || isSkipping"
            @click="confirmSkip"
          >
            {{ isSkipping ? 'Skipping…' : 'Confirm skip' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="successMessage" class="fixed bottom-4 right-4 z-50 px-4 py-3 rounded-md bg-green-600 text-white text-sm shadow-lg">
      {{ successMessage }}
    </div>
  </sba-instance-section>
</template>

<script>
const ATTENTION_STATUSES = ['FETCHED', 'READY_FOR_VALIDATION', 'STAGED', 'REVIEW_REQUIRED', 'FAILED']

const STATUS_CLASSES = {
  FETCHED: 'bg-gray-100 text-gray-800',
  READY_FOR_VALIDATION: 'bg-blue-100 text-blue-800',
  STAGED: 'bg-blue-100 text-blue-800',
  REVIEW_REQUIRED: 'bg-amber-100 text-amber-800',
  UNCHANGED: 'bg-green-100 text-green-800',
  PUBLISHED: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-800',
  SKIPPED: 'bg-gray-200 text-gray-700'
}

export default {
  props: {
    instance: { type: Object, required: true }
  },
  data: () => ({
    hasLoaded: false,
    error: null,
    run: null,
    showSkipDialog: false,
    skipReason: '',
    skipError: null,
    isSkipping: false,
    showRetryDialog: false,
    retryError: null,
    isRetrying: false,
    successMessage: null
  }),
  computed: {
    timelineFields() {
      return [
        { label: 'Fetched', value: this.run?.fetchedAt },
        { label: 'Processed', value: this.run?.processedAt },
        { label: 'Staged', value: this.run?.stagedAt },
        { label: 'Diff generated', value: this.run?.diffGeneratedAt },
        { label: 'Failed', value: this.run?.failedAt }
      ]
    },
    counts() {
      return [
        { label: 'Raw rows', value: this.run?.rawRowCount },
        { label: 'Normalized rows', value: this.run?.normalizedRowCount },
        { label: 'Added', value: this.run?.addedRowCount },
        { label: 'Metadata changed', value: this.run?.metadataChangedRowCount },
        { label: 'JKL changed', value: this.run?.jklChangedRowCount },
        { label: 'Unchanged', value: this.run?.unchangedRowCount },
        { label: 'Missing from source', value: this.run?.missingFromSourceRowCount }
      ]
    }
  },
  async created() {
    await this.fetchRun()
  },
  methods: {
    async fetchRun() {
      this.hasLoaded = false
      this.error = null
      try {
        const response = await this.instance.axios.get('actuator/alims-import-runs')
        this.run = response.data
      } catch (error) {
        this.error = error
      } finally {
        this.hasLoaded = true
      }
    },
    openSkipDialog() {
      this.skipReason = ''
      this.skipError = null
      this.showSkipDialog = true
      this.$nextTick(() => this.$refs.skipReasonInput?.focus())
    },
    closeSkipDialog() {
      this.showSkipDialog = false
      this.skipError = null
    },
    async confirmSkip() {
      this.isSkipping = true
      this.skipError = null
      try {
        await this.instance.axios.post(
          `actuator/alims-import-runs/${this.run.runId}/skip`,
          null,
          { params: { confirmed: true, reason: this.skipReason.trim() } }
        )
        this.showSkipDialog = false
        this.successMessage = 'Run skipped. Showing the next unresolved run.'
        setTimeout(() => { this.successMessage = null }, 5000)
        await this.fetchRun()
      } catch (error) {
        this.skipError = error.response?.data?.message || 'The skip command was refused.'
      } finally {
        this.isSkipping = false
      }
    },
    openRetryDialog() {
      this.retryError = null
      this.showRetryDialog = true
    },
    closeRetryDialog() {
      this.showRetryDialog = false
      this.retryError = null
    },
    async confirmRetry() {
      this.isRetrying = true
      this.retryError = null
      try {
        const response = await this.instance.axios.post(
          `actuator/alims-import-runs/${this.run.runId}/retry`,
          null,
          { params: { confirmed: true } }
        )
        this.run = response.data
        this.showRetryDialog = false
        this.successMessage = `Retry finished. Run is now ${response.data.status}.`
        setTimeout(() => { this.successMessage = null }, 5000)
      } catch (error) {
        this.retryError = error.response?.data?.message || 'The retry command was refused.'
      } finally {
        this.isRetrying = false
      }
    },
    needsAttention(status) {
      return ATTENTION_STATUSES.includes(status)
    },
    statusClass(status) {
      return STATUS_CLASSES[status] || 'bg-gray-100 text-gray-800'
    },
    formatInstant(value) {
      return value ? new Date(value).toLocaleString() : '—'
    },
    shortHash(hash) {
      return hash ? hash.slice(0, 16) + '…' : '—'
    }
  }
}
</script>
