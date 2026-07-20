<template>
  <sba-instance-section :error="error" :loading="!hasLoaded">
    <div v-if="!hasLoaded" class="flex items-center justify-center py-12">
      <svg class="animate-spin h-8 w-8 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
    </div>
    <div v-else-if="error" class="p-4 bg-red-50 border border-red-200 rounded-lg">
      <h3 class="text-red-800 font-semibold mb-2">Error loading employees</h3>
      <pre class="text-red-700 text-sm whitespace-pre-wrap overflow-auto">{{ error }}</pre>
    </div>
    <div v-else>
      <div v-if="successMessage" data-testid="employees-success" class="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg">
        <p class="text-green-700 text-sm">{{ successMessage }}</p>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full border-collapse min-w-[800px]">
          <thead>
            <tr class="bg-gray-100">
              <th class="p-2 text-left border-b">Name</th>
              <th class="p-2 text-left border-b">Role</th>
              <th class="p-2 text-left border-b">Email</th>
              <th class="p-2 text-left border-b">Phone</th>
              <th class="p-2 text-left border-b">Status</th>
              <th class="p-2 text-left border-b">Verifications</th>
              <th class="p-2 text-left border-b">Active</th>
              <th class="p-2 text-left border-b">Can Validate</th>
              <th class="p-2 text-right border-b">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="employee in employees"
              :key="employee.id"
              :data-testid="`employee-row-${employee.id}`"
              class="hover:bg-gray-50"
            >
              <td class="p-2 border-b">
                <div class="font-medium">{{ employee.firstName }} {{ employee.lastName }}</div>
                <div class="text-xs text-gray-500 font-mono">{{ employee.id }}</div>
              </td>
              <td class="p-2 border-b">
                <span class="px-2 py-1 rounded text-sm bg-blue-100 text-blue-800">{{ formatRole(employee.accountRole) }}</span>
              </td>
              <td class="p-2 border-b text-sm">{{ employee.email || '—' }}</td>
              <td class="p-2 border-b text-sm">{{ employee.phoneNumber || '—' }}</td>
              <td class="p-2 border-b">
                <span
                  class="px-2 py-1 rounded text-sm"
                  :class="statusClass(employee.accountStatus)"
                >{{ formatStatus(employee.accountStatus) }}</span>
              </td>
              <td class="p-2 border-b">
                <div class="flex flex-col gap-1 text-xs">
                  <span :class="badgeClass(employee.emailVerificationStatus)">
                    Email: {{ formatVerification(employee.emailVerificationStatus) }}
                  </span>
                  <span :class="badgeClass(employee.phoneVerificationStatus)">
                    Phone: {{ formatVerification(employee.phoneVerificationStatus) }}
                  </span>
                </div>
              </td>
              <td class="p-2 border-b">
                <span
                  class="inline-flex items-center gap-1 px-2 py-1 rounded text-xs"
                  :class="employee.accountActive ? 'bg-green-100 text-green-800' : 'bg-gray-200 text-gray-600'"
                >
                  <span class="h-2 w-2 rounded-full" :class="employee.accountActive ? 'bg-green-500' : 'bg-gray-400'"></span>
                  {{ employee.accountActive ? 'Yes' : 'No' }}
                </span>
              </td>
              <td class="p-2 border-b">
                <span
                  class="px-2 py-1 rounded text-xs"
                  :class="employee.canValidate ? 'bg-indigo-100 text-indigo-800' : 'bg-gray-100 text-gray-600'"
                >{{ employee.canValidate ? 'Yes' : 'No' }}</span>
              </td>
              <td class="p-2 border-b text-right">
                <button
                  v-if="employee.canValidate"
                  :data-testid="`employee-validate-btn-${employee.id}`"
                  @click="validateEmployee(employee)"
                  :disabled="isValidating"
                  class="employee-validate-button"
                >
                  <svg v-if="isValidatingFor(employee.id)" class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <span v-if="isValidatingFor(employee.id)">Validating...</span>
                  <span v-else>Validate</span>
                </button>
                <span v-else class="text-sm text-gray-400">—</span>
              </td>
            </tr>
            <tr v-if="employees.length === 0">
              <td colspan="9" class="p-4 text-center text-gray-500">No employees found</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

  </sba-instance-section>
</template>

<script>
export default {
  props: {
    instance: { type: Object, required: true }
  },
  data: () => ({
    hasLoaded: false,
    error: null,
    employees: [],
    validatingEmployeeId: null,
    isValidating: false,
    successMessage: null
  }),
  async created() {
    await this.fetchEmployees()
  },
  methods: {
    async fetchEmployees() {
      const businessId = this.$route.params.businessId
      this.hasLoaded = false
      this.error = null
      try {
        const response = await this.instance.axios.get('actuator/employees', {
          params: { businessId }
        })
        this.employees = response.data
      } catch (error) {
        this.error = error
      } finally {
        this.hasLoaded = true
      }
    },
    async validateEmployee(employee) {
      this.isValidating = true
      this.validatingEmployeeId = employee.id
      this.successMessage = null

      const businessId = this.$route.params.businessId
      const employeeId = employee.id

      try {
        const response = await this.instance.axios.post(
          `actuator/employees/${businessId}/${employeeId}/validateAccount`
        )
        const updated = response.data
        const idx = this.employees.findIndex(e => e.id === updated.id)
        if (idx !== -1) {
          this.employees[idx] = updated
        }
        this.successMessage = `Employee ${updated.firstName} ${updated.lastName} validated successfully.`
      } catch (error) {
        this.error = error
      } finally {
        this.isValidating = false
        this.validatingEmployeeId = null
      }
    },
    isValidatingFor(employeeId) {
      return this.isValidating && this.validatingEmployeeId === employeeId
    },
    formatRole(role) {
      if (!role) return '—'
      return role.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
    },
    formatStatus(status) {
      if (!status) return '—'
      return status.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
    },
    formatVerification(status) {
      if (!status) return '—'
      return status.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
    },
    statusClass(status) {
      if (status?.startsWith('PENDING_')) return 'bg-blue-100 text-blue-800'
      const map = {
        ACTIVE: 'bg-green-100 text-green-800',
        ACCOUNT_MISSING: 'bg-red-100 text-red-800',
        NO_ACCOUNT_REQUIRED: 'bg-gray-100 text-gray-700'
      }
      return map[status] || 'bg-gray-100 text-gray-800'
    },
    badgeClass(status) {
      const map = {
        VERIFIED: 'bg-green-100 text-green-700',
        UNVERIFIED: 'bg-yellow-100 text-yellow-700',
        PENDING: 'bg-blue-100 text-blue-700'
      }
      return map[status] || 'bg-gray-100 text-gray-600'
    }
  }
}
</script>

<style scoped>
.employee-validate-button {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.375rem 0.75rem;
  border: 1px solid #0f172a;
  border-radius: 0.375rem;
  background-color: #0f172a;
  color: #fff;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 150ms, border-color 150ms;
}

.employee-validate-button:hover:not(:disabled) {
  border-color: #1e293b;
  background-color: #1e293b;
}

.employee-validate-button:focus-visible {
  outline: 2px solid #3b82f6;
  outline-offset: 2px;
}

.employee-validate-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
</style>
