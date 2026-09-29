<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import api from '../api/client';

const router = useRouter();
const authStore = useAuthStore();

const step = ref(1);
const loading = ref(false);
const error = ref('');

const form = ref({
  propertyAddress: '',
  propertyType: 'Single Family Home',
  coverageLimit: 350000,
  planName: 'Premium',
  monthlyRate: 45,
  name: '',
  email: '',
  phoneNumber: '',
  password: ''
});

const selectPlan = (limit, name, rate) => {
  form.value.coverageLimit = limit;
  form.value.planName = name;
  form.value.monthlyRate = rate;
};

const nextStep = () => {
  error.value = '';
  if (step.value === 1 && !form.value.propertyAddress.trim()) {
    error.value = 'Please provide your property address.';
    return;
  }
  step.value++;
};

const prevStep = () => {
  error.value = '';
  step.value--;
};

const handleCompletePurchase = async () => {
  loading.value = true;
  error.value = '';

  try {
    const customerRes = await api.post('/customers', {
      name: form.value.name,
      email: form.value.email,
      phoneNumber: form.value.phoneNumber || null,
      role: 'CUSTOMER',
      password: form.value.password,
    });

    const newCustomerId = customerRes.data.id;
    await authStore.login(form.value.email, form.value.password);

    const policyNumber = `POL-FIRE-${Math.floor(1000 + Math.random() * 9000)}`;
    await api.post('/policies', {
      customerId: newCustomerId,
      policyNumber: policyNumber,
      propertyAddress: form.value.propertyAddress,
      coverageLimit: form.value.coverageLimit,
    });

    router.push('/dashboard');
  } catch (err) {
    if (err.response?.data?.error) {
      error.value = err.response.data.error;
    } else {
      error.value = 'Failed to activate coverage. Please try again.';
    }
  } finally {
    loading.value = false;
  }
};

// FAQ Accordion State
const activeFaq = ref(0);
const toggleFaq = (index) => {
  activeFaq.value = activeFaq.value === index ? null : index;
};

const faqs = [
  {
    q: 'How quickly is my policy active after purchasing?',
    a: 'Instantly. Once you submit the activation form, your policy document (POL-FIRE-XXXX) is bound, registered in our database, and active from that exact second.'
  },
  {
    q: 'Is my temporary hotel stay covered if a fire makes my home unlivable?',
    a: 'Yes. All plans include Loss of Use (Additional Living Expenses) protection. When you file a claim with "Property Livable: No", emergency accommodation and food allowances are dispatched immediately.'
  },
  {
    q: 'What documents are required to file a fire claim?',
    a: 'You can initiate a claim immediately with just damage estimates and incident details. You can also upload fire department incident reports, photos of affected rooms, and contractor quotes directly through the claims portal.'
  },
  {
    q: 'How fast are claim settlements paid out?',
    a: 'Small damages under $2,000 are automatically approved in seconds by our automated underwriting engine. Larger structure claims are reviewed by licensed adjusters within 24 to 48 hours.'
  },
  {
    q: 'Can I provide proof of insurance to my mortgage lender?',
    a: 'Yes. Your active policy number, covered property address, and coverage limit appear directly on your dashboard and meet standard Fannie Mae and Freddie Mac lender requirements.'
  }
];

const scrollToWizard = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' });
};
</script>

