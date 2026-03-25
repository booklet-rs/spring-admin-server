<script setup>
import { computed } from 'vue'
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-vue-next'

const props = defineProps({
  page: { type: Number, default: 0 },
  pageSize: { type: Number, default: 10 },
  total: { type: Number, default: 0 },
  siblingCount: { type: Number, default: 1 },
  showEdges: { type: Boolean, default: true }
})

const emit = defineEmits(['update:page', 'update:pageSize'])

const totalPages = computed(() => Math.ceil(props.total / props.pageSize))
const currentPage = computed(() => props.page + 1)

const handlePageChange = (newPage) => {
  if (newPage >= 1 && newPage <= totalPages.value) {
    emit('update:page', newPage - 1)
  }
}

const handlePageSizeChange = (event) => {
  emit('update:pageSize', Number(event.target.value))
  emit('update:page', 0)
}

const pages = computed(() => {
  const result = []
  for (let i = 1; i <= totalPages.value; i++) {
    result.push(i)
  }
  return result
})

const visiblePages = computed(() => {
  const page = currentPage.value
  const { siblingCount } = props
  const total = totalPages.value
  
  if (total <= 7) return pages.value
  
  const left = Math.max(1, page - siblingCount)
  const right = Math.min(total, page + siblingCount)
  
  if (left <= 2) {
    return [...Array.from({ length: 5 }, (_, i) => i + 1), '...', total]
  }
  
  if (right >= total - 1) {
    return [1, '...', ...Array.from({ length: 5 }, (_, i) => total - 4 + i)]
  }
  
  return [1, '...', left, page, right, '...', total]
})
</script>

<template>
  <div class="flex items-center justify-between px-2 py-4">
    <div class="flex items-center gap-2">
      <span class="text-sm text-gray-600">Rows per page:</span>
      <select
        :value="pageSize"
        @change="handlePageSizeChange"
        class="h-8 rounded-md border border-gray-300 bg-white px-2 text-sm"
      >
        <option v-for="size in [10, 20, 50, 100]" :key="size" :value="size">{{ size }}</option>
      </select>
    </div>
    
    <div class="flex items-center gap-1">
      <button
        class="inline-flex h-8 w-8 items-center justify-center rounded-md text-sm disabled:opacity-50 hover:bg-gray-100 disabled:cursor-not-allowed"
        :disabled="page === 0"
        @click="handlePageChange(1)"
      >
        <ChevronsLeft class="h-4 w-4"/>
      </button>
      <button
        class="inline-flex h-8 w-8 items-center justify-center rounded-md text-sm disabled:opacity-50 hover:bg-gray-100 disabled:cursor-not-allowed"
        :disabled="page === 0"
        @click="handlePageChange(currentPage - 1)"
      >
        <ChevronLeft class="h-4 w-4"/>
      </button>
      
      <template v-for="p in visiblePages" :key="p">
        <span v-if="p === '...'" class="px-2 text-gray-400">...</span>
        <button
          v-else
          class="inline-flex h-8 w-8 items-center justify-center rounded-md text-sm hover:bg-gray-100"
          :class="{ 'bg-gray-200 font-semibold': p === currentPage }"
          @click="handlePageChange(p)"
        >
          {{ p }}
        </button>
      </template>
      
      <button
        class="inline-flex h-8 w-8 items-center justify-center rounded-md text-sm disabled:opacity-50 hover:bg-gray-100 disabled:cursor-not-allowed"
        :disabled="page >= totalPages - 1"
        @click="handlePageChange(currentPage + 1)"
      >
        <ChevronRight class="h-4 w-4"/>
      </button>
      <button
        class="inline-flex h-8 w-8 items-center justify-center rounded-md text-sm disabled:opacity-50 hover:bg-gray-100 disabled:cursor-not-allowed"
        :disabled="page >= totalPages - 1"
        @click="handlePageChange(totalPages)"
      >
        <ChevronsRight class="h-4 w-4"/>
      </button>
    </div>
  </div>
</template>