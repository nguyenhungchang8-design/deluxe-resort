// housekeeping-tasks.js - Quản lý dọn phòng (100% Tiếng Việt, tối giản, chuyên nghiệp)

let currentUser = null;
let staffList = [];
let roomsList = [];
let allRoomsData = [];
let filteredRoomsData = [];
let isMyTasksOnly = false;
let editingOriginalStatus = null;
const PAGE_SIZE = 5;
let currentPage = 1;

document.addEventListener('DOMContentLoaded', async () => {
  try {
    currentUser = await requireRole(['ROLE_HOUSEKEEPING', 'ROLE_RECEPTIONIST', 'ROLE_ADMIN']);
    if (!currentUser) return;

    setupNavbarForRole(currentUser.role);
    await loadStaffList();
    await loadRoomsList();
    await loadRoomsToClean();
    setupEventListeners();
  } catch (err) {
    console.error('Lỗi khởi tạo:', err);
    showAlert('danger', 'Lỗi khi tải dữ liệu: ' + err.message);
  }
});

// Thiết lập thanh điều hướng theo vai trò (100% Tiếng Việt, tinh gọn)
function setupNavbarForRole(role) {
  ['nav-payment-item', 'nav-user-item', 'nav-logout-item'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.remove();
  });

  const navContainer = document.getElementById('role-nav-items');
  const btnCreateTask = document.getElementById('btn-open-create-modal');
  const taskFilterButtons = document.getElementById('task-filter-buttons');
  const userNameEl = document.getElementById('user-name');
  const pageTitle = document.getElementById('page-main-title');

  const fullName = currentUser.fullName || currentUser.username;

  if (taskFilterButtons) {
    if (role === 'ROLE_HOUSEKEEPING') {
      taskFilterButtons.classList.remove('d-none');
      taskFilterButtons.classList.add('d-flex');
    } else {
      taskFilterButtons.classList.remove('d-flex');
      taskFilterButtons.classList.add('d-none');
      isMyTasksOnly = false;
    }
  }

  if (role === 'ROLE_RECEPTIONIST') {
    if (navContainer) {
      navContainer.innerHTML = `
        <li class="nav-item"><a href="../reception/bookings.html" class="nav-link">Đơn đặt phòng</a></li>
        <li class="nav-item"><a href="../reception/checkin-checkout.html" class="nav-link">Nhận / Trả phòng</a></li>
        <li class="nav-item"><a href="../reception/rooms.html" class="nav-link">Sơ đồ phòng</a></li>
        <li class="nav-item active"><a href="tasks.html" class="nav-link font-weight-bold text-white">Quản lý dọn phòng</a></li>
        <li class="nav-item"><a href="maintenance.html" class="nav-link">Bảo trì phòng</a></li>
        <li class="nav-item"><a href="../reception/invoices.html" class="nav-link">Hóa đơn</a></li>
      `;
    }
    if (btnCreateTask) btnCreateTask.style.display = 'inline-flex';
    if (pageTitle) pageTitle.textContent = 'Quản lý dọn phòng';
    const breadRole = document.getElementById('bread-role');
    if (breadRole) breadRole.textContent = 'Lễ tân';
    if (userNameEl) {
      userNameEl.textContent = `Lễ tân: ${fullName}`;
      userNameEl.style.whiteSpace = 'nowrap';
    }
  } else if (role === 'ROLE_HOUSEKEEPING') {
    if (navContainer) {
      navContainer.innerHTML = `
        <li class="nav-item active"><a href="tasks.html" class="nav-link font-weight-bold text-white">Danh sách dọn phòng</a></li>
        <li class="nav-item"><a href="maintenance.html" class="nav-link">Bảo trì phòng</a></li>
      `;
    }
    if (btnCreateTask) btnCreateTask.style.display = 'none';
    if (pageTitle) pageTitle.textContent = 'Danh sách dọn phòng';
    const breadRole = document.getElementById('bread-role');
    if (breadRole) breadRole.textContent = 'Buồng phòng';
    if (userNameEl) {
      userNameEl.textContent = `Buồng phòng: ${fullName}`;
      userNameEl.style.whiteSpace = 'nowrap';
    }
  } else if (role === 'ROLE_ADMIN') {
    if (navContainer) {
      navContainer.innerHTML = `
        <li class="nav-item"><a href="../admin/dashboard.html" class="nav-link">Bảng điều khiển</a></li>
        <li class="nav-item active"><a href="tasks.html" class="nav-link font-weight-bold text-white">Quản lý dọn phòng</a></li>
        <li class="nav-item"><a href="maintenance.html" class="nav-link">Bảo trì phòng</a></li>
      `;
    }
    if (btnCreateTask) btnCreateTask.style.display = 'inline-flex';
    if (pageTitle) pageTitle.textContent = 'Quản lý dọn phòng';
    const breadRole = document.getElementById('bread-role');
    if (breadRole) breadRole.textContent = 'Quản trị viên';
    if (userNameEl) {
      userNameEl.textContent = `Quản trị viên: ${fullName}`;
      userNameEl.style.whiteSpace = 'nowrap';
    }
  }
}

