<template>
  <div class="m-4">
    <sba-panel title="Custom Dashboard">
      <p>Welcome to your custom UI extension!</p>
    </sba-panel>

    <template v-for="application in applications" :key="application.name">
      <sba-panel :title="application.name">
        Instances:
        <ul>
          <template v-for="instance in application.instances" :key="instance.id">
            <li>
              <span class="mx-1" v-text="instance.registration.name"></span>
              <sba-status :status="instance.statusInfo.status" class="mx-1" />
            </li>
          </template>
        </ul>
      </sba-panel>
    </template>
  </div>
</template>

<script>
/* global SBA */

export default {
  setup() {
    const { applications } = SBA.useApplicationStore();
    return {
      applications,
    };
  },
};
</script>