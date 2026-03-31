<template>
  <sba-instance-section :error="error" :loading="!hasLoaded">
    <div v-if="hasLoaded" class="px-12">
      <div class="flex items-center justify-between py-4 border-b border-gray-400">
        <h2 class="text-xl font-bold">Clinic Migration</h2>
      </div>

      <div v-if="successMessage" class="mt-4 p-3 bg-green-50 border border-green-200 rounded-lg">
        <p class="text-green-700 text-sm">{{ successMessage }}</p>
      </div>

      <div v-if="submitError" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
        <p class="text-red-700 text-sm">{{ submitError }}</p>
      </div>

      <form @submit.prevent="submitMigration" class="py-6 lg:mx-12">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Patient Starting Number</label>
            <input
              v-model.number="form.patientStartingNumber"
              type="number"
              min="1"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              :disabled="isSubmitting"
            />
            <p v-if="formErrors.patientStartingNumber" class="mt-1 text-sm text-red-600">{{ formErrors.patientStartingNumber }}</p>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Protocol Starting Number</label>
            <input
              v-model.number="form.protocolStartingNumber"
              type="number"
              min="1"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
              :disabled="isSubmitting || hasUpperLimit"
            />
            <p v-if="formErrors.protocolStartingNumber" class="mt-1 text-sm text-red-600">{{ formErrors.protocolStartingNumber }}</p>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Start Date</label>
            <input
              v-model="form.startDate"
              type="date"
              class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
              :disabled="isSubmitting || hasUpperLimit"
            />
            <p v-if="formErrors.startDate" class="mt-1 text-sm text-red-600">{{ formErrors.startDate }}</p>
          </div>
        </div>

        <div class="mt-10 py-6 flex justify-end">
          <button
            type="submit"
            :disabled="isSubmitting || !isFormValid"
            class="px-6 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span v-if="isSubmitting" class="flex items-center gap-2">
              <svg class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Saving...
            </span>
            <span v-else>Save Migration</span>
          </button>
        </div>
      </form>
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
    upperLimit: null,
    form: {
      patientStartingNumber: null,
      protocolStartingNumber: null,
      startDate: null
    },
    formErrors: {
      patientStartingNumber: null,
      protocolStartingNumber: null,
      startDate: null
    },
    isSubmitting: false,
    submitError: null,
    successMessage: null
  }),
  computed: {
    hasUpperLimit() {
      return this.upperLimit !== null
    },
    isFormValid() {
      return this.form.patientStartingNumber >= 1
        && this.form.protocolStartingNumber >= 1
        && this.form.startDate
    }
  },
  async created() {
    await this.fetchUpperLimit()
  },
  methods: {
    async fetchUpperLimit() {
      const businessId = this.$route.params.businessId
      this.hasLoaded = false
      this.error = null
      try {
        const response = await this.instance.axios.get(`actuator/client-migrations/${businessId}`)
        if (response.data) {
          this.upperLimit = response.data
          this.form.protocolStartingNumber = response.data.nextProtocolNumber
          this.form.startDate = response.data.previousReportDate
        }
      } catch (error) {
        if (error.response?.status !== 404) {
          this.error = error
        }
      } finally {
        this.hasLoaded = true
      }
    },
    validate() {
      this.formErrors = {
        patientStartingNumber: null,
        protocolStartingNumber: null,
        startDate: null
      }

      let valid = true

      if (!this.form.patientStartingNumber || this.form.patientStartingNumber < 1) {
        this.formErrors.patientStartingNumber = 'Patient starting number must be greater than 0.'
        valid = false
      }

      if (!this.form.protocolStartingNumber || this.form.protocolStartingNumber < 1) {
        this.formErrors.protocolStartingNumber = 'Protocol starting number must be greater than 0.'
        valid = false
      }

      if (!this.form.startDate) {
        this.formErrors.startDate = 'Start date is required.'
        valid = false
      }

      return valid
    },
    async submitMigration() {
      if (!this.validate()) return

      this.isSubmitting = true
      this.submitError = null
      this.successMessage = null

      try {
        const businessId = this.$route.params.businessId
        const protocolStartingNumber = this.hasUpperLimit
          ? this.upperLimit.nextProtocolNumber
          : this.form.protocolStartingNumber
        const startDate = this.hasUpperLimit
          ? this.upperLimit.previousReportDate
          : this.form.startDate

        await this.instance.axios.post('actuator/client-migrations', {
          businessId,
          patientStartingNumber: this.form.patientStartingNumber,
          protocolStartingNumber,
          startDate
        }, {
          headers: { 'Content-Type': 'application/json' }
        })
        this.successMessage = 'Migration saved successfully.'
      } catch (error) {
        this.submitError = error.response?.data?.message || error.message || 'Failed to save migration'
      } finally {
        this.isSubmitting = false
      }
    }
  }
}
</script>