// Tải danh sách nhân viên buồng phòng để phân công
async function loadStaffList() {
  try {
    staffList = await apiFetch('/housekeeping/staff');
    populateStaffDropdowns();
  } catch (e) {
    console.warn('Không thể tải danh sách nhân viên:', e);
  }
}

function populateStaffDropdowns() {
  const selects = [
    document.getElementById('modal-create-staff'),
    document.getElementById('modal-edit-staff')
  ];

  selects.forEach(select => {
    if (select) {
      select.innerHTML = '<option value="">-- Chưa phân công (buồng phòng tự nhận) --</option>';
      staffList.forEach(s => {
        select.innerHTML += `<option value="${s.id}">${escapeHtml(s.fullName || s.username)}</option>`;
      });
    }
  });
}

// Tải danh sách phòng
async function loadRoomsList() {
  try {
    roomsList = await apiFetch('/housekeeping/rooms');
    const select = document.getElementById('modal-create-room');
    if (select) {
      select.innerHTML = '<option value="">-- Chọn phòng cần dọn --</option>';
      roomsList.forEach(r => {
        const typeName = formatRoomTypeName(r.roomTypeName);
        select.innerHTML += `<option value="${r.id}">Phòng ${escapeHtml(r.roomNumber)} - ${escapeHtml(typeName)}</option>`;
      });
    }
  } catch (e) {
    console.warn('Không thể tải danh sách phòng:', e);
  }
}

// Tải danh sách phòng cần dọn
async function loadRoomsToClean(highlightRoomId = null) {
  const tbody = document.getElementById('rooms-tbody');
  const loading = document.getElementById('loading-indicator');
  const emptyState = document.getElementById('empty-state');
  const paginationContainer = document.getElementById('pagination-container');

  if (loading) loading.style.display = 'block';
  if (emptyState) emptyState.style.display = 'none';
  if (paginationContainer) paginationContainer.style.display = 'none';
  if (tbody) tbody.innerHTML = '';

  const searchRoom = (document.getElementById('search-room')?.value || '').trim().toLowerCase();
  const filterStatus = document.getElementById('filter-status')?.value || 'ALL_ACTIVE';
  const filterDate = document.getElementById('filter-date')?.value || '';

  try {
    const data = await apiFetch('/housekeeping/tasks?status=ALL');
    allRoomsData = data;

    updateMyTasksBadge(data);

    let displayedData = data;

    // Lọc theo chế độ xem "Nhiệm vụ của tôi"
    if (isMyTasksOnly && currentUser) {
      displayedData = displayedData.filter(item => item.assignedTo && item.assignedTo === currentUser.id);
    }

    // Lọc theo tiến độ dọn dẹp (filter-status)
    if (filterStatus === 'ALL_ACTIVE') {
      displayedData = displayedData.filter(item => item.cleaningStatus === 'PENDING' || item.cleaningStatus === 'IN_PROGRESS' || item.roomStatus === 'CLEANING');
    } else if (filterStatus && filterStatus !== 'ALL') {
      displayedData = displayedData.filter(item => item.cleaningStatus === filterStatus);
    }

    // Lọc theo mã phòng
    if (searchRoom) {
      displayedData = displayedData.filter(item =>
        (item.roomNumber && String(item.roomNumber).toLowerCase().includes(searchRoom))
      );
    }

    // Lọc theo ngày (YYYY-MM-DD)
    if (filterDate) {
      displayedData = displayedData.filter(item => toLocalDateString(item.createdAt) === filterDate);
    }

    // Sắp xếp theo thời gian giao mới nhất lên trước
    displayedData.sort((a, b) => {
      const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      if (tb !== ta) return tb - ta;
      return String(a.roomNumber || '').localeCompare(String(b.roomNumber || ''));
    });

    filteredRoomsData = displayedData;

    if (loading) loading.style.display = 'none';

    if (!filteredRoomsData || filteredRoomsData.length === 0) {
      if (emptyState) emptyState.style.display = 'block';
      return;
    }

    if (highlightRoomId) {
      const idx = filteredRoomsData.findIndex(r => r.roomId === highlightRoomId);
      if (idx >= 0) {
        currentPage = Math.floor(idx / PAGE_SIZE) + 1;
      }
    }

    const totalPages = Math.max(1, Math.ceil(filteredRoomsData.length / PAGE_SIZE));
    if (currentPage > totalPages) currentPage = totalPages;
    if (currentPage < 1) currentPage = 1;

    renderCurrentPage(highlightRoomId);
  } catch (err) {
    if (loading) loading.style.display = 'none';
    console.error('Lỗi tải danh sách phòng:', err);
    showAlert('danger', 'Không thể nạp dữ liệu: ' + err.message);
  }
}

