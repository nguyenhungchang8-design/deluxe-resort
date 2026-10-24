// Cau hinh URL backend Spring Boot
const API_BASE = 'http://localhost:8080/api';

async function apiFetch(path, options = {}) {
  const res = await fetch(API_BASE + path, {
    credentials: 'include', // gui kem session cookie
    headers: { 'Content-Type': 'application/json' },
    ...options
  });
  let data = null;
  try { data = await res.json(); } catch (e) { /* no body */ }
  if (!res.ok) {
    const msg = (data && data.message) || (res.status === 401 ? 'Chua dang nhap' : 'Co loi xay ra');
    throw new Error(msg);
  }
  return data;
}
