import { defineStore } from 'pinia';
import api from '../api/client';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    loading: false,
    error: null,
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    role: (state) => state.user?.role || null,
    email: (state) => state.user?.email || null,
    isAdjuster: (state) => state.user?.role === 'ADJUSTER',
    isCustomer: (state) => state.user?.role === 'CUSTOMER',
  },
  actions: {
    async login(email, password) {
      this.loading = true;
      this.error = null;
      try {
        const response = await api.post('/auth/login', { email, password });
        const { token, role } = response.data;
        this.token = token;
        this.user = { email, role };

        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify({ email, role }));

        return response.data;
      } catch (err) {
        this.error = err.response?.data?.error || 'Invalid credentials or login failed.';
        throw this.error;
      } finally {
        this.loading = false;
      }
    },
    logout() {
      this.token = null;
      this.user = null;
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    },
  },
});