function toLocalDateString(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return '';
  const pad = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function renderCurrentPage(highlightRoomId = null) {
  const start = (currentPage - 1) * PAGE_SIZE;
  const pageItems = filteredRoomsData.slice(start, start + PAGE_SIZE);
  renderTable(pageItems, highlightRoomId);
  renderPagination();
}

function renderPagination() {
  const container = document.getElementById('pagination-container');
  const infoEl = document.getElementById('pagination-info');
  const controlsEl = document.getElementById('pagination-controls');
  if (!container || !infoEl || !controlsEl) return;

  const total = filteredRoomsData.length;
  if (total === 0) {
    container.style.display = 'none';
    return;
  }

  const totalPages = Math.ceil(total / PAGE_SIZE);
  const startItem = (currentPage - 1) * PAGE_SIZE + 1;
  const endItem = Math.min(currentPage * PAGE_SIZE, total);

  container.style.display = 'flex';
  infoEl.textContent = `Hiển thị ${startItem}-${endItem} trên tổng số ${total} phòng`;

  let html = `
    <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
      <a class="page-link" href="#" data-page="${currentPage - 1}">Trước</a>
    </li>
  `;

  for (let p = 1; p <= totalPages; p++) {
    html += `
      <li class="page-item ${p === currentPage ? 'active' : ''}">
        <a class="page-link" href="#" data-page="${p}">${p}</a>
      </li>
    `;
  }

  html += `
    <li class="page-item ${currentPage === totalPages ? 'disabled' : ''}">
      <a class="page-link" href="#" data-page="${currentPage + 1}">Sau</a>
    </li>
  `;

  controlsEl.innerHTML = html;
  controlsEl.querySelectorAll('a.page-link').forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const targetPage = parseInt(e.currentTarget.dataset.page);
      if (!isNaN(targetPage) && targetPage >= 1 && targetPage <= totalPages && targetPage !== currentPage) {
        currentPage = targetPage;
        renderCurrentPage();
      }
    });
  });
}

// Cập nhật số lượng nhiệm vụ của tôi trên huy hiệu (badge)
function updateMyTasksBadge(data) {
  const badge = document.getElementById('my-tasks-badge');
  if (!badge) return;
  if (!currentUser) {
    badge.style.display = 'none';
    return;
  }
  const myCount = data.filter(r => r.assignedTo && r.assignedTo === currentUser.id && (r.cleaningStatus === 'PENDING' || r.cleaningStatus === 'IN_PROGRESS')).length;
  if (myCount > 0) {
    badge.textContent = myCount;
    badge.style.display = 'inline-block';
  } else {
    badge.style.display = 'none';
  }
}

