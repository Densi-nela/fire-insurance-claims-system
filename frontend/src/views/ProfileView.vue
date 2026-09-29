<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import api from '../api/client';

const router = useRouter();
const authStore = useAuthStore();

const profile = ref(null);
const policies = ref([]);
const loading = ref(true);
const saving = ref(false);
const error = ref('');
const successMessage = ref('');

const editForm = ref({
  name: '',
  phoneNumber: '',
});

const fetchProfileData = async () => {
  loading.value = true;
  error.value = '';
  try {
    // 1. Fetch current profile (try /me, with fallback to customer registry lookup)
    let currentProfile = null;
    try {
      const profileRes = await api.get('/customers/me');
      currentProfile = profileRes.data;
    } catch (meErr) {
      const allRes = await api.get('/customers');
      currentProfile = allRes.data.find(c => c.email && c.email.toLowerCase() === authStore.email?.toLowerCase());
    }

    if (!currentProfile) {
      currentProfile = {
        name: authStore.email ? authStore.email.split('@')[0] : 'User',
        email: authStore.email,
        role: authStore.role,
        phoneNumber: '',
        id: 1,
      };
    }

    profile.value = currentProfile;
    editForm.value.name = profile.value.name || '';
    editForm.value.phoneNumber = profile.value.phoneNumber || '';

    // 2. Fetch policies owned by this customer
    const policiesRes = await api.get('/policies');
    if (authStore.isCustomer) {
      policies.value = policiesRes.data.filter(p => p.customerEmail && p.customerEmail.toLowerCase() === authStore.email.toLowerCase());
    } else {
      policies.value = policiesRes.data;
    }
  } catch (err) {
    error.value = err.response?.data?.error || 'Failed to load profile details.';
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchProfileData();
});

const handleSaveProfile = async () => {
  saving.value = true;
  error.value = '';
  successMessage.value = '';
  try {
    const res = await api.put('/customers/me', {
      name: editForm.value.name,
      phoneNumber: editForm.value.phoneNumber,
    });
    profile.value = res.data;
    successMessage.value = 'Profile updated successfully!';
    setTimeout(() => {
      successMessage.value = '';
    }, 3000);
  } catch (err) {
    error.value = err.response?.data?.error || 'Failed to update profile.';
  } finally {
    saving.value = false;
  }
};

