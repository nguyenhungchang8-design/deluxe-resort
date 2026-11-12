// Lễ tân - Sơ đồ phòng & Quản lý trạng thái phòng (UC07, UC08, UC14)
(function () {
  const {
    escapeHtml,
    formatVND,
    formatDateVN,
    todayIso,
    addDaysIso,
    ROOM_STATUS_META,
    renderRoomStatusBadge,
    showAlert,
    hideAlert
  } = window.F2;

  let allRooms = [];
  let roomTypes = [];
  let availFilterActive = false;

  async function initReceptionRooms() {
    const tbody = document.getElementById('reception-rooms-tbody');
    if (!tbody) return;

    const alertEl = document.getElementById('reception-rooms-alert');
    const statusFilter = document.getElementById('rec-room-status-filter');
    const typeFilter = document.getElementById('rec-room-type-filter');
    const checkInInput = document.getElementById('rec-avail-checkin');
    const checkOutInput = document.getElementById('rec-avail-checkout');
    const btnSearchAvail = document.getElementById('btn-rec-avail-search');
    const btnClearAvail = document.getElementById('btn-rec-avail-clear');
    const gridWrap = document.getElementById('reception-rooms-cards');

    if (checkInInput) checkInInput.min = todayIso();
    if (checkOutInput) checkOutInput.min = addDaysIso(todayIso(), 1);

    async function loadRooms() {
      availFilterActive = false;
      try {
        const [rooms, types] = await Promise.all([
          apiFetch('/rooms'),
          apiFetch('/room-types')
        ]);
        allRooms = rooms || [];
        roomTypes = types || [];

        if (typeFilter && typeFilter.options.length <= 1) {
          typeFilter.innerHTML = '<option value="">Tất cả hạng phòng</option>';
          roomTypes.forEach((rt) => {
            const opt = document.createElement('option');
            opt.value = rt.id;
            opt.textContent = rt.name;
            typeFilter.appendChild(opt);
          });
        }

        renderStats(allRooms);
        renderRooms();
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    function renderStats(rooms) {
      const setCount = (id, st) => {
        const el = document.getElementById(id);
        if (el) el.textContent = rooms.filter((r) => r.status === st).length;
      };
      setCount('rec-rm-available', 'AVAILABLE');
      setCount('rec-rm-booked', 'BOOKED');
      setCount('rec-rm-occupied', 'OCCUPIED');
      setCount('rec-rm-cleaning', 'CLEANING');
      setCount('rec-rm-maintenance', 'MAINTENANCE');
    }

    function getFilteredRooms() {
      const st = statusFilter ? statusFilter.value : 'ALL';
      const rt = typeFilter ? typeFilter.value : '';
      return allRooms.filter((r) => {
        if (!availFilterActive && st !== 'ALL' && r.status !== st) return false;
        if (rt && String(r.roomTypeId) !== String(rt)) return false;
        return true;
      });
    }

    function renderRooms() {
      const list = getFilteredRooms();

      // Render Sơ đồ trực quan (cards)
      if (gridWrap) {
        gridWrap.innerHTML = '';
        list.forEach((r) => {
          const meta = ROOM_STATUS_META[r.status] || { color: '#475569', bg: '#e2e8f0', label: r.status };
          const cardCol = document.createElement('div');
          cardCol.className = 'col-6 col-md-4 col-lg-2 mb-3';
          cardCol.innerHTML = `
            <div class="p-3 bg-white rounded shadow-sm text-center h-100" style="border-top: 4px solid ${meta.color};">
              <div class="font-weight-bold" style="font-size:20px;">P.${escapeHtml(r.roomNumber)}</div>
              <div class="small text-muted mb-2">${escapeHtml(r.roomTypeName || '')}</div>
              <div>${renderRoomStatusBadge(r.status)}</div>
            </div>
          `;
          gridWrap.appendChild(cardCol);
        });
      }

      // Render Bảng thao tác đổi trạng thái
      tbody.innerHTML = '';
      if (list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">Không có phòng nào phù hợp.</td></tr>';
        return;
      }

      list.forEach((r) => {
        const statusOptions = Object.keys(ROOM_STATUS_META)
          .map((st) => `<option value="${st}" ${r.status === st ? 'selected' : ''}>${ROOM_STATUS_META[st].label}</option>`)
          .join('');

        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td class="align-middle font-weight-bold">Phòng ${escapeHtml(r.roomNumber)}</td>
          <td class="align-middle">${escapeHtml(r.roomTypeName || '-')}</td>
          <td class="align-middle">${formatVND(r.pricePerNight || 0)}</td>
          <td class="align-middle">${r.capacity || 2} khách</td>
          <td class="align-middle">${renderRoomStatusBadge(r.status)}</td>
          <td class="align-middle">
            <div class="d-flex align-items-center">
              <select class="form-control form-control-sm mr-2 rec-room-status-select" data-id="${r.id}" style="max-width: 170px;">
                ${statusOptions}
              </select>
              <button type="button" class="btn btn-primary btn-sm btn-save-room-status" data-id="${r.id}">Lưu</button>
            </div>
          </td>
        `;
        tbody.appendChild(tr);
      });

      tbody.querySelectorAll('.btn-save-room-status').forEach((btn) => {
        btn.addEventListener('click', async () => {
          const roomId = btn.dataset.id;
          const select = tbody.querySelector(`.rec-room-status-select[data-id="${roomId}"]`);
          const newStatus = select ? select.value : '';
          await updateRoomStatus(roomId, newStatus);
        });
      });
    }

    async function updateRoomStatus(roomId, newStatus) {
      hideAlert(alertEl);
      try {
        const updated = await apiFetch(`/rooms/${roomId}/status`, {
          method: 'PUT',
          body: JSON.stringify({ status: newStatus })
        });
        showAlert(alertEl, `Đã cập nhật trạng thái Phòng ${updated.roomNumber} thành "${ROOM_STATUS_META[updated.status]?.label || updated.status}".`, 'success');
        if (availFilterActive) {
          await searchAvailableByDate();
        } else {
          await loadRooms();
        }
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    async function searchAvailableByDate() {
      hideAlert(alertEl);
      const checkIn = checkInInput?.value;
      const checkOut = checkOutInput?.value;
      if (!checkIn || !checkOut) {
        showAlert(alertEl, 'Vui lòng chọn đầy đủ ngày nhận và ngày trả phòng để tìm phòng trống.', 'warning');
        return;
      }
      try {
        const q = new URLSearchParams({ checkIn, checkOut });
        if (typeFilter?.value) q.set('roomTypeId', typeFilter.value);
        allRooms = await apiFetch('/rooms/available?' + q.toString());
        availFilterActive = true;
        showAlert(alertEl, `Tìm thấy ${allRooms.length} phòng trống từ ${formatDateVN(checkIn)} đến ${formatDateVN(checkOut)}.`, 'info');
        renderRooms();
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    statusFilter?.addEventListener('change', renderRooms);
    typeFilter?.addEventListener('change', () => {
      if (availFilterActive) searchAvailableByDate();
      else renderRooms();
    });
    btnSearchAvail?.addEventListener('click', searchAvailableByDate);
    btnClearAvail?.addEventListener('click', () => {
      if (checkInInput) checkInInput.value = '';
      if (checkOutInput) checkOutInput.value = '';
      if (statusFilter) statusFilter.value = 'ALL';
      if (typeFilter) typeFilter.value = '';
      hideAlert(alertEl);
      loadRooms();
    });
    checkInInput?.addEventListener('change', () => {
      if (checkInInput.value && checkOutInput) {
        checkOutInput.min = addDaysIso(checkInInput.value, 1);
        if (checkOutInput.value && checkOutInput.value <= checkInInput.value) {
          checkOutInput.value = addDaysIso(checkInInput.value, 1);
        }
      }
    });

    await loadRooms();
  }

  document.addEventListener('DOMContentLoaded', initReceptionRooms);
})();