// Hiển thị danh sách phòng ra bảng ĐÚNG 6 CỘT THEO THỨ TỰ
// Mã phòng | Loại phòng | Thời gian giao | Trạng thái phòng | Người dọn | Thao tác
function renderTable(rooms, highlightRoomId = null) {
  const tbody = document.getElementById('rooms-tbody');
  if (!tbody) return;
  tbody.innerHTML = '';

  rooms.forEach(item => {
    const tr = document.createElement('tr');
    tr.id = 'room-row-' + item.roomId;

    if (highlightRoomId && item.roomId === highlightRoomId) {
      tr.style.backgroundColor = '#f0fdf4';
    }

    // 1. Mã phòng
    const roomNumber = item.roomNumber || '';

    // 2. Loại phòng (Deluxe, Standard, Suite...)
    const typeName = formatRoomTypeName(item.roomTypeName);

    // 3. Thời gian giao (dd/MM/yyyy HH:mm)
    const timeFormatted = formatDateTime(item.createdAt);

    // 4. Trạng thái phòng (Đang dọn, Chờ dọn, Sẵn sàng, Đang bảo trì...)
    const statusBadge = formatRoomStatusBadge(item.roomStatus, item.cleaningStatus);
    const statusText = getStatusText(item.roomStatus, item.cleaningStatus);

    // 5. Người dọn (Tên nhân viên hoặc Chưa phân công)
    const staffName = item.assignedToName || 'Chưa phân công';

    // 6. Thao tác: Phân quyền rõ ràng
    // - Buồng phòng (ROLE_HOUSEKEEPING): CHỈ xem danh sách và thao tác NHẬN NHIỆM VỤ / DỌN XONG
    // - Lễ tân / Quản trị viên: Nút Sửa (chỉnh sửa phân công, tiến độ, xóa)
    let actionsHtml = '';
    const isHousekeeping = currentUser && currentUser.role === 'ROLE_HOUSEKEEPING';

    if (isHousekeeping) {
      const isAssignedToOther = item.assignedTo && currentUser && item.assignedTo !== currentUser.id;
      if (isAssignedToOther && item.cleaningStatus !== 'DONE') {
        actionsHtml = `<span class="badge badge-light border text-muted p-2 font-weight-500">Đã giao người khác</span>`;
      } else if (item.cleaningStatus === 'PENDING') {
        actionsHtml = `
          <button class="btn btn-sm btn-primary px-3 font-weight-bold btn-accept-task" 
                  data-task-id="${item.taskId || ''}" 
                  data-room-id="${item.roomId || ''}" 
                  data-room-number="${escapeHtml(roomNumber)}"
                  title="Nhận nhiệm vụ để bắt đầu dọn phòng">
            <i class="oi oi-check mr-1"></i>Nhận nhiệm vụ
          </button>
        `;
      } else if (item.cleaningStatus === 'IN_PROGRESS') {
        actionsHtml = `
          <button class="btn btn-sm btn-success px-3 font-weight-bold btn-complete-task" 
                  data-task-id="${item.taskId || ''}" 
                  data-room-id="${item.roomId || ''}" 
                  data-room-number="${escapeHtml(roomNumber)}"
                  title="Xác nhận đã dọn dẹp xong">
            <i class="oi oi-circle-check mr-1"></i>Dọn xong
          </button>
        `;
      } else if (item.cleaningStatus === 'DONE') {
        actionsHtml = `<span class="badge badge-success p-2 font-weight-500">Dọn xong</span>`;
      } else {
        actionsHtml = `<span class="text-muted small">--</span>`;
      }
    } else {
      actionsHtml = `
        <button class="btn btn-sm btn-outline-primary px-3 font-weight-bold btn-edit-task" 
                data-room-id="${item.roomId}" 
                data-room-number="${escapeHtml(roomNumber)}" 
                data-task-id="${item.taskId || ''}" 
                data-assigned-to="${item.assignedTo || ''}" 
                data-cleaning-status="${item.cleaningStatus || 'PENDING'}"
                data-current-status="${escapeHtml(statusText)}">
          <i class="oi oi-pencil mr-1"></i>Sửa
        </button>
      `;
    }

    tr.innerHTML = `
      <td class="font-weight-bold text-dark align-middle">${escapeHtml(roomNumber)}</td>
      <td class="align-middle">${escapeHtml(typeName)}</td>
      <td class="align-middle text-muted small">${escapeHtml(timeFormatted)}</td>
      <td class="align-middle">${statusBadge}</td>
      <td class="align-middle">${escapeHtml(staffName)}</td>
      <td class="align-middle text-right">${actionsHtml}</td>
    `;
    tbody.appendChild(tr);
  });

  attachActionHandlers();
}

