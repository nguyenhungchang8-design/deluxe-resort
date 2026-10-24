// Tien ich dung chung: xac thuc, phan quyen va dieu huong theo role
// Goc cua frontend (thu muc cha cua js/), dung duoc ca khi trang nam trong thu muc con
const ROOT = new URL('../', document.currentScript.src).href;

const ROLE_HOME = {
  ROLE_ADMIN: 'portal/admin/dashboard.html',
  ROLE_RECEPTIONIST: 'portal/reception/bookings.html',
  ROLE_HOUSEKEEPING: 'portal/housekeeping/tasks.html',
  ROLE_ACCOUNTANT: 'portal/accountant/invoices.html',
  ROLE_WAITER: 'portal/waiter/orders.html',
  ROLE_CUSTOMER: 'index.html'
};

const ROLE_LABEL = {
  ROLE_ADMIN: 'Admin',
  ROLE_RECEPTIONIST: 'Receptionist',
  ROLE_HOUSEKEEPING: 'Housekeeping',
  ROLE_ACCOUNTANT: 'Accountant',
  ROLE_WAITER: 'Waiter',
  ROLE_CUSTOMER: 'Customer'
};

function getUser() {
  try { return JSON.parse(localStorage.getItem('user')); } catch (e) { return null; }
}

function homeFor(role) { return ROOT + (ROLE_HOME[role] || 'index.html'); }

async function logout() {
  try { await apiFetch('/auth/logout', { method: 'POST' }); } catch (e) { /* bo qua */ }
  localStorage.removeItem('user');
  window.location.href = ROOT + 'login.html';
}

// Bao ve trang: chua dang nhap -> login, sai quyen -> access-denied
async function requireRole(allowedRoles) {
  let user = null;
  try {
    user = await apiFetch('/auth/me');
    localStorage.setItem('user', JSON.stringify(user));
  } catch (e) {
    localStorage.removeItem('user');
    window.location.href = ROOT + 'login.html';
    return null;
  }
  if (!allowedRoles.includes(user.role)) {
    window.location.href = ROOT + 'access-denied.html';
    return null;
  }
  return user;
}

// Them muc Login / ten user + Logout vao navbar cua trang khach
function renderNavAuth() {
  const ul = document.querySelector('#ftco-nav .navbar-nav');
  if (!ul || document.getElementById('nav-auth')) return;
  const user = getUser();
  const li = document.createElement('li');
  li.id = 'nav-auth';
  li.className = 'nav-item';
  if (user) {
    const target = homeFor(user.role);
    li.innerHTML = '<a href="' + target + '" class="nav-link">' + user.username + '</a>';
    const out = document.createElement('li');
    out.className = 'nav-item';
    out.innerHTML = '<a href="#" class="nav-link" id="nav-logout">Logout</a>';
    ul.appendChild(li);
    ul.appendChild(out);
    document.getElementById('nav-logout').addEventListener('click', (e) => { e.preventDefault(); logout(); });
  } else {
    li.innerHTML = '<a href="' + ROOT + 'login.html" class="nav-link">Login</a>';
    ul.appendChild(li);
  }
}

document.addEventListener('DOMContentLoaded', renderNavAuth);



