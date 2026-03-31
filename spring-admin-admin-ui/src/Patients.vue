<template>
  <sba-instance-section :error="error" :loading="!hasLoaded">
    <div v-if="!hasLoaded" class="flex items-center justify-center py-12">
      <svg class="animate-spin h-8 w-8 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
    </div>
    <div v-else-if="error" class="p-4 bg-red-50 border border-red-200 rounded-lg">
      <h3 class="text-red-800 font-semibold mb-2">Error loading patients</h3>
      <pre class="text-red-700 text-sm whitespace-pre-wrap overflow-auto">{{ error }}</pre>
    </div>
    <div v-else>
      <div class="flex items-center gap-4 mt-4 mb-4">
        <div class="flex-1 max-w-md">
          <input
            v-model="searchTerm"
            type="text"
            placeholder="Search patients..."
            class="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
            @keyup.enter="handleSearch"
          />
        </div>
        <button
          @click="handleSearch"
          class="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700"
        >
          Search
        </button>
        <button
          v-if="isSearchActive"
          @click="clearSearch"
          class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
        >
          Clear
        </button>
      </div>

      <table class="w-full border-collapse">
        <thead>
          <tr class="bg-gray-100">
            <th class="p-2 text-left border-b">Patient ID</th>
            <th class="p-2 text-left border-b">First Name</th>
            <th class="p-2 text-left border-b">Last Name</th>
            <th class="p-2 text-left border-b">Patient Number</th>
            <th class="p-2 text-left border-b">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="patient in patients" :key="patient.id" class="hover:bg-gray-50">
            <td class="p-2 border-b font-mono text-sm">{{ patient.id }}</td>
            <td class="p-2 border-b">{{ patient.firstName }}</td>
            <td class="p-2 border-b">{{ patient.lastName }}</td>
            <td class="p-2 border-b">{{ patient.patientNumber || '—' }}</td>
            <td class="p-2 border-b">
              <button
                @click="promptDelete(patient)"
                class="text-red-600 hover:text-red-800"
                title="Delete patient"
              >
                <Trash2 class="h-5 w-5" />
              </button>
            </td>
          </tr>
          <tr v-if="patients.length === 0">
            <td colspan="5" class="p-4 text-center text-gray-500">No patients found</td>
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

    <div v-if="patientToDelete && !awaitingConfirmation" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
        <div class="p-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-2">Request Patient Deletion</h3>
          <p class="text-gray-600 mb-4">
            A confirmation code will be sent to your email. You'll need to enter it to complete the deletion of
            <span class="font-semibold">{{ patientToDelete.firstName }} {{ patientToDelete.lastName }}</span>.
          </p>

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
    patients: [],
    page: 0,
    pageSize: 20,
    totalElements: 0,
    totalPages: 0,
    searchTerm: '',
    activeSearchTerm: '',
    isSearchActive: false,
    patientToDelete: null,
    isRequesting: false,
    requestError: null,
    awaitingConfirmation: false,
    confirmationCode: '',
    isConfirming: false,
    confirmError: null
  }),
  async created() {
    await this.fetchPatients();
  },
  methods: {
    async fetchPatients() {
      const businessId = this.$route.params.businessId;
      this.hasLoaded = false;
      this.error = null;
      try {
        const response = await this.instance.axios.get('actuator/clients', {
          params: {
            businessId: businessId,
            page: this.page,
            size: this.pageSize,
            sort: 'lastName',
            direction: 'asc'
          }
        });
        this.patients = response.data.content;
        this.totalElements = response.data.totalElements;
        this.totalPages = response.data.totalPages;
      } catch (error) {
        this.error = error;
      } finally {
        this.hasLoaded = true;
      }
    },
    async searchPatients() {
      const businessId = this.$route.params.businessId;
      this.hasLoaded = false;
      this.error = null;
      try {
        const response = await this.instance.axios.post('actuator/clients', {
          businessId: businessId,
          searchTerm: this.activeSearchTerm,
          page: this.page,
          size: this.pageSize
        }, {
          headers: { 'Content-Type': 'application/json' }
        });
        this.patients = response.data.content;
        this.totalElements = response.data.totalElements;
        this.totalPages = response.data.totalPages;
      } catch (error) {
        this.error = error;
      } finally {
        this.hasLoaded = true;
      }
    },
    handleSearch() {
      const term = this.searchTerm.trim();
      if (!term) {
        this.clearSearch();
        return;
      }
      this.activeSearchTerm = term;
      this.isSearchActive = true;
      this.page = 0;
      this.searchPatients();
    },
    clearSearch() {
      this.searchTerm = '';
      this.activeSearchTerm = '';
      this.isSearchActive = false;
      this.page = 0;
      this.fetchPatients();
    },
    handlePageChange(newPage) {
      this.page = newPage;
      if (this.isSearchActive) {
        this.searchPatients();
      } else {
        this.fetchPatients();
      }
    },
    handlePageSizeChange(newSize) {
      this.pageSize = newSize;
      this.page = 0;
      if (this.isSearchActive) {
        this.searchPatients();
      } else {
        this.fetchPatients();
      }
    },
    promptDelete(patient) {
      const businessId = this.$route.params.businessId;
      this.patientToDelete = { ...patient, businessId };
      this.requestError = null;
    },
    cancelRequest() {
      this.patientToDelete = null;
      this.requestError = null;
    },
    async requestDelete() {
      if (!this.patientToDelete) return;

      this.isRequesting = true;
      this.requestError = null;

      try {
        await this.instance.axios.delete(
          `actuator/clients/${this.patientToDelete.businessId}/${this.patientToDelete.id}`
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
      this.patientToDelete = null;
      this.confirmationCode = '';
      this.confirmError = null;
    },
    async confirmDelete() {
      if (!this.patientToDelete || !this.confirmationCode) return;

      this.isConfirming = true;
      this.confirmError = null;

      try {
        await this.instance.axios.post(
          `actuator/clients/${this.patientToDelete.businessId}/${this.patientToDelete.id}/${this.confirmationCode}`
        );
        this.awaitingConfirmation = false;
        this.patientToDelete = null;
        this.confirmationCode = '';
        if (this.isSearchActive) {
          this.searchPatients();
        } else {
          this.fetchPatients();
        }
      } catch (error) {
        this.confirmError = error.response?.data?.message || error.message || 'Failed to delete patient';
      } finally {
        this.isConfirming = false;
      }
    }
  }
}
</script>
