// =========================================================================
// Feature 3: Food Menu & Ordering (Khách hàng xem Menu, Đặt món & Lịch sử)
// Sinh viên thực hiện: Nguyễn Lâm Phúc (Member 3)
// =========================================================================

let cartItems = []; // Danh sách món đang được chọn trong giỏ hàng
let menuCache = []; // Danh sách toàn bộ món ăn lấy từ DB
let myOrdersCache = []; // Danh sách đơn của khách

function formatVND(amount) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
}

function showCustomerAlert(msg, isSuccess = true) {
  const box = document.getElementById('customer-alert-box');
  if (!box) return;
  box.innerHTML = `
    <div class="alert alert-${isSuccess ? 'success' : 'danger'} alert-dismissible fade show shadow-sm" role="alert">
      ${msg}
      <button type="button" class="close" data-dismiss="alert">&times;</button>
    </div>
  `;
  setTimeout(() => { if (box) box.innerHTML = ''; }, 4500);
}

// 1. TẢI THỰC ĐƠN ĐỘNG TỪ DATABASE (UC15)
async function loadCustomerMenu() {
  const container = document.getElementById('dynamic-menu-list');
  if (!container) return;

  try {
    const items = await apiFetch('/menu');
    menuCache = items || [];
    renderCustomerMenu(menuCache);
  } catch (err) {
    container.innerHTML = `<div class="col-12 text-center text-danger py-4">Lỗi tải thực đơn: ${err.message}</div>`;
  }
}

