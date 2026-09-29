<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import api from '../api/client';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const claimId = route.params.id;
const claim = ref(null);
const loading = ref(true);
const error = ref('');

// Adjuster Review state
const reviewStatus = ref('APPROVED');
const reviewerNotes = ref('');
const reviewing = ref(false);
const reviewSuccess = ref(false);
const reviewError = ref('');

// Extra attachment state
const uploadFile = ref(null);
const uploading = ref(false);
const uploadError = ref('');
const uploadSuccess = ref(false);

// Lightbox Modal state
const previewImage = ref(null);

const openLightbox = (attachment) => {
  previewImage.value = attachment;
};

const closeLightbox = () => {
  previewImage.value = null;
};

const handleKeyDown = (e) => {
  if (e.key === 'Escape') {
    closeLightbox();
  }
};

onMounted(() => {
  fetchClaim();
  window.addEventListener('keydown', handleKeyDown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown);
});

const fetchClaim = async () => {
  loading.value = true;
  error.value = '';
  try {
    const res = await api.get(`/claims/${claimId}`);
    claim.value = res.data;
    if (claim.value.reviewerNotes) {
      reviewerNotes.value = claim.value.reviewerNotes;
    }
  } catch (err) {
    error.value = err.response?.data?.error || 'Failed to load claim details.';
  } finally {
    loading.value = false;
  }
};

const handleReview = async () => {
  reviewing.value = true;
  reviewError.value = '';
  reviewSuccess.value = false;
  try {
    const res = await api.put(`/claims/${claimId}/review`, {
      status: reviewStatus.value,
      reviewerNotes: reviewerNotes.value,
    });
    claim.value = res.data;
    reviewSuccess.value = true;
  } catch (err) {
    reviewError.value = err.response?.data?.error || 'Failed to review claim.';
  } finally {
    reviewing.value = false;
  }
};