// Định dạng tên hạng phòng (ngắn gọn: Deluxe, Standard, Suite...)
function formatRoomTypeName(name) {
  if (!name) return 'Standard';
  const upper = name.toUpperCase();
  if (upper.includes('DELUXE')) return 'Deluxe';
  if (upper.includes('STANDARD')) return 'Standard';
  if (upper.includes('SUITE')) return 'Suite';
  if (upper.includes('PENTHOUSE') || upper.includes('PRESIDENTIAL')) return 'Tổng thống';
  return name;
}

// Định dạng thời gian theo định dạng dd/MM/yyyy HH:mm
function formatDateTime(dateStr) {
  if (!dateStr) return '--';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  const pad = n => String(n).padStart(2, '0');
  const day = pad(d.getDate());
  const month = pad(d.getMonth() + 1);
  const year = d.getFullYear();
  const hours = pad(d.getHours());
  const minutes = pad(d.getMinutes());
  return `${day}/${month}/${year} ${hours}:${minutes}`;
}

// Text trạng thái phòng (không hiển thị mã kỹ thuật)
function getStatusText(roomStatus, cleaningStatus) {
  if (roomStatus === 'MAINTENANCE') return 'Đang bảo trì';
  if (cleaningStatus === 'IN_PROGRESS') return 'Đang dọn';
  if (cleaningStatus === 'PENDING') return 'Chờ dọn';
  if (cleaningStatus === 'DONE') return 'Dọn xong';
  if (roomStatus === 'READY' || roomStatus === 'AVAILABLE') return 'Sẵn sàng';
  if (roomStatus === 'CLEANING') return 'Chờ dọn';
  if (roomStatus === 'OCCUPIED') return 'Đang có khách';
  if (roomStatus === 'BOOKED') return 'Đã đặt trước';
  return roomStatus || 'Chưa rõ';
}

// Nhãn trạng thái phòng nhẹ nhàng, tinh gọn (100% Tiếng Việt)
function formatRoomStatusBadge(roomStatus, cleaningStatus) {
  if (roomStatus === 'MAINTENANCE') {
    return `<span class="badge badge-danger p-2 font-weight-500">Đang bảo trì</span>`;
  }
  if (cleaningStatus === 'IN_PROGRESS') {
    return `<span class="badge badge-warning p-2 font-weight-500 text-dark" style="background-color:#fef3c7; border:1px solid #fde68a; color:#92400e;">Đang dọn</span>`;
  }
  if (cleaningStatus === 'PENDING') {
    return `<span class="badge badge-info p-2 font-weight-500" style="background-color:#eff6ff; border:1px solid #dbeafe; color:#1e40af;">Chờ dọn</span>`;
  }
  if (cleaningStatus === 'DONE') {
    return `<span class="badge badge-success p-2 font-weight-500" style="background-color:#dcfce7; border:1px solid #bbf7d0; color:#166534;">Dọn xong</span>`;
  }
  if (roomStatus === 'READY' || roomStatus === 'AVAILABLE') {
    return `<span class="badge badge-success p-2 font-weight-500" style="background-color:#dcfce7; border:1px solid #bbf7d0; color:#166534;">Sẵn sàng</span>`;
  }
  if (roomStatus === 'CLEANING') {
    return `<span class="badge badge-info p-2 font-weight-500" style="background-color:#eff6ff; border:1px solid #dbeafe; color:#1e40af;">Chờ dọn</span>`;
  }
  if (roomStatus === 'OCCUPIED') {
    return `<span class="badge badge-secondary p-2 font-weight-500">Đang có khách</span>`;
  }
  if (roomStatus === 'BOOKED') {
    return `<span class="badge badge-light p-2 font-weight-500" style="background-color:#e0f2fe; border:1px solid #bae6fd; color:#0369a1;">Đã đặt trước</span>`;
  }
  return `<span class="badge badge-light p-2">${escapeHtml(roomStatus || 'Chưa rõ')}</span>`;
}

