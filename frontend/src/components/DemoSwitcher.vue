<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const isOpen = ref(false);
const switching = ref(false);
const notice = ref('');

const toggleOpen = () => {
  isOpen.value = !isOpen.value;
};

const switchPersona = async (email, password, targetRoute, name, role) => {
  switching.value = true;
  notice.value = `Switching to ${name}...`;

  try {
    if (email && password) {
      await authStore.login(email, password);
      router.push(targetRoute);
      notice.value = `Switched to ${name} (${role})!`;
    } else {
      // Guest mode
      authStore.logout();
      router.push('/');
      notice.value = 'Switched to Public Guest view!';
    }
  } catch (err) {
    notice.value = 'Switch failed: ' + (typeof err === 'string' ? err : 'check backend');
  } finally {
    switching.value = false;
    setTimeout(() => {
      notice.value = '';
    }, 2500);
  }
};
</script>

<template>
  <div class="fixed bottom-5 right-5 z-50 print:hidden font-sans">
    <!-- Notice Toast -->
    <div
      v-if="notice"
      class="mb-2 p-2.5 px-3.5 bg-slate-900 text-white text-xs font-semibold rounded-xl shadow-lg border border-slate-700 flex items-center space-x-2 animate-bounce"
    >
      <span>⚡</span>
      <span>{{ notice }}</span>
    </div>

    <!-- Collapsed Toggle Chip -->
    <button
      v-if="!isOpen"
      @click="toggleOpen"
      class="flex items-center space-x-2 px-3.5 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-full shadow-lg border border-slate-700 text-xs font-bold transition hover:scale-105 cursor-pointer"
    >
      <span class="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
      <span>🎭 Demo Persona Switcher</span>
      <span class="text-[10px] text-slate-400 bg-slate-800 px-1.5 py-0.5 rounded-full font-mono uppercase">
        {{ authStore.role || 'GUEST' }}
      </span>
    </button>

    <!-- Expanded Switcher Panel -->
    <div
      v-else
      class="w-80 bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden text-slate-800 animate-in fade-in slide-in-from-bottom-4 duration-200"
    >
      <!-- Panel Header -->
      <div class="bg-slate-900 text-white p-3.5 px-4 flex items-center justify-between">
        <div class="flex items-center space-x-2">
          <span class="text-sm">⚡</span>
          <span class="font-bold text-xs tracking-wide">Demo Persona Switcher</span>
        </div>
        <button
          @click="toggleOpen"
          class="text-slate-400 hover:text-white font-bold text-sm px-1 cursor-pointer transition"
        >
          &times;
        </button>
      </div>

      <!-- Active User Status -->
      <div class="p-3 bg-slate-50 border-b border-slate-100 flex items-center justify-between text-xs">
        <span class="text-slate-500">Current Session:</span>
        <span class="font-semibold text-slate-900 truncate max-w-[150px]">
          {{ authStore.isAuthenticated ? authStore.email : 'Public Guest (Logged Out)' }}
        </span>
      </div>

      <!-- Persona List -->
      <div class="p-2 space-y-1.5 max-h-80 overflow-y-auto">
        <!-- Persona 1: Customer (Michael Scott) -->
        <button
          @click="switchPersona('michael.scott@dundermifflin.com', 'password123', '/dashboard', 'Michael Scott', 'Customer')"
          :disabled="switching"
          :class="[
            'w-full p-2.5 rounded-xl border text-left transition flex items-center justify-between cursor-pointer text-xs',
            authStore.email === 'michael.scott@dundermifflin.com' ? 'border-blue-500 bg-blue-50/60 font-semibold text-blue-900' : 'border-slate-100 hover:bg-slate-50 hover:border-slate-200 text-slate-800'
          ]"
        >
          <div class="flex items-center space-x-2.5 overflow-hidden">
            <span class="text-base">👤</span>
            <div class="overflow-hidden">
              <div class="font-bold truncate">Michael Scott (Customer)</div>
              <div class="text-[11px] text-slate-500 truncate">Policy #POL-FIRE-9988 • Active Claim</div>
            </div>
          </div>
          <span v-if="authStore.email === 'michael.scott@dundermifflin.com'" class="text-blue-600 font-bold text-xs">Active</span>
        </button>

        <!-- Persona 2: Adjuster (David Wallace) -->
        <button
          @click="switchPersona('david.wallace@dundermifflin.com', 'admin123', '/dashboard', 'David Wallace', 'Adjuster')"
          :disabled="switching"
          :class="[
            'w-full p-2.5 rounded-xl border text-left transition flex items-center justify-between cursor-pointer text-xs',
            authStore.email === 'david.wallace@dundermifflin.com' ? 'border-indigo-500 bg-indigo-50/60 font-semibold text-indigo-900' : 'border-slate-100 hover:bg-slate-50 hover:border-slate-200 text-slate-800'
          ]"
        >
          <div class="flex items-center space-x-2.5 overflow-hidden">
            <span class="text-base">⚖️</span>
            <div class="overflow-hidden">
              <div class="font-bold truncate">David Wallace (Adjuster)</div>
              <div class="text-[11px] text-slate-500 truncate">Claims Examiner & Policy Issuer</div>
            </div>
          </div>
          <span v-if="authStore.email === 'david.wallace@dundermifflin.com'" class="text-indigo-600 font-bold text-xs">Active</span>
        </button>

        <!-- Persona 3: Customer (Faith Korosso) -->
        <button
          @click="switchPersona('faithkorosso@gmail.com', 'password123', '/dashboard', 'Faith Korosso', 'Customer')"
          :disabled="switching"
          :class="[
            'w-full p-2.5 rounded-xl border text-left transition flex items-center justify-between cursor-pointer text-xs',
            authStore.email === 'faithkorosso@gmail.com' ? 'border-blue-500 bg-blue-50/60 font-semibold text-blue-900' : 'border-slate-100 hover:bg-slate-50 hover:border-slate-200 text-slate-800'
          ]"
        >
          <div class="flex items-center space-x-2.5 overflow-hidden">
            <span class="text-base">👤</span>
            <div class="overflow-hidden">
              <div class="font-bold truncate">Faith Korosso (Customer)</div>
              <div class="text-[11px] text-slate-500 truncate">Policy #POL-FIRE-8301 • Clean Slate</div>
            </div>
          </div>
          <span v-if="authStore.email === 'faithkorosso@gmail.com'" class="text-blue-600 font-bold text-xs">Active</span>
        </button>

        <!-- Persona 4: Public Guest -->
        <button
          @click="switchPersona(null, null, '/', 'Guest', 'Public')"
          :disabled="switching"
          :class="[
            'w-full p-2.5 rounded-xl border text-left transition flex items-center justify-between cursor-pointer text-xs',
            !authStore.isAuthenticated ? 'border-amber-500 bg-amber-50/60 font-semibold text-amber-900' : 'border-slate-100 hover:bg-slate-50 hover:border-slate-200 text-slate-800'
          ]"
        >
          <div class="flex items-center space-x-2.5 overflow-hidden">
            <span class="text-base">🌐</span>
            <div class="overflow-hidden">
              <div class="font-bold truncate">Public Visitor (Guest)</div>
              <div class="text-[11px] text-slate-500 truncate">Quote & Buy Landing Page</div>
            </div>
          </div>
          <span v-if="!authStore.isAuthenticated" class="text-amber-600 font-bold text-xs">Active</span>
        </button>
      </div>

      <!-- Panel Footer Note -->
      <div class="p-2.5 px-3 bg-slate-50 border-t border-slate-100 text-[10px] text-slate-400 text-center">
        Instant 1-click persona switching for evaluations & demos.
      </div>
    </div>
  </div>
</template>
