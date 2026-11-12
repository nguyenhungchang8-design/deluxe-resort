// maintenance.js - Quản lý sự cố bảo trì phòng (100% Tiếng Việt, chuẩn template Deluxe Resort)
let currentUser = null;
let allMaintenanceData = [];
let filteredMaintenanceData = [];
let roomsList = [];
const PAGE_SIZE = 5;
let currentPage = 1;

document.addEventListener('DOMContentLoaded', async () => {
  try {
    currentUser = await requireRole(['ROLE_HOUSEKEEPING', 'ROLE_RECEPTIONIST', 'ROLE_ADMIN']);
    if (!currentUser) return;

    setupNavbar(currentUser.role);
    await loadRooms();
    await loadMaintenanceRequests();
    setupEventListeners();
  } catch (err) {
    console.error('Lỗi khởi tạo bảo trì:', err);
    showAlert('danger', 'Lỗi khi tải dữ liệu: ' + err.message);
  }
});

function setupNavbar(role) {
  const navContainer = document.getElementById('role-nav-items');
  const userNameEl = document.getElementById('user-name');
  const pageTitle = document.getElementById('page-main-title');
  const breadRole = document.getElementById('bread-role');
  const fullName = currentUser.fullName || currentUser.username;

  if (role === 'ROLE_RECEPTIONIST') {
    if (navContainer) {
      navContainer.innerHTML = `
        <li class="nav-item"><a href="../reception/bookings.html" class="nav-link">Đơn đặt phòng</a></li>
        <li class="nav-item"><a href="../reception/checkin-checkout.html" class="nav-link">Nhận / Trả phòng</a></li>
        <li class="nav-item"><a href="../reception/rooms.html" class="nav-link">Sơ đồ phòng</a></li>
        <li class="nav-item"><a href="tasks.html" class="nav-link">Quản lý dọn phòng</a></li>
        <li class="nav-item active"><a href="maintenance.html" class="nav-link font-weight-bold text-white">Bảo trì phòng</a></li>
        <li class="nav-item"><a href="../reception/invoices.html" class="nav-link">Hóa đơn</a></li>
      `;
    }
    if (pageTitle) pageTitle.textContent = 'Quản lý sự cố bảo trì';
    if (breadRole) breadRole.textContent = 'Lễ tân';
    if (userNameEl) {
      userNameEl.textContent = `Lễ tân: ${fullName}`;
      userNameEl.style.whiteSpace = 'nowrap';
    }
  } else if (role === 'ROLE_HOUSEKEEPING') {
    if (navContainer) {
      navContainer.innerHTML = `
        <li class="nav-item"><a href="tasks.html" class="nav-link">Danh sách dọn phòng</a></li>
        <li class="nav-item active"><a href="maintenance.html" class="nav-link font-weight-bold text-white">Bảo trì phòng</a></li>
      `;
    }
    if (pageTitle) pageTitle.textContent = 'Sự cố bảo trì phòng';
    if (breadRole) breadRole.textContent = 'Buồng phòng';
    if (userNameEl) {
      userNameEl.textContent = `Buồng phòng: ${fullName}`;
      userNameEl.style.whiteSpace = 'nowrap';
    }
  } else if (role === 'ROLE_ADMIN') {
    if (navContainer) {
      navContainer.innerHTML = `
        <li class="nav-item"><a href="../admin/dashboard.html" class="nav-link">Bảng điều khiển</a></li>
        <li class="nav-item"><a href="tasks.html" class="nav-link">Quản lý dọn phòng</a></li>
        <li class="nav-item active"><a href="maintenance.html" class="nav-link font-weight-bold text-white">Bảo trì phòng</a></li>
      `;
    }
    if (pageTitle) pageTitle.textContent = 'Quản lý sự cố bảo trì';
    if (breadRole) breadRole.textContent = 'Quản trị viên';
    if (userNameEl) {
      userNameEl.textContent = `Quản trị viên: ${fullName}`;
      userNameEl.style.whiteSpace = 'nowrap';
    }
  }
}

