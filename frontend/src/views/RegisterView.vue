<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import api from '../api/client';

const router = useRouter();

const form = ref({
  name: '',
  email: '',
  phoneNumber: '',
  role: 'CUSTOMER',
  password: '',
  confirmPassword: '',
});

const loading = ref(false);
const error = ref('');
const successMessage = ref('');

const handleRegister = async () => {
  error.value = '';
  successMessage.value = '';

  if (form.value.password !== form.value.confirmPassword) {
    error.value = 'Passwords do not match.';
    return;
  }

  loading.value = true;
  try {
    // 1. Call registration endpoint
    await api.post('/customers', {
      name: form.value.name,
      email: form.value.email,
      phoneNumber: form.value.phoneNumber || null,
      role: form.value.role,
      password: form.value.password,
    });

    successMessage.value = 'Account created! Redirecting to login...';

    // 2. Redirect user to Login page so they can sign in with their new credentials
    setTimeout(() => {
      router.push({
        path: '/login',
        query: { registered: 'true', email: form.value.email },
      });
    }, 1000);
  } catch (err) {
    error.value = err.response?.data?.error || err.message || 'Registration failed.';
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="max-w-md mx-auto mt-8 bg-white p-8 rounded-2xl shadow-sm border border-slate-200">
    <div class="text-center mb-6">
      <div class="inline-flex items-center justify-center w-12 h-12 bg-blue-50 text-blue-600 rounded-full mb-3 text-2xl">
        🛡️
      </div>
      <h1 class="text-2xl font-bold text-slate-900 tracking-tight">Create an Account</h1>
      <p class="text-sm text-slate-500 mt-1">Join SafeHaven to file claims and manage insurance coverage</p>
    </div>

    <!-- Error Alert -->
    <div v-if="error" class="mb-5 p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-sm flex items-start space-x-2">
      <span class="text-base leading-none">⚠️</span>
      <span>{{ error }}</span>
    </div>

    <!-- Success Alert -->
    <div v-if="successMessage" class="mb-5 p-3 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm flex items-start space-x-2">
      <span class="text-base leading-none">✨</span>
      <span>{{ successMessage }}</span>
    </div>

    <form @submit.prevent="handleRegister" class="space-y-4">
      <div>
        <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Full Name</label>
        <input
          v-model="form.name"
          type="text"
          required
          placeholder="e.g. Jane Doe"
          class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
        />
      </div>

      <div>
        <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Email Address</label>
        <input
          v-model="form.email"
          type="email"
          required
          placeholder="jane@example.com"
          class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
        />
      </div>

      <div>
        <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Phone Number</label>
        <input
          v-model="form.phoneNumber"
          type="tel"
          placeholder="+1 (555) 000-0000"
          class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
        />
      </div>

      <div>
        <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Account Role</label>
        <select
          v-model="form.role"
          class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
        >
          <option value="CUSTOMER">Customer / Policyholder</option>
          <option value="ADJUSTER">Claims Adjuster / Reviewer</option>
        </select>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div>
          <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Password</label>
          <input
            v-model="form.password"
            type="password"
            required
            placeholder="••••••••"
            class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
          />
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Confirm</label>
          <input
            v-model="form.confirmPassword"
            type="password"
            required
            placeholder="••••••••"
            class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
          />
        </div>
      </div>

      <button
        type="submit"
        :disabled="loading"
        class="w-full py-2.5 px-4 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg shadow-sm transition disabled:opacity-50 text-sm cursor-pointer mt-2"
      >
        <span v-if="loading">Creating Account...</span>
        <span v-else>Register Account</span>
      </button>
    </form>

    <div class="mt-6 text-center text-xs text-slate-500">
      Already have an account?
      <router-link to="/login" class="text-blue-600 font-semibold hover:underline">
        Sign in here
      </router-link>
    </div>
  </div>
</template>
