<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import api from '../api/client';

const router = useRouter();
const authStore = useAuthStore();

const allClaims = ref([]);
const policies = ref([]);
const customers = ref([]);
const loading = ref(true);
const error = ref('');

// Filter and Search states
const selectedFilter = ref('ALL');
const searchQuery = ref('');

// Policy Issuance Modal State (for Adjusters)
const showIssueModal = ref(false);
const issuingPolicy = ref(false);
const issueError = ref('');
const issueSuccess = ref('');
const newPolicyForm = ref({
  customerId: '',
  policyNumber: '',
  propertyAddress: '',
  coverageLimit: 250000,
});

const generatePolicyNumber = () => {
  return `POL-FIRE-${Math.floor(1000 + Math.random() * 9000)}`;
};

const openIssueModal = async () => {
  newPolicyForm.value = {
    customerId: customers.value.length > 0 ? customers.value[0].id : '',
    policyNumber: generatePolicyNumber(),
    propertyAddress: '',
    coverageLimit: 250000,
  };
  issueError.value = '';
  issueSuccess.value = '';
  showIssueModal.value = true;
};

const handleIssuePolicy = async () => {
  issuingPolicy.value = true;
  issueError.value = '';
  issueSuccess.value = '';

  try {
    const res = await api.post('/policies', {
      customerId: parseInt(newPolicyForm.value.customerId),
      policyNumber: newPolicyForm.value.policyNumber,
      propertyAddress: newPolicyForm.value.propertyAddress,
      coverageLimit: parseFloat(newPolicyForm.value.coverageLimit),
    });

    issueSuccess.value = `Policy ${res.data.policyNumber} issued successfully!`;
    // Refresh policies
    await fetchDashboardData();
    setTimeout(() => {
      showIssueModal.value = false;
    }, 1200);
  } catch (err) {
    issueError.value = err.response?.data?.error || err.message || 'Failed to issue policy.';
  } finally {
    issuingPolicy.value = false;
  }
};

const fetchDashboardData = async () => {
  loading.value = true;
  error.value = '';
  try {
    const claimsRes = await api.get('/claims');
    allClaims.value = claimsRes.data;

    // Fetch policies (customer only sees their own; adjuster sees all)
    const policiesRes = await api.get('/policies');
    if (authStore.isCustomer) {
      policies.value = policiesRes.data.filter(p => p.customerEmail && p.customerEmail.toLowerCase() === authStore.email.toLowerCase());
    } else {
      policies.value = policiesRes.data;
    }

    // If adjuster, also fetch customers for policy issuance
    if (authStore.isAdjuster) {
      const custRes = await api.get('/customers');
      customers.value = custRes.data;
    }
  } catch (err) {
    error.value = err.response?.data?.error || 'Failed to load dashboard data.';
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchDashboardData();
});

// KPI Computations
const totalClaimsCount = computed(() => allClaims.value.length);
const pendingClaimsCount = computed(() => allClaims.value.filter(c => c.status === 'SUBMITTED').length);
const approvedClaimsCount = computed(() => allClaims.value.filter(c => c.status === 'APPROVED').length);
const rejectedClaimsCount = computed(() => allClaims.value.filter(c => c.status === 'REJECTED').length);
const unlivableCount = computed(() => allClaims.value.filter(c => !c.isLivable).length);

const totalDamagesAmount = computed(() => 
  allClaims.value.reduce((sum, c) => sum + (c.totalEstimatedDamage || 0), 0)
);

const totalCoverageLimit = computed(() =>
  policies.value.reduce((sum, p) => sum + (p.coverageLimit || 0), 0)
);

const approvalRate = computed(() => {
  const settled = approvedClaimsCount.value + rejectedClaimsCount.value;
  if (settled === 0) return 0;
  return Math.round((approvedClaimsCount.value / settled) * 100);
});

// Percentages for status bar
const submittedPercent = computed(() => totalClaimsCount.value ? (pendingClaimsCount.value / totalClaimsCount.value) * 100 : 0);
const approvedPercent = computed(() => totalClaimsCount.value ? (approvedClaimsCount.value / totalClaimsCount.value) * 100 : 0);
const rejectedPercent = computed(() => totalClaimsCount.value ? (rejectedClaimsCount.value / totalClaimsCount.value) * 100 : 0);