async function loadRooms() {
  try {
    roomsList = await apiFetch('/housekeeping/rooms');
    const select = document.getElementById('modal-report-room');
    const filterSelect = document.getElementById('filter-room-id');

    if (select) {
      select.innerHTML = '<option value="">-- Chọn phòng gặp sự cố --</option>';
      roomsList.forEach(r => {
        const typeName = formatRoomTypeName(r.roomTypeName);
        select.innerHTML += `<option value="${r.id}">Phòng ${escapeHtml(r.roomNumber)} - ${escapeHtml(typeName)}</option>`;
      });
    }

    if (filterSelect) {
      filterSelect.innerHTML = '<option value="">Tất cả phòng</option>';
      roomsList.forEach(r => {
        filterSelect.innerHTML += `<option value="${r.id}">Phòng ${escapeHtml(r.roomNumber)}</option>`;
      });
    }

    const urlParams = new URLSearchParams(window.location.search);
    const preselectedRoomId = urlParams.get('roomId');
    if (preselectedRoomId && select) {
      select.value = preselectedRoomId;
      openReportMaintenanceModal();
    }
  } catch (e) {
    console.warn('Không thể nạp danh sách phòng:', e);
  }
}

async function loadMaintenanceRequests() {
  const tbody = document.getElementById('maintenance-tbody');
  const loading = document.getElementById('loading-indicator');
  const emptyState = document.getElementById('empty-state');
  const paginationContainer = document.getElementById('pagination-container');

  if (loading) loading.style.display = 'block';
  if (emptyState) emptyState.style.display = 'none';
  if (paginationContainer) paginationContainer.style.display = 'none';
  if (tbody) tbody.innerHTML = '';

  const filterStatus = document.getElementById('filter-status')?.value || 'ALL';
  const filterDate = document.getElementById('filter-date')?.value || '';
  const searchKeyword = (document.getElementById('search-keyword')?.value || '').trim().toLowerCase();

  try {
    const params = new URLSearchParams();
    if (filterStatus && filterStatus !== 'ALL') params.append('status', filterStatus);

    const data = await apiFetch('/housekeeping/maintenance?' + params.toString());
    allMaintenanceData = data;

    let displayedData = data;

    // Lọc theo từ khóa tìm kiếm (phòng, mô tả, người báo)
    if (searchKeyword) {
      displayedData = displayedData.filter(d => 
        (d.roomNumber && String(d.roomNumber).toLowerCase().includes(searchKeyword)) ||
        (d.description && d.description.toLowerCase().includes(searchKeyword)) ||
        (d.reportedByName && d.reportedByName.toLowerCase().includes(searchKeyword))
      );
    }

    // Lọc theo ngày
    if (filterDate) {
      displayedData = displayedData.filter(d => toLocalDateString(d.createdAt) === filterDate);
    }

    // Sắp xếp thời gian mới nhất lên trước
    displayedData.sort((a, b) => {
      const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      if (tb !== ta) return tb - ta;
      return String(a.roomNumber || '').localeCompare(String(b.roomNumber || ''));
    });

    filteredMaintenanceData = displayedData;

    if (loading) loading.style.display = 'none';

    if (!filteredMaintenanceData || filteredMaintenanceData.length === 0) {
      if (emptyState) emptyState.style.display = 'block';
      return;
    }

    const totalPages = Math.max(1, Math.ceil(filteredMaintenanceData.length / PAGE_SIZE));
    if (currentPage > totalPages) currentPage = totalPages;
    if (currentPage < 1) currentPage = 1;

    renderCurrentPage();
  } catch (err) {
    if (loading) loading.style.display = 'none';
    console.error('Lỗi tải danh sách sự cố:', err);
    showAlert('danger', 'Không thể nạp dữ liệu sự cố bảo trì: ' + err.message);
  }
}

