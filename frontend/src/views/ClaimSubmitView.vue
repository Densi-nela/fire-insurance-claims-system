<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '../api/client';

const router = useRouter();

const policies = ref([]);
const loading = ref(false);
const submitting = ref(false);
const error = ref('');
const success = ref(false);

const form = ref({
  policyNumber: '',
  causeOfFire: '',
  estimatedPropertyDamage: null,
  estimatedContentDamage: null,
  isLivable: true,
});

const selectedFile = ref(null);

onMounted(async () => {
  loading.value = true;
  try {
    const res = await api.get('/policies');
    // Strictly filter policies to only show ones owned by the current customer
    const userEmail = localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')).email : '';
    policies.value = res.data.filter(p => p.customerEmail && p.customerEmail.toLowerCase() === userEmail.toLowerCase());
    if (policies.value.length > 0) {
      form.value.policyNumber = policies.value[0].policyNumber;
    }
  } catch (err) {
    error.value = 'Failed to load policy list.';
  } finally {
    loading.value = false;
  }
});

const handleFileChange = (e) => {
  const file = e.target.files[0];
  if (file) {
    selectedFile.value = file;
  }
};

const handleSubmit = async () => {
  submitting.value = true;
  error.value = '';

  if (!form.value.policyNumber) {
    error.value = 'Please select a valid policy.';
    submitting.value = false;
    return;
  }

  if (form.value.causeOfFire.trim().length < 10) {
    error.value = 'Cause of fire description must be at least 10 characters long (e.g. "Toaster caught fire in the kitchen").';
    submitting.value = false;
    return;
  }

  try {
    // 1. Submit claim payload
    const claimRes = await api.post('/claims', {
      policyNumber: form.value.policyNumber,
      causeOfFire: form.value.causeOfFire,
      estimatedPropertyDamage: parseFloat(form.value.estimatedPropertyDamage) || 0,
      estimatedContentDamage: parseFloat(form.value.estimatedContentDamage) || 0,
      isLivable: form.value.isLivable,
    });

    const newClaimId = claimRes.data.id;

    // 2. Upload attachment if selected
    if (selectedFile.value) {
      const formData = new FormData();
      formData.append('file', selectedFile.value);
      await api.post(`/claims/${newClaimId}/attachments`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
    }

    success.value = true;
    setTimeout(() => {
      router.push(`/claims/${newClaimId}`);
    }, 1200);
  } catch (err) {
    if (err.response?.data?.details) {
      const detailMessages = Object.values(err.response.data.details).join(' ');
      error.value = `Validation failed: ${detailMessages}`;
    } else {
      error.value = err.response?.data?.error || err.message || 'Failed to submit claim.';
    }
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <div class="max-w-2xl mx-auto space-y-6">
    <div class="flex items-center space-x-2 text-sm text-slate-500">
      <router-link to="/dashboard" class="hover:text-slate-800">&larr; Back to Dashboard</router-link>
    </div>

    <div class="bg-white p-6 sm:p-8 rounded-2xl border border-slate-200 shadow-xs">
      <h1 class="text-xl font-bold text-slate-900 tracking-tight mb-1">File a New Fire Claim</h1>
      <p class="text-xs text-slate-500 mb-6">Submit initial damage estimates and supporting evidence for rapid assessment.</p>

      <!-- Success Notification -->
      <div v-if="success" class="mb-5 p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm">
        ✨ Claim filed successfully! Redirecting to claim details...
      </div>

      <!-- Error Notification -->
      <div v-if="error" class="mb-5 p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-sm">
        {{ error }}
      </div>

      <!-- No Policies Warning Banner -->
      <div v-if="!loading && policies.length === 0" class="p-6 bg-amber-50 border border-amber-200 rounded-xl text-center space-y-3">
        <div class="text-2xl">⚠️</div>
        <div class="text-sm font-bold text-amber-900">No Active Insurance Policy Found</div>
        <p class="text-xs text-amber-700 max-w-md mx-auto">
          You do not currently have any active insurance policies linked to your account. Claims must be associated with an active policy. Please have an adjuster issue a policy first.
        </p>
        <router-link to="/dashboard" class="inline-block px-4 py-2 bg-amber-600 text-white rounded-lg text-xs font-semibold hover:bg-amber-700 transition">
          Return to Dashboard
        </router-link>
      </div>

      <form v-else @submit.prevent="handleSubmit" class="space-y-5">
        <!-- Policy Selection -->
        <div>
          <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Associated Policy</label>
          <select
            v-model="form.policyNumber"
            required
            class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
          >
            <option v-for="policy in policies" :key="policy.id" :value="policy.policyNumber">
              {{ policy.policyNumber }} - {{ policy.propertyAddress }} (Limit: ${{ policy.coverageLimit.toLocaleString() }})
            </option>
          </select>
        </div>

        <!-- Cause of Fire -->
        <div>
          <div class="flex justify-between items-center mb-1">
            <label class="block text-xs font-semibold text-slate-700 uppercase tracking-wider">Cause of Fire</label>
            <span class="text-[11px] text-slate-400">At least 10 characters</span>
          </div>
          <input
            v-model="form.causeOfFire"
            type="text"
            required
            minlength="10"
            placeholder="e.g., Electrical short circuit in kitchen behind stove"
            class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
          />
        </div>

        <!-- Damage Estimates -->
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Property Damage ($)</label>
            <input
              v-model="form.estimatedPropertyDamage"
              type="number"
              step="0.01"
              min="0"
              required
              placeholder="e.g. 5000"
              class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
            />
          </div>
          <div>
            <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Content / Items Damage ($)</label>
            <input
              v-model="form.estimatedContentDamage"
              type="number"
              step="0.01"
              min="0"
              required
              placeholder="e.g. 1500"
              class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
            />
          </div>
        </div>

        <!-- Livable Checkbox -->
        <div class="flex items-center space-x-3 pt-2">
          <input
            id="isLivable"
            v-model="form.isLivable"
            type="checkbox"
            class="w-4 h-4 text-blue-600 rounded border-slate-300 focus:ring-blue-500"
          />
          <label for="isLivable" class="text-sm font-medium text-slate-700">
            Is the property currently livable?
          </label>
        </div>

        <!-- Supporting Document / Photo Upload -->
        <div class="pt-3 border-t border-slate-100">
          <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Supporting File / Photo (Optional)</label>
          <input
            type="file"
            @change="handleFileChange"
            class="w-full px-3 py-2 border border-dashed border-slate-300 rounded-lg text-slate-500 text-sm cursor-pointer hover:bg-slate-50 transition"
          />
          <p class="text-[11px] text-slate-400 mt-1">Upload an image, PDF report, or quote.</p>
        </div>

        <!-- Submit Button -->
        <div class="pt-4 flex justify-end">
          <button
            type="submit"
            :disabled="submitting"
            class="px-6 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg shadow-sm transition disabled:opacity-50 text-sm cursor-pointer"
          >
            <span v-if="submitting">Submitting...</span>
            <span v-else>Submit Claim</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
