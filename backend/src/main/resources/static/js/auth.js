// Tiện ích dùng chung: Xác thực, Phân quyền và Phân chia Guest / Portal
// Gốc của frontend (thư mục cha của js/), dùng được cả khi trang nằm trong thư mục con
const ROOT = new URL('../', document.currentScript.src).href;

const ROLE_HOME = {
  ROLE_ADMIN: 'portal/admin/dashboard.html',
  ROLE_RECEPTIONIST: 'portal/reception/bookings.html',
  ROLE_HOUSEKEEPING: 'portal/housekeeping/tasks.html',
  ROLE_ACCOUNTANT: 'portal/accountant/invoices.html',
  ROLE_WAITER: 'portal/waiter/orders.html',
  ROLE_CUSTOMER: 'portal/customer/home.html'
};

const ROLE_LABEL = {
  ROLE_ADMIN: 'Quản trị viên',
  ROLE_RECEPTIONIST: 'Lễ tân',
  ROLE_HOUSEKEEPING: 'Buồng phòng',
  ROLE_ACCOUNTANT: 'Kế toán',
  ROLE_WAITER: 'Phục vụ',
  ROLE_CUSTOMER: 'Khách hàng'
};

function getUser() {
  try { return JSON.parse(localStorage.getItem('user')); } catch (e) { return null; }
}

function homeFor(role) { 
  return ROOT + (ROLE_HOME[role] || 'portal/customer/home.html'); 
}

async function logout() {
  try { 
    await apiFetch('/auth/logout', { method: 'POST' }); 
  } catch (e) { 
    /* bỏ qua */ 
  }
  localStorage.removeItem('user');
  window.location.href = ROOT + 'index.html';
}

// Bảo vệ trang: chưa đăng nhập -> login, sai quyền -> access-denied
async function requireRole(allowedRoles) {
  let user = null;
  try {
    user = await apiFetch('/auth/me');
    localStorage.setItem('user', JSON.stringify(user));
  } catch (e) {
    localStorage.removeItem('user');
    window.location.href = ROOT + 'portal/auth/login.html';
    return null;
  }
  if (allowedRoles && allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
    window.location.href = ROOT + 'portal/auth/access-denied.html';
    return null;
  }
  return user;
}

// Tự động kiểm tra bảo vệ nếu người dùng đang ở trong thư mục /portal/ (trừ portal/auth/)
async function enforcePortalAccess() {
  const path = window.location.pathname;
  if (!path.includes('/portal/') || path.includes('/portal/auth/')) {
    return; // Trang ngoài hoặc trang login dành cho Guest, cho phép truy cập
  }

  // Nếu đang truy cập vào các thư mục sâu trong portal
  let requiredRoles = ['ROLE_ADMIN'];
  if (path.includes('/portal/customer/')) {
    requiredRoles.push('ROLE_CUSTOMER');
  } else if (path.includes('/portal/reception/')) {
    requiredRoles.push('ROLE_RECEPTIONIST');
  } else if (path.includes('/portal/housekeeping/')) {
    requiredRoles.push('ROLE_HOUSEKEEPING', 'ROLE_RECEPTIONIST');
  } else if (path.includes('/portal/accountant/')) {
    requiredRoles.push('ROLE_ACCOUNTANT');
  } else if (path.includes('/portal/waiter/')) {
    requiredRoles.push('ROLE_WAITER');
  } else if (path.includes('/portal/admin/')) {
    // Chỉ có ROLE_ADMIN
  }

  const user = await requireRole(requiredRoles);
  if (!user) return;

  // Hiển thị tên và vai trò trên thanh navbar / header của trang portal
  const nameEl = document.getElementById('user-name');
  if (nameEl) {
    nameEl.textContent = (user.fullName || user.username) + ' (' + (ROLE_LABEL[user.role] || user.role) + ')';
  }
  const btnLogout = document.getElementById('btn-logout');
  if (btnLogout) {
    btnLogout.addEventListener('click', (e) => { 
      e.preventDefault(); 
      logout(); 
    });
  }
}