const handleLogout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<template>
  <div class="max-w-4xl mx-auto space-y-8">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-slate-900 tracking-tight">Account Settings & Profile</h1>
        <p class="text-sm text-slate-500 mt-0.5">Manage your contact details, view insured properties, and security settings</p>
      </div>

      <router-link to="/dashboard" class="text-sm text-blue-600 hover:underline font-semibold flex items-center space-x-1">
        <span>&larr; Back to Dashboard</span>
      </router-link>
    </div>

    <!-- Loading Skeleton -->
    <div v-if="loading" class="bg-white p-12 text-center rounded-2xl border border-slate-200 text-slate-400">
      Loading profile details...
    </div>

    <!-- Error State -->
    <div v-else-if="error && !profile" class="p-6 bg-red-50 border border-red-200 text-red-700 rounded-2xl text-sm">
      {{ error }}
    </div>

    <div v-else class="space-y-6">
      <!-- Success Banner -->
      <div v-if="successMessage" class="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm rounded-xl flex items-center space-x-2">
        <span>✨</span>
        <span class="font-medium">{{ successMessage }}</span>
      </div>

      <!-- Identity Card -->
      <div class="bg-white p-6 sm:p-8 rounded-2xl border border-slate-200 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-6">
        <div class="flex items-center space-x-4">
          <div class="w-16 h-16 rounded-full bg-blue-600 text-white font-bold text-2xl flex items-center justify-center shadow-sm">
            {{ (profile.name || 'U').charAt(0).toUpperCase() }}
          </div>
          <div>
            <div class="flex items-center space-x-2">
              <h2 class="text-xl font-bold text-slate-900">{{ profile.name }}</h2>
              <span class="text-[11px] font-bold px-2 py-0.5 rounded-full bg-blue-50 text-blue-700 border border-blue-200 uppercase">
                {{ profile.role }}
              </span>
            </div>
            <div class="text-sm text-slate-500 mt-0.5">{{ profile.email }}</div>
            <div class="text-xs text-slate-400 font-mono mt-1">Account ID: #ACC-{{ profile.id?.toString().padStart(5, '0') }}</div>
          </div>
        </div>

        <button
          @click="handleLogout"
          class="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs rounded-lg transition cursor-pointer self-start sm:self-center"
        >
          Sign out of Account
        </button>
      </div>

      <!-- Edit Contact Info Card -->
      <div class="bg-white p-6 sm:p-8 rounded-2xl border border-slate-200 shadow-xs space-y-5">
        <div class="border-b border-slate-100 pb-3">
          <h3 class="text-base font-bold text-slate-900">Personal Information</h3>
          <p class="text-xs text-slate-500">Update your primary contact information for claims notices and disbursements</p>
        </div>

        <form @submit.prevent="handleSaveProfile" class="space-y-4 max-w-lg">
          <div>
            <label class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">Full Legal Name</label>
            <input
              v-model="editForm.name"
              type="text"
              required
              class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white"
            />
          </div>

          <div>
            <label class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">Registered Email</label>
            <input
              :value="profile.email"
              disabled
              class="w-full px-3.5 py-2.5 bg-slate-100 border border-slate-200 rounded-lg text-sm text-slate-500 cursor-not-allowed font-mono"
            />
            <p class="text-[11px] text-slate-400 mt-1">Email is verified for authentication and policy binding.</p>
          </div>

          <div>
            <label class="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">Contact Phone Number</label>
            <input
              v-model="editForm.phoneNumber"
              type="tel"
              placeholder="+1 (555) 000-0000"
              class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white"
            />
          </div>

          <div class="pt-2">
            <button
              type="submit"
              :disabled="saving"
              class="px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold shadow-xs transition disabled:opacity-50 cursor-pointer"
            >
              {{ saving ? 'Saving Changes...' : 'Save Profile' }}
            </button>
          </div>
        </form>
      </div>

      <!-- Active Policies Registry for this Customer -->
      <div class="bg-white p-6 sm:p-8 rounded-2xl border border-slate-200 shadow-xs space-y-4">
        <div class="flex items-center justify-between border-b border-slate-100 pb-3">
          <div>
            <h3 class="text-base font-bold text-slate-900">Covered Properties & Policies</h3>
            <p class="text-xs text-slate-500">Active fire casualty contracts bound to your profile</p>
          </div>
          <span class="text-xs font-semibold text-blue-600 bg-blue-50 px-2.5 py-1 rounded-full">
            {{ policies.length }} Active Policy(ies)
          </span>
        </div>

        <div v-if="policies.length === 0" class="text-xs text-slate-400 italic py-4">
          No policies are currently registered under your email address.
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div
            v-for="policy in policies"
            :key="policy.id"
            class="p-4 rounded-xl border border-slate-200 bg-slate-50/50 space-y-2 hover:border-slate-300 transition"
          >
            <div class="flex justify-between items-start">
              <span class="text-xs font-mono font-bold text-blue-700 bg-blue-50 px-2 py-0.5 rounded border border-blue-200">
                {{ policy.policyNumber }}
              </span>
              <span class="text-xs font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                ACTIVE
              </span>
            </div>
            <div class="text-sm font-semibold text-slate-800">{{ policy.propertyAddress }}</div>
            <div class="flex items-center justify-between text-xs text-slate-500 pt-1 border-t border-slate-200/60">
              <span>Coverage Limit:</span>
              <span class="font-bold text-slate-900 font-mono">${{ policy.coverageLimit.toLocaleString() }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