// Filtered Claims List
const filteredClaims = computed(() => {
  let list = allClaims.value;

  if (selectedFilter.value === 'SUBMITTED') {
    list = list.filter(c => c.status === 'SUBMITTED');
  } else if (selectedFilter.value === 'APPROVED') {
    list = list.filter(c => c.status === 'APPROVED');
  } else if (selectedFilter.value === 'REJECTED') {
    list = list.filter(c => c.status === 'REJECTED');
  } else if (selectedFilter.value === 'UNLIVABLE') {
    list = list.filter(c => !c.isLivable);
  }

  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase().trim();
    list = list.filter(c => 
      c.id.toString().includes(q) ||
      c.policyNumber.toLowerCase().includes(q) ||
      (c.claimantName && c.claimantName.toLowerCase().includes(q)) ||
      (c.propertyAddress && c.propertyAddress.toLowerCase().includes(q)) ||
      (c.causeOfFire && c.causeOfFire.toLowerCase().includes(q))
    );
  }

  return list;
});

const getStatusBadge = (status) => {
  switch (status) {
    case 'APPROVED':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200';
    case 'REJECTED':
      return 'bg-rose-50 text-rose-700 border-rose-200';
    case 'SUBMITTED':
      return 'bg-amber-50 text-amber-700 border-amber-200';
    default:
      return 'bg-slate-50 text-slate-700 border-slate-200';
  }
};
</script>

