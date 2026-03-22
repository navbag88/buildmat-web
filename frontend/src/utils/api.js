import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use(cfg => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

api.interceptors.response.use(
  r => r,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default api

// ── Download helper ────────────────────────────────────────────────
export async function downloadFile(url, filename) {
  const res = await api.get(url, { responseType: 'blob' })
  const href = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = href; a.download = filename; a.click()
  URL.revokeObjectURL(href)
}

// ── INR formatter ──────────────────────────────────────────────────
export const inr = v => new Intl.NumberFormat('en-IN', {
  style: 'currency', currency: 'INR', maximumFractionDigits: 2
}).format(v ?? 0)

export const fmtDate = d => d ? new Date(d).toLocaleDateString('en-IN') : '—'