function attachActionHandlers() {
  // Gắn sự kiện cho nút Sửa (Lễ tân / Quản trị viên)
  document.querySelectorAll('.btn-edit-task').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const { roomId, roomNumber, taskId, assignedTo, cleaningStatus, currentStatus } = e.currentTarget.dataset;
      openEditTaskModal({
        roomId,
        roomNumber,
        taskId,
        assignedTo,
        cleaningStatus,
        currentStatus
      });
    });
  });

  // Gắn sự kiện cho nút Nhận nhiệm vụ (Buồng phòng)
  document.querySelectorAll('.btn-accept-task').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      const { taskId, roomId, roomNumber } = e.currentTarget.dataset;
      const originalText = e.currentTarget.innerHTML;
      e.currentTarget.disabled = true;
      e.currentTarget.textContent = 'Đang nhận việc...';

      try {
        const idToUse = taskId || 0;
        await apiFetch(`/housekeeping/tasks/${idToUse}/accept?roomId=${roomId || ''}`, {
          method: 'PUT'
        });
        showAlert('success', `Đã nhận nhiệm vụ dọn phòng <strong>${roomNumber}</strong> thành công.`);
        await loadRoomsToClean();
      } catch (err) {
        console.error('Lỗi nhận nhiệm vụ:', err);
        showAlert('danger', 'Không thể nhận nhiệm vụ: ' + err.message);
        e.currentTarget.disabled = false;
        e.currentTarget.innerHTML = originalText;
      }
    });
  });

  // Gắn sự kiện cho nút Hoàn thành dọn phòng (Buồng phòng)
  document.querySelectorAll('.btn-complete-task').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      const { taskId, roomId, roomNumber } = e.currentTarget.dataset;
      if (!(await deluxeConfirm(`Xác nhận phòng ${roomNumber} đã được dọn sạch sẽ và sẵn sàng đón khách?`))) {
        return;
      }

      const originalText = e.currentTarget.innerHTML;
      e.currentTarget.disabled = true;
      e.currentTarget.textContent = 'Đang hoàn thành...';

      try {
        const idToUse = taskId || 0;
        await apiFetch(`/housekeeping/tasks/${idToUse}/status?roomId=${roomId || ''}`, {
          method: 'PUT',
          body: JSON.stringify({ status: 'DONE' })
        });
        showAlert('success', `Đã hoàn thành dọn phòng <strong>${roomNumber}</strong>. Trạng thái phòng chuyển sang Sẵn sàng.`);
        await loadRoomsToClean();
      } catch (err) {
        console.error('Lỗi hoàn thành nhiệm vụ:', err);
        showAlert('danger', 'Không thể hoàn thành nhiệm vụ: ' + err.message);
        e.currentTarget.disabled = false;
        e.currentTarget.innerHTML = originalText;
      }
    });
  });
}

// Mở modal Sửa phân công
function openEditTaskModal(data) {
  const inputTaskId = document.getElementById('modal-edit-task-id');
  const inputRoomId = document.getElementById('modal-edit-room-id');
  const textRoom = document.getElementById('modal-edit-room-number');
  const textStatus = document.getElementById('modal-edit-current-status');
  const selectStaff = document.getElementById('modal-edit-staff');
  const selectStatus = document.getElementById('modal-edit-cleaning-status');

  if (inputTaskId) inputTaskId.value = data.taskId || '';
  if (inputRoomId) inputRoomId.value = data.roomId || '';
  if (textRoom) textRoom.textContent = data.roomNumber || '--';
  if (textStatus) textStatus.textContent = data.currentStatus || '--';
  editingOriginalStatus = data.cleaningStatus || 'PENDING';

  if (selectStaff) {
    selectStaff.value = data.assignedTo || '';
  }
  if (selectStatus && data.cleaningStatus) {
    selectStatus.value = data.cleaningStatus;
  }

  if (window.$ && $('#editTaskModal').modal) {
    $('#editTaskModal').modal('show');
  }
}

