// Quản trị viên (Admin) - Quản lý Phòng & Hạng phòng (CRUD Rooms & RoomTypes)
(function () {
  const {
    escapeHtml,
    formatVND,
    ROOM_STATUS_META,
    renderRoomStatusBadge,
    showAlert,
    hideAlert
  } = window.F2;

  let allRooms = [];
  let allRoomTypes = [];

  async function initAdminRooms() {
    const roomsTbody = document.getElementById('admin-rooms-tbody');
    const typesTbody = document.getElementById('admin-roomtypes-tbody');
    if (!roomsTbody || !typesTbody) return;

    const alertEl = document.getElementById('admin-rooms-alert');
    const roomModalAlert = document.getElementById('admin-room-modal-alert');
    const typeModalAlert = document.getElementById('admin-type-modal-alert');

    async function loadAll() {
      try {
        const [rooms, types] = await Promise.all([
          apiFetch('/rooms'),
          apiFetch('/room-types')
        ]);
        allRooms = rooms || [];
        allRoomTypes = types || [];
        renderRoomTypeOptions();
        renderRoomsTable();
        renderTypesTable();
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    function renderRoomTypeOptions() {
      const sel = document.getElementById('adm-rm-type');
      const filterSel = document.getElementById('adm-filter-type');
      if (sel) {
        sel.innerHTML = '';
        allRoomTypes.forEach((rt) => {
          const opt = document.createElement('option');
          opt.value = rt.id;
          opt.textContent = `${rt.name} (${formatVND(rt.pricePerNight)})`;
          sel.appendChild(opt);
        });
      }
      if (filterSel) {
        const cur = filterSel.value;
        filterSel.innerHTML = '<option value="">Tất cả hạng phòng</option>';
        allRoomTypes.forEach((rt) => {
          const opt = document.createElement('option');
          opt.value = rt.id;
          opt.textContent = rt.name;
          filterSel.appendChild(opt);
        });
        filterSel.value = cur;
      }
    }

    function renderRoomsTable() {
      const filterType = document.getElementById('adm-filter-type')?.value || '';
      const filterStatus = document.getElementById('adm-filter-status')?.value || '';

      const list = allRooms.filter((r) => {
        if (filterType && String(r.roomTypeId) !== String(filterType)) return false;
        if (filterStatus && r.status !== filterStatus) return false;
        return true;
      });

      roomsTbody.innerHTML = '';
      if (list.length === 0) {
        roomsTbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">Không có phòng nào.</td></tr>';
        return;
      }

      list.forEach((r) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td class="align-middle font-weight-bold">Phòng ${escapeHtml(r.roomNumber)}</td>
          <td class="align-middle">${escapeHtml(r.roomTypeName || '-')}</td>
          <td class="align-middle">${formatVND(r.pricePerNight || 0)}</td>
          <td class="align-middle">${r.capacity || 2} khách</td>
          <td class="align-middle">${renderRoomStatusBadge(r.status)}</td>
          <td class="align-middle text-nowrap">
            <button type="button" class="btn btn-primary btn-sm mr-1 mb-1 btn-edit-room" data-id="${r.id}">Sửa</button>
            <button type="button" class="btn btn-outline-danger btn-sm mb-1 btn-del-room" data-id="${r.id}">Xóa</button>
          </td>
        `;
        roomsTbody.appendChild(tr);
      });

      roomsTbody.querySelectorAll('.btn-edit-room').forEach((btn) => {
        btn.addEventListener('click', () => openRoomModal(btn.dataset.id));
      });
      roomsTbody.querySelectorAll('.btn-del-room').forEach((btn) => {
        btn.addEventListener('click', () => deleteRoom(btn.dataset.id));
      });
    }

    function renderTypesTable() {
      typesTbody.innerHTML = '';
      if (allRoomTypes.length === 0) {
        typesTbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">Chưa có hạng phòng nào.</td></tr>';
        return;
      }

      allRoomTypes.forEach((rt) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td class="align-middle">#${rt.id}</td>
          <td class="align-middle font-weight-bold">${escapeHtml(rt.name)}</td>
          <td class="align-middle">${formatVND(rt.pricePerNight)}</td>
          <td class="align-middle">${rt.capacity || 2} người</td>
          <td class="align-middle small text-muted">${escapeHtml(rt.description || '')}</td>
          <td class="align-middle text-nowrap">
            <button type="button" class="btn btn-primary btn-sm mr-1 mb-1 btn-edit-type" data-id="${rt.id}">Sửa</button>
            <button type="button" class="btn btn-outline-danger btn-sm mb-1 btn-del-type" data-id="${rt.id}">Xóa</button>
          </td>
        `;
        typesTbody.appendChild(tr);
      });

      typesTbody.querySelectorAll('.btn-edit-type').forEach((btn) => {
        btn.addEventListener('click', () => openTypeModal(btn.dataset.id));
      });
      typesTbody.querySelectorAll('.btn-del-type').forEach((btn) => {
        btn.addEventListener('click', () => deleteType(btn.dataset.id));
      });
    }

    // ---- Room Modal ----
    function openRoomModal(roomId) {
      hideAlert(roomModalAlert);
      const idInput = document.getElementById('adm-rm-id');
      const numInput = document.getElementById('adm-rm-number');
      const typeSelect = document.getElementById('adm-rm-type');
      const statusSelect = document.getElementById('adm-rm-status');
      const titleEl = document.getElementById('adminRoomModalTitle');

      if (roomId) {
        const r = allRooms.find((x) => String(x.id) === String(roomId));
        if (!r) return;
        if (titleEl) titleEl.textContent = `Cập nhật Phòng ${r.roomNumber}`;
        idInput.value = r.id;
        numInput.value = r.roomNumber;
        typeSelect.value = r.roomTypeId;
        statusSelect.value = r.status;
      } else {
        if (titleEl) titleEl.textContent = 'Thêm Phòng Mới';
        idInput.value = '';
        numInput.value = '';
        if (typeSelect.options.length > 0) typeSelect.selectedIndex = 0;
        statusSelect.value = 'AVAILABLE';
      }

      if (window.$ && $('#adminRoomModal').length) {
        $('#adminRoomModal').modal('show');
      }
    }

    async function deleteRoom(id) {
      const r = allRooms.find((x) => String(x.id) === String(id));
      if (!confirm(`Xác nhận xóa Phòng ${r ? r.roomNumber : '#' + id}?`)) return;
      hideAlert(alertEl);
      try {
        await apiFetch(`/rooms/${id}`, { method: 'DELETE' });
        showAlert(alertEl, 'Đã xóa phòng thành công.', 'success');
        await loadAll();
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    document.getElementById('btn-open-add-room')?.addEventListener('click', () => openRoomModal(null));
    document.getElementById('admin-room-form')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      hideAlert(roomModalAlert);
      const id = document.getElementById('adm-rm-id').value;
      const payload = {
        roomNumber: document.getElementById('adm-rm-number').value.trim(),
        roomTypeId: Number(document.getElementById('adm-rm-type').value),
        status: document.getElementById('adm-rm-status').value
      };
      try {
        if (id) {
          await apiFetch(`/rooms/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
          showAlert(alertEl, `Đã cập nhật Phòng ${payload.roomNumber}.`, 'success');
        } else {
          await apiFetch('/rooms', { method: 'POST', body: JSON.stringify(payload) });
          showAlert(alertEl, `Đã thêm Phòng ${payload.roomNumber}.`, 'success');
        }
        if (window.$ && $('#adminRoomModal').length) $('#adminRoomModal').modal('hide');
        await loadAll();
      } catch (err) {
        showAlert(roomModalAlert, err.message, 'danger');
      }
    });

    // ---- Room Type Modal ----
    function openTypeModal(typeId) {
      hideAlert(typeModalAlert);
      const idInput = document.getElementById('adm-rt-id');
      const nameInput = document.getElementById('adm-rt-name');
      const priceInput = document.getElementById('adm-rt-price');
      const capInput = document.getElementById('adm-rt-capacity');
      const descInput = document.getElementById('adm-rt-desc');
      const titleEl = document.getElementById('adminTypeModalTitle');

      if (typeId) {
        const rt = allRoomTypes.find((x) => String(x.id) === String(typeId));
        if (!rt) return;
        if (titleEl) titleEl.textContent = `Cập nhật Hạng phòng: ${rt.name}`;
        idInput.value = rt.id;
        nameInput.value = rt.name;
        priceInput.value = rt.pricePerNight;
        capInput.value = rt.capacity || 2;
        descInput.value = rt.description || '';
      } else {
        if (titleEl) titleEl.textContent = 'Thêm Hạng Phòng Mới';
        idInput.value = '';
        nameInput.value = '';
        priceInput.value = '';
        capInput.value = '2';
        descInput.value = '';
      }

      if (window.$ && $('#adminTypeModal').length) {
        $('#adminTypeModal').modal('show');
      }
    }

    async function deleteType(id) {
      const rt = allRoomTypes.find((x) => String(x.id) === String(id));
      if (!confirm(`Xác nhận xóa hạng phòng "${rt ? rt.name : '#' + id}"?`)) return;
      hideAlert(alertEl);
      try {
        await apiFetch(`/room-types/${id}`, { method: 'DELETE' });
        showAlert(alertEl, 'Đã xóa hạng phòng thành công.', 'success');
        await loadAll();
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    document.getElementById('btn-open-add-type')?.addEventListener('click', () => openTypeModal(null));
    document.getElementById('admin-type-form')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      hideAlert(typeModalAlert);
      const id = document.getElementById('adm-rt-id').value;
      const payload = {
        name: document.getElementById('adm-rt-name').value.trim(),
        pricePerNight: Number(document.getElementById('adm-rt-price').value),
        capacity: Number(document.getElementById('adm-rt-capacity').value),
        description: document.getElementById('adm-rt-desc').value.trim()
      };
      try {
        if (id) {
          await apiFetch(`/room-types/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
          showAlert(alertEl, `Đã cập nhật hạng phòng "${payload.name}".`, 'success');
        } else {
          await apiFetch('/room-types', { method: 'POST', body: JSON.stringify(payload) });
          showAlert(alertEl, `Đã thêm hạng phòng "${payload.name}".`, 'success');
        }
        if (window.$ && $('#adminTypeModal').length) $('#adminTypeModal').modal('hide');
        await loadAll();
      } catch (err) {
        showAlert(typeModalAlert, err.message, 'danger');
      }
    });

    // Tab switching & filters
    document.getElementById('adm-filter-type')?.addEventListener('change', renderRoomsTable);
    document.getElementById('adm-filter-status')?.addEventListener('change', renderRoomsTable);

    document.querySelectorAll('.adm-room-tab').forEach((btn) => {
      btn.addEventListener('click', () => {
        const tab = btn.dataset.tab;
        document.querySelectorAll('.adm-room-tab').forEach((b) => {
          b.classList.remove('btn-primary');
          b.classList.add('btn-outline-secondary');
        });
        btn.classList.remove('btn-outline-secondary');
        btn.classList.add('btn-primary');
        document.getElementById('adm-panel-rooms')?.classList.toggle('d-none', tab !== 'ROOMS');
        document.getElementById('adm-panel-types')?.classList.toggle('d-none', tab !== 'TYPES');
      });
    });

    await loadAll();
  }

  document.addEventListener('DOMContentLoaded', initAdminRooms);
})();

