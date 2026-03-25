<template>
  <sba-instance-section :error="error" :loading="!hasLoaded">
    <div v-if="!hasLoaded" class="flex items-center justify-center py-12">
      <svg class="animate-spin h-8 w-8 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
    </div>
    <div v-else-if="error" class="p-4 bg-red-50 border border-red-200 rounded-lg">
      <h3 class="text-red-800 font-semibold mb-2">Error loading medical reports</h3>
      <pre class="text-red-700 text-sm whitespace-pre-wrap overflow-auto">{{ error }}</pre>
    </div>
    <div v-else>
      <table class="w-full border-collapse">
        <thead>
          <tr class="bg-gray-100">
            <th class="p-2 text-left border-b">Report ID</th>
            <th class="p-2 text-left border-b">Patient</th>
            <th class="p-2 text-left border-b">Create Method</th>
            <th class="p-2 text-left border-b">Employee ID</th>
            <th class="p-2 text-left border-b">Doctor</th>
            <th class="p-2 text-left border-b">Report Date</th>
            <th class="p-2 text-left border-b">Created At</th>
            <th class="p-2 text-left border-b">Protocol #</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="report in reports" :key="report.reportId" class="hover:bg-gray-50">
            <td class="p-2 border-b font-mono text-sm">{{ report.reportId }}</td>
            <td class="p-2 border-b">{{ report.patientFirstName }} {{ report.patientLastName }}</td>
            <td class="p-2 border-b">
              <span 
                class="px-2 py-1 rounded text-sm"
                :class="getCreateMethodClass(report.createMethod)"
              >
                {{ formatCreateMethod(report.createMethod) }}
              </span>
            </td>
            <td class="p-2 border-b font-mono text-sm">{{ report.employeeId }}</td>
            <td class="p-2 border-b">
              <div>{{ report.doctorName }}</div>
              <div class="text-sm text-gray-500">{{ report.doctorTitle }} • {{ report.doctorSpecialty }}</div>
            </td>
            <td class="p-2 border-b">{{ formatDate(report.reportDate) }}</td>
            <td class="p-2 border-b">{{ formatDateTime(report.createdAt) }}</td>
            <td class="p-2 border-b">{{ report.protocolNumber || '—' }}</td>
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
import Pagination from '@/components/ui/pagination/Pagination.vue'

export default {
  components: { Pagination },
  props: {
    instance: { type: Object, required: true }
  },
  data: () => ({
    hasLoaded: false,
    error: null,
    reports: [],
    page: 0,
    pageSize: 20,
    totalElements: 0,
    totalPages: 0
  }),
  async created() {
    await this.fetchMedicalReports()
  },
  methods: {
    async fetchMedicalReports() {
      const businessId = this.$route.params.businessId
      this.hasLoaded = false
      this.error = null
      try {
        const response = await this.instance.axios.get('actuator/medicalReports', {
          params: {
            businessId: businessId,
            page: this.page,
            size: this.pageSize
          }
        })
        this.reports = response.data.content
        this.totalElements = response.data.totalElements
        this.totalPages = response.data.totalPages
      } catch (error) {
        this.error = error
      } finally {
        this.hasLoaded = true
      }
    },
    handlePageChange(newPage) {
      this.page = newPage
      this.fetchMedicalReports()
    },
    handlePageSizeChange(newSize) {
      this.pageSize = newSize
      this.page = 0
      this.fetchMedicalReports()
    },
    getCreateMethodClass(method) {
      const classes = {
        NATIVE: 'bg-green-100 text-green-800',
        BACKFILL: 'bg-yellow-100 text-yellow-800',
        MIGRATION: 'bg-blue-100 text-blue-800'
      }
      return classes[method] || 'bg-gray-100 text-gray-800'
    },
    formatCreateMethod(method) {
      return method ? method.charAt(0) + method.slice(1).toLowerCase() : '—'
    },
    formatDate(date) {
      if (!date) return '—'
      return new Date(date).toLocaleDateString()
    },
    formatDateTime(dateTime) {
      if (!dateTime) return '—'
      return new Date(dateTime).toLocaleString()
    }
  }
}
</script>