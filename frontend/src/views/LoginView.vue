<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const email = ref('');
const password = ref('');
const errorMessage = ref('');
const successNotice = ref('');

onMounted(() => {
  if (route.query.registered === 'true') {
    if (route.query.email) {
      email.value = route.query.email;
    }
    successNotice.value = 'Account created successfully! Please enter your password to sign in.';
  }
});

const handleLogin = async () => {
  errorMessage.value = '';
  try {
    await authStore.login(email.value, password.value);
    router.push('/dashboard');
  } catch (err) {
    errorMessage.value = typeof err === 'string' ? err : 'Login failed. Please check your credentials.';
  }
};

const prefill = (userEmail, userPassword) => {
  email.value = userEmail;
  password.value = userPassword;
  successNotice.value = '';
};
</script>

<template>
  <div class="max-w-md mx-auto mt-12 bg-white p-8 rounded-2xl shadow-sm border border-slate-200">
    <div class="text-center mb-8">
      <div class="inline-flex items-center justify-center w-12 h-12 bg-blue-50 text-blue-600 rounded-full mb-3 text-2xl">
        🛡️
      </div>
      <h1 class="text-2xl font-bold text-slate-900 tracking-tight">Sign In to SafeHaven</h1>
      <p class="text-sm text-slate-500 mt-1">Access claims, coverage policies, and reviews</p>
    </div>

    <!-- Registration Success Banner -->
    <div v-if="successNotice" class="mb-5 p-3 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm flex items-start space-x-2">
      <span class="text-base leading-none">✨</span>
      <span>{{ successNotice }}</span>
    </div>

    <!-- Error Alert -->
    <div v-if="errorMessage" class="mb-5 p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm flex items-start space-x-2">
      <span class="text-base leading-none">⚠️</span>
      <span>{{ errorMessage }}</span>
    </div>

    <form @submit.prevent="handleLogin" class="space-y-4">
      <div>
        <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Email Address</label>
        <input
          v-model="email"
          type="email"
          required
          placeholder="name@example.com"
          class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
        />
      </div>

      <div>
        <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Password</label>
        <input
          v-model="password"
          type="password"
          required
          placeholder="••••••••"
          class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
        />
      </div>

      <button
        type="submit"
        :disabled="authStore.loading"
        class="w-full py-2.5 px-4 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg shadow-sm transition disabled:opacity-50 text-sm cursor-pointer"
      >
        <span v-if="authStore.loading">Signing in...</span>
        <span v-else>Sign In</span>
      </button>

      <div class="text-center text-xs text-slate-500 pt-2">
        Don't have an account?
        <router-link to="/register" class="text-blue-600 font-semibold hover:underline">
          Register now
        </router-link>
      </div>
    </form>

    <!-- Seeded Users Helper -->
    <div class="mt-8 pt-6 border-t border-slate-100">
      <div class="text-xs font-medium text-slate-400 mb-2.5 text-center">Quick Fill (Seeded Test Accounts)</div>
      <div class="grid grid-cols-2 gap-2 text-xs">
        <button
          type="button"
          @click="prefill('michael.scott@dundermifflin.com', 'password123')"
          class="p-2 border border-slate-200 rounded-lg hover:bg-slate-50 text-left transition"
        >
          <div class="font-semibold text-slate-700">Michael (Customer)</div>
          <div class="text-[11px] text-slate-400 truncate">michael.scott@...</div>
        </button>
        <button
          type="button"
          @click="prefill('david.wallace@dundermifflin.com', 'admin123')"
          class="p-2 border border-slate-200 rounded-lg hover:bg-slate-50 text-left transition"
        >
          <div class="font-semibold text-slate-700">David (Adjuster)</div>
          <div class="text-[11px] text-slate-400 truncate">david.wallace@...</div>
        </button>
      </div>
    </div>
  </div>
</template>