function toLocalDateString(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return '';
  const pad = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function renderCurrentPage() {
  const start = (currentPage - 1) * PAGE_SIZE;
  const pageItems = filteredMaintenanceData.slice(start, start + PAGE_SIZE);
  renderTable(pageItems);
  renderPagination();
}

function renderPagination() {
  const container = document.getElementById('pagination-container');
  const infoEl = document.getElementById('pagination-info');
  const controlsEl = document.getElementById('pagination-controls');
  if (!container || !infoEl || !controlsEl) return;

  const total = filteredMaintenanceData.length;
  if (total === 0) {
    container.style.display = 'none';
    return;
  }

  const totalPages = Math.ceil(total / PAGE_SIZE);
  const startItem = (currentPage - 1) * PAGE_SIZE + 1;
  const endItem = Math.min(currentPage * PAGE_SIZE, total);

  container.style.display = 'flex';
  infoEl.textContent = `Hiển thị ${startItem}-${endItem} trên tổng số ${total} sự cố`;

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

function renderTable(list) {
  const tbody = document.getElementById('maintenance-tbody');
  if (!tbody) return;
  tbody.innerHTML = '';

  const isReceptionOrAdmin = currentUser && (currentUser.role === 'ROLE_RECEPTIONIST' || currentUser.role === 'ROLE_ADMIN');

  list.forEach(item => {
    const tr = document.createElement('tr');
    tr.id = 'maintenance-row-' + item.id;

    const typeName = formatRoomTypeName(item.roomTypeName);
    const severityBadge = formatSeverityBadge(item.severity);
    const statusBadge = formatMaintenanceStatusBadge(item.status);
    const timeFormatted = item.formattedCreatedAt || (item.createdAt ? new Date(item.createdAt).toLocaleString('vi-VN') : 'Mới tạo');

    // Thao tác: Phân quyền rõ ràng
    // - Buồng phòng (ROLE_HOUSEKEEPING): CHỈ xem danh sách và thao tác NHẬN VIỆC / HOÀN THÀNH
    // - Lễ tân / Quản trị viên: Nút Sửa (chỉnh sửa trạng thái, xóa sự cố)
    let actionsHtml = '';
    const isHousekeeping = currentUser && currentUser.role === 'ROLE_HOUSEKEEPING';
    const statusUpper = (item.status || '').toUpperCase();

    if (isHousekeeping) {
      if (statusUpper === 'PENDING') {
        actionsHtml = `
          <button class="btn btn-sm btn-primary px-3 font-weight-bold btn-accept-maint" 
                  data-id="${item.id}" 
                  data-room-number="${escapeHtml(item.roomNumber || '')}"
                  title="Bắt đầu xử lý sự cố">
            <i class="oi oi-check mr-1"></i>Nhận nhiệm vụ
          </button>
        `;
      } else if (statusUpper === 'IN_PROGRESS') {
        actionsHtml = `
          <button class="btn btn-sm btn-success px-3 font-weight-bold btn-complete-maint" 
                  data-id="${item.id}" 
                  data-room-number="${escapeHtml(item.roomNumber || '')}"
                  title="Đã khắc phục xong">
            <i class="oi oi-circle-check mr-1"></i>Hoàn thành
          </button>
        `;
      } else if (statusUpper === 'RESOLVED' || statusUpper === 'CLOSED') {
        actionsHtml = `<span class="badge badge-success p-2 font-weight-500">Đã xong</span>`;
      } else {
        actionsHtml = `<span class="text-muted small">--</span>`;
      }
    } else {
      actionsHtml = `
        <button class="btn btn-sm btn-outline-primary px-3 font-weight-bold btn-edit-m-status" 
                data-id="${item.id}" 
                data-room-number="${escapeHtml(item.roomNumber || '')}" 
                data-status="${item.status}">
          <i class="oi oi-pencil mr-1"></i>Sửa
        </button>
      `;
    }

    tr.innerHTML = `
      <td class="align-middle font-weight-bold">
        <span class="badge badge-light border border-danger text-danger px-3 py-2 font-weight-bold" style="font-size: 1rem;">
          ${escapeHtml(item.roomNumber || '')}
        </span>
      </td>
      <td class="align-middle font-weight-bold text-dark">${escapeHtml(typeName)}</td>
      <td class="align-middle" style="max-width: 320px;">
        <div class="font-weight-500 text-dark">${escapeHtml(item.description)}</div>
      </td>
      <td class="align-middle">${severityBadge}</td>
      <td class="align-middle text-muted small">${escapeHtml(timeFormatted)}</td>
      <td class="align-middle">${escapeHtml(item.reportedByName || 'Nhân viên')}</td>
      <td class="align-middle">${statusBadge}</td>
      <td class="align-middle text-right">${actionsHtml}</td>
    `;
    tbody.appendChild(tr);
  });

  // Lễ tân / Quản trị viên: Gắn sự kiện nút Sửa
  document.querySelectorAll('.btn-edit-m-status').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const { id, roomNumber, status } = e.currentTarget.dataset;
      openEditMaintenanceModal(id, roomNumber, status);
    });
  });

  // Buồng phòng: Nhận việc xử lý sự cố bảo trì
  document.querySelectorAll('.btn-accept-maint').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      const { id, roomNumber } = e.currentTarget.dataset;
      const originalText = e.currentTarget.innerHTML;
      e.currentTarget.disabled = true;
      e.currentTarget.textContent = 'Đang nhận việc...';

      try {
        await apiFetch(`/housekeeping/maintenance/${id}/status`, {
          method: 'PUT',
          body: JSON.stringify({ status: 'IN_PROGRESS' })
        });
        showAlert('success', `Đã tiếp nhận xử lý sự cố phòng <strong>${roomNumber}</strong>.`);
        await loadMaintenanceRequests();
        await loadRooms();
      } catch (err) {
        console.error('Lỗi nhận xử lý sự cố:', err);
        showAlert('danger', 'Không thể nhận xử lý sự cố: ' + err.message);
        e.currentTarget.disabled = false;
        e.currentTarget.innerHTML = originalText;
      }
    });
  });

  // Buồng phòng: Hoàn thành xử lý sự cố bảo trì
  document.querySelectorAll('.btn-complete-maint').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      const { id, roomNumber } = e.currentTarget.dataset;
      if (!(await deluxeConfirm(`Xác nhận sự cố phòng ${roomNumber} đã được sửa chữa khắc phục xong?`))) {
        return;
      }

      const originalText = e.currentTarget.innerHTML;
      e.currentTarget.disabled = true;
      e.currentTarget.textContent = 'Đang hoàn thành...';

      try {
        await apiFetch(`/housekeeping/maintenance/${id}/status`, {
          method: 'PUT',
          body: JSON.stringify({ status: 'RESOLVED' })
        });
        showAlert('success', `Đã hoàn thành sửa chữa phòng <strong>${roomNumber}</strong>. Phòng đã được gỡ trạng thái bảo trì.`);
        await loadMaintenanceRequests();
        await loadRooms();
      } catch (err) {
        console.error('Lỗi hoàn thành bảo trì:', err);
        showAlert('danger', 'Không thể hoàn tất bảo trì: ' + err.message);
        e.currentTarget.disabled = false;
        e.currentTarget.innerHTML = originalText;
      }
    });
  });
}