// Thêm mục Thanh toán + Tên user + Đăng xuất (hoặc Đăng nhập) vào navbar đồng bộ kiểu chữ trắng và thẳng hàng
function renderNavAuth() {
  const ul = document.querySelector('#ftco-nav .navbar-nav');
  if (!ul) return;

  // Đảm bảo tất cả các phần tử trên thanh menu căn giữa thẳng hàng nhau
  ul.style.alignItems = 'center';

  // Xóa các item auth cũ nếu đã render trước đó để tránh trùng lặp
  ['nav-payment-item', 'nav-user-item', 'nav-logout-item'].forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.remove();
  });

  // Tìm li Login có sẵn trên navbar (nếu có)
  let loginLi = document.getElementById('nav-auth');
  if (!loginLi) {
    const lis = ul.querySelectorAll('li.nav-item');
    for (let li of lis) {
      const text = li.textContent.trim().toLowerCase();
      if (text === 'login' || text === 'đăng nhập' || text.includes('guest') || text.includes('khách')) {
        loginLi = li;
        break;
      }
    }
  }

  const user = getUser();

  if (user) {
    // ĐÃ ĐĂNG NHẬP: Hiển thị Thanh toán | Tên User | Đăng xuất (đồng bộ class nav-item / nav-link, chữ trắng, không viền)
    const displayName = (user.fullName && user.fullName.trim() !== '') ? user.fullName : user.username;
    const target = homeFor(user.role);
    const paymentUrl = ROOT + 'portal/customer/payment.html';
    const isPaymentPage = window.location.pathname.endsWith('/payment.html');

    const paymentLi = document.createElement('li');
    paymentLi.id = 'nav-payment-item';
    paymentLi.className = 'nav-item' + (isPaymentPage ? ' active' : '');
    paymentLi.innerHTML = `<a href="${paymentUrl}" class="nav-link">Thanh toán</a>`;

    const userLi = document.createElement('li');
    userLi.id = 'nav-user-item';
    userLi.className = 'nav-item';
    userLi.innerHTML = `<a href="${target}" class="nav-link" title="Trang quản lý của bạn"><span class="icon-user mr-1"></span>${displayName}</a>`;

    const logoutLi = document.createElement('li');
    logoutLi.id = 'nav-logout-item';
    logoutLi.className = 'nav-item';
    logoutLi.innerHTML = `<a href="#" class="nav-link" id="nav-logout" style="color: #ff4d4f !important; font-weight: 500;" title="Đăng xuất khỏi hệ thống"><span class="icon-sign-out mr-1"></span>Đăng xuất</a>`;

    if (loginLi) {
      loginLi.replaceWith(paymentLi);
      paymentLi.after(userLi);
      userLi.after(logoutLi);
    } else {
      ul.appendChild(paymentLi);
      ul.appendChild(userLi);
      ul.appendChild(logoutLi);
    }

    const btnLogout = document.getElementById('nav-logout');
    if (btnLogout) {
      btnLogout.addEventListener('click', (e) => {
        e.preventDefault();
        logout();
      });
    }
  } else {
    // CHƯA ĐĂNG NHẬP (GUEST MODE): Chữ màu xanh lá cây + icon, bỏ viền/khung nền, thẳng hàng
    const loginUrl = ROOT + 'portal/auth/login.html';
    const guestLi = document.createElement('li');
    guestLi.id = 'nav-auth';
    guestLi.className = 'nav-item';
    guestLi.innerHTML = `<a href="${loginUrl}" class="nav-link" style="color: #2ecc71 !important; font-weight: 500;" title="Đăng nhập hệ thống"><span class="icon-user mr-1"></span>Đăng nhập</a>`;

    if (loginLi) {
      loginLi.replaceWith(guestLi);
    } else {
      ul.appendChild(guestLi);
    }
  }
}

// Bắt sự kiện hạn chế cho Guest trên các trang ngoài khi bấm chức năng cần đăng nhập
function setupGuestRestrictions() {
  const user = getUser();
  const isInsidePortal = window.location.pathname.includes('/portal/');
  
  // Nếu đã đăng nhập hoặc đang ở trang login thì không chặn
  if (user || isInsidePortal) return;

  function promptGuestLogin(message) {
    if (typeof $ !== 'undefined' && $('#guestAuthModal').length) {
      $('#guestAuthModal').modal('show');
      return;
    }
    const defaultMsg = 'Quý khách đang duyệt với tư cách Khách (Guest).\n\nVui lòng Đăng nhập để sử dụng tính năng Đặt phòng, Đặt món và Thanh toán phòng!\n\nBạn có muốn chuyển đến trang Đăng nhập ngay bây giờ?';
    if (confirm(message || defaultMsg)) {
      window.location.href = ROOT + 'portal/auth/login.html';
    }
  }

  // 1. Chặn submit form đặt phòng / tìm kiếm của Guest
  document.querySelectorAll('form').forEach((f) => {
    if (f.id === 'login-form' || f.id === 'register-form') return;
    f.addEventListener('submit', (e) => {
      e.preventDefault();
      promptGuestLogin('Để kiểm tra phòng còn trống và tiến hành Đặt phòng trực tuyến, quý khách vui lòng Đăng nhập vào hệ thống!');
    });
  });

  // 2. Chặn các nút Đặt phòng, Book Now, Order món
  document.querySelectorAll('a, button').forEach((el) => {
    const text = (el.textContent || '').trim().toLowerCase();
    const href = (el.getAttribute('href') || '').toLowerCase();
    
    const isActionBooking = text.includes('đặt phòng') || text.includes('book now') || text.includes('đặt món') || text.includes('order');
    const isDirectPortalLink = href.includes('portal/') && !href.includes('portal/auth/');

    if (isActionBooking || isDirectPortalLink) {
      el.addEventListener('click', (e) => {
        e.preventDefault();
        promptGuestLogin('Tính năng này dành riêng cho thành viên đã đăng nhập. Quý khách có muốn đăng nhập ngay bây giờ?');
      });
    }
  });
}

document.addEventListener('DOMContentLoaded', () => {
  enforcePortalAccess();
  renderNavAuth();
  setupGuestRestrictions();
});