// Lưu thay đổi phân công và tiến độ từ modal Sửa
async function submitEditTask() {
  const taskId = document.getElementById('modal-edit-task-id')?.value;
  const roomId = document.getElementById('modal-edit-room-id')?.value;
  const assignedTo = document.getElementById('modal-edit-staff')?.value;
  const cleaningStatus = document.getElementById('modal-edit-cleaning-status')?.value;
  const roomNumber = document.getElementById('modal-edit-room-number')?.textContent || '';
  const btnSave = document.getElementById('btn-save-edit-task');

  if (btnSave) {
    btnSave.disabled = true;
    btnSave.textContent = 'Đang lưu...';
  }

  try {
    // 1. Cập nhật phân công người dọn (có thể bỏ trống để chưa phân công)
    const assignRes = await apiFetch('/housekeeping/tasks/assign', {
      method: 'POST',
      body: JSON.stringify({
        roomId: parseInt(roomId),
        assignedTo: assignedTo ? parseInt(assignedTo) : null
      })
    });

    const activeTaskId = taskId || assignRes.taskId || assignRes.id;

    // 2. Nếu có thay đổi tiến độ dọn phòng so với ban đầu, gọi update status
    if (activeTaskId && cleaningStatus && cleaningStatus !== editingOriginalStatus) {
      await apiFetch(`/housekeeping/tasks/${activeTaskId}/status?roomId=${roomId || ''}`, {
        method: 'PUT',
        body: JSON.stringify({ status: cleaningStatus })
      });
    }

    if (window.$ && $('#editTaskModal').modal) {
      $('#editTaskModal').modal('hide');
    }

    showAlert('success', `Đã cập nhật thông tin phân công phòng <strong>${roomNumber}</strong> thành công.`);
    await loadRoomsToClean(parseInt(roomId));
  } catch (err) {
    console.error('Lỗi cập nhật phân công:', err);
    showAlert('danger', 'Không thể lưu thông tin: ' + err.message);
  } finally {
    if (btnSave) {
      btnSave.disabled = false;
      btnSave.textContent = 'Lưu thay đổi';
    }
  }
}

// Xóa nhiệm vụ dọn phòng từ modal Sửa
async function submitDeleteTask() {
  const taskId = document.getElementById('modal-edit-task-id')?.value;
  const roomId = document.getElementById('modal-edit-room-id')?.value;
  const roomNumber = document.getElementById('modal-edit-room-number')?.textContent || '';

  if (!(await deluxeConfirm(`Bạn có chắc chắn muốn xóa nhiệm vụ dọn phòng cho phòng ${roomNumber}? Phòng sẽ được chuyển về trạng thái sẵn sàng nếu không còn nhiệm vụ nào khác.`))) {
    return;
  }

  const btnDelete = document.getElementById('btn-delete-task');
  if (btnDelete) {
    btnDelete.disabled = true;
    btnDelete.textContent = 'Đang xóa...';
  }

  try {
    const idToUse = taskId || 0;
    await apiFetch(`/housekeeping/tasks/${idToUse}?roomId=${roomId || ''}`, {
      method: 'DELETE'
    });

    if (window.$ && $('#editTaskModal').modal) {
      $('#editTaskModal').modal('hide');
    }

    showAlert('success', `Đã xóa nhiệm vụ dọn phòng <strong>${roomNumber}</strong> thành công.`);
    await loadRoomsList();
    await loadRoomsToClean();
  } catch (err) {
    console.error('Lỗi xóa nhiệm vụ:', err);
    showAlert('danger', 'Không thể xóa nhiệm vụ: ' + err.message);
  } finally {
    if (btnDelete) {
      btnDelete.disabled = false;
      btnDelete.innerHTML = '<i class="oi oi-trash mr-1"></i>Xóa nhiệm vụ';
    }
  }
}

// Mở modal tạo nhiệm vụ dọn phòng
function openCreateTaskModal() {
  const selectRoom = document.getElementById('modal-create-room');
  const selectStaff = document.getElementById('modal-create-staff');

  if (selectRoom) selectRoom.value = '';
  if (selectStaff) selectStaff.value = '';

  if (window.$ && $('#createTaskModal').modal) {
    $('#createTaskModal').modal('show');
  }
}

