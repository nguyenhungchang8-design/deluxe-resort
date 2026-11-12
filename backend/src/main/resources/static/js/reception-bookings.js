// Lễ tân - Quản lý đặt phòng (UC08, UC09, UC10, UC11)
(function () {
  const {
    escapeHtml,
    formatVND,
    formatDateVN,
    formatDateTimeVN,
    todayIso,
    addDaysIso,
    nightsBetween,
    renderBookingStatusBadge,
    showAlert,
    hideAlert
  } = window.F2;

  let allBookings = [];
  let roomTypes = [];
  let customerOptions = [];

  async function initReceptionBookings() {
    const tbody = document.getElementById('reception-bookings-tbody');
    if (!tbody) return;

    const alertEl = document.getElementById('reception-bookings-alert');
    const filterSelect = document.getElementById('rec-booking-status-filter');
    const searchInput = document.getElementById('rec-booking-search');
    const btnRefresh = document.getElementById('btn-refresh-bookings');

    async function loadDashboardData() {
      try {
        const [bookings, todayIn, todayOut, availRooms] = await Promise.all([
          apiFetch('/bookings'),
          apiFetch('/bookings/today/check-in'),
          apiFetch('/bookings/today/check-out'),
          apiFetch('/rooms/status/AVAILABLE')
        ]);
        allBookings = bookings || [];

        const setCount = (id, val) => {
          const el = document.getElementById(id);
          if (el) el.textContent = val;
        };
        setCount('stat-today-checkin', todayIn.length);
        setCount('stat-today-checkout', todayOut.length);
        setCount('stat-available-rooms', availRooms.length);
        setCount('stat-pending-bookings', allBookings.filter((b) => b.status === 'PENDING').length);

        renderTable();
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    function renderTable() {
      const statusVal = filterSelect ? filterSelect.value : 'ALL';
      const keyword = (searchInput ? searchInput.value : '').trim().toLowerCase();

      const filtered = allBookings.filter((b) => {
        if (statusVal !== 'ALL' && b.status !== statusVal) return false;
        if (keyword) {
          const text = `#${b.id} ${b.customerName || ''} ${b.customerEmail || ''} ${b.roomNumber || ''} ${b.roomTypeName || ''}`.toLowerCase();
          if (!text.includes(keyword)) return false;
        }
        return true;
      });

      tbody.innerHTML = '';
      if (filtered.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">Không có đơn đặt phòng phù hợp.</td></tr>';
        return;
      }

      filtered.forEach((b) => {
        const canConfirm = b.status === 'PENDING';
        const canCancel = b.status === 'PENDING' || b.status === 'CONFIRMED';

        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td class="align-middle font-weight-bold" style="color:#8d703b;">#${b.id}</td>
          <td class="align-middle">
            <div class="font-weight-bold">${escapeHtml(b.customerName || 'Khách hàng #' + b.customerId)}</div>
            <small class="text-muted">${escapeHtml(b.customerEmail || '')}</small>
          </td>
          <td class="align-middle">
            <div class="font-weight-bold">Phòng ${escapeHtml(b.roomNumber || '-')}</div>
            <small class="text-muted">${escapeHtml(b.roomTypeName || '')}</small>
          </td>
          <td class="align-middle small">
            <div>Nhận: <strong>${formatDateVN(b.checkIn)}</strong></div>
            <div>Trả: <strong>${formatDateVN(b.checkOut)}</strong> (${b.nights || 1} đêm)</div>
          </td>
          <td class="align-middle small">
            <div>Tổng: <strong>${formatVND(b.totalAmount || 0)}</strong></div>
            <div class="text-muted">Cọc: ${formatVND(b.deposit || 0)}</div>
          </td>
          <td class="align-middle">${renderBookingStatusBadge(b.status)}</td>
          <td class="align-middle text-nowrap">
            <button type="button" class="btn btn-outline-secondary btn-sm mr-1 mb-1 btn-rec-detail" data-id="${b.id}">Chi tiết</button>
            ${canConfirm ? `<button type="button" class="btn btn-success btn-sm mr-1 mb-1 btn-rec-confirm" data-id="${b.id}">Xác nhận</button>` : ''}
            ${canCancel ? `<button type="button" class="btn btn-outline-danger btn-sm mb-1 btn-rec-cancel" data-id="${b.id}">Hủy bỏ</button>` : ''}
          </td>
        `;
        tbody.appendChild(tr);
      });

      tbody.querySelectorAll('.btn-rec-detail').forEach((btn) => {
        btn.addEventListener('click', () => openBookingDetail(btn.dataset.id));
      });
      tbody.querySelectorAll('.btn-rec-confirm').forEach((btn) => {
        btn.addEventListener('click', () => performBookingAction(btn.dataset.id, 'confirm', 'xác nhận'));
      });
      tbody.querySelectorAll('.btn-rec-cancel').forEach((btn) => {
        btn.addEventListener('click', () => performBookingAction(btn.dataset.id, 'cancel', 'hủy bỏ'));
      });
    }

    async function performBookingAction(id, action, label) {
      if (action === 'cancel' && !confirm(`Xác nhận hủy đơn đặt phòng #${id}?`)) return;
      hideAlert(alertEl);
      try {
        await apiFetch(`/bookings/${id}/${action}`, { method: 'PUT' });
        showAlert(alertEl, `Đã ${label} đơn đặt phòng #${id} thành công.`, 'success');
        await loadDashboardData();
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    async function openBookingDetail(id) {
      hideAlert(alertEl);
      try {
        const b = await apiFetch(`/bookings/${id}`);
        const setText = (elId, text) => {
          const el = document.getElementById(elId);
          if (el) el.textContent = text;
        };
        setText('rec-dt-id', `#${b.id}`);
        setText('rec-dt-customer', `${b.customerName || ''} (${b.customerEmail || 'ID ' + b.customerId})`);
        setText('rec-dt-room', `Phòng ${b.roomNumber || '-'} — ${b.roomTypeName || ''}`);
        setText('rec-dt-dates', `${formatDateVN(b.checkIn)} → ${formatDateVN(b.checkOut)} (${b.nights || 1} đêm)`);
        setText('rec-dt-rate', formatVND(b.pricePerNight || 0) + ' / đêm');
        setText('rec-dt-total', formatVND(b.totalAmount || 0));
        setText('rec-dt-deposit', formatVND(b.deposit || 0));
        setText('rec-dt-created', formatDateTimeVN(b.createdAt));

        const statusWrap = document.getElementById('rec-dt-status');
        if (statusWrap) statusWrap.innerHTML = renderBookingStatusBadge(b.status);

        const actionsWrap = document.getElementById('rec-dt-actions');
        if (actionsWrap) {
          actionsWrap.innerHTML = '';
          const addBtn = (text, cls, action, label) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = `btn btn-sm ${cls} mr-2`;
            btn.textContent = text;
            btn.addEventListener('click', async () => {
              if (window.$ && $('#recBookingDetailModal').length) {
                $('#recBookingDetailModal').modal('hide');
              }
              await performBookingAction(b.id, action, label);
            });
            actionsWrap.appendChild(btn);
          };

          if (b.status === 'PENDING') {
            addBtn('Xác nhận đặt phòng', 'btn-success', 'confirm', 'xác nhận');
          }
          if (b.status === 'CONFIRMED') {
            addBtn('Làm thủ tục Check-in', 'btn-primary', 'check-in', 'check-in');
          }
          if (b.status === 'CHECKED_IN') {
            addBtn('Làm thủ tục Check-out', 'btn-warning text-white', 'check-out', 'check-out');
          }
          if (b.status === 'PENDING' || b.status === 'CONFIRMED') {
            addBtn('Hủy đơn', 'btn-outline-danger', 'cancel', 'hủy bỏ');
          }
        }

        if (window.$ && $('#recBookingDetailModal').length) {
          $('#recBookingDetailModal').modal('show');
        }
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    // ---- Modal Tạo Đặt Phòng Mới (UC08 + UC09 cho Lễ tân) ----
    async function setupCreateBookingModal() {
      const btnOpen = document.getElementById('btn-open-create-booking');
      const form = document.getElementById('rec-create-booking-form');
      if (!btnOpen || !form) return;

      const cusSelect = document.getElementById('rec-bk-customer');
      const typeSelect = document.getElementById('rec-bk-roomtype');
      const inInput = document.getElementById('rec-bk-checkin');
      const outInput = document.getElementById('rec-bk-checkout');
      const roomSelect = document.getElementById('rec-bk-room');
      const depositInput = document.getElementById('rec-bk-deposit');
      const estEl = document.getElementById('rec-bk-estimated');
      const modalAlert = document.getElementById('rec-create-booking-alert');
      const btnFindAvail = document.getElementById('btn-rec-find-avail');

      let currentAvailRooms = [];

      function updateEstimate() {
        if (!estEl) return;
        const selectedRoom = currentAvailRooms.find((r) => String(r.id) === String(roomSelect?.value));
        const nights = nightsBetween(inInput?.value, outInput?.value);
        if (selectedRoom && nights > 0) {
          const total = Number(selectedRoom.pricePerNight || 0) * nights;
          estEl.textContent = `${nights} đêm × ${formatVND(selectedRoom.pricePerNight)} = ${formatVND(total)}`;
        } else {
          estEl.textContent = 'Chọn ngày và phòng trống để xem dự tính tiền phòng';
        }
      }

      async function findAvail() {
        hideAlert(modalAlert);
        const checkIn = inInput?.value;
        const checkOut = outInput?.value;
        const roomTypeId = typeSelect?.value || '';
        if (!checkIn || !checkOut) {
          showAlert(modalAlert, 'Vui lòng chọn ngày nhận và ngày trả phòng.', 'warning');
          return;
        }
        try {
          const q = new URLSearchParams({ checkIn, checkOut });
          if (roomTypeId) q.set('roomTypeId', roomTypeId);
          currentAvailRooms = await apiFetch('/rooms/available?' + q.toString());
          if (roomSelect) {
            roomSelect.innerHTML = '';
            if (currentAvailRooms.length === 0) {
              roomSelect.innerHTML = '<option value="">-- Không có phòng trống trong khoảng ngày này --</option>';
              showAlert(modalAlert, 'Không tìm thấy phòng trống phù hợp.', 'warning');
            } else {
              currentAvailRooms.forEach((r) => {
                const opt = document.createElement('option');
                opt.value = r.id;
                opt.textContent = `Phòng ${r.roomNumber} — ${r.roomTypeName} (${formatVND(r.pricePerNight)}/đêm)`;
                roomSelect.appendChild(opt);
              });
            }
          }
          updateEstimate();
        } catch (err) {
          showAlert(modalAlert, err.message, 'danger');
        }
      }

      btnOpen.addEventListener('click', async () => {
        hideAlert(modalAlert);
        const today = todayIso();
        if (inInput) {
          inInput.min = today;
          inInput.value = today;
        }
        if (outInput) {
          outInput.min = addDaysIso(today, 1);
          outInput.value = addDaysIso(today, 2);
        }
        if (depositInput) depositInput.value = '0';

        try {
          if (customerOptions.length === 0) {
            customerOptions = await apiFetch('/bookings/customers');
          }
          if (roomTypes.length === 0) {
            roomTypes = await apiFetch('/room-types');
          }
          if (cusSelect) {
            cusSelect.innerHTML = '<option value="">-- Đặt cho tài khoản đang đăng nhập --</option>';
            customerOptions.forEach((c) => {
              const opt = document.createElement('option');
              opt.value = c.id;
              opt.textContent = `${c.fullName} (${c.username})`;
              cusSelect.appendChild(opt);
            });
            if (customerOptions.length > 0) cusSelect.value = String(customerOptions[0].id);
          }
          if (typeSelect) {
            typeSelect.innerHTML = '<option value="">Tất cả hạng phòng</option>';
            roomTypes.forEach((rt) => {
              const opt = document.createElement('option');
              opt.value = rt.id;
              opt.textContent = `${rt.name} (${formatVND(rt.pricePerNight)})`;
              typeSelect.appendChild(opt);
            });
          }
          await findAvail();
        } catch (err) {
          showAlert(modalAlert, err.message, 'danger');
        }

        if (window.$ && $('#recCreateBookingModal').length) {
          $('#recCreateBookingModal').modal('show');
        }
      });

      btnFindAvail?.addEventListener('click', findAvail);
      inInput?.addEventListener('change', () => {
        if (inInput.value) {
          outInput.min = addDaysIso(inInput.value, 1);
          if (outInput.value <= inInput.value) outInput.value = addDaysIso(inInput.value, 1);
        }
        findAvail();
      });
      outInput?.addEventListener('change', findAvail);
      typeSelect?.addEventListener('change', findAvail);
      roomSelect?.addEventListener('change', updateEstimate);

      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideAlert(modalAlert);
        const roomId = roomSelect?.value;
        if (!roomId) {
          showAlert(modalAlert, 'Vui lòng chọn một phòng trống.', 'warning');
          return;
        }
        try {
          const payload = {
            roomId: Number(roomId),
            checkIn: inInput.value,
            checkOut: outInput.value,
            deposit: depositInput?.value ? Number(depositInput.value) : 0
          };
          if (cusSelect?.value) payload.customerId = Number(cusSelect.value);

          const created = await apiFetch('/bookings', {
            method: 'POST',
            body: JSON.stringify(payload)
          });
          if (window.$ && $('#recCreateBookingModal').length) {
            $('#recCreateBookingModal').modal('hide');
          }
          showAlert(alertEl, `Tạo đơn đặt phòng #${created.id} (Phòng ${created.roomNumber}) thành công!`, 'success');
          await loadDashboardData();
        } catch (err) {
          showAlert(modalAlert, err.message, 'danger');
        }
      });
    }

    filterSelect?.addEventListener('change', renderTable);
    searchInput?.addEventListener('input', renderTable);
    btnRefresh?.addEventListener('click', loadDashboardData);

    await setupCreateBookingModal();
    await loadDashboardData();
  }

  document.addEventListener('DOMContentLoaded', initReceptionBookings);
})();

