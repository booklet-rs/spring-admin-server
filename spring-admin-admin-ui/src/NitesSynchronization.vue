<template>
  <sba-instance-section :error="null" :loading="false">
    <div class="px-6 py-4 md:px-12">
      <div class="border-b border-gray-400 pb-4">
        <h2 class="text-xl font-bold">NITES Synchronization</h2>
        <p class="mt-1 text-sm text-gray-500">
          Fetch and apply the current NITES drug codebook without waiting for the nightly schedule.
        </p>
      </div>

      <div class="mt-6 rounded-md border border-amber-200 bg-amber-50 p-4 text-amber-900">
        <h3 class="font-bold">Before you run synchronization</h3>
        <ul class="mt-2 list-disc space-y-1 pl-5 text-sm">
          <li>Connect at least one doctor and verify that the credentials are accepted.</li>
          <li>Enable the NITES synchronization feature flag.</li>
          <li>Disable the flag again if this run fails.</li>
        </ul>
        <p class="mt-3 text-sm">
          This page does not enable the feature flag. An enabled flag also permits scheduled runs.
        </p>
      </div>

      <section class="py-6">
        <button
          class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 disabled:opacity-50"
          :disabled="isSynchronizing"
          @click="openConfirmation"
        >
          {{ isSynchronizing ? 'Synchronizing...' : 'Run synchronization now' }}
        </button>
        <p v-if="requestError" class="mt-3 text-sm text-red-600">{{ requestError }}</p>
      </section>

      <section v-if="result" class="border-t border-gray-200 py-6">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <h3 class="text-lg font-bold">Latest requested run</h3>
          <span class="rounded-full px-3 py-1 text-sm" :class="outcomeClass(result.outcome)">
            {{ result.outcome }}
          </span>
        </div>

        <p class="mt-3 text-sm" :class="outcomeMessageClass(result.outcome)">
          {{ outcomeMessage(result.outcome) }}
        </p>

        <div v-if="result.runId" class="mt-5 grid gap-4 md:grid-cols-3">
          <div class="rounded-lg border border-gray-200 bg-white p-4">
            <p class="text-xs text-gray-500">Run ID</p>
            <p class="mt-1 break-all font-mono text-sm">{{ result.runId }}</p>
          </div>
          <div class="rounded-lg border border-gray-200 bg-white p-4 text-center">
            <p class="text-2xl font-bold">{{ result.rawRowCount ?? '-' }}</p>
            <p class="mt-1 text-xs text-gray-500">Raw rows</p>
          </div>
          <div class="rounded-lg border border-gray-200 bg-white p-4 text-center">
            <p class="text-2xl font-bold">{{ result.drugCodeCount ?? '-' }}</p>
            <p class="mt-1 text-xs text-gray-500">Unique drug codes</p>
          </div>
        </div>

        <div v-if="result.failureSummary" class="mt-4 rounded-md border border-red-200 bg-red-50 p-4">
          <p class="text-sm font-medium text-red-900">Failure</p>
          <p class="mt-1 break-words font-mono text-sm text-red-800">{{ result.failureSummary }}</p>
        </div>
      </section>
    </div>

    <div
      v-if="showConfirmation"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      @keydown.esc="closeConfirmation"
    >
      <div class="mx-4 w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
        <h3 class="text-lg font-bold">Run NITES synchronization now?</h3>
        <p class="mt-2 text-sm text-gray-600">
          Booklet will authenticate with a connected doctor, fetch the complete NITES drug codebook,
          validate it, and save a new synchronization run.
        </p>
        <p class="mt-3 text-sm font-medium text-amber-800">
          Continue only after the synchronization feature flag is enabled and a doctor is connected.
        </p>
        <div class="mt-5 flex justify-end gap-2">
          <button
            class="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium hover:bg-gray-50"
            :disabled="isSynchronizing"
            @click="closeConfirmation"
          >
            Cancel
          </button>
          <button
            class="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            :disabled="isSynchronizing"
            @click="confirmSynchronization"
          >
            {{ isSynchronizing ? 'Synchronizing...' : 'Confirm synchronization' }}
          </button>
        </div>
      </div>
    </div>
  </sba-instance-section>
</template>

<script>
const OUTCOME_CLASSES = {
  SUCCESS: 'bg-green-100 text-green-800',
  UNCHANGED: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-800',
  DISABLED: 'bg-amber-100 text-amber-800',
  ALREADY_RUNNING: 'bg-blue-100 text-blue-800',
  ALREADY_COMPLETED_TODAY: 'bg-blue-100 text-blue-800'
}

const OUTCOME_MESSAGES = {
  SUCCESS: 'The current NITES drug codebook was saved successfully.',
  UNCHANGED: 'The NITES drug-code set is unchanged. The run completed successfully.',
  FAILED: 'Synchronization failed. Disable the feature flag and inspect the failure before retrying.',
  DISABLED: 'Synchronization is disabled. Enable the NITES synchronization feature flag before retrying.',
  ALREADY_RUNNING: 'Another NITES synchronization currently holds the database lock.',
  ALREADY_COMPLETED_TODAY: 'A scheduled synchronization already completed today.'
}

export default {
  props: {
    instance: { type: Object, required: true }
  },
  data: () => ({
    showConfirmation: false,
    isSynchronizing: false,
    requestError: null,
    result: null
  }),
  methods: {
    openConfirmation() {
      this.requestError = null
      this.showConfirmation = true
    },
    closeConfirmation() {
      if (!this.isSynchronizing) this.showConfirmation = false
    },
    async confirmSynchronization() {
      this.isSynchronizing = true
      this.requestError = null
      try {
        const response = await this.instance.axios.post('actuator/nites-synchronization/refresh')
        this.result = response.data
        this.showConfirmation = false
      } catch (error) {
        this.requestError = error.response?.data?.message || 'The synchronization request failed.'
        this.showConfirmation = false
      } finally {
        this.isSynchronizing = false
      }
    },
    outcomeClass(outcome) {
      return OUTCOME_CLASSES[outcome] || 'bg-gray-100 text-gray-800'
    },
    outcomeMessage(outcome) {
      return OUTCOME_MESSAGES[outcome] || 'The synchronization request returned an unknown outcome.'
    },
    outcomeMessageClass(outcome) {
      return outcome === 'FAILED' || outcome === 'DISABLED' ? 'text-red-700' : 'text-gray-700'
    }
  }
}
</script>
