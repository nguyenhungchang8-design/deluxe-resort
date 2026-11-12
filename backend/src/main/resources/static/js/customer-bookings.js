// Chức năng Feature 2 phía Khách hàng (Customer): Xem lịch sử đặt phòng, chi tiết & hủy đặt phòng (UC11)
(function () {
  const {
    escapeHtml,
    formatVND,
    formatDateVN,
    formatDateTimeVN,
    renderBookingStatusBadge,
    showAlert,
    hideAlert
  } = window.F2;

  let allMyBookings = [];
  let currentFilter = 'ALL';

  async function initCustomerBookings() {
    const tbody = document.getElementById('customer-bookings-tbody');
    if (!tbody) return;

    const alertEl = document.getElementById('customer-bookings-alert');

    async function loadBookings() {
      try {
        allMyBookings = await apiFetch('/bookings/my');
        renderStats();
        renderTable();
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    function renderStats() {
      const setCount = (id, count) => {
        const el = document.getElementById(id);
        if (el) el.textContent = count;
      };
      setCount('my-stat-total', allMyBookings.length);
      setCount('my-stat-pending', allMyBookings.filter((b) => b.status === 'PENDING').length);
      setCount('my-stat-confirmed', allMyBookings.filter((b) => b.status === 'CONFIRMED').length);
      setCount('my-stat-checkedin', allMyBookings.filter((b) => b.status === 'CHECKED_IN').length);
    }

    function renderTable() {
      const filtered = currentFilter === 'ALL'
        ? allMyBookings
        : allMyBookings.filter((b) => b.status === currentFilter);

      tbody.innerHTML = '';
      if (filtered.length === 0) {
        tbody.innerHTML = `
          <tr>
            <td colspan="7" class="text-center py-4 text-muted">
              Không có đơn đặt phòng nào${currentFilter !== 'ALL' ? ' ở trạng thái này' : ''}.
              <div class="mt-2"><a href="rooms.html" class="btn btn-primary btn-sm">Tìm &amp; Đặt phòng ngay</a></div>
            </td>
          </tr>
        `;
        return;
      }

      filtered.forEach((b) => {
        const canCancel = b.status === 'PENDING' || b.status === 'CONFIRMED';
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td class="align-middle font-weight-bold" style="color:#8d703b;">#${b.id}</td>
          <td class="align-middle">
            <div class="font-weight-bold">Phòng ${escapeHtml(b.roomNumber || '-')}</div>
            <small class="text-muted">${escapeHtml(b.roomTypeName || '')}</small>
          </td>
          <td class="align-middle small">
            <div>Nhận: <strong>${formatDateVN(b.checkIn)}</strong></div>
            <div>Trả: <strong>${formatDateVN(b.checkOut)}</strong> (${b.nights || 1} đêm)</div>
          </td>
          <td class="align-middle">${formatVND(b.totalAmount || 0)}</td>
          <td class="align-middle">${formatVND(b.deposit || 0)}</td>
          <td class="align-middle">${renderBookingStatusBadge(b.status)}</td>
          <td class="align-middle text-nowrap">
            <button type="button" class="btn btn-outline-secondary btn-sm mr-1 btn-view-detail" data-id="${b.id}">Chi tiết</button>
            ${canCancel ? `<button type="button" class="btn btn-outline-danger btn-sm btn-cancel-booking" data-id="${b.id}">Hủy đơn</button>` : ''}
          </td>
        `;
        tbody.appendChild(tr);
      });

      tbody.querySelectorAll('.btn-view-detail').forEach((btn) => {
        btn.addEventListener('click', () => openBookingDetail(btn.dataset.id));
      });
      tbody.querySelectorAll('.btn-cancel-booking').forEach((btn) => {
        btn.addEventListener('click', () => cancelBooking(btn.dataset.id));
      });
    }

    async function openBookingDetail(bookingId) {
      hideAlert(alertEl);
      try {
        const b = await apiFetch(`/bookings/${bookingId}`);
        const setText = (id, text) => {
          const el = document.getElementById(id);
          if (el) el.textContent = text;
        };
        setText('cus-dt-id', `#${b.id}`);
        setText('cus-dt-room', `Phòng ${b.roomNumber || '-'} — ${b.roomTypeName || ''}`);
        setText('cus-dt-dates', `${formatDateVN(b.checkIn)} → ${formatDateVN(b.checkOut)} (${b.nights || 1} đêm)`);
        setText('cus-dt-rate', formatVND(b.pricePerNight || 0) + ' / đêm');
        setText('cus-dt-total', formatVND(b.totalAmount || 0));
        setText('cus-dt-deposit', formatVND(b.deposit || 0));
        setText('cus-dt-created', formatDateTimeVN(b.createdAt));

        const statusWrap = document.getElementById('cus-dt-status');
        if (statusWrap) statusWrap.innerHTML = renderBookingStatusBadge(b.status);

        const cancelModalBtn = document.getElementById('cus-dt-cancel-btn');
        if (cancelModalBtn) {
          const canCancel = b.status === 'PENDING' || b.status === 'CONFIRMED';
          cancelModalBtn.classList.toggle('d-none', !canCancel);
          cancelModalBtn.onclick = async () => {
            if (window.$ && $('#customerBookingDetailModal').length) {
              $('#customerBookingDetailModal').modal('hide');
            }
            await cancelBooking(b.id);
          };
        }

        if (window.$ && $('#customerBookingDetailModal').length) {
          $('#customerBookingDetailModal').modal('show');
        }
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    async function cancelBooking(bookingId) {
      if (!confirm(`Bạn có chắc chắn muốn hủy đơn đặt phòng #${bookingId}?`)) return;
      hideAlert(alertEl);
      try {
        await apiFetch(`/bookings/${bookingId}/cancel`, { method: 'PUT' });
        showAlert(alertEl, `Đã hủy đơn đặt phòng #${bookingId} thành công.`, 'success');
        await loadBookings();
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    document.querySelectorAll('.booking-filter-tab').forEach((btn) => {
      btn.addEventListener('click', () => {
        currentFilter = btn.dataset.status || 'ALL';
        document.querySelectorAll('.booking-filter-tab').forEach((x) => {
          x.classList.remove('btn-primary');
          x.classList.add('btn-outline-secondary');
        });
        btn.classList.remove('btn-outline-secondary');
        btn.classList.add('btn-primary');
        renderTable();
      });
    });

    await loadBookings();
  }

  document.addEventListener('DOMContentLoaded', initCustomerBookings);
})();

