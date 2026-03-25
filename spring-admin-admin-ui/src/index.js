/* global SBA */
import Businesses from './Businesses.vue';
import BusinessProfile from './BusinessProfile.vue';
import MedicalReports from './MedicalReports.vue';

SBA.use({
  install({ viewRegistry }) {
    viewRegistry.addView({
      name: 'instances/businesses',
      parent: 'instances',
      path: 'businesses',
      label: 'Businesses',
      group: 'Maintenance',
      component: Businesses,
      order: 5000
    });

    viewRegistry.addView({
      name: 'instances/business-profile',
      parent: 'instances',
      path: 'businesses/:businessId',
      component: BusinessProfile
    });

    viewRegistry.addView({
      name: 'instances/medical-reports',
      parent: 'instances',
      path: 'businesses/:businessId/medical-reports',
      label: 'Medical Reports',
      group: 'Maintenance',
      component: MedicalReports,
      order: 5100
    });
  }
});

SBA.viewRegistry.setGroupIcon(
  "Maintenance",
  `<svg xmlns='http://www.w3.org/2000/svg' class='h-5 mr-3' viewBox='0 0 576 512'><path d='M512 80c8.8 0 16 7.2 16 16V416c0 8.8-7.2 16-16 16H64c-8.8 0-16-7.2-16-16V96c0-8.8 7.2-16 16-16H512zM64 32C28.7 32 0 60.7 0 96V416c0 35.3 28.7 64 64 64H512c35.3 0 64-28.7 64-64V96c0-35.3-28.7-64-64-64H64zM200 208c14.2 0 27 6.1 35.8 16c8.8 9.9 24 10.7 33.9 1.9s10.7-24 1.9-33.9c-17.5-19.6-43.1-32-71.5-32c-53 0-96 43-96 96s43 96 96 96c28.4 0 54-12.4 71.5-32c8.8-9.9 8-25-1.9-33.9s-25-8-33.9 1.9c-8.8 9.9-21.6 16-35.8 16c-26.5 0-48-21.5-48-48s21.5-48 48-48zm144 48c0-26.5 21.5-48 48-48c14.2 0 27 6.1 35.8 16c8.8 9.9 24 10.7 33.9 1.9s10.7-24 1.9-33.9c-17.5-19.6-43.1-32-71.5-32c-53 0-96 43-96 96s43 96 96 96c28.4 0 54-12.4 71.5-32c8.8-9.9 8-25-1.9-33.9s-25-8-33.9 1.9c-8.8 9.9-21.6 16-35.8 16c-26.5 0-48-21.5-48-48z'/></svg>`
);