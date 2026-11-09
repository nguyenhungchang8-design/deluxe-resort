// Cau hinh URL API (cung host & port 8080 voi Backend)
const API_BASE = '/api';

async function apiFetch(path, options = {}) {
  const res = await fetch(API_BASE + path, {
    credentials: 'include', // gui kem session cookie
    headers: { 'Content-Type': 'application/json' },
    ...options
  });
  let data = null;
  try { data = await res.json(); } catch (e) { /* no body */ }
  if (!res.ok) {
    const msg = (data && data.message) || (res.status === 401 ? 'Chưa đăng nhập' : 'Đã xảy ra lỗi');
    throw new Error(msg);
  }
  return data;
}