function openEditMaintenanceModal(id, roomNumber, currentStatus) {
  const inputId = document.getElementById('modal-maint-id');
  const textRoom = document.getElementById('modal-maint-room-number');
  const textStatus = document.getElementById('modal-maint-current-status');
  const selectStatus = document.getElementById('modal-maint-status');

  if (inputId) inputId.value = id;
  if (textRoom) textRoom.textContent = roomNumber || '--';
  if (textStatus) {
    const map = {
      'PENDING': 'Chờ tiếp nhận',
      'IN_PROGRESS': 'Đang khắc phục',
      'RESOLVED': 'Đã hoàn thành',
      'CLOSED': 'Đã hoàn thành'
    };
    textStatus.textContent = map[currentStatus] || currentStatus;
  }
  if (selectStatus) {
    selectStatus.value = currentStatus === 'CLOSED' ? 'RESOLVED' : currentStatus;
  }

  if (window.$ && $('#updateMaintenanceModal').modal) {
    $('#updateMaintenanceModal').modal('show');
  }
}

function formatRoomTypeName(name) {
  if (!name) return 'Standard';
  const upper = name.toUpperCase();
  if (upper.includes('DELUXE')) return 'Deluxe';
  if (upper.includes('STANDARD')) return 'Standard';
  if (upper.includes('SUITE')) return 'Suite';
  if (upper.includes('PENTHOUSE') || upper.includes('PRESIDENTIAL')) return 'Tổng thống';
  return name;
}

