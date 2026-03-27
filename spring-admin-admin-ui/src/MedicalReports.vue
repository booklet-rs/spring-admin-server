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
            <th class="p-2 text-left border-b">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(report, index) in reports" :key="report.reportId" class="hover:bg-gray-50">
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
            <td class="p-2 border-b">
              <button
                v-if="index === 0"
                @click="promptDelete(report.reportId)"
                class="text-red-600 hover:text-red-800"
                title="Delete report"
              >
                <Trash2 class="h-5 w-5" />
              </button>
            </td>
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

    <div v-if="reportToDelete && !awaitingConfirmation" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
        <div class="p-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-2">Request Report Deletion</h3>
          <p class="text-gray-600 mb-4">A confirmation code will be sent to your email. You'll need to enter it to complete the deletion.</p>
          
          <div v-if="requestError" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
            <p class="text-red-700 text-sm">{{ requestError }}</p>
          </div>

          <div class="flex justify-end gap-4">
            <button
              @click="cancelRequest"
              :disabled="isRequesting"
              class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Cancel
            </button>
            <button
              @click="requestDelete"
              :disabled="isRequesting"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-md hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span v-if="isRequesting" class="flex items-center gap-2">
                <svg class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Sending...
              </span>
              <span v-else>Send Confirmation Code</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="awaitingConfirmation" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
        <div class="p-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-2">Enter Confirmation Code</h3>
          <p class="text-gray-600 mb-4">Enter the confirmation code sent to your email to complete the deletion.</p>
          
          <div class="mb-4">
            <input
              v-model="confirmationCode"
              type="text"
              placeholder="Paste confirmation code here"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 font-mono text-sm"
              :disabled="isConfirming"
            />
          </div>

          <div v-if="confirmError" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
            <p class="text-red-700 text-sm">{{ confirmError }}</p>
          </div>

          <div class="flex justify-end gap-4">
            <button
              @click="cancelConfirmation"
              :disabled="isConfirming"
              class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Cancel
            </button>
            <button
              @click="confirmDelete"
              :disabled="isConfirming || !confirmationCode"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-md hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span v-if="isConfirming" class="flex items-center gap-2">
                <svg class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Deleting...
              </span>
              <span v-else>Confirm Deletion</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </sba-instance-section>
</template>

<script>
import { Trash2 } from 'lucide-vue-next';
import Pagination from '@/components/ui/pagination/Pagination.vue';

export default {
  components: { Pagination, Trash2 },
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
    totalPages: 0,
    reportToDelete: null,
    isRequesting: false,
    requestError: null,
    awaitingConfirmation: false,
    confirmationCode: '',
    isConfirming: false,
    confirmError: null
  }),
  async created() {
    await this.fetchMedicalReports();
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
    promptDelete(reportId) {
      const businessId = this.$route.params.businessId;
      this.reportToDelete = { businessId, reportId };
      this.requestError = null;
    },
    cancelRequest() {
      this.reportToDelete = null;
      this.requestError = null;
    },
    async requestDelete() {
      if (!this.reportToDelete) return;
      
      this.isRequesting = true;
      this.requestError = null;
      
      try {
        await this.instance.axios.delete(
          `actuator/medicalReports/${this.reportToDelete.businessId}/${this.reportToDelete.reportId}`
        );
        this.awaitingConfirmation = true;
        this.confirmationCode = '';
        this.confirmError = null;
      } catch (error) {
        this.requestError = error.response?.data?.message || error.message || 'Failed to request deletion';
      } finally {
        this.isRequesting = false;
      }
    },
    cancelConfirmation() {
      this.awaitingConfirmation = false;
      this.reportToDelete = null;
      this.confirmationCode = '';
      this.confirmError = null;
    },
    async confirmDelete() {
      if (!this.reportToDelete || !this.confirmationCode) return;
      
      this.isConfirming = true;
      this.confirmError = null;
      
      try {
        await this.instance.axios.post(
          `actuator/medicalReports/${this.reportToDelete.businessId}/${this.reportToDelete.reportId}/${this.confirmationCode}`
        );
        this.awaitingConfirmation = false;
        this.reportToDelete = null;
        this.confirmationCode = '';
        await this.fetchMedicalReports();
      } catch (error) {
        this.confirmError = error.response?.data?.message || error.message || 'Failed to delete report';
      } finally {
        this.isConfirming = false;
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