<template>
  <div class="space-y-8">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-slate-900 tracking-tight">
          {{ authStore.isAdjuster ? 'Claims Adjuster Command Center' : 'Customer Overview & Dashboard' }}
        </h1>
        <p class="text-sm text-slate-500 mt-0.5">
          {{ authStore.isAdjuster ? 'Review metrics, track claim queues, and issue coverage policies' : 'Monitor insurance policies, track submitted claim statuses, and file new reports' }}
        </p>
      </div>

      <div class="flex items-center space-x-3">
        <!-- Customer Button: File Claim -->
        <router-link
          v-if="authStore.isCustomer"
          to="/claims/new"
          class="inline-flex items-center justify-center px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold rounded-lg shadow-sm transition cursor-pointer"
        >
          <span class="mr-1.5 font-bold">+</span> File New Claim
        </router-link>

        <!-- Adjuster Button: Issue Policy -->
        <button
          v-if="authStore.isAdjuster"
          @click="openIssueModal"
          class="inline-flex items-center justify-center px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold rounded-lg shadow-sm transition cursor-pointer"
        >
          <span class="mr-1.5 font-bold">+</span> Issue Policy
        </button>
      </div>
    </div>

    <!-- Error Banner -->
    <div v-if="error" class="p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm">
      {{ error }}
    </div>

    <!-- Loading Skeleton / Indicator -->
    <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div v-for="i in 4" :key="i" class="h-28 bg-white rounded-xl border border-slate-200 animate-pulse"></div>
    </div>

    <!-- KPI Metric Cards Grid -->
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- Card 1: Total Claims -->
      <div class="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition">
        <div class="flex items-center justify-between text-slate-500 mb-2">
          <span class="text-xs font-semibold uppercase tracking-wider">Total Claims</span>
          <span class="text-xl">📋</span>
        </div>
        <div class="text-2xl font-bold text-slate-900">{{ totalClaimsCount }}</div>
        <div class="text-xs text-slate-400 mt-1">Total recorded incidents</div>
      </div>

      <!-- Card 2: Pending Reviews -->
      <div class="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition">
        <div class="flex items-center justify-between text-slate-500 mb-2">
          <span class="text-xs font-semibold uppercase tracking-wider">Pending Review</span>
          <span class="text-xl">⏳</span>
        </div>
        <div class="flex items-baseline space-x-2">
          <span class="text-2xl font-bold text-amber-600">{{ pendingClaimsCount }}</span>
          <span v-if="pendingClaimsCount > 0" class="text-[11px] font-semibold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
            Action required
          </span>
        </div>
        <div class="text-xs text-slate-400 mt-1">Awaiting adjuster assessment</div>
      </div>

      <!-- Card 3: Settled & Rate -->
      <div class="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition">
        <div class="flex items-center justify-between text-slate-500 mb-2">
          <span class="text-xs font-semibold uppercase tracking-wider">Approved / Rate</span>
          <span class="text-xl">✅</span>
        </div>
        <div class="flex items-baseline space-x-2">
          <span class="text-2xl font-bold text-emerald-600">{{ approvedClaimsCount }}</span>
          <span class="text-xs font-medium text-slate-500">({{ approvalRate }}% settled rate)</span>
        </div>
        <div class="text-xs text-slate-400 mt-1">{{ rejectedClaimsCount }} claim(s) rejected</div>
      </div>

      <!-- Card 4: Financial Exposure / Damage Value -->
      <div class="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition">
        <div class="flex items-center justify-between text-slate-500 mb-2">
          <span class="text-xs font-semibold uppercase tracking-wider">Total Claim Value</span>
          <span class="text-xl">💰</span>
        </div>
        <div class="text-2xl font-bold text-slate-900">${{ totalDamagesAmount.toLocaleString() }}</div>
        <div v-if="authStore.isCustomer && totalCoverageLimit > 0" class="text-xs text-blue-600 mt-1 font-medium">
          Out of ${{ totalCoverageLimit.toLocaleString() }} coverage
        </div>
        <div v-else class="text-xs text-slate-400 mt-1">
          Property & content damages
        </div>
      </div>
    </div>

    <!-- Visual Status Distribution Bar -->
    <div v-if="totalClaimsCount > 0" class="bg-white p-5 rounded-xl border border-slate-200 shadow-xs space-y-3">
      <div class="flex items-center justify-between text-xs font-semibold text-slate-700">
        <span>Claims Lifecycle Distribution</span>
        <div class="flex items-center space-x-4">
          <span class="flex items-center space-x-1.5">
            <span class="w-2.5 h-2.5 rounded-full bg-amber-500"></span>
            <span class="text-slate-600">Submitted: {{ pendingClaimsCount }}</span>
          </span>
          <span class="flex items-center space-x-1.5">
            <span class="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
            <span class="text-slate-600">Approved: {{ approvedClaimsCount }}</span>
          </span>
          <span class="flex items-center space-x-1.5">
            <span class="w-2.5 h-2.5 rounded-full bg-rose-500"></span>
            <span class="text-slate-600">Rejected: {{ rejectedClaimsCount }}</span>
          </span>
          <span v-if="unlivableCount > 0" class="flex items-center space-x-1.5 text-rose-600 font-bold">
            <span class="w-2.5 h-2.5 rounded-full bg-rose-600"></span>
            <span>Unlivable: {{ unlivableCount }}</span>
          </span>
        </div>
      </div>

      <!-- Multi-segment progress bar -->
      <div class="w-full h-3 bg-slate-100 rounded-full overflow-hidden flex">
        <div
          :style="{ width: `${submittedPercent}%` }"
          class="bg-amber-400 transition-all duration-500"
          title="Submitted"
        ></div>
        <div
          :style="{ width: `${approvedPercent}%` }"
          class="bg-emerald-500 transition-all duration-500"
          title="Approved"
        ></div>
        <div
          :style="{ width: `${rejectedPercent}%` }"
          class="bg-rose-500 transition-all duration-500"
          title="Rejected"
        ></div>
      </div>
    </div>

    <!-- Active Policies Section -->
    <div v-if="policies.length > 0" class="space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="text-base font-bold text-slate-900">
          {{ authStore.isAdjuster ? 'Active Policies Registry' : 'Your Active Policies' }}
        </h2>
        <span class="text-xs text-slate-400 font-medium">{{ policies.length }} policy(ies)</span>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="policy in policies"
          :key="policy.id"
          class="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition"
        >
          <div class="flex justify-between items-start mb-2">
            <span class="text-xs font-bold text-blue-600 bg-blue-50 px-2 py-0.5 rounded">
              {{ policy.policyNumber }}
            </span>
            <span class="text-xs text-slate-400 font-mono">Limit: ${{ policy.coverageLimit.toLocaleString() }}</span>
          </div>
          <div class="text-sm font-semibold text-slate-800">{{ policy.propertyAddress }}</div>
          <div class="text-xs text-slate-500 mt-1">Holder: {{ policy.customerName }}</div>
        </div>
      </div>
    </div>

    <!-- Registered Claims Section with Search & Chips -->
    <div class="bg-white border border-slate-200 rounded-xl overflow-hidden shadow-xs">
      <div class="p-5 border-b border-slate-200 flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <h2 class="text-base font-bold text-slate-900">Registered Claims</h2>
          <div class="text-xs text-slate-500">Showing {{ filteredClaims.length }} of {{ allClaims.length }} claim(s)</div>
        </div>

        <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          <!-- Quick Search -->
          <div class="relative">
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Search claim, policy, cause..."
              class="w-full sm:w-64 pl-8 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-300 rounded-lg text-slate-800 focus:outline-none focus:ring-1 focus:ring-blue-500 focus:bg-white"
            />
            <span class="absolute left-2.5 top-2 text-slate-400 text-xs">🔍</span>
          </div>

          <!-- Filter Chips -->
          <div class="flex items-center space-x-1 bg-slate-100 p-1 rounded-lg text-xs font-medium text-slate-600">
            <button
              @click="selectedFilter = 'ALL'"
              :class="['px-2.5 py-1 rounded-md transition cursor-pointer', selectedFilter === 'ALL' ? 'bg-white text-slate-900 shadow-xs font-semibold' : 'hover:text-slate-900']"
            >
              All
            </button>
            <button
              @click="selectedFilter = 'SUBMITTED'"
              :class="['px-2.5 py-1 rounded-md transition cursor-pointer', selectedFilter === 'SUBMITTED' ? 'bg-white text-slate-900 shadow-xs font-semibold' : 'hover:text-slate-900']"
            >
              Pending
            </button>
            <button
              @click="selectedFilter = 'APPROVED'"
              :class="['px-2.5 py-1 rounded-md transition cursor-pointer', selectedFilter === 'APPROVED' ? 'bg-white text-slate-900 shadow-xs font-semibold' : 'hover:text-slate-900']"
            >
              Approved
            </button>
            <button
              @click="selectedFilter = 'REJECTED'"
              :class="['px-2.5 py-1 rounded-md transition cursor-pointer', selectedFilter === 'REJECTED' ? 'bg-white text-slate-900 shadow-xs font-semibold' : 'hover:text-slate-900']"
            >
              Rejected
            </button>
            <button
              v-if="unlivableCount > 0"
              @click="selectedFilter = 'UNLIVABLE'"
              :class="['px-2.5 py-1 rounded-md transition cursor-pointer', selectedFilter === 'UNLIVABLE' ? 'bg-rose-50 text-rose-700 shadow-xs font-semibold' : 'hover:text-rose-700']"
            >
              🚨 Urgent ({{ unlivableCount }})
            </button>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="filteredClaims.length === 0" class="p-12 text-center">
        <div class="text-3xl mb-2">📂</div>
        <div class="text-slate-800 font-semibold">No claims match your filter</div>
        <p class="text-slate-400 text-xs mt-1">Try adjusting your filter chips or search keywords.</p>
      </div>

      <!-- Table View -->
      <div v-else class="overflow-x-auto">
        <table class="w-full text-left text-sm">
          <thead class="bg-slate-50 text-slate-600 text-xs uppercase font-semibold border-b border-slate-200">
            <tr>
              <th class="px-5 py-3">Claim ID</th>
              <th class="px-5 py-3">Policy Number</th>
              <th class="px-5 py-3">Claimant / Property</th>
              <th class="px-5 py-3">Cause & Conditions</th>
              <th class="px-5 py-3">Total Damage</th>
              <th class="px-5 py-3">Status</th>
              <th class="px-5 py-3 text-right">Action</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr
              v-for="claim in filteredClaims"
              :key="claim.id"
              class="hover:bg-slate-50/75 transition"
            >
              <td class="px-5 py-3.5 font-mono font-medium text-slate-900">#{{ claim.id }}</td>
              <td class="px-5 py-3.5 font-medium text-blue-600">{{ claim.policyNumber }}</td>
              <td class="px-5 py-3.5">
                <div class="font-medium text-slate-900">{{ claim.claimantName }}</div>
                <div class="text-xs text-slate-400 truncate max-w-xs">{{ claim.propertyAddress }}</div>
              </td>
              <td class="px-5 py-3.5">
                <div class="text-slate-800">{{ claim.causeOfFire }}</div>
                <div v-if="!claim.isLivable" class="mt-0.5">
                  <span class="text-[10px] bg-rose-50 text-rose-700 border border-rose-200 font-bold px-1.5 py-0.5 rounded">
                    🚨 Unlivable Property
                  </span>
                </div>
              </td>
              <td class="px-5 py-3.5 font-medium text-slate-900">
                ${{ (claim.totalEstimatedDamage || 0).toLocaleString() }}
              </td>
              <td class="px-5 py-3.5">
                <span
                  :class="[
                    'text-xs px-2.5 py-1 rounded-full font-semibold border inline-block',
                    getStatusBadge(claim.status)
                  ]"
                >
                  {{ claim.status }}
                </span>
              </td>
              <td class="px-5 py-3.5 text-right">
                <router-link
                  :to="`/claims/${claim.id}`"
                  class="text-xs font-semibold text-blue-600 hover:text-blue-800 bg-blue-50 hover:bg-blue-100 px-3 py-1.5 rounded-md transition"
                >
                  View Details &rarr;
                </router-link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Issue Policy Modal for Adjusters -->
    <div
      v-if="showIssueModal"
      class="fixed inset-0 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4 z-50"
    >
      <div class="bg-white rounded-2xl max-w-lg w-full p-6 sm:p-8 shadow-xl border border-slate-200 space-y-6">
        <div class="flex items-center justify-between border-b border-slate-100 pb-4">
          <div>
            <h3 class="text-lg font-bold text-slate-900">Issue New Fire Policy</h3>
            <p class="text-xs text-slate-500">Create and bind coverage to a registered customer account</p>
          </div>
          <button
            @click="showIssueModal = false"
            class="text-slate-400 hover:text-slate-600 font-bold text-lg cursor-pointer"
          >
            &times;
          </button>
        </div>

        <div v-if="issueSuccess" class="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-lg">
          {{ issueSuccess }}
        </div>
        <div v-if="issueError" class="p-3 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-lg">
          {{ issueError }}
        </div>

        <form @submit.prevent="handleIssuePolicy" class="space-y-4 text-sm">
          <div>
            <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Customer Holder</label>
            <select
              v-model="newPolicyForm.customerId"
              required
              class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option v-for="c in customers" :key="c.id" :value="c.id">
                {{ c.name }} ({{ c.email }})
              </option>
            </select>
          </div>

          <div>
            <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Policy Number</label>
            <input
              v-model="newPolicyForm.policyNumber"
              type="text"
              required
              class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Covered Property Address</label>
            <input
              v-model="newPolicyForm.propertyAddress"
              type="text"
              required
              placeholder="e.g. 100 Main St, Suite 4, Springfield"
              class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Coverage Limit ($)</label>
            <input
              v-model="newPolicyForm.coverageLimit"
              type="number"
              min="1000"
              step="1000"
              required
              class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div class="flex justify-end space-x-3 pt-3">
            <button
              type="button"
              @click="showIssueModal = false"
              class="px-4 py-2 border border-slate-300 rounded-lg text-slate-700 text-xs font-semibold hover:bg-slate-50 cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              :disabled="issuingPolicy"
              class="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold shadow-sm transition disabled:opacity-50 cursor-pointer"
            >
              {{ issuingPolicy ? 'Issuing...' : 'Confirm Policy Issuance' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