function formatSeverityBadge(severity) {
  switch ((severity || '').toUpperCase()) {
    case 'CRITICAL':
      return `<span class="badge badge-danger p-2 font-weight-bold">Khẩn cấp</span>`;
    case 'HIGH':
      return `<span class="badge badge-danger p-2 font-weight-bold">Nghiêm trọng</span>`;
    case 'MEDIUM':
      return `<span class="badge badge-warning p-2 text-dark font-weight-bold" style="background-color:#ffeeba;">Trung bình</span>`;
    case 'LOW':
      return `<span class="badge badge-info p-2 font-weight-bold">Nhẹ</span>`;
    default:
      return `<span class="badge badge-secondary p-2 font-weight-bold">${escapeHtml(severity || 'Trung bình')}</span>`;
  }
}

function formatMaintenanceStatusBadge(status) {
  switch ((status || '').toUpperCase()) {
    case 'PENDING':
      return `<span class="badge badge-warning p-2 text-dark font-weight-bold" style="background-color:#ffeeba;">Chờ tiếp nhận</span>`;
    case 'IN_PROGRESS':
      return `<span class="badge badge-primary p-2 font-weight-bold">Đang khắc phục</span>`;
    case 'RESOLVED':
    case 'CLOSED':
      return `<span class="badge badge-success p-2 font-weight-bold">Đã hoàn thành</span>`;
    default:
      return `<span class="badge badge-light p-2 font-weight-bold">${escapeHtml(status || 'Chờ tiếp nhận')}</span>`;
  }
}