const handleUploadAttachment = async () => {
  if (!uploadFile.value) return;
  uploading.value = true;
  uploadError.value = '';
  uploadSuccess.value = false;
  try {
    const formData = new FormData();
    formData.append('file', uploadFile.value);
    await api.post(`/claims/${claimId}/attachments`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    uploadSuccess.value = true;
    uploadFile.value = null;
    await fetchClaim();
  } catch (err) {
    uploadError.value = err.response?.data?.error || 'Failed to upload attachment.';
  } finally {
    uploading.value = false;
  }
};

const onAttachmentChange = (e) => {
  uploadFile.value = e.target.files[0] || null;
};

const isImage = (att) => {
  if (!att) return false;
  if (att.fileType && att.fileType.startsWith('image/')) return true;
  const ext = att.fileName ? att.fileName.split('.').pop().toLowerCase() : '';
  return ['jpg', 'jpeg', 'png', 'webp', 'gif', 'bmp', 'svg'].includes(ext);
};

const isPdf = (att) => {
  if (!att) return false;
  if (att.fileType && att.fileType.includes('pdf')) return true;
  return att.fileName && att.fileName.toLowerCase().endsWith('.pdf');
};

const formatSize = (bytes) => {
  if (!bytes) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
};

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

const triggerPrint = () => {
  window.print();
};
</script>

<template>
  <div>
    <!-- Interactive Web View (Hidden on Print) -->
    <div class="space-y-6 max-w-4xl mx-auto print:hidden">
      <!-- Top Navigation & Action Bar -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <router-link to="/dashboard" class="text-sm text-slate-500 hover:text-slate-800 transition flex items-center space-x-1">
          <span>&larr;</span>
          <span>Back to Dashboard</span>
        </router-link>

        <div class="flex items-center space-x-3">
          <button
            v-if="claim"
            @click="triggerPrint"
            class="inline-flex items-center space-x-1.5 px-3.5 py-1.5 bg-white hover:bg-slate-50 border border-slate-300 text-slate-700 font-semibold text-xs rounded-lg shadow-2xs transition cursor-pointer"
          >
            <span>🖨️</span>
            <span>Download Claim Report (PDF)</span>
          </button>

          <span
            v-if="claim"
            :class="[
              'text-xs px-3 py-1.5 rounded-full font-bold border inline-block tracking-wide',
              getStatusBadge(claim.status)
            ]"
          >
            {{ claim.status }}
          </span>
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="bg-white p-12 text-center rounded-2xl border border-slate-200 text-slate-400">
        Loading claim details...
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="p-6 bg-red-50 border border-red-200 text-red-700 rounded-2xl">
        {{ error }}
      </div>

      <div v-else class="space-y-6">
        <!-- Visual Claim Lifecycle Stepper -->
        <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs">
          <div class="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-4">Claim Settlement Lifecycle</div>
          <div class="grid grid-cols-4 gap-2 relative">
            <!-- Step 1: Filed -->
            <div class="text-center space-y-1">
              <div class="w-8 h-8 rounded-full bg-emerald-500 text-white flex items-center justify-center font-bold text-xs mx-auto shadow-xs">
                ✓
              </div>
              <div class="text-xs font-bold text-slate-900">Incident Filed</div>
              <div class="text-[10px] text-slate-400 hidden sm:block">Report submitted</div>
            </div>

            <!-- Step 2: Evidence -->
            <div class="text-center space-y-1">
              <div :class="['w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs mx-auto shadow-xs', (claim.attachments && claim.attachments.length > 0) ? 'bg-emerald-500 text-white' : 'bg-slate-100 text-slate-400']">
                {{ (claim.attachments && claim.attachments.length > 0) ? '✓' : '2' }}
              </div>
              <div class="text-xs font-bold text-slate-900">Evidence Intake</div>
              <div class="text-[10px] text-slate-400 hidden sm:block">{{ claim.attachments ? claim.attachments.length : 0 }} document(s)</div>
            </div>

            <!-- Step 3: Review -->
            <div class="text-center space-y-1">
              <div :class="['w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs mx-auto shadow-xs', claim.status === 'SUBMITTED' ? 'bg-amber-500 text-white animate-pulse' : 'bg-emerald-500 text-white']">
                {{ claim.status === 'SUBMITTED' ? '⏳' : '✓' }}
              </div>
              <div class="text-xs font-bold text-slate-900">Adjuster Review</div>
              <div class="text-[10px] text-slate-400 hidden sm:block">Coverage audit</div>
            </div>

            <!-- Step 4: Final Settlement -->
            <div class="text-center space-y-1">
              <div :class="['w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs mx-auto shadow-xs', claim.status === 'APPROVED' ? 'bg-emerald-600 text-white' : claim.status === 'REJECTED' ? 'bg-rose-600 text-white' : 'bg-slate-100 text-slate-400']">
                {{ claim.status === 'APPROVED' ? '💰' : claim.status === 'REJECTED' ? '✕' : '4' }}
              </div>
              <div class="text-xs font-bold text-slate-900">
                {{ claim.status === 'APPROVED' ? 'Approved' : claim.status === 'REJECTED' ? 'Rejected' : 'Settlement' }}
              </div>
              <div class="text-[10px] text-slate-400 hidden sm:block">Final decision</div>
            </div>
          </div>
        </div>

        <!-- Main Overview Card -->
        <div class="bg-white p-6 sm:p-8 rounded-2xl border border-slate-200 shadow-xs space-y-6">
          <div class="border-b border-slate-100 pb-5 flex flex-col sm:flex-row justify-between sm:items-center gap-2">
            <div>
              <span class="text-xs font-mono font-bold text-blue-600 bg-blue-50 px-2.5 py-1 rounded">
                CLAIM #{{ claim.id }}
              </span>
              <h1 class="text-xl font-bold text-slate-900 mt-2">Policy: {{ claim.policyNumber }}</h1>
              <p class="text-xs text-slate-500">Submitted on: {{ new Date(claim.submissionDate).toLocaleString() }}</p>
            </div>
            <div class="text-right sm:border-l sm:border-slate-100 sm:pl-6">
              <div class="text-xs text-slate-400 uppercase tracking-wider font-semibold">Total Damage</div>
              <div class="text-2xl font-bold text-slate-900">${{ (claim.totalEstimatedDamage || 0).toLocaleString() }}</div>
            </div>
          </div>

          <!-- Detail Grid -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm">
            <div>
              <h3 class="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Claimant & Property</h3>
              <p class="font-semibold text-slate-800">{{ claim.claimantName }}</p>
              <p class="text-slate-600 mt-1">{{ claim.propertyAddress }}</p>
            </div>

            <div>
              <h3 class="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Incident Information</h3>
              <p class="text-slate-700"><span class="font-semibold">Cause of Fire:</span> {{ claim.causeOfFire }}</p>
              <p class="text-slate-700 mt-1">
                <span class="font-semibold">Property Livable:</span>
                <span :class="claim.isLivable ? 'text-emerald-600' : 'text-rose-600 font-semibold'">
                  {{ claim.isLivable ? 'Yes (Occupied)' : 'No (Requires Emergency Relocation)' }}
                </span>
              </p>
            </div>

            <div>
              <h3 class="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Damage Breakdown</h3>
              <p class="text-slate-600">Property Damage: <span class="font-semibold text-slate-800">${{ (claim.estimatedPropertyDamage || 0).toLocaleString() }}</span></p>
              <p class="text-slate-600 mt-1">Content / Belongings: <span class="font-semibold text-slate-800">${{ (claim.estimatedContentDamage || 0).toLocaleString() }}</span></p>
            </div>

            <div>
              <h3 class="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">Reviewer Feedback</h3>
              <p v-if="claim.reviewerNotes" class="text-slate-700 italic bg-slate-50 p-3 rounded-lg border border-slate-100">
                "{{ claim.reviewerNotes }}"
              </p>
              <p v-else class="text-slate-400 text-xs italic">No reviewer notes recorded yet.</p>
            </div>
          </div>
        </div>

        <!-- Attachments & Evidence Section -->
        <div class="bg-white p-6 sm:p-8 rounded-2xl border border-slate-200 shadow-xs space-y-5">
          <div class="flex items-center justify-between">
            <div>
              <h2 class="text-base font-bold text-slate-900">Incident Evidence & Damage Photos</h2>
              <p class="text-xs text-slate-500">Photographs and official documents submitted as proof of loss.</p>
            </div>
            <span class="text-xs bg-slate-100 text-slate-600 font-semibold px-2.5 py-1 rounded-full">
              {{ claim.attachments ? claim.attachments.length : 0 }} file(s)
            </span>
          </div>

          <!-- Upload Form for Customer -->
          <div v-if="authStore.isCustomer && claim.status === 'SUBMITTED'" class="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-3">
            <div class="text-xs font-semibold text-slate-700">Upload Additional Evidence (Photos, Invoices, Fire Report)</div>
            <div class="flex flex-col sm:flex-row gap-3">
              <input
                type="file"
                @change="onAttachmentChange"
                class="text-xs text-slate-600 file:mr-3 file:py-1.5 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 cursor-pointer"
              />
              <button
                @click="handleUploadAttachment"
                :disabled="!uploadFile || uploading"
                class="px-4 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-md text-xs font-semibold transition disabled:opacity-50 cursor-pointer"
              >
                {{ uploading ? 'Uploading...' : 'Upload Evidence' }}
              </button>
            </div>
            <div v-if="uploadSuccess" class="text-xs text-emerald-600">File uploaded successfully!</div>
            <div v-if="uploadError" class="text-xs text-rose-600">{{ uploadError }}</div>
          </div>

          <!-- Empty State -->
          <div v-if="!claim.attachments || claim.attachments.length === 0" class="text-sm text-slate-400 italic py-6 text-center border-2 border-dashed border-slate-100 rounded-xl">
            No photos or documents uploaded for this claim yet.
          </div>

          <!-- Rich Visual Attachments Grid -->
          <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <div
              v-for="att in claim.attachments"
              :key="att.id"
              class="group bg-slate-50 rounded-xl border border-slate-200 overflow-hidden hover:border-blue-400 hover:shadow-xs transition flex flex-col justify-between"
            >
              <!-- Image Thumbnail Area -->
              <div v-if="isImage(att)" class="relative h-44 bg-slate-100 overflow-hidden cursor-pointer" @click="openLightbox(att)">
                <img
                  :src="att.downloadUrl"
                  :alt="att.fileName"
                  class="w-full h-full object-cover group-hover:scale-105 transition duration-300"
                  loading="lazy"
                />
                <div class="absolute inset-0 bg-slate-900/30 opacity-0 group-hover:opacity-100 transition flex items-center justify-center text-white text-xs font-semibold backdrop-blur-2xs">
                  <span>🔍 Click to Preview</span>
                </div>
                <span class="absolute top-2 left-2 bg-blue-600/90 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-xs">
                  Photo
                </span>
              </div>

              <!-- Non-Image Document Thumbnail Area -->
              <div v-else class="h-32 bg-slate-100 flex flex-col items-center justify-center p-4 text-center">
                <span class="text-3xl mb-1">{{ isPdf(att) ? '📕' : '📄' }}</span>
                <span class="text-xs font-semibold text-slate-600 uppercase tracking-wider">
                  {{ isPdf(att) ? 'PDF Document' : 'Attachment' }}
                </span>
              </div>

              <!-- File Details & Action Footer -->
              <div class="p-3 bg-white border-t border-slate-200 space-y-2">
                <div>
                  <div class="text-xs font-semibold text-slate-800 truncate" :title="att.fileName">
                    {{ att.fileName }}
                  </div>
                  <div class="text-[11px] text-slate-400">{{ formatSize(att.fileSize) }}</div>
                </div>

                <div class="flex items-center space-x-2 pt-1">
                  <button
                    v-if="isImage(att)"
                    @click="openLightbox(att)"
                    class="flex-1 text-center text-xs font-semibold text-blue-600 hover:text-blue-800 bg-blue-50 hover:bg-blue-100 py-1.5 rounded-lg transition cursor-pointer"
                  >
                    Preview
                  </button>
                  <a
                    :href="att.downloadUrl"
                    target="_blank"
                    download
                    class="flex-1 text-center text-xs font-semibold text-slate-700 hover:text-slate-900 bg-slate-100 hover:bg-slate-200 py-1.5 rounded-lg transition"
                  >
                    Download
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Adjuster Review Action Card -->
        <div
          v-if="authStore.isAdjuster && claim.status === 'SUBMITTED'"
          class="bg-white p-6 sm:p-8 rounded-2xl border-2 border-indigo-100 shadow-xs space-y-4"
        >
          <div class="flex items-center space-x-2">
            <span class="text-lg">⚖️</span>
            <h2 class="text-base font-bold text-slate-900">Adjuster Claim Review</h2>
          </div>
          <p class="text-xs text-slate-500">
            Make a final settlement decision on this claim. Inspect all damage photos above before approving payout.
          </p>

          <div v-if="reviewSuccess" class="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm rounded-lg">
            Review submitted successfully!
          </div>
          <div v-if="reviewError" class="p-3 bg-rose-50 border border-rose-200 text-rose-700 text-sm rounded-lg">
            {{ reviewError }}
          </div>

          <form @submit.prevent="handleReview" class="space-y-4">
            <div>
              <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Decision</label>
              <div class="flex space-x-4">
                <label class="flex items-center space-x-2 text-sm text-slate-700 cursor-pointer">
                  <input type="radio" value="APPROVED" v-model="reviewStatus" class="text-emerald-600 focus:ring-emerald-500" />
                  <span class="font-medium text-emerald-700">Approve Claim</span>
                </label>
                <label class="flex items-center space-x-2 text-sm text-slate-700 cursor-pointer">
                  <input type="radio" value="REJECTED" v-model="reviewStatus" class="text-rose-600 focus:ring-rose-500" />
                  <span class="font-medium text-rose-700">Reject Claim</span>
                </label>
              </div>
            </div>

            <div>
              <label class="block text-xs font-semibold text-slate-700 mb-1 uppercase tracking-wider">Reviewer Notes</label>
              <textarea
                v-model="reviewerNotes"
                rows="3"
                required
                placeholder="Provide justification, contractor review notes, or settlement explanations..."
                class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition"
              ></textarea>
            </div>

            <div class="flex justify-end">
              <button
                type="submit"
                :disabled="reviewing"
                class="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-lg text-sm shadow-sm transition disabled:opacity-50 cursor-pointer"
              >
                {{ reviewing ? 'Submitting Review...' : 'Submit Decision' }}
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Image Lightbox Modal -->
      <div
        v-if="previewImage"
        class="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-xs flex items-center justify-center p-4 sm:p-6"
        @click.self="closeLightbox"
      >
        <div class="relative max-w-4xl w-full bg-slate-900 rounded-2xl overflow-hidden shadow-2xl border border-slate-800 flex flex-col">
          <!-- Lightbox Header -->
          <div class="p-4 bg-slate-900/90 border-b border-slate-800 flex items-center justify-between text-white">
            <div class="flex items-center space-x-2 overflow-hidden">
              <span class="text-lg">📷</span>
              <div class="truncate text-sm font-semibold">{{ previewImage.fileName }}</div>
            </div>
            <div class="flex items-center space-x-3">
              <a
                :href="previewImage.downloadUrl"
                target="_blank"
                download
                class="text-xs bg-slate-800 hover:bg-slate-700 text-slate-200 px-3 py-1.5 rounded-lg transition"
              >
                Download
              </a>
              <button
                @click="closeLightbox"
                class="text-slate-400 hover:text-white font-bold text-xl px-2 cursor-pointer transition"
              >
                &times;
              </button>
            </div>
          </div>

          <!-- Full-Size Image Preview Area -->
          <div class="p-4 flex items-center justify-center bg-black/60 max-h-[75vh] overflow-auto">
            <img
              :src="previewImage.downloadUrl"
              :alt="previewImage.fileName"
              class="max-w-full max-h-[70vh] object-contain rounded-lg shadow-lg"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- ======================================================== -->
    <!-- Official Printable Claim Report (Rendered ONLY on Print) -->
    <!-- ======================================================== -->
    <div v-if="claim" class="hidden print:block font-serif text-slate-900 p-8 max-w-4xl mx-auto space-y-8 bg-white">
      <!-- Report Letterhead Header -->
      <div class="border-b-2 border-slate-900 pb-5 flex justify-between items-start">
        <div>
          <div class="flex items-center space-x-2">
            <span class="text-2xl">🛡️</span>
            <span class="text-2xl font-bold tracking-tight text-slate-900 font-sans">SafeHaven Insurance Co.</span>
          </div>
          <div class="text-xs text-slate-500 font-sans mt-1">National Casualty Consortium • NAIC #98721 • 24/7 Hotline: 1-800-555-FIRE</div>
          <div class="text-xs text-slate-500 font-sans">Underwriting & Claims Adjudication Division</div>
        </div>
        <div class="text-right font-sans">
          <div class="text-xs uppercase tracking-widest text-slate-400 font-bold">Official Document</div>
          <div class="text-lg font-bold text-slate-900">Proof of Loss & Settlement Audit</div>
          <div class="text-xs text-slate-500 mt-1">Generated: {{ new Date().toLocaleDateString() }}</div>
        </div>
      </div>

      <!-- Case Reference Ribbon -->
      <div class="bg-slate-100 p-4 rounded-lg flex justify-between items-center font-sans text-xs">
        <div>
          <span class="font-bold text-slate-700 uppercase tracking-wider">Claim Reference ID:</span>
          <span class="font-mono font-bold text-blue-800 ml-2">#CLM-{{ claim.id.toString().padStart(6, '0') }}</span>
        </div>
        <div>
          <span class="font-bold text-slate-700 uppercase tracking-wider">Policy Number:</span>
          <span class="font-mono font-bold text-slate-900 ml-2">{{ claim.policyNumber }}</span>
        </div>
        <div>
          <span class="font-bold text-slate-700 uppercase tracking-wider">Adjudication Status:</span>
          <span class="font-bold ml-2 px-2 py-0.5 rounded border border-slate-300">{{ claim.status }}</span>
        </div>
      </div>

      <!-- Section A: Policyholder & Property Information -->
      <div class="space-y-3 font-sans">
        <h3 class="text-xs font-bold uppercase tracking-wider border-b border-slate-200 pb-1 text-slate-800">
          Section A: Policyholder & Property Details
        </h3>
        <div class="grid grid-cols-2 gap-4 text-xs">
          <div>
            <div class="text-slate-400 uppercase text-[10px] font-semibold">Primary Policyholder</div>
            <div class="font-bold text-slate-900 text-sm mt-0.5">{{ claim.claimantName }}</div>
          </div>
          <div>
            <div class="text-slate-400 uppercase text-[10px] font-semibold">Insured Property Location</div>
            <div class="font-bold text-slate-900 text-sm mt-0.5">{{ claim.propertyAddress }}</div>
          </div>
        </div>
      </div>

      <!-- Section B: Incident Statement & Conditions -->
      <div class="space-y-3 font-sans">
        <h3 class="text-xs font-bold uppercase tracking-wider border-b border-slate-200 pb-1 text-slate-800">
          Section B: Incident Statement & Conditions
        </h3>
        <div class="grid grid-cols-2 gap-4 text-xs">
          <div>
            <div class="text-slate-400 uppercase text-[10px] font-semibold">Date of First Notice of Loss</div>
            <div class="text-slate-800 font-medium mt-0.5">{{ new Date(claim.submissionDate).toLocaleString() }}</div>
          </div>
          <div>
            <div class="text-slate-400 uppercase text-[10px] font-semibold">Dwelling Habitability Status</div>
            <div class="font-bold mt-0.5" :class="claim.isLivable ? 'text-emerald-700' : 'text-rose-700'">
              {{ claim.isLivable ? 'Habitable (Normal Occupancy)' : 'UNLIVABLE (Emergency Relocation Dispatched)' }}
            </div>
          </div>
          <div class="col-span-2">
            <div class="text-slate-400 uppercase text-[10px] font-semibold">Reported Cause of Fire</div>
            <div class="p-3 bg-slate-50 border border-slate-200 rounded text-slate-800 mt-1 italic">
              "{{ claim.causeOfFire }}"
            </div>
          </div>
        </div>
      </div>

      <!-- Section C: Itemized Damage & Payout Schedule -->
      <div class="space-y-3 font-sans">
        <h3 class="text-xs font-bold uppercase tracking-wider border-b border-slate-200 pb-1 text-slate-800">
          Section C: Itemized Damage Assessment
        </h3>
        <table class="w-full text-left text-xs border border-slate-200">
          <thead class="bg-slate-100 text-slate-700 font-semibold border-b border-slate-200 uppercase text-[10px]">
            <tr>
              <th class="p-2.5">Coverage Category</th>
              <th class="p-2.5">Damage Description</th>
              <th class="p-2.5 text-right">Claimed Valuation ($)</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 text-slate-800">
            <tr>
              <td class="p-2.5 font-bold">Dwelling Structure (Coverage A)</td>
              <td class="p-2.5 text-slate-500">Physical rebuild, framing, walls, roofing, and utilities</td>
              <td class="p-2.5 text-right font-mono font-medium">${{ (claim.estimatedPropertyDamage || 0).toLocaleString() }}</td>
            </tr>
            <tr>
              <td class="p-2.5 font-bold">Personal Property (Coverage C)</td>
              <td class="p-2.5 text-slate-500">Furniture, appliances, electronics, clothing, and interior effects</td>
              <td class="p-2.5 text-right font-mono font-medium">${{ (claim.estimatedContentDamage || 0).toLocaleString() }}</td>
            </tr>
            <tr class="bg-slate-50 font-bold text-slate-900 border-t-2 border-slate-300">
              <td class="p-2.5" colspan="2">TOTAL LOSS EVALUATION</td>
              <td class="p-2.5 text-right font-mono text-sm">${{ (claim.totalEstimatedDamage || 0).toLocaleString() }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Section D: Evidence & Supporting Files Audit Log -->
      <div class="space-y-3 font-sans">
        <h3 class="text-xs font-bold uppercase tracking-wider border-b border-slate-200 pb-1 text-slate-800">
          Section D: Supporting Evidence Audit Log
        </h3>
        <div v-if="!claim.attachments || claim.attachments.length === 0" class="text-xs text-slate-400 italic">
          No external photographic or document attachments registered in audit trail.
        </div>
        <table v-else class="w-full text-left text-xs border border-slate-200">
          <thead class="bg-slate-100 text-slate-700 font-semibold border-b border-slate-200 uppercase text-[10px]">
            <tr>
              <th class="p-2">Attachment ID</th>
              <th class="p-2">Document Name</th>
              <th class="p-2">File Format</th>
              <th class="p-2 text-right">File Size</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100 text-slate-700">
            <tr v-for="att in claim.attachments" :key="att.id">
              <td class="p-2 font-mono">#ATT-{{ att.id }}</td>
              <td class="p-2 font-semibold text-slate-900">{{ att.fileName }}</td>
              <td class="p-2 uppercase text-[11px]">{{ att.fileType }}</td>
              <td class="p-2 text-right font-mono">{{ formatSize(att.fileSize) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Section E: Examiner Determination & Authorization -->
      <div class="space-y-4 font-sans border-t-2 border-slate-300 pt-4">
        <h3 class="text-xs font-bold uppercase tracking-wider text-slate-800">
          Section E: Examiner Findings & Settlement Certification
        </h3>
        
        <div class="p-3 bg-slate-50 border border-slate-200 rounded text-xs space-y-1">
          <div class="font-bold text-slate-800">Official Examiner Notes:</div>
          <div class="text-slate-700 italic">
            {{ claim.reviewerNotes || 'Claim evaluation is in progress pending inspection and underwriter assessment.' }}
          </div>
        </div>

        <div class="grid grid-cols-2 gap-12 pt-6 text-xs">
          <div class="space-y-2">
            <div class="border-b border-slate-400 h-8"></div>
            <div class="text-slate-600 font-semibold">Authorized Claims Examiner Signature</div>
            <div class="text-slate-400 text-[10px]">SafeHaven Special Investigation & Settlement Unit</div>
          </div>
          <div class="space-y-2">
            <div class="border-b border-slate-400 h-8"></div>
            <div class="text-slate-600 font-semibold">Policyholder Acknowledgment</div>
            <div class="text-slate-400 text-[10px]">Signature signifies affirmation of true and accurate statement</div>
          </div>
        </div>
      </div>

      <!-- Legal Regulatory Footnote -->
      <div class="border-t border-slate-200 pt-4 text-[10px] text-slate-400 font-sans leading-normal">
        CONFIDENTIAL & PROPRIETARY. This official settlement evaluation is issued by SafeHaven Insurance Underwriting Inc. pursuant to state insurance commission regulations and casualty policy contract terms. Any intentionally false or misleading statements in connection with an insurance claim constitute insurance fraud punishable under statutory penal codes.
      </div>
    </div>
  </div>
</template>
