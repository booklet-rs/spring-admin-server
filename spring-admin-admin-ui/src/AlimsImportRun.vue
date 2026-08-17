<template>
  <sba-instance-section :error="error" :loading="!hasLoaded">
    <div class="px-12">
      <div class="flex items-center justify-between py-4 border-b border-gray-400">
        <div>
          <router-link
            :to="{ name: 'instances/alims-imports' }"
            class="text-sm text-blue-600 hover:text-blue-800 hover:underline"
          >
            &larr; ALIMS Import Runs
          </router-link>
          <h2 class="text-xl font-bold mt-1">Run Diffs</h2>
          <p class="text-sm text-gray-500 mt-1 font-mono">{{ runId }}</p>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-sm text-gray-600">Change type:</span>
          <select
            :value="changeType ?? ''"
            @change="handleFilterChange"
            class="h-8 rounded-md border border-gray-300 bg-white pl-3 pr-8 text-sm"
          >
            <option value="">All</option>
            <option v-for="type in CHANGE_TYPES" :key="type" :value="type">{{ type }}</option>
          </select>
        </div>
      </div>

      <table class="w-full border-collapse mt-4">
        <thead>
          <tr class="bg-gray-100">
            <th class="p-2 text-left border-b">Product Code</th>
            <th class="p-2 text-left border-b">Type</th>
            <th class="p-2 text-left border-b">Changed Fields</th>
            <th class="p-2 text-left border-b">Medicine Name</th>
            <th class="p-2 text-left border-b">JKL</th>
            <th class="p-2 text-left border-b">INN</th>
            <th class="p-2 text-left border-b">Form &amp; Dosage</th>
            <th class="p-2 text-left border-b">ATC</th>
            <th class="p-2 text-left border-b">Regime</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="change in changes" :key="change.changeId" class="align-top hover:bg-gray-50">
            <td class="p-2 border-b font-mono">{{ change.alimsProductCode }}</td>
            <td class="p-2 border-b">
              <span class="px-2 py-1 rounded text-xs" :class="typeClass(change.changeType)">
                {{ change.changeType }}
              </span>
            </td>
            <td class="p-2 border-b text-xs text-gray-600">
              {{ change.changedFields?.join(', ') || '—' }}
            </td>
            <td class="p-2 border-b">
              <diff-cell :current-value="change.currentMedicineName" :staged-value="change.stagedMedicineName" />
            </td>
            <td class="p-2 border-b font-mono">
              <diff-cell :current-value="change.currentJkl" :staged-value="change.stagedJkl" />
            </td>
            <td class="p-2 border-b">
              <diff-cell :current-value="change.currentInn" :staged-value="change.stagedInn" />
            </td>
            <td class="p-2 border-b">
              <diff-cell :current-value="change.currentFormAndDosage" :staged-value="change.stagedFormAndDosage" />
            </td>
            <td class="p-2 border-b font-mono">
              <diff-cell :current-value="change.currentAtc" :staged-value="change.stagedAtc" />
            </td>
            <td class="p-2 border-b">
              <diff-cell :current-value="change.currentDispensingRegime" :staged-value="change.stagedDispensingRegime" />
            </td>
          </tr>
          <tr v-if="hasLoaded && changes.length === 0">
            <td colspan="9" class="p-6 text-center text-gray-500">No changes match this filter.</td>
          </tr>
        </tbody>
      </table>

      <Pagination
        v-if="totalElements > 0"
        :page="page"
        :page-size="pageSize"
        :total="totalElements"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </div>
  </sba-instance-section>
</template>

<script>
import { h } from 'vue'
import Pagination from '@/components/ui/pagination/Pagination.vue'

const CHANGE_TYPES = ['ADDED', 'METADATA_CHANGED', 'JKL_CHANGED', 'MISSING_FROM_SOURCE']

const TYPE_CLASSES = {
  ADDED: 'bg-green-100 text-green-800',
  METADATA_CHANGED: 'bg-blue-100 text-blue-800',
  JKL_CHANGED: 'bg-purple-100 text-purple-800',
  MISSING_FROM_SOURCE: 'bg-red-100 text-red-800'
}

const DiffCell = {
  name: 'DiffCell',
  props: {
    currentValue: { type: [String, Number], default: null },
    stagedValue: { type: [String, Number], default: null }
  },
  computed: {
    changed() {
      return this.currentValue != null && this.stagedValue != null &&
        String(this.currentValue) !== String(this.stagedValue)
    }
  },
  render() {
    if (this.changed) {
      return h('span', [
        h('span', { class: 'text-red-600 line-through' }, String(this.currentValue)),
        h('span', { class: 'text-gray-400 mx-1' }, '→'),
        h('span', { class: 'text-green-700 font-medium' }, String(this.stagedValue))
      ])
    }
    return h('span', null, String(this.stagedValue ?? this.currentValue ?? '—'))
  }
}

export default {
  components: { Pagination, DiffCell },
  props: {
    instance: { type: Object, required: true }
  },
  data: () => ({
    hasLoaded: false,
    error: null,
    changes: [],
    page: 0,
    pageSize: 20,
    totalElements: 0,
    changeType: null
  }),
  computed: {
    runId() {
      return this.$route.params.runId
    },
    changeTypes() {
      return CHANGE_TYPES
    }
  },
  async created() {
    await this.fetchChanges()
  },
  methods: {
    async fetchChanges() {
      this.hasLoaded = false
      this.error = null
      try {
        const response = await this.instance.axios.get(`actuator/alims-import-runs/${this.runId}`, {
          params: {
            page: this.page,
            size: this.pageSize,
            ...(this.changeType ? { changeType: this.changeType } : {})
          }
        })
        this.changes = response.data.content
        this.totalElements = response.data.totalElements
      } catch (error) {
        this.error = error
      } finally {
        this.hasLoaded = true
      }
    },
    handlePageChange(newPage) {
      this.page = newPage
      this.fetchChanges()
    },
    handlePageSizeChange(newSize) {
      this.pageSize = newSize
      this.page = 0
      this.fetchChanges()
    },
    handleFilterChange(event) {
      this.changeType = event.target.value || null
      this.page = 0
      this.fetchChanges()
    },
    typeClass(type) {
      return TYPE_CLASSES[type] || 'bg-gray-100 text-gray-800'
    }
  }
}
</script>
