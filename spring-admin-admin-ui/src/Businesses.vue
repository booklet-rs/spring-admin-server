<template>
  <sba-instance-section :error="error" :loading="!hasLoaded">
    <table class="w-full border-collapse">
      <thead>
        <tr class="bg-gray-100">
          <th class="p-2 text-left border-b">ID</th>
          <th class="p-2 text-left border-b">Name</th>
          <th class="p-2 text-left border-b">City</th>
          <th class="p-2 text-left border-b">Address</th>
          <th class="p-2 text-left border-b">Phone</th>
          <th class="p-2 text-left border-b">Email</th>
          <th class="p-2 text-left border-b">Reg. Number</th>
          <th class="p-2 text-left border-b">Tax Number</th>
          <th class="p-2 text-left border-b">Status</th>
          <th class="p-2 text-left border-b">Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="business in businesses" :key="business.businessId" class="hover:bg-gray-50">
          <td class="p-2 border-b">{{ business.businessId }}</td>
          <td class="p-2 border-b">{{ business.name }}</td>
          <td class="p-2 border-b">{{ business.city }}</td>
          <td class="p-2 border-b">{{ formatAddress(business) }}</td>
          <td class="p-2 border-b">{{ formatPhone(business.phoneNumbers?.[0]) }}</td>
          <td class="p-2 border-b">{{ business.emails?.[0] || '' }}</td>
          <td class="p-2 border-b">{{ business.businessRegistrationNumber || '' }}</td>
          <td class="p-2 border-b">{{ business.businessTaxNumber || '' }}</td>
          <td class="p-2 border-b">
            <span 
              class="px-2 py-1 rounded text-sm"
              :class="business.isProfilePublished ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'"
            >
              {{ business.isProfilePublished ? 'Published' : 'Hidden' }}
            </span>
          </td>
          <td class="p-2 border-b">
            <router-link 
              :to="`businesses/${business.businessId}`"
              class="text-blue-600 hover:text-blue-800 hover:underline"
            >
              View
            </router-link>
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
    businesses: [],
    page: 0,
    pageSize: 10,
    totalElements: 0,
    totalPages: 0
  }),
  async created() {
    await this.fetchBusinesses()
  },
  methods: {
    async fetchBusinesses() {
      this.hasLoaded = false
      this.error = null
      try {
        const response = await this.instance.axios.get('actuator/businesses', {
          params: {
            page: this.page,
            size: this.pageSize
          }
        })
        this.businesses = response.data.content
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
      this.fetchBusinesses()
    },
    handlePageSizeChange(newSize) {
      this.pageSize = newSize
      this.page = 0
      this.fetchBusinesses()
    },
    formatAddress(business) {
      return `${business.streetName} ${business.streetNumber}`.trim()
    },
    formatPhone(phone) {
      if (!phone) return ''
      return `+${phone.countryCodeNumber} ${phone.phoneNumber}`
    }
  }
}
</script>