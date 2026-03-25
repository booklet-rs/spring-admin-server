<template>
  <sba-instance-section :error="error" :loading="!hasLoaded">
    <div v-if="business" class="px-12">
      <div class="flex items-center justify-between py-4 border-b border-gray-400">
        <h2 class="text-xl font-bold">{{ business.name }}</h2>
        <span 
          class="px-3 py-1 rounded-full text-sm"
          :class="business.isProfilePublished ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'"
        >
          {{ business.isProfilePublished ? 'Published' : 'Hidden' }}
        </span>
      </div>

      <section class="py-4 border-b border-gray-400 lg:border-0 lg:py-0">
        <h3 class="text-lg font-bold mb-3">Basic Info</h3>
        <div class="lg:py-4 lg:grid lg:grid-cols-2 lg:gap-6 lg:border-b lg:border-gray-200 lg:mx-12">
          <div>
            <label class="text-sm text-gray-500">Business ID</label>
            <p class="font-mono">{{ business.businessId }}</p>
          </div>
          <div>
            <label class="text-sm text-gray-500">Alias</label>
            <p>{{ business.alias || '—' }}</p>
          </div>
        </div>
      </section>

      <section class="py-4 border-b border-gray-400 lg:border-0 lg:py-0">
        <h3 class="text-lg font-bold mb-3">Location</h3>
        <div class="lg:py-4 lg:grid lg:grid-cols-3 lg:gap-6 lg:border-b lg:border-gray-200 lg:mx-12">
          <div>
            <label class="text-sm text-gray-500">Country</label>
            <p>{{ business.country }}</p>
          </div>
          <div>
            <label class="text-sm text-gray-500">City</label>
            <p>{{ business.city }}</p>
          </div>
          <div>
            <label class="text-sm text-gray-500">Address</label>
            <p>{{ formatAddress(business) }}</p>
          </div>
        </div>
      </section>

      <section class="py-4 border-b border-gray-400 lg:border-0 lg:py-0">
        <h3 class="text-lg font-bold mb-3">Contact</h3>
        <div class="lg:py-4 lg:grid lg:grid-cols-2 lg:gap-6 lg:border-b lg:border-gray-200 lg:mx-12">
          <div>
            <label class="text-sm text-gray-500">Phone Numbers</label>
            <ul v-if="business.phoneNumbers?.length" class="mt-1">
              <li v-for="phone in business.phoneNumbers" :key="phone.id">{{ formatPhone(phone) }}</li>
            </ul>
            <p v-else>—</p>
          </div>
          <div>
            <label class="text-sm text-gray-500">Emails</label>
            <ul v-if="business.emails?.length" class="mt-1">
              <li v-for="email in business.emails" :key="email">{{ email }}</li>
            </ul>
            <p v-else>—</p>
          </div>
        </div>
      </section>

      <section class="py-4 border-b border-gray-400 lg:border-0 lg:py-0">
        <h3 class="text-lg font-bold mb-3">Business Details</h3>
        <div class="lg:py-4 lg:grid lg:grid-cols-2 lg:gap-6 lg:border-b lg:border-gray-200 lg:mx-12">
          <div>
            <label class="text-sm text-gray-500">Registration Number</label>
            <p>{{ business.businessRegistrationNumber || '—' }}</p>
          </div>
          <div>
            <label class="text-sm text-gray-500">Tax Number</label>
            <p>{{ business.businessTaxNumber || '—' }}</p>
          </div>
        </div>
      </section>

      <section v-if="business.about" class="py-4 border-b border-gray-400 lg:border-0 lg:py-0">
        <h3 class="text-lg font-bold mb-3">About</h3>
        <div class="lg:py-4 lg:border-b lg:border-gray-200 lg:mx-12">
          <p class="text-gray-700">{{ business.about }}</p>
        </div>
      </section>

      <section v-if="business.bankAccounts?.length" class="py-4 border-b border-gray-400 lg:border-0 lg:py-0">
        <h3 class="text-lg font-bold mb-3">Bank Accounts</h3>
        <ul class="lg:py-2 lg:mx-12">
          <li v-for="account in business.bankAccounts" :key="account.id" class="py-2">
            <span class="font-medium">{{ account.bank }}</span>: {{ account.accountNumber }}
          </li>
        </ul>
      </section>

      <section class="py-6">
        <h3 class="text-lg font-bold mb-4">Data</h3>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 lg:mx-12">
          <router-link 
            :to="`${business.businessId}/medical-reports`"
            class="block p-6 bg-white border border-gray-200 rounded-lg shadow hover:bg-gray-50 transition-colors"
          >
            <h4 class="text-lg font-semibold text-gray-900">Medical Reports</h4>
            <p class="text-sm text-gray-500 mt-1">View and manage medical reports</p>
          </router-link>
        </div>
      </section>
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
    business: null
  }),
  async created() {
    await this.fetchBusiness()
  },
  methods: {
    async fetchBusiness() {
      const businessId = this.$route.params.businessId
      this.hasLoaded = false
      this.error = null
      try {
        const response = await this.instance.axios.get(`actuator/businesses/${businessId}`)
        this.business = response.data
      } catch (error) {
        this.error = error
      } finally {
        this.hasLoaded = true
      }
    },
    formatAddress(business) {
      if (!business) return '—'
      return `${business.streetName} ${business.streetNumber}`.trim()
    },
    formatPhone(phone) {
      if (!phone) return '—'
      return `+${phone.countryCodeNumber} ${phone.phoneNumber}`
    }
  }
}
</script>