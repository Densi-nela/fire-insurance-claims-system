<script setup>
import { useAuthStore } from './stores/auth';
import { useRouter } from 'vue-router';
import DemoSwitcher from './components/DemoSwitcher.vue';

const authStore = useAuthStore();
const router = useRouter();

const handleLogout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<template>
  <div class="min-h-screen bg-slate-50 text-slate-800 flex flex-col font-sans">
    <!-- Navigation Bar -->
    <header class="bg-white border-b border-slate-200 sticky top-0 z-50 shadow-xs print:hidden">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <div class="flex items-center space-x-3">
          <div class="w-9 h-9 bg-blue-600 rounded-lg flex items-center justify-center text-white font-bold text-lg shadow-sm">
            🛡️
          </div>
          <router-link to="/dashboard" class="font-bold text-xl tracking-tight text-slate-900 hover:text-blue-600 transition">
            SafeHaven Insurance
          </router-link>
        </div>

        <nav v-if="authStore.isAuthenticated" class="flex items-center space-x-6">
          <router-link
            to="/dashboard"
            class="text-sm font-medium text-slate-600 hover:text-blue-600 transition"
            active-class="text-blue-600 font-semibold"
          >
            Dashboard
          </router-link>

          <router-link
            to="/profile"
            class="text-sm font-medium text-slate-600 hover:text-blue-600 transition"
            active-class="text-blue-600 font-semibold"
          >
            Profile
          </router-link>

          <router-link
            v-if="authStore.isCustomer"
            to="/claims/new"
            class="text-sm font-medium text-slate-600 hover:text-blue-600 transition"
            active-class="text-blue-600 font-semibold"
          >
            File a Claim
          </router-link>

          <div class="flex items-center space-x-3 pl-4 border-l border-slate-200">
            <div class="text-right hidden sm:block">
              <div class="text-xs font-semibold text-slate-900">{{ authStore.email }}</div>
              <div class="text-[11px] font-medium tracking-wide uppercase px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 inline-block">
                {{ authStore.role }}
              </div>
            </div>
            <button
              @click="handleLogout"
              class="text-sm px-3 py-1.5 rounded-lg border border-slate-300 hover:bg-slate-100 text-slate-700 font-medium transition cursor-pointer"
            >
              Sign out
            </button>
          </div>
        </nav>

        <nav v-else class="flex items-center space-x-4">
          <router-link
            to="/"
            class="text-sm font-medium text-slate-600 hover:text-blue-600 transition"
          >
            Get a Quote
          </router-link>
          <router-link
            to="/login"
            class="text-sm px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-semibold transition"
          >
            Sign In
          </router-link>
        </nav>
      </div>
    </header>

    <!-- Main Content Area -->
    <main class="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8 print:p-0 print:max-w-none">
      <router-view />
    </main>

    <!-- Enterprise Corporate Footer -->
    <footer class="bg-white border-t border-slate-200 mt-20 pt-12 pb-8 text-xs text-slate-500 print:hidden">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 mb-12">
          <!-- Col 1: Brand & Emergency -->
          <div class="space-y-3">
            <div class="flex items-center space-x-2">
              <span class="text-lg">🛡️</span>
              <span class="font-bold text-slate-900 text-sm tracking-tight">SafeHaven Insurance Co.</span>
            </div>
            <p class="text-slate-500 leading-relaxed">
              Certified direct underwriter protecting families, structures, and personal property from fire incidents across all 50 states.
            </p>
            <div class="pt-1">
              <div class="font-semibold text-slate-700">24/7 Emergency Dispatch:</div>
              <div class="text-blue-600 font-bold text-sm font-mono">1-800-555-FIRE</div>
            </div>
          </div>

          <!-- Col 2: Products -->
          <div class="space-y-2.5">
            <div class="font-bold text-slate-900 uppercase tracking-wider text-[11px]">Insurance Coverage</div>
            <ul class="space-y-1.5 text-slate-600">
              <li><router-link to="/" class="hover:text-blue-600 transition">Single Family Dwelling</router-link></li>
              <li><router-link to="/" class="hover:text-blue-600 transition">Condo & Townhouse Fire</router-link></li>
              <li><router-link to="/" class="hover:text-blue-600 transition">High-Value Estate Protection</router-link></li>
              <li><router-link to="/" class="hover:text-blue-600 transition">Loss of Use & Hotel Allowance</router-link></li>
            </ul>
          </div>

          <!-- Col 3: Claims Center -->
          <div class="space-y-2.5">
            <div class="font-bold text-slate-900 uppercase tracking-wider text-[11px]">Claims Workbench</div>
            <ul class="space-y-1.5 text-slate-600">
              <li><router-link to="/login" class="hover:text-blue-600 transition">Report an Incident (FNOL)</router-link></li>
              <li><router-link to="/dashboard" class="hover:text-blue-600 transition">Track Active Claims</router-link></li>
              <li><router-link to="/login" class="hover:text-blue-600 transition">Adjuster Review Desk</router-link></li>
              <li><router-link to="/" class="hover:text-blue-600 transition">Emergency Relocation Form</router-link></li>
            </ul>
          </div>

          <!-- Col 4: Trust & Compliance -->
          <div class="space-y-2.5">
            <div class="font-bold text-slate-900 uppercase tracking-wider text-[11px]">Financial Security</div>
            <div class="p-3 rounded-xl bg-slate-50 border border-slate-200 space-y-1.5">
              <div class="font-bold text-slate-900 flex items-center justify-between">
                <span>AM Best Rating:</span>
                <span class="text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded text-[11px] font-mono">A+ (Superior)</span>
              </div>
              <p class="text-[11px] text-slate-500 leading-normal">
                Underwritten by SafeHaven National Casualty Consortium. NAIC #98721.
              </p>
            </div>
            <div class="text-[11px] text-slate-400">
              Approved by all major Fannie Mae & Freddie Mac mortgage originators.
            </div>
          </div>
        </div>

        <div class="border-t border-slate-200 pt-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-[11px] text-slate-400">
          <div>
            &copy; 2026 SafeHaven Insurance Underwriting Inc. All rights reserved.
          </div>
          <div class="flex space-x-4">
            <a href="#" class="hover:underline">Privacy Policy</a>
            <a href="#" class="hover:underline">Terms of Underwriting</a>
            <a href="#" class="hover:underline">State Disclosures</a>
            <a href="#" class="hover:underline">Security Architecture</a>
          </div>
        </div>
      </div>
    </footer>

    <!-- Global Floating Demo Switcher -->
    <DemoSwitcher />
  </div>
</template>