function renderCustomerMenu(items) {
  const container = document.getElementById('dynamic-menu-list');
  if (!container) return;

  if (!items || items.length === 0) {
    container.innerHTML = '<div class="col-12 text-center text-muted py-5">Không tìm thấy món ăn nào phù hợp.</div>';
    return;
  }

  container.innerHTML = items.map((item, idx) => {
    const imgIdx = (idx % 8) + 1;
    return `
      <div class="col-md-6 mb-4">
        <div class="pricing-entry d-flex p-3 bg-white rounded shadow-sm h-100 border">
          <div class="img mr-3 rounded" style="background-image: url(../../images/menu-${imgIdx}.jpg); width: 90px; height: 90px; background-size: cover; background-position: center; flex-shrink: 0;"></div>
          <div class="desc d-flex flex-column justify-content-between flex-grow-1">
            <div>
              <div class="d-flex justify-content-between align-items-start">
                <h4 class="h6 font-weight-bold text-dark mb-1">${item.name}</h4>
                <span class="text-primary font-weight-bold small">${formatVND(item.price)}</span>
              </div>
              <p class="text-muted small mb-2">${item.description || 'Hương vị thơm ngon chế biến theo tiêu chuẩn khu nghỉ dưỡng 5 sao.'}</p>
            </div>
            <div class="text-right">
              <button class="btn btn-outline-primary btn-sm py-1 px-3" onclick="addToCart(${item.id})">
                + Chọn món
              </button>
            </div>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

// Tìm kiếm món ăn realtime
function searchCustomerMenu() {
  const q = (document.getElementById('menu-search-input')?.value || '').trim().toLowerCase();
  if (!q) {
    renderCustomerMenu(menuCache);
    return;
  }
  const filtered = menuCache.filter(item => 
    (item.name && item.name.toLowerCase().includes(q)) ||
    (item.description && item.description.toLowerCase().includes(q))
  );
  renderCustomerMenu(filtered);
}

// 2. QUẢN LÝ GIỎ HÀNG (CART)
function addToCart(menuItemId) {
  const item = menuCache.find(m => m.id === menuItemId);
  if (!item) return;

  const existing = cartItems.find(c => c.menuItemId === menuItemId);
  if (existing) {
    existing.quantity += 1;
  } else {
    cartItems.push({
      menuItemId: item.id,
      name: item.name,
      price: item.price,
      quantity: 1,
      note: ''
    });
  }
  renderCart();
  showCustomerAlert(`Đã thêm món "${item.name}" vào giỏ hàng!`);
}

function updateCartQuantity(menuItemId, change) {
  const item = cartItems.find(c => c.menuItemId === menuItemId);
  if (!item) return;

  item.quantity += change;
  if (item.quantity <= 0) {
    cartItems = cartItems.filter(c => c.menuItemId !== menuItemId);
  }
  renderCart();
}

function renderCart() {
  const container = document.getElementById('cart-items-container');
  const countBadge = document.getElementById('cart-count');
  const totalSpan = document.getElementById('cart-total');

  if (!container) return;

  const totalQty = cartItems.reduce((acc, cur) => acc + cur.quantity, 0);
  const grandTotal = cartItems.reduce((acc, cur) => acc + (cur.price * cur.quantity), 0);

  if (countBadge) countBadge.textContent = `${totalQty} món`;
  if (totalSpan) totalSpan.textContent = formatVND(grandTotal);

  if (cartItems.length === 0) {
    container.innerHTML = '<p class="text-muted small text-center my-3">Chưa có món nào được chọn. Hãy bấm <strong>+ Chọn món</strong> để thêm vào đơn.</p>';
    return;
  }

  container.innerHTML = cartItems.map(item => `
    <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
      <div style="max-width: 65%;">
        <div class="font-weight-bold small text-dark">${item.name}</div>
        <div class="text-muted small">${formatVND(item.price)} x ${item.quantity} = <strong class="text-primary">${formatVND(item.price * item.quantity)}</strong></div>
      </div>
      <div class="d-flex align-items-center">
        <button class="btn btn-outline-secondary btn-sm px-2 py-0" onclick="updateCartQuantity(${item.menuItemId}, -1)">-</button>
        <span class="mx-2 small font-weight-bold">${item.quantity}</span>
        <button class="btn btn-outline-secondary btn-sm px-2 py-0" onclick="updateCartQuantity(${item.menuItemId}, 1)">+</button>
      </div>
    </div>
  `).join('');
}

// 3. TẠO ĐƠN ĐẶT MÓN ĂN (UC16)
async function submitFoodOrder() {
  if (cartItems.length === 0) {
    alert('Vui lòng chọn ít nhất một món ăn trước khi gửi đơn.');
    return;
  }

  const roomInput = document.getElementById('order-room-number');
  const noteInput = document.getElementById('order-note');
  const btn = document.getElementById('btn-submit-order');

  const roomNumber = roomInput ? roomInput.value.trim() : '';
  const note = noteInput ? noteInput.value.trim() : '';

  const payload = {
    roomNumber: roomNumber || null,
    note: note,
    items: cartItems.map(c => ({
      menuItemId: c.menuItemId,
      quantity: c.quantity,
      note: c.note || null
    }))
  };

  try {
    if (btn) btn.disabled = true;
    await apiFetch('/orders', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    showCustomerAlert('Đã gửi yêu cầu gọi món thành công! Nhân viên phục vụ đang tiếp nhận đơn.');
    cartItems = [];
    if (noteInput) noteInput.value = '';
    renderCart();
    loadMyOrders(); // Tải lại lịch sử đơn
  } catch (err) {
    showCustomerAlert('Không thể gửi đơn: ' + err.message, false);
  } finally {
    if (btn) btn.disabled = false;
  }
}

// 4. XEM LỊCH SỬ ĐƠN CỦA TÔI & HỦY ĐƠN (UC17, UC18)
const ORDER_STATUS_MAP = {
  NEW: '<span class="badge badge-warning text-dark px-2 py-1">Chờ duyệt</span>',
  CONFIRMED: '<span class="badge badge-info px-2 py-1">Đã xác nhận</span>',
  PREPARING: '<span class="badge badge-secondary px-2 py-1">Đang chế biến</span>',
  SERVING: '<span class="badge badge-primary px-2 py-1">Đang giao món</span>',
  COMPLETED: '<span class="badge badge-success px-2 py-1">Hoàn thành</span>',
  CANCELLED: '<span class="badge badge-danger px-2 py-1">Đã hủy</span>'
};

async function loadMyOrders() {
  const tbody = document.getElementById('my-orders-body');
  if (!tbody) return;

  try {
    const list = await apiFetch('/orders/my-orders');
    myOrdersCache = list || [];

    if (!myOrdersCache || myOrdersCache.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-3">Bạn chưa có đơn đặt món nào.</td></tr>';
      return;
    }

    tbody.innerHTML = myOrdersCache.map(o => {
      const itemsSummary = (o.items || []).map(i => `${i.quantity}x ${i.menuItemName}`).join(', ');
      const canCancel = o.status === 'NEW';
      
      const cancelBtn = canCancel 
        ? `<button class="btn btn-outline-danger btn-sm py-0 px-2 mr-1" onclick="cancelMyOrder(${o.id})">Hủy</button>` 
        : '';
      const viewBtn = `<button class="btn btn-outline-info btn-sm py-0 px-2" onclick="showCustomerOrderDetail(${o.id})">Chi tiết</button>`;

      const dateStr = o.createdAt ? new Date(o.createdAt).toLocaleString('vi-VN') : '-';

      return `
        <tr>
          <td><strong>#O-${o.id}</strong></td>
          <td class="small text-muted">${dateStr}</td>
          <td><span class="badge badge-light border font-weight-bold">${o.roomNumber || 'Tại bàn'}</span></td>
          <td class="small text-truncate" style="max-width: 250px;">${itemsSummary}</td>
          <td class="text-primary font-weight-bold small">${formatVND(o.totalAmount)}</td>
          <td>${ORDER_STATUS_MAP[o.status] || o.status}</td>
          <td>${cancelBtn}${viewBtn}</td>
        </tr>
      `;
    }).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted py-3">Vui lòng đăng nhập để xem lịch sử đơn món.</td></tr>`;
  }
}

// Xem chi tiết đơn hàng dạng Popup Modal
function showCustomerOrderDetail(orderId) {
  const o = myOrdersCache.find(x => x.id === orderId);
  if (!o) return;

  const titleEl = document.getElementById('custModalOrderTitle');
  const bodyEl = document.getElementById('custModalOrderBody');

  if (titleEl) titleEl.textContent = `Chi tiết đơn gọi món #O-${o.id}`;
  if (bodyEl) {
    const itemsHtml = (o.items || []).map(i => `
      <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
        <div>
          <span class="font-weight-bold">${i.menuItemName}</span>
          <div class="small text-muted">${formatVND(i.price)} &times; ${i.quantity}</div>
        </div>
        <span class="font-weight-bold text-dark">${formatVND(i.subTotal)}</span>
      </div>
    `).join('');

    bodyEl.innerHTML = `
      <div class="mb-3">
        <p class="mb-1"><strong>Nơi phục vụ:</strong> ${o.roomNumber || 'Tại bàn nhà hàng'}</p>
        <p class="mb-1"><strong>Thời gian đặt:</strong> ${new Date(o.createdAt).toLocaleString('vi-VN')}</p>
        <p class="mb-1"><strong>Trạng thái:</strong> ${ORDER_STATUS_MAP[o.status] || o.status}</p>
        ${o.note ? `<p class="mb-1 text-danger"><strong>Ghi chú bếp:</strong> ${o.note}</p>` : ''}
      </div>
      <h6 class="font-weight-bold border-bottom pb-2">Danh sách món ăn:</h6>
      <div class="mb-3">${itemsHtml}</div>
      <div class="d-flex justify-content-between align-items-center font-weight-bold h5 text-primary">
        <span>Tổng thanh toán:</span>
        <span>${formatVND(o.totalAmount)}</span>
      </div>
    `;
  }

  $('#customerOrderDetailModal').modal('show');
}

async function cancelMyOrder(orderId) {
  if (!confirm(`Bạn có chắc chắn muốn hủy đơn hàng #O-${orderId}?`)) return;
  try {
    await apiFetch(`/orders/${orderId}/cancel`, { method: 'PUT' });
    showCustomerAlert(`Đã hủy thành công đơn #O-${orderId}`);
    loadMyOrders();
  } catch (err) {
    showCustomerAlert('Không thể hủy đơn: ' + err.message, false);
  }
}

// Tự động khởi chạy khi vào trang
document.addEventListener('DOMContentLoaded', () => {
  loadCustomerMenu();
  loadMyOrders();
});
