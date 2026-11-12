// Lễ tân - Thủ tục Nhận & Trả phòng (UC12 Check-in, UC13 Check-out)
(function () {
  const {
    escapeHtml,
    formatVND,
    formatDateVN,
    formatDateTimeVN,
    todayIso,
    renderBookingStatusBadge,
    renderRoomStatusBadge,
    showAlert,
    hideAlert
  } = window.F2;

  let confirmedBookings = [];
  let checkedInBookings = [];
  let todayCheckInIds = new Set();
  let todayCheckOutIds = new Set();
  let activeTab = 'CHECKIN'; // CHECKIN | CHECKOUT

  async function initReceptionCheckin() {
    const tbody = document.getElementById('checkin-checkout-tbody');
    if (!tbody) return;

    const alertEl = document.getElementById('checkin-alert');
    const onlyTodayCheckbox = document.getElementById('chk-only-today');
    const searchInput = document.getElementById('chk-search');
    const btnRefresh = document.getElementById('btn-refresh-checkin');

    async function loadData() {
      try {
        const [todayIn, todayOut, allConfirmed, allCheckedIn] = await Promise.all([
          apiFetch('/bookings/today/check-in'),
          apiFetch('/bookings/today/check-out'),
          apiFetch('/bookings/status/CONFIRMED'),
          apiFetch('/bookings/status/CHECKED_IN')
        ]);

        confirmedBookings = allConfirmed || [];
        checkedInBookings = allCheckedIn || [];
        todayCheckInIds = new Set((todayIn || []).map((b) => b.id));
        todayCheckOutIds = new Set((todayOut || []).map((b) => b.id));

        const setCount = (id, val) => {
          const el = document.getElementById(id);
          if (el) el.textContent = val;
        };
        setCount('stat-ci-today', todayIn.length);
        setCount('stat-ci-all', confirmedBookings.length);
        setCount('stat-co-today', todayOut.length);
        setCount('stat-co-all', checkedInBookings.length);

        renderTable();
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    function renderTable() {
      const onlyToday = onlyTodayCheckbox ? onlyTodayCheckbox.checked : false;
      const keyword = (searchInput ? searchInput.value : '').trim().toLowerCase();
      const today = todayIso();

      const source = activeTab === 'CHECKIN' ? confirmedBookings : checkedInBookings;
      const todaySet = activeTab === 'CHECKIN' ? todayCheckInIds : todayCheckOutIds;

      const filtered = source.filter((b) => {
        if (onlyToday && !todaySet.has(b.id)) return false;
        if (keyword) {
          const text = `#${b.id} ${b.customerName || ''} ${b.roomNumber || ''} ${b.roomTypeName || ''}`.toLowerCase();
          if (!text.includes(keyword)) return false;
        }
        return true;
      });

      tbody.innerHTML = '';
      if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">Không có đơn nào trong danh sách ${activeTab === 'CHECKIN' ? 'chờ nhận phòng' : 'đang lưu trú'}${onlyToday ? ' hôm nay' : ''}.</td></tr>`;
        return;
      }

      filtered.forEach((b) => {
        const isDueToday = activeTab === 'CHECKIN' ? b.checkIn === today : b.checkOut === today;
        const dueBadge = isDueToday
          ? '<span class="badge badge-danger ml-1">Hôm nay</span>'
          : '';

        const actionBtn = activeTab === 'CHECKIN'
          ? `<button type="button" class="btn btn-primary btn-sm mr-1 mb-1 btn-do-checkin" data-id="${b.id}">Nhận phòng (Check-in)</button>`
          : `<button type="button" class="btn btn-warning text-white btn-sm mr-1 mb-1 btn-do-checkout" data-id="${b.id}">Trả phòng (Check-out)</button>`;

        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td class="align-middle font-weight-bold" style="color:#8d703b;">#${b.id} ${dueBadge}</td>
          <td class="align-middle">
            <div class="font-weight-bold">${escapeHtml(b.customerName || 'Khách #' + b.customerId)}</div>
            <small class="text-muted">${escapeHtml(b.customerEmail || '')}</small>
          </td>
          <td class="align-middle">
            <div class="font-weight-bold">Phòng ${escapeHtml(b.roomNumber || '-')}</div>
            <div>${renderRoomStatusBadge(b.roomStatus)}</div>
          </td>
          <td class="align-middle">${escapeHtml(b.roomTypeName || '-')}</td>
          <td class="align-middle small">
            <div>Nhận: <strong>${formatDateVN(b.checkIn)}</strong></div>
            <div>Trả: <strong>${formatDateVN(b.checkOut)}</strong> (${b.nights || 1} đêm)</div>
          </td>
          <td class="align-middle">${renderBookingStatusBadge(b.status)}</td>
          <td class="align-middle text-nowrap">
            ${actionBtn}
            <button type="button" class="btn btn-outline-secondary btn-sm mb-1 btn-chk-detail" data-id="${b.id}">Chi tiết</button>
          </td>
        `;
        tbody.appendChild(tr);
      });

      tbody.querySelectorAll('.btn-do-checkin').forEach((btn) => {
        btn.addEventListener('click', () => handleCheckIn(btn.dataset.id));
      });
      tbody.querySelectorAll('.btn-do-checkout').forEach((btn) => {
        btn.addEventListener('click', () => handleCheckOut(btn.dataset.id));
      });
      tbody.querySelectorAll('.btn-chk-detail').forEach((btn) => {
        btn.addEventListener('click', () => openDetail(btn.dataset.id));
      });
    }

    async function handleCheckIn(id) {
      if (!confirm(`Xác nhận làm thủ tục Nhận phòng (Check-in) cho đơn #${id}?`)) return;
      hideAlert(alertEl);
      try {
        const res = await apiFetch(`/bookings/${id}/check-in`, { method: 'PUT' });
        showAlert(alertEl, `Check-in thành công đơn #${res.id}! Phòng ${res.roomNumber} đã chuyển sang Đang có khách (OCCUPIED).`, 'success');
        await loadData();
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    async function handleCheckOut(id) {
      if (!confirm(`Xác nhận làm thủ tục Trả phòng (Check-out) cho đơn #${id}? Phòng sẽ chuyển sang trạng thái Đang dọn dẹp (CLEANING).`)) return;
      hideAlert(alertEl);
      try {
        const res = await apiFetch(`/bookings/${id}/check-out`, { method: 'PUT' });
        showAlert(alertEl, `Check-out thành công đơn #${res.id}! Phòng ${res.roomNumber} đã chuyển sang Đang dọn dẹp (CLEANING).`, 'success');
        await loadData();
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    async function openDetail(id) {
      hideAlert(alertEl);
      try {
        const b = await apiFetch(`/bookings/${id}`);
        const setText = (elId, text) => {
          const el = document.getElementById(elId);
          if (el) el.textContent = text;
        };
        setText('chk-dt-id', `#${b.id}`);
        setText('chk-dt-customer', `${b.customerName || ''} (${b.customerEmail || 'ID ' + b.customerId})`);
        setText('chk-dt-room', `Phòng ${b.roomNumber || '-'} — ${b.roomTypeName || ''}`);
        setText('chk-dt-dates', `${formatDateVN(b.checkIn)} → ${formatDateVN(b.checkOut)} (${b.nights || 1} đêm)`);
        setText('chk-dt-total', formatVND(b.totalAmount || 0));
        setText('chk-dt-deposit', formatVND(b.deposit || 0));
        setText('chk-dt-created', formatDateTimeVN(b.createdAt));
        const st = document.getElementById('chk-dt-status');
        if (st) st.innerHTML = renderBookingStatusBadge(b.status);

        if (window.$ && $('#chkDetailModal').length) {
          $('#chkDetailModal').modal('show');
        }
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    document.querySelectorAll('.chk-tab-btn').forEach((btn) => {
      btn.addEventListener('click', () => {
        activeTab = btn.dataset.tab;
        document.querySelectorAll('.chk-tab-btn').forEach((x) => {
          x.classList.remove('btn-primary');
          x.classList.add('btn-outline-secondary');
        });
        btn.classList.remove('btn-outline-secondary');
        btn.classList.add('btn-primary');
        renderTable();
      });
    });

    onlyTodayCheckbox?.addEventListener('change', renderTable);
    searchInput?.addEventListener('input', renderTable);
    btnRefresh?.addEventListener('click', loadData);

    await loadData();
  }

  document.addEventListener('DOMContentLoaded', initReceptionCheckin);
})();