<template>
  <div class="space-y-20">
    <!-- Emergency Notification Ribbon -->
    <div class="bg-gradient-to-r from-red-600 to-amber-600 text-white px-4 py-2.5 rounded-xl shadow-xs flex flex-col sm:flex-row items-center justify-between gap-2 text-xs">
      <div class="flex items-center space-x-2">
        <span class="text-base">🚨</span>
        <span class="font-medium"><strong>Displaced by a fire emergency right now?</strong> Our 24/7 Rapid Response team is standing by.</span>
      </div>
      <div class="flex items-center space-x-3 shrink-0">
        <span class="font-mono font-bold bg-white/20 px-2 py-0.5 rounded">1-800-555-FIRE</span>
        <router-link to="/login" class="underline font-semibold hover:text-white/80">Log in to Report</router-link>
      </div>
    </div>

    <!-- Hero Header & Wizard Section -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-10 items-center">
      <!-- Left Hero Description -->
      <div class="lg:col-span-6 space-y-6 text-left">
        <div class="inline-flex items-center space-x-2 bg-blue-50 text-blue-700 px-3 py-1 rounded-full text-xs font-semibold">
          <span>🛡️ Certified Direct Fire Underwriter</span>
        </div>
        <h1 class="text-4xl sm:text-5xl lg:text-6xl font-extrabold text-slate-900 tracking-tight leading-tight">
          Next-Generation Fire Protection for Your Home.
        </h1>
        <p class="text-base sm:text-lg text-slate-600 leading-relaxed">
          Zero paperwork. Zero commission markups. Get a transparent quote in seconds, bind your official policy online, and enjoy peace of mind with 24/7 digital claim settlement.
        </p>

        <!-- Value Bullet Points -->
        <div class="space-y-3 pt-2 text-sm text-slate-700">
          <div class="flex items-center space-x-2.5">
            <span class="text-emerald-500 font-bold text-base">✓</span>
            <span><strong>Full Structure & Belongs:</strong> Rebuilding costs paid at modern replacement rates.</span>
          </div>
          <div class="flex items-center space-x-2.5">
            <span class="text-emerald-500 font-bold text-base">✓</span>
            <span><strong>Instant Mortgage Proof:</strong> Digital binders accepted by all major lenders.</span>
          </div>
          <div class="flex items-center space-x-2.5">
            <span class="text-emerald-500 font-bold text-base">✓</span>
            <span><strong>AI-Assisted Claims:</strong> Rapid payout triage and direct adjuster review desk.</span>
          </div>
        </div>

        <!-- Social Proof Stats -->
        <div class="grid grid-cols-3 gap-4 pt-6 border-t border-slate-200">
          <div>
            <div class="text-2xl font-bold text-slate-900">A+</div>
            <div class="text-xs text-slate-500 mt-0.5">AM Best Financial Rating</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-slate-900">99.2%</div>
            <div class="text-xs text-slate-500 mt-0.5">Claim Settlement Rate</div>
          </div>
          <div>
            <div class="text-2xl font-bold text-slate-900">&lt; 48h</div>
            <div class="text-xs text-slate-500 mt-0.5">Average Payout Review</div>
          </div>
        </div>
      </div>

      <!-- Right Interactive Wizard -->
      <div class="lg:col-span-6">
        <div class="bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden">
          <!-- Wizard Progress Bar -->
          <div class="bg-slate-50 border-b border-slate-200 p-4">
            <div class="flex items-center justify-between text-xs font-semibold text-slate-500 mb-2">
              <span :class="{'text-blue-600 font-bold': step === 1}">1. Property</span>
              <span :class="{'text-blue-600 font-bold': step === 2}">2. Coverage Plan</span>
              <span :class="{'text-blue-600 font-bold': step === 3}">3. Activate</span>
            </div>
            <div class="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden">
              <div 
                class="bg-blue-600 h-full transition-all duration-300"
                :style="{ width: step === 1 ? '33%' : step === 2 ? '66%' : '100%' }"
              ></div>
            </div>
          </div>

          <div class="p-6 sm:p-8">
            <!-- Error Banner -->
            <div v-if="error" class="mb-5 p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-start space-x-2">
              <span>⚠️</span>
              <span>{{ error }}</span>
            </div>

            <!-- Step 1: Property Info -->
            <div v-if="step === 1" class="space-y-4">
              <div>
                <h2 class="text-lg font-bold text-slate-900">Get Your Instant Quote</h2>
                <p class="text-xs text-slate-500 mt-0.5">Enter the address you wish to protect against fire and smoke damage.</p>
              </div>

              <div>
                <label class="block text-xs font-semibold text-slate-700 uppercase mb-1">Property Address</label>
                <input 
                  v-model="form.propertyAddress" 
                  type="text" 
                  placeholder="e.g. 742 Evergreen Terrace, Springfield" 
                  class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label class="block text-xs font-semibold text-slate-700 uppercase mb-1">Building Structure Type</label>
                <select 
                  v-model="form.propertyType" 
                  class="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="Single Family Home">Single Family Home</option>
                  <option value="Townhouse">Townhouse</option>
                  <option value="Apartment / Condo">Apartment / Condo</option>
                  <option value="Multi-Family Property">Multi-Family Residential</option>
                </select>
              </div>

              <div class="pt-3">
                <button 
                  @click="nextStep" 
                  class="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-semibold transition shadow-sm cursor-pointer"
                >
                  Calculate My Quote &rarr;
                </button>
              </div>
            </div>

            <!-- Step 2: Choose Coverage Plan -->
            <div v-if="step === 2" class="space-y-4">
              <div>
                <h2 class="text-lg font-bold text-slate-900">Select Your Coverage Tier</h2>
                <p class="text-xs text-slate-500 mt-0.5">All plans cover both structural damages and personal belongings.</p>
              </div>

              <div class="space-y-3">
                <!-- Standard Plan -->
                <div 
                  @click="selectPlan(150000, 'Standard', 25)"
                  :class="[
                    'p-4 rounded-xl border-2 cursor-pointer transition flex items-center justify-between',
                    form.coverageLimit === 150000 ? 'border-blue-600 bg-blue-50/40' : 'border-slate-200 hover:border-slate-300'
                  ]"
                >
                  <div>
                    <div class="font-bold text-slate-900 text-sm">Standard Protection</div>
                    <div class="text-xs text-slate-500 mt-0.5">$150,000 Total Coverage Limit</div>
                  </div>
                  <div class="text-right">
                    <div class="font-bold text-slate-900 text-base">$25<span class="text-xs font-normal text-slate-500">/mo</span></div>
                  </div>
                </div>

                <!-- Premium Plan -->
                <div 
                  @click="selectPlan(350000, 'Premium', 45)"
                  :class="[
                    'p-4 rounded-xl border-2 cursor-pointer transition flex items-center justify-between relative',
                    form.coverageLimit === 350000 ? 'border-blue-600 bg-blue-50/40' : 'border-slate-200 hover:border-slate-300'
                  ]"
                >
                  <span class="absolute -top-2.5 right-4 bg-blue-600 text-white text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider">
                    Recommended
                  </span>
                  <div>
                    <div class="font-bold text-slate-900 text-sm">Premium Protection</div>
                    <div class="text-xs text-slate-500 mt-0.5">$350,000 Total Coverage Limit</div>
                  </div>
                  <div class="text-right">
                    <div class="font-bold text-slate-900 text-base">$45<span class="text-xs font-normal text-slate-500">/mo</span></div>
                  </div>
                </div>

                <!-- Deluxe Plan -->
                <div 
                  @click="selectPlan(500000, 'Deluxe', 65)"
                  :class="[
                    'p-4 rounded-xl border-2 cursor-pointer transition flex items-center justify-between',
                    form.coverageLimit === 500000 ? 'border-blue-600 bg-blue-50/40' : 'border-slate-200 hover:border-slate-300'
                  ]"
                >
                  <div>
                    <div class="font-bold text-slate-900 text-sm">Deluxe High-Value</div>
                    <div class="text-xs text-slate-500 mt-0.5">$500,000 Complete Structural & Belongings</div>
                  </div>
                  <div class="text-right">
                    <div class="font-bold text-slate-900 text-base">$65<span class="text-xs font-normal text-slate-500">/mo</span></div>
                  </div>
                </div>
              </div>

              <div class="pt-3 flex justify-between items-center">
                <button 
                  @click="prevStep" 
                  class="text-xs text-slate-500 hover:text-slate-800 font-semibold cursor-pointer"
                >
                  &larr; Back
                </button>
                <button 
                  @click="nextStep" 
                  class="px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-semibold transition cursor-pointer"
                >
                  Confirm Plan &rarr;
                </button>
              </div>
            </div>

            <!-- Step 3: Account Creation & Activation -->
            <div v-if="step === 3" class="space-y-4">
              <div class="bg-blue-50 p-3 rounded-lg border border-blue-100 flex items-center justify-between text-xs">
                <div>
                  <span class="font-bold text-blue-900">{{ form.planName }} Plan</span>
                  <span class="text-blue-700"> (${{ form.coverageLimit.toLocaleString() }} limit)</span>
                </div>
                <div class="font-bold text-blue-900">${{ form.monthlyRate }}/mo</div>
              </div>

              <div>
                <h2 class="text-lg font-bold text-slate-900">Activate Policyholder Account</h2>
                <p class="text-xs text-slate-500 mt-0.5">Your official insurance policy document will be bound immediately.</p>
              </div>

              <form @submit.prevent="handleCompletePurchase" class="space-y-3">
                <div>
                  <label class="block text-xs font-semibold text-slate-700 uppercase mb-1">Full Legal Name</label>
                  <input 
                    v-model="form.name" 
                    type="text" 
                    required 
                    placeholder="Jane Doe" 
                    class="w-full px-3.5 py-2 bg-slate-50 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <div>
                  <label class="block text-xs font-semibold text-slate-700 uppercase mb-1">Email Address</label>
                  <input 
                    v-model="form.email" 
                    type="email" 
                    required 
                    placeholder="jane@example.com" 
                    class="w-full px-3.5 py-2 bg-slate-50 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <div>
                  <label class="block text-xs font-semibold text-slate-700 uppercase mb-1">Phone Number</label>
                  <input 
                    v-model="form.phoneNumber" 
                    type="tel" 
                    placeholder="+1 (555) 000-0000" 
                    class="w-full px-3.5 py-2 bg-slate-50 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <div>
                  <label class="block text-xs font-semibold text-slate-700 uppercase mb-1">Create Password</label>
                  <input 
                    v-model="form.password" 
                    type="password" 
                    required 
                    placeholder="••••••••" 
                    class="w-full px-3.5 py-2 bg-slate-50 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <div class="pt-3 flex justify-between items-center">
                  <button 
                    type="button" 
                    @click="prevStep" 
                    class="text-xs text-slate-500 hover:text-slate-800 font-semibold cursor-pointer"
                  >
                    &larr; Back
                  </button>
                  <button 
                    type="submit" 
                    :disabled="loading"
                    class="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-sm transition disabled:opacity-50 cursor-pointer"
                  >
                    {{ loading ? 'Activating Policy...' : 'Bind & Activate Coverage' }}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Section 2: What We Cover -->
    <div class="space-y-10">
      <div class="text-center max-w-2xl mx-auto space-y-2">
        <h2 class="text-3xl font-extrabold text-slate-900 tracking-tight">Comprehensive Fire Protection</h2>
        <p class="text-sm text-slate-500">Every policy includes end-to-end coverage from first spark to final reconstruction.</p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <!-- Card 1 -->
        <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs hover:border-slate-300 transition space-y-3">
          <div class="w-12 h-12 rounded-xl bg-orange-50 text-orange-600 flex items-center justify-center text-2xl font-bold">
            🏠
          </div>
          <h3 class="font-bold text-slate-900 text-base">Dwelling & Structure</h3>
          <p class="text-xs text-slate-600 leading-relaxed">
            Rebuilding costs for foundations, framing, drywall, electrical wiring, and roofing damaged by fire, smoke, and firefighting water hoses.
          </p>
        </div>

        <!-- Card 2 -->
        <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs hover:border-slate-300 transition space-y-3">
          <div class="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center text-2xl font-bold">
            🛋️
          </div>
          <h3 class="font-bold text-slate-900 text-base">Personal Belongings</h3>
          <p class="text-xs text-slate-600 leading-relaxed">
            Full replacement cost value for furniture, electronics, clothing, and household items. No unfair depreciation deductions.
          </p>
        </div>

        <!-- Card 3 -->
        <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs hover:border-slate-300 transition space-y-3">
          <div class="w-12 h-12 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center text-2xl font-bold">
            🏨
          </div>
          <h3 class="font-bold text-slate-900 text-base">Relocation & Loss of Use</h3>
          <p class="text-xs text-slate-600 leading-relaxed">
            Immediate housing allowances for hotels, extended-stay rentals, and daily meal stipends while your home is undergoing restoration.
          </p>
        </div>

        <!-- Card 4 -->
        <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs hover:border-slate-300 transition space-y-3">
          <div class="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center text-2xl font-bold">
            🧹
          </div>
          <h3 class="font-bold text-slate-900 text-base">Debris & Clean-Up</h3>
          <p class="text-xs text-slate-600 leading-relaxed">
            Certified hazardous soot removal, environmental odor scrubbing, and demolition clearance paid directly to remediation contractors.
          </p>
        </div>
      </div>
    </div>

    <!-- Section 3: How It Works Timeline -->
    <div class="bg-white rounded-3xl border border-slate-200 p-8 sm:p-12 space-y-10 shadow-xs">
      <div class="text-center max-w-2xl mx-auto space-y-2">
        <div class="text-xs font-bold text-blue-600 uppercase tracking-widest">Simple & Transparent</div>
        <h2 class="text-3xl font-extrabold text-slate-900 tracking-tight">How SafeHaven Insurance Works</h2>
        <p class="text-sm text-slate-500">Three straightforward steps from quotation to paperless claims.</p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-8 relative">
        <!-- Step 1 -->
        <div class="space-y-3 text-center md:text-left">
          <div class="w-10 h-10 rounded-full bg-blue-600 text-white font-bold flex items-center justify-center mx-auto md:mx-0 shadow-sm">
            1
          </div>
          <h3 class="font-bold text-slate-900 text-base">Instant Quote & Binding</h3>
          <p class="text-xs text-slate-600 leading-relaxed">
            Enter your property address, customize your coverage limits, and bind your policy in under 2 minutes. Your official policy number is ready immediately.
          </p>
        </div>

        <!-- Step 2 -->
        <div class="space-y-3 text-center md:text-left">
          <div class="w-10 h-10 rounded-full bg-blue-600 text-white font-bold flex items-center justify-center mx-auto md:mx-0 shadow-sm">
            2
          </div>
          <h3 class="font-bold text-slate-900 text-base">Digital Portal Access</h3>
          <p class="text-xs text-slate-600 leading-relaxed">
            Access your policy document 24/7, forward binders directly to mortgage lenders, or manage coverage settings without ever calling a call center.
          </p>
        </div>

        <!-- Step 3 -->
        <div class="space-y-3 text-center md:text-left">
          <div class="w-10 h-10 rounded-full bg-blue-600 text-white font-bold flex items-center justify-center mx-auto md:mx-0 shadow-sm">
            3
          </div>
          <h3 class="font-bold text-slate-900 text-base">Rapid First Notice of Loss</h3>
          <p class="text-xs text-slate-600 leading-relaxed">
            In the event of an incident, snap photos of the damages, submit through the claims workbench, and receive quick review decisions and emergency payout assistance.
          </p>
        </div>
      </div>
    </div>

    <!-- Section 4: Customer Testimonials -->
    <div class="space-y-8">
      <div class="text-center max-w-2xl mx-auto space-y-2">
        <h2 class="text-3xl font-extrabold text-slate-900 tracking-tight">Real Policyholder Experiences</h2>
        <p class="text-sm text-slate-500">Trusted by over 120,000 families across the country.</p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs space-y-4">
          <div class="text-amber-400 text-sm">★★★★★</div>
          <p class="text-xs text-slate-700 italic leading-relaxed">
            "When our kitchen caught fire from a toaster oven, our home was full of smoke. SafeHaven approved our initial claim and placed us in a nearby hotel within hours."
          </p>
          <div class="border-t border-slate-100 pt-3 flex items-center space-x-3">
            <div class="w-8 h-8 rounded-full bg-slate-200 flex items-center justify-center font-bold text-xs text-slate-700">MS</div>
            <div>
              <div class="text-xs font-bold text-slate-900">Michael S.</div>
              <div class="text-[11px] text-slate-400">Scranton, PA • Homeowner</div>
            </div>
          </div>
        </div>

        <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs space-y-4">
          <div class="text-amber-400 text-sm">★★★★★</div>
          <p class="text-xs text-slate-700 italic leading-relaxed">
            "Getting coverage took literally 90 seconds while sitting in my car outside the closing attorney’s office. My lender had the binder in their inbox before I walked inside."
          </p>
          <div class="border-t border-slate-100 pt-3 flex items-center space-x-3">
            <div class="w-8 h-8 rounded-full bg-slate-200 flex items-center justify-center font-bold text-xs text-slate-700">SC</div>
            <div>
              <div class="text-xs font-bold text-slate-900">Sarah C.</div>
              <div class="text-[11px] text-slate-400">Austin, TX • Single Family</div>
            </div>
          </div>
        </div>

        <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs space-y-4">
          <div class="text-amber-400 text-sm">★★★★★</div>
          <p class="text-xs text-slate-700 italic leading-relaxed">
            "The adjuster reviewed my photos online and settled the repair estimate without back-and-forth arguments. By far the most modern insurance company I've used."
          </p>
          <div class="border-t border-slate-100 pt-3 flex items-center space-x-3">
            <div class="w-8 h-8 rounded-full bg-slate-200 flex items-center justify-center font-bold text-xs text-slate-700">DW</div>
            <div>
              <div class="text-xs font-bold text-slate-900">David W.</div>
              <div class="text-[11px] text-slate-400">Denver, CO • Townhouse</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Section 5: Interactive FAQ Accordion -->
    <div class="bg-white rounded-3xl border border-slate-200 p-8 sm:p-12 space-y-8 shadow-xs max-w-3xl mx-auto">
      <div class="text-center space-y-2">
        <h2 class="text-3xl font-extrabold text-slate-900 tracking-tight">Frequently Asked Questions</h2>
        <p class="text-sm text-slate-500">Everything you need to know about your fire coverage.</p>
      </div>

      <div class="divide-y divide-slate-100">
        <div v-for="(faq, i) in faqs" :key="i" class="py-4">
          <button 
            @click="toggleFaq(i)"
            class="w-full flex items-center justify-between text-left text-sm font-bold text-slate-900 hover:text-blue-600 transition cursor-pointer"
          >
            <span>{{ faq.q }}</span>
            <span class="text-lg text-slate-400 font-mono">{{ activeFaq === i ? '−' : '+' }}</span>
          </button>
          <div v-if="activeFaq === i" class="mt-2 text-xs text-slate-600 leading-relaxed pr-6">
            {{ faq.a }}
          </div>
        </div>
      </div>
    </div>

    <!-- Section 6: Pre-Footer Call to Action -->
    <div class="bg-gradient-to-r from-blue-700 to-indigo-800 rounded-3xl p-8 sm:p-12 text-white text-center space-y-6 shadow-md">
      <h2 class="text-3xl sm:text-4xl font-extrabold tracking-tight">
        Ready to Protect What Matters Most?
      </h2>
      <p class="text-sm sm:text-base text-blue-100 max-w-xl mx-auto">
        Join over 120,000 homeowners with guaranteed, immediate fire insurance coverage. No phone calls required.
      </p>
      <div>
        <button 
          @click="scrollToWizard"
          class="px-8 py-3 bg-white hover:bg-slate-100 text-blue-900 font-bold rounded-xl text-sm shadow-md transition cursor-pointer"
        >
          Get My Instant Quote Now &uarr;
        </button>
      </div>
    </div>
  </div>
</template>
