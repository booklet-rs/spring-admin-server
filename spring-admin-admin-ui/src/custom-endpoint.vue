<template>
  <div class="m-4">
    <sba-panel title="Custom Endpoint">
      <p>Instance: {{ instance?.registration?.name }}</p>
      <sba-status v-if="instance" :status="instance.statusInfo.status" />
    </sba-panel>
  </div>
</template>

<script>
export default {
  props: {
    instance: {
      type: Object,
      default: null,
    },
  },
};
</script>