// Gửi yêu cầu tạo nhiệm vụ dọn phòng
async function submitCreateTask() {
  const roomId = document.getElementById('modal-create-room')?.value;
  const assignedTo = document.getElementById('modal-create-staff')?.value;
  const btnSave = document.getElementById('btn-save-create-task');

  if (!roomId) {
    await deluxeAlert('Vui lòng chọn phòng cần dọn dẹp.');
    return;
  }

  if (btnSave) {
    btnSave.disabled = true;
    btnSave.textContent = 'Đang tạo...';
  }

  try {
    const res = await apiFetch('/housekeeping/tasks', {
      method: 'POST',
      body: JSON.stringify({
        roomId: parseInt(roomId),
        assignedTo: assignedTo ? parseInt(assignedTo) : null
      })
    });

    if (window.$ && $('#createTaskModal').modal) {
      $('#createTaskModal').modal('hide');
    }

    showAlert('success', `Đã tạo nhiệm vụ dọn phòng <strong>${res.roomNumber || ''}</strong> thành công.`);
    await loadRoomsToClean();
  } catch (err) {
    console.error('Lỗi tạo nhiệm vụ:', err);
    showAlert('danger', 'Không thể tạo nhiệm vụ: ' + err.message);
  } finally {
    if (btnSave) {
      btnSave.disabled = false;
      btnSave.textContent = 'Tạo nhiệm vụ';
    }
  }
}

function setupEventListeners() {
  document.getElementById('btn-open-create-modal')?.addEventListener('click', openCreateTaskModal);
  document.getElementById('btn-save-create-task')?.addEventListener('click', submitCreateTask);
  document.getElementById('btn-save-edit-task')?.addEventListener('click', submitEditTask);
  document.getElementById('btn-delete-task')?.addEventListener('click', submitDeleteTask);

  // Chuyển đổi giữa Tất cả phòng và Nhiệm vụ của tôi
  const btnViewAll = document.getElementById('btn-view-all');
  const btnViewMyTasks = document.getElementById('btn-view-my-tasks');

  btnViewAll?.addEventListener('click', async () => {
    isMyTasksOnly = false;
    currentPage = 1;
    btnViewAll.classList.add('btn-primary', 'active');
    btnViewAll.classList.remove('btn-outline-primary');
    btnViewMyTasks?.classList.remove('btn-primary', 'active');
    btnViewMyTasks?.classList.add('btn-outline-primary');
    await loadRoomsToClean();
  });

  btnViewMyTasks?.addEventListener('click', async () => {
    isMyTasksOnly = true;
    currentPage = 1;
    btnViewMyTasks.classList.add('btn-primary', 'active');
    btnViewMyTasks.classList.remove('btn-outline-primary');
    btnViewAll?.classList.remove('btn-primary', 'active');
    btnViewAll?.classList.add('btn-outline-primary');
    await loadRoomsToClean();
  });

  const searchInput = document.getElementById('search-room');
  if (searchInput) {
    let debounceTimer;
    searchInput.addEventListener('input', () => {
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => {
        currentPage = 1;
        loadRoomsToClean();
      }, 250);
    });
  }

  document.getElementById('filter-status')?.addEventListener('change', () => {
    currentPage = 1;
    loadRoomsToClean();
  });
  document.getElementById('filter-date')?.addEventListener('change', () => {
    currentPage = 1;
    loadRoomsToClean();
  });
  document.getElementById('btn-clear-date')?.addEventListener('click', () => {
    const dateInput = document.getElementById('filter-date');
    if (dateInput) dateInput.value = '';
    currentPage = 1;
    loadRoomsToClean();
  });
  document.getElementById('btn-refresh')?.addEventListener('click', () => loadRoomsToClean());
}

function showAlert(type, htmlMessage) {
  const container = document.getElementById('alert-container');
  if (!container) return;

  const bgClass = type === 'success' ? 'alert-success' : (type === 'danger' ? 'alert-danger' : (type === 'warning' ? 'alert-warning' : 'alert-info'));

  container.innerHTML = `
    <div class="alert ${bgClass} alert-dismissible fade show shadow-sm" role="alert" style="font-size: 13.5px;">
      ${htmlMessage}
      <button type="button" class="close" data-dismiss="alert" aria-label="Đóng">
        <span aria-hidden="true">&times;</span>
      </button>
    </div>
  `;

  if (type === 'success' || type === 'info') {
    setTimeout(() => {
      const alert = container.querySelector('.alert');
      if (alert) alert.remove();
    }, 6000);
  }
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