async function updateMaintenanceStatus(id, nextStatus) {
  try {
    const res = await apiFetch(`/housekeeping/maintenance/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status: nextStatus })
    });

    const statusLabel = nextStatus === 'RESOLVED' ? 'đã được xử lý xong' : 'đang được khắc phục';
    showAlert('success', `Sự cố phòng <strong>${res.roomNumber}</strong> ${statusLabel}.`);

    await loadMaintenanceRequests();
    await loadRooms();
  } catch (err) {
    showAlert('danger', 'Không thể cập nhật sự cố: ' + err.message);
  }
}

// Xóa sự cố bảo trì từ modal Sửa
async function deleteMaintenanceRequest() {
  const id = document.getElementById('modal-maint-id')?.value;
  const roomNumber = document.getElementById('modal-maint-room-number')?.textContent || '';
  if (!id) return;

  if (!(await deluxeConfirm(`Bạn có chắc chắn muốn xóa sự cố bảo trì của phòng ${roomNumber}? Nếu không còn sự cố nào khác, phòng sẽ tự động thoát trạng thái bảo trì.`))) {
    return;
  }

  const btnDelete = document.getElementById('btn-delete-maint');
  if (btnDelete) {
    btnDelete.disabled = true;
    btnDelete.textContent = 'Đang xóa...';
  }

  try {
    await apiFetch(`/housekeeping/maintenance/${id}`, {
      method: 'DELETE'
    });

    if (window.$ && $('#updateMaintenanceModal').modal) {
      $('#updateMaintenanceModal').modal('hide');
    }

    showAlert('success', `Đã xóa sự cố bảo trì của phòng <strong>${roomNumber}</strong> thành công.`);
    await loadMaintenanceRequests();
    await loadRooms();
  } catch (err) {
    console.error('Lỗi xóa sự cố bảo trì:', err);
    showAlert('danger', 'Không thể xóa sự cố: ' + err.message);
  } finally {
    if (btnDelete) {
      btnDelete.disabled = false;
      btnDelete.innerHTML = '<i class="oi oi-trash mr-1"></i>Xóa sự cố';
    }
  }
}

// Mở modal Báo sự cố phòng
function openReportMaintenanceModal() {
  const select = document.getElementById('modal-report-room');
  const inputDesc = document.getElementById('modal-report-desc');
  const selectSeverity = document.getElementById('modal-report-severity');

  if (select && (!select.children || select.children.length <= 1)) {
    select.innerHTML = '<option value="">-- Chọn phòng gặp sự cố --</option>';
    roomsList.forEach(r => {
      const typeName = formatRoomTypeName(r.roomTypeName);
      select.innerHTML += `<option value="${r.id}">Phòng ${escapeHtml(r.roomNumber)} - ${escapeHtml(typeName)}</option>`;
    });
  }

  if (inputDesc) inputDesc.value = '';
  if (selectSeverity) selectSeverity.value = 'MEDIUM';

  if (window.$ && $('#reportIssueModal').modal) {
    $('#reportIssueModal').modal('show');
  }
}

// Gửi báo cáo sự cố phòng
async function submitReportMaintenance() {
  const select = document.getElementById('modal-report-room');
  const inputDesc = document.getElementById('modal-report-desc');
  const selectSeverity = document.getElementById('modal-report-severity');
  const btnSave = document.getElementById('btn-save-report-issue');

  const roomId = select ? select.value : null;
  const description = inputDesc ? inputDesc.value.trim() : '';
  const severity = selectSeverity ? selectSeverity.value : 'MEDIUM';

  if (!roomId) {
    await deluxeAlert('Vui lòng chọn phòng gặp sự cố.');
    return;
  }
  if (!description) {
    await deluxeAlert('Vui lòng nhập mô tả chi tiết hư hỏng.');
    return;
  }

  if (btnSave) {
    btnSave.disabled = true;
    btnSave.textContent = 'Đang gửi...';
  }

  try {
    const res = await apiFetch('/housekeeping/maintenance', {
      method: 'POST',
      body: JSON.stringify({
        roomId: parseInt(roomId),
        description: description,
        severity: severity
      })
    });

    if (window.$ && $('#reportIssueModal').modal) {
      $('#reportIssueModal').modal('hide');
    }

    showAlert('success', `Đã gửi báo cáo sự cố phòng <strong>${res.roomNumber}</strong> thành công.`);
    await loadRooms();
    await loadMaintenanceRequests();
  } catch (err) {
    console.error('Lỗi gửi báo cáo sự cố:', err);
    showAlert('danger', 'Không thể gửi báo cáo sự cố: ' + err.message);
  } finally {
    if (btnSave) {
      btnSave.disabled = false;
      btnSave.textContent = 'Gửi báo cáo';
    }
  }
}

function setupEventListeners() {
  document.getElementById('btn-open-report-maintenance')?.addEventListener('click', openReportMaintenanceModal);
  document.getElementById('btn-save-report-issue')?.addEventListener('click', submitReportMaintenance);
  document.getElementById('btn-delete-maint')?.addEventListener('click', deleteMaintenanceRequest);
  document.getElementById('btn-save-maint-status')?.addEventListener('click', async () => {
    const id = document.getElementById('modal-maint-id')?.value;
    const nextStatus = document.getElementById('modal-maint-status')?.value;
    if (!id || !nextStatus) return;

    if (window.$ && $('#updateMaintenanceModal').modal) {
      $('#updateMaintenanceModal').modal('hide');
    }
    await updateMaintenanceStatus(id, nextStatus);
  });

  document.getElementById('filter-status')?.addEventListener('change', () => {
    currentPage = 1;
    loadMaintenanceRequests();
  });
  document.getElementById('filter-date')?.addEventListener('change', () => {
    currentPage = 1;
    loadMaintenanceRequests();
  });
  document.getElementById('btn-clear-date')?.addEventListener('click', () => {
    const dateInput = document.getElementById('filter-date');
    if (dateInput) dateInput.value = '';
    currentPage = 1;
    loadMaintenanceRequests();
  });

  const searchKeyword = document.getElementById('search-keyword');
  if (searchKeyword) {
    let debounceTimer;
    searchKeyword.addEventListener('input', () => {
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => {
        currentPage = 1;
        loadMaintenanceRequests();
      }, 250);
    });
  }

  document.getElementById('btn-refresh')?.addEventListener('click', () => {
    currentPage = 1;
    loadMaintenanceRequests();
    loadRooms();
  });
}

function showAlert(type, htmlContent) {
  const container = document.getElementById('alert-container');
  if (!container) return;
  const bgClass = type === 'success' ? 'alert-success' : (type === 'danger' ? 'alert-danger' : (type === 'warning' ? 'alert-warning' : 'alert-info'));

  container.innerHTML = `
    <div class="alert ${bgClass} alert-custom alert-dismissible fade show shadow-sm" role="alert">
      ${htmlContent}
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
