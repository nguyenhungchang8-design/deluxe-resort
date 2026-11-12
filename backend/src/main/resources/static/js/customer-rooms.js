// Chức năng Feature 2 phía Khách hàng (Customer): Xem phòng, Tìm phòng trống, Chi tiết & Đặt phòng
(function () {
  const {
    escapeHtml,
    formatVND,
    formatDateVN,
    todayIso,
    addDaysIso,
    parseDateToIso,
    nightsBetween,
    roomImageForType,
    renderRoomStatusBadge,
    showAlert,
    hideAlert
  } = window.F2;

  let cachedRoomTypes = [];
  let cachedRooms = [];

  async function ensureCatalogLoaded() {
    if (cachedRoomTypes.length === 0) {
      cachedRoomTypes = await apiFetch('/room-types');
    }
    if (cachedRooms.length === 0) {
      cachedRooms = await apiFetch('/rooms');
    }
  }

  function findTypeById(id) {
    return cachedRoomTypes.find((t) => String(t.id) === String(id));
  }

  // =========================================================================
  // 1. TRANG CHỦ CUSTOMER (portal/customer/home.html)
  // =========================================================================
  async function initCustomerHome() {
    const grid = document.getElementById('customer-home-rooms-grid');
    const searchForm = document.getElementById('home-booking-search-form');
    if (!grid && !searchForm) return;

    try {
      await ensureCatalogLoaded();
    } catch (e) {
      return; // Nếu chưa đăng nhập hoặc lỗi, auth.js sẽ điều hướng
    }

    const typeSelect = document.getElementById('home-room-type-select');
    if (typeSelect) {
      typeSelect.innerHTML = '<option value="">Tất cả hạng phòng</option>';
      cachedRoomTypes.forEach((rt) => {
        const opt = document.createElement('option');
        opt.value = rt.id;
        opt.textContent = `${rt.name} (${formatVND(rt.pricePerNight)})`;
        typeSelect.appendChild(opt);
      });
    }

    if (searchForm) {
      searchForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const rawIn = document.getElementById('home-checkin')?.value || '';
        const rawOut = document.getElementById('home-checkout')?.value || '';
        const checkIn = parseDateToIso(rawIn);
        const checkOut = parseDateToIso(rawOut);
        const roomTypeId = document.getElementById('home-room-type-select')?.value || '';
        const capacity = document.getElementById('home-capacity-select')?.value || '';

        if ((checkIn && !checkOut) || (!checkIn && checkOut)) {
          alert('Vui lòng chọn cả Ngày nhận phòng và Ngày trả phòng để tìm phòng trống.');
          return;
        }
        if (checkIn && checkOut) {
          if (checkIn < todayIso()) {
            alert('Ngày nhận phòng không được ở trong quá khứ.');
            return;
          }
          if (checkOut <= checkIn) {
            alert('Ngày trả phòng phải sau ngày nhận phòng.');
            return;
          }
        }

        const params = new URLSearchParams();
        if (checkIn) params.set('checkIn', checkIn);
        if (checkOut) params.set('checkOut', checkOut);
        if (roomTypeId) params.set('roomTypeId', roomTypeId);
        if (capacity) params.set('capacity', capacity);
        window.location.href = 'rooms.html' + (params.toString() ? '?' + params.toString() : '');
      });
    }

    if (grid) {
      grid.innerHTML = '';
      if (cachedRoomTypes.length === 0) {
        grid.innerHTML = '<div class="col-12 text-center text-muted">Chưa có hạng phòng nào trong hệ thống.</div>';
        return;
      }

      cachedRoomTypes.forEach((rt) => {
        const roomsOfType = cachedRooms.filter((r) => r.roomTypeId === rt.id);
        const availableCount = roomsOfType.filter((r) => r.status === 'AVAILABLE').length;
        const imgUrl = roomImageForType(rt.id);

        const col = document.createElement('div');
        col.className = 'col-sm-12 col-md-6 col-lg-3 mb-4 d-flex';
        col.innerHTML = `
          <div class="room w-100 d-flex flex-column bg-white shadow-sm rounded overflow-hidden">
            <a href="rooms-single.html?roomTypeId=${rt.id}" class="img d-flex justify-content-center align-items-center" style="background-image: url(${imgUrl}); height: 220px;">
              <div class="icon d-flex justify-content-center align-items-center">
                <span class="icon-search2"></span>
              </div>
            </a>
            <div class="text p-3 text-center d-flex flex-column flex-grow-1">
              <h3 class="mb-2" style="font-size:20px;"><a href="rooms-single.html?roomTypeId=${rt.id}">${escapeHtml(rt.name)}</a></h3>
              <p class="mb-2"><span class="price mr-1">${formatVND(rt.pricePerNight)}</span> <span class="per">/ đêm</span></p>
              <p class="small text-muted mb-2">Sức chứa: <strong>${rt.capacity || 2} người</strong> &bull; Hiện trống: <strong class="text-success">${availableCount}/${roomsOfType.length}</strong></p>
              <p class="small text-muted flex-grow-1">${escapeHtml(rt.description || '')}</p>
              <hr class="my-2">
              <p class="pt-1 mb-0"><a href="rooms-single.html?roomTypeId=${rt.id}" class="btn-custom">Xem &amp; Đặt phòng <span class="icon-long-arrow-right"></span></a></p>
            </div>
          </div>
        `;
        grid.appendChild(col);
      });
    }
  }

  // =========================================================================
  // 2. TRANG DANH SÁCH PHÒNG & TÌM PHÒNG TRỐNG (portal/customer/rooms.html)
  // =========================================================================
  async function initCustomerRoomsPage() {
    const grid = document.getElementById('customer-rooms-grid');
    if (!grid) return;

    const searchForm = document.getElementById('customer-room-search-form');
    const checkInInput = document.getElementById('search-checkin');
    const checkOutInput = document.getElementById('search-checkout');
    const typeSelect = document.getElementById('search-room-type');
    const capacitySelect = document.getElementById('search-capacity');
    const maxPriceSelect = document.getElementById('search-max-price');
    const bannerEl = document.getElementById('customer-rooms-status-banner');
    const pillsEl = document.getElementById('room-type-filter-pills');
    const resetBtn = document.getElementById('btn-reset-room-search');

    try {
      await ensureCatalogLoaded();
    } catch (e) {
      grid.innerHTML = `<div class="col-12"><div class="alert alert-danger">${escapeHtml(e.message)}</div></div>`;
      return;
    }

    const today = todayIso();
    if (checkInInput) checkInInput.min = today;
    if (checkOutInput) checkOutInput.min = addDaysIso(today, 1);

    if (typeSelect) {
      typeSelect.innerHTML = '<option value="">Tất cả hạng phòng</option>';
      cachedRoomTypes.forEach((rt) => {
        const opt = document.createElement('option');
        opt.value = rt.id;
        opt.textContent = `${rt.name} - ${formatVND(rt.pricePerNight)}`;
        typeSelect.appendChild(opt);
      });
    }

    // Đọc query params nếu có từ Home
    const urlParams = new URLSearchParams(window.location.search);
    if (checkInInput && urlParams.get('checkIn')) checkInInput.value = urlParams.get('checkIn');
    if (checkOutInput && urlParams.get('checkOut')) checkOutInput.value = urlParams.get('checkOut');
    if (typeSelect && urlParams.get('roomTypeId')) typeSelect.value = urlParams.get('roomTypeId');
    if (capacitySelect && urlParams.get('capacity')) capacitySelect.value = urlParams.get('capacity');
    if (maxPriceSelect && urlParams.get('maxPrice')) maxPriceSelect.value = urlParams.get('maxPrice');

    function renderTypePills() {
      if (!pillsEl) return;
      const activeType = typeSelect ? typeSelect.value : '';
      pillsEl.innerHTML = '';

      const allBtn = document.createElement('button');
      allBtn.type = 'button';
      allBtn.className = `btn btn-sm mr-2 mb-2 ${!activeType ? 'btn-primary' : 'btn-outline-secondary'}`;
      allBtn.textContent = `Tất cả (${cachedRoomTypes.length} hạng)`;
      allBtn.addEventListener('click', () => {
        if (typeSelect) typeSelect.value = '';
        executeSearch();
      });
      pillsEl.appendChild(allBtn);

      cachedRoomTypes.forEach((rt) => {
        const btn = document.createElement('button');
        btn.type = 'button';
        const isActive = String(activeType) === String(rt.id);
        btn.className = `btn btn-sm mr-2 mb-2 ${isActive ? 'btn-primary' : 'btn-outline-secondary'}`;
        btn.textContent = `${rt.name} (${formatVND(rt.pricePerNight)})`;
        btn.addEventListener('click', () => {
          if (typeSelect) typeSelect.value = String(rt.id);
          executeSearch();
        });
        pillsEl.appendChild(btn);
      });
    }

    async function executeSearch() {
      renderTypePills();
      const checkIn = checkInInput?.value || '';
      const checkOut = checkOutInput?.value || '';
      const roomTypeId = typeSelect?.value || '';
      const capacity = capacitySelect?.value || '';
      const maxPrice = maxPriceSelect?.value ? Number(maxPriceSelect.value) : null;

      let rooms = [];
      let isDateSearch = false;
      let nights = 0;

      if (checkIn || checkOut) {
        if (!checkIn || !checkOut) {
          showAlert(bannerEl, 'Vui lòng chọn đầy đủ cả Ngày nhận phòng và Ngày trả phòng.', 'warning');
          return;
        }
        if (checkIn < todayIso()) {
          showAlert(bannerEl, 'Ngày nhận phòng không được là ngày trong quá khứ.', 'danger');
          return;
        }
        if (checkOut <= checkIn) {
          showAlert(bannerEl, 'Ngày trả phòng phải sau ngày nhận phòng.', 'danger');
          return;
        }

        isDateSearch = true;
        nights = nightsBetween(checkIn, checkOut);
        const q = new URLSearchParams({ checkIn, checkOut });
        if (roomTypeId) q.set('roomTypeId', roomTypeId);
        if (capacity) q.set('capacity', capacity);

        try {
          rooms = await apiFetch('/rooms/available?' + q.toString());
        } catch (err) {
          showAlert(bannerEl, err.message, 'danger');
          grid.innerHTML = '';
          return;
        }
      } else {
        // Xem toàn bộ danh sách phòng (UC07)
        try {
          const q = new URLSearchParams();
          if (roomTypeId) q.set('roomTypeId', roomTypeId);
          const url = q.toString() ? '/rooms?' + q.toString() : '/rooms';
          rooms = await apiFetch(url);
        } catch (err) {
          showAlert(bannerEl, err.message, 'danger');
          return;
        }

        if (capacity) {
          const minCap = Number(capacity);
          rooms = rooms.filter((r) => (r.capacity || 0) >= minCap);
        }
      }

      if (maxPrice) {
        rooms = rooms.filter((r) => Number(r.pricePerNight || 0) <= maxPrice);
      }

      if (isDateSearch) {
        showAlert(
          bannerEl,
          `Tìm thấy ${rooms.length} phòng trống từ ${formatDateVN(checkIn)} đến ${formatDateVN(checkOut)} (${nights} đêm).`,
          rooms.length > 0 ? 'success' : 'warning'
        );
      } else {
        showAlert(
          bannerEl,
          `Đang hiển thị ${rooms.length} phòng thuộc hệ thống Deluxe Resort. Chọn ngày nhận/trả phòng ở cột phải để lọc phòng trống theo lịch.`,
          'info'
        );
      }

      renderRoomsGrid(rooms, isDateSearch, checkIn, checkOut, nights);
    }

    function renderRoomsGrid(rooms, isDateSearch, checkIn, checkOut, nights) {
      grid.innerHTML = '';
      if (!rooms || rooms.length === 0) {
        grid.innerHTML = `
          <div class="col-12">
            <div class="bg-white p-5 text-center rounded shadow-sm">
              <h5 class="mb-2">Không tìm thấy phòng phù hợp</h5>
              <p class="text-muted mb-3">Vui lòng thử chọn khoảng ngày khác hoặc nới lỏng điều kiện tìm kiếm.</p>
              <button type="button" class="btn btn-outline-primary btn-sm" id="btn-inline-reset">Xem tất cả phòng</button>
            </div>
          </div>
        `;
        document.getElementById('btn-inline-reset')?.addEventListener('click', resetSearch);
        return;
      }

      rooms.forEach((r) => {
        const imgUrl = roomImageForType(r.roomTypeId);
        const detailParams = new URLSearchParams({
          roomId: r.id,
          roomTypeId: r.roomTypeId
        });
        if (checkIn) detailParams.set('checkIn', checkIn);
        if (checkOut) detailParams.set('checkOut', checkOut);
        const detailUrl = `rooms-single.html?${detailParams.toString()}`;

        const estimatedBlock = isDateSearch && nights > 0
          ? `<div class="small text-primary font-weight-bold mb-2">Ước tính ${nights} đêm: ${formatVND(Number(r.pricePerNight || 0) * nights)}</div>`
          : '';

        const isMaintenance = r.status === 'MAINTENANCE';
        const btnLabel = isDateSearch ? 'Đặt phòng này' : (isMaintenance ? 'Xem chi tiết' : 'Chi tiết & Đặt phòng');

        const col = document.createElement('div');
        col.className = 'col-sm-12 col-md-6 col-lg-4 mb-4 d-flex';
        col.innerHTML = `
          <div class="room w-100 d-flex flex-column bg-white shadow-sm rounded overflow-hidden">
            <a href="${detailUrl}" class="img d-flex justify-content-center align-items-center" style="background-image: url(${imgUrl}); height: 220px;">
              <div class="icon d-flex justify-content-center align-items-center">
                <span class="icon-search2"></span>
              </div>
            </a>
            <div class="text p-3 text-center d-flex flex-column flex-grow-1">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="badge badge-light border px-2 py-1" style="font-size:12px;">Phòng ${escapeHtml(r.roomNumber)}</span>
                ${renderRoomStatusBadge(r.status)}
              </div>
              <h3 class="mb-2" style="font-size:19px;"><a href="${detailUrl}">${escapeHtml(r.roomTypeName || 'Hạng phòng')}</a></h3>
              <p class="mb-2"><span class="price mr-1">${formatVND(r.pricePerNight)}</span> <span class="per">/ đêm</span></p>
              ${estimatedBlock}
              <ul class="list mb-2 flex-grow-1">
                <li><span>Sức chứa tối đa:</span> ${r.capacity || 2} người</li>
                <li><span>Mô tả:</span> ${escapeHtml((r.description || '').slice(0, 75))}${(r.description || '').length > 75 ? '...' : ''}</li>
              </ul>
              <hr class="my-2">
              <p class="pt-1 mb-0">
                <a href="${detailUrl}" class="btn-custom">${btnLabel} <span class="icon-long-arrow-right"></span></a>
              </p>
            </div>
          </div>
        `;
        grid.appendChild(col);
      });
    }

    function resetSearch() {
      if (checkInInput) checkInInput.value = '';
      if (checkOutInput) checkOutInput.value = '';
      if (typeSelect) typeSelect.value = '';
      if (capacitySelect) capacitySelect.value = '';
      if (maxPriceSelect) maxPriceSelect.value = '';
      executeSearch();
    }

    if (searchForm) {
      searchForm.addEventListener('submit', (e) => {
        e.preventDefault();
        executeSearch();
      });
    }
    if (resetBtn) {
      resetBtn.addEventListener('click', resetSearch);
    }
    if (checkInInput && checkOutInput) {
      checkInInput.addEventListener('change', () => {
        if (checkInInput.value) {
          checkOutInput.min = addDaysIso(checkInInput.value, 1);
          if (checkOutInput.value && checkOutInput.value <= checkInInput.value) {
            checkOutInput.value = addDaysIso(checkInInput.value, 1);
          }
        }
      });
    }

    executeSearch();
  }

  // =========================================================================
  // 3. TRANG CHI TIẾT PHÒNG & FORM ĐẶT PHÒNG (portal/customer/rooms-single.html)
  // =========================================================================
  async function initCustomerRoomSingle() {
    const form = document.getElementById('booking-form');
    if (!form) return;

    const urlParams = new URLSearchParams(window.location.search);
    let selectedRoomId = urlParams.get('roomId') || '';
    let selectedRoomTypeId = urlParams.get('roomTypeId') || '';
    const paramCheckIn = urlParams.get('checkIn') || '';
    const paramCheckOut = urlParams.get('checkOut') || '';

    const typeSelect = document.getElementById('bk-room-type');
    const checkInInput = document.getElementById('bk-checkin');
    const checkOutInput = document.getElementById('bk-checkout');
    const roomSelect = document.getElementById('bk-room-id');
    const depositInput = document.getElementById('bk-deposit');
    const alertEl = document.getElementById('booking-alert');
    const btnCheckAvail = document.getElementById('btn-check-avail');
    const btnSuggestDeposit = document.getElementById('btn-suggest-deposit');
    const btnSubmit = document.getElementById('btn-submit-booking');

    try {
      await ensureCatalogLoaded();
    } catch (e) {
      showAlert(alertEl, e.message, 'danger');
      return;
    }

    if (!selectedRoomTypeId && selectedRoomId) {
      const r = cachedRooms.find((item) => String(item.id) === String(selectedRoomId));
      if (r) selectedRoomTypeId = String(r.roomTypeId);
    }
    if (!selectedRoomTypeId && cachedRoomTypes.length > 0) {
      selectedRoomTypeId = String(cachedRoomTypes[0].id);
    }

    // Populate roomType select
    if (typeSelect) {
      typeSelect.innerHTML = '';
      cachedRoomTypes.forEach((rt) => {
        const opt = document.createElement('option');
        opt.value = rt.id;
        opt.textContent = `${rt.name} (${formatVND(rt.pricePerNight)}/đêm)`;
        if (String(rt.id) === String(selectedRoomTypeId)) opt.selected = true;
        typeSelect.appendChild(opt);
      });
    }

    // Default dates
    const today = todayIso();
    if (checkInInput) {
      checkInInput.min = today;
      checkInInput.value = paramCheckIn && paramCheckIn >= today ? paramCheckIn : today;
    }
    if (checkOutInput) {
      const baseIn = checkInInput ? checkInInput.value : today;
      checkOutInput.min = addDaysIso(baseIn, 1);
      checkOutInput.value = paramCheckOut && paramCheckOut > baseIn ? paramCheckOut : addDaysIso(baseIn, 2);
    }

    function renderRoomTypeDetails(rtId) {
      const rt = findTypeById(rtId);
      if (!rt) return;

      const setText = (id, text) => {
        const el = document.getElementById(id);
        if (el) el.textContent = text;
      };
      setText('detail-type-name', rt.name);
      setText('detail-bread-name', rt.name);
      setText('detail-price', formatVND(rt.pricePerNight) + ' / đêm');
      setText('detail-capacity', `${rt.capacity || 2} Người lớn`);
      setText('detail-description', rt.description || 'Phòng nghỉ sang trọng chuẩn 5 sao tại Deluxe Resort.');

      const mainImg = document.getElementById('detail-main-image');
      if (mainImg) {
        mainImg.style.backgroundImage = `url(${roomImageForType(rt.id)})`;
      }

      // Render danh sách các phòng thuộc hạng này
      const roomsWrap = document.getElementById('detail-rooms-in-type');
      if (roomsWrap) {
        const roomsOfType = cachedRooms.filter((r) => String(r.roomTypeId) === String(rt.id));
        roomsWrap.innerHTML = '';
        roomsOfType.forEach((r) => {
          const badge = document.createElement('button');
          badge.type = 'button';
          const isSelected = String(r.id) === String(selectedRoomId);
          badge.className = `btn btn-sm mr-2 mb-2 ${isSelected ? 'btn-primary' : 'btn-outline-secondary'}`;
          badge.innerHTML = `Phòng ${escapeHtml(r.roomNumber)} &bull; ${renderRoomStatusBadge(r.status)}`;
          badge.addEventListener('click', () => {
            selectedRoomId = String(r.id);
            if (roomSelect) {
              const optExists = Array.from(roomSelect.options).some((o) => o.value === String(r.id));
              if (optExists) roomSelect.value = String(r.id);
            }
            renderRoomTypeDetails(rt.id);
          });
          roomsWrap.appendChild(badge);
        });
      }

      // Render các hạng phòng khác
      const othersWrap = document.getElementById('detail-other-types');
      if (othersWrap) {
        othersWrap.innerHTML = '';
        cachedRoomTypes
          .filter((other) => String(other.id) !== String(rt.id))
          .slice(0, 2)
          .forEach((other) => {
            const col = document.createElement('div');
            col.className = 'col-sm-12 col-md-6 mb-3';
            col.innerHTML = `
              <div class="room bg-white shadow-sm rounded overflow-hidden">
                <a href="rooms-single.html?roomTypeId=${other.id}" class="img img-2 d-flex justify-content-center align-items-center" style="background-image: url(${roomImageForType(other.id)}); height: 190px;">
                  <div class="icon d-flex justify-content-center align-items-center"><span class="icon-search2"></span></div>
                </a>
                <div class="text p-3 text-center">
                  <h3 class="mb-2" style="font-size:18px;"><a href="rooms-single.html?roomTypeId=${other.id}">${escapeHtml(other.name)}</a></h3>
                  <p class="mb-2"><span class="price mr-1">${formatVND(other.pricePerNight)}</span> <span class="per">/ đêm</span></p>
                  <hr class="my-2">
                  <p class="pt-1 mb-0"><a href="rooms-single.html?roomTypeId=${other.id}" class="btn-custom">Xem chi tiết <span class="icon-long-arrow-right"></span></a></p>
                </div>
              </div>
            `;
            othersWrap.appendChild(col);
          });
      }
    }

    function updateSummary() {
      const rt = findTypeById(typeSelect?.value || selectedRoomTypeId);
      const nights = nightsBetween(checkInInput?.value, checkOutInput?.value);
      const price = Number(rt?.pricePerNight || 0);
      const total = price * nights;

      const elNights = document.getElementById('bk-nights');
      const elRate = document.getElementById('bk-price-per-night');
      const elTotal = document.getElementById('bk-total-amount');
      if (elNights) elNights.textContent = nights > 0 ? `${nights} đêm` : '0 đêm';
      if (elRate) elRate.textContent = formatVND(price);
      if (elTotal) elTotal.textContent = formatVND(total);
      return total;
    }

    async function loadAvailableRoomsForBooking() {
      hideAlert(alertEl);
      const checkIn = checkInInput?.value || '';
      const checkOut = checkOutInput?.value || '';
      const rtId = typeSelect?.value || selectedRoomTypeId;
      updateSummary();

      if (!checkIn || !checkOut) {
        if (roomSelect) roomSelect.innerHTML = '<option value="">-- Vui lòng chọn ngày nhận và trả phòng --</option>';
        return;
      }
      if (checkIn < todayIso()) {
        showAlert(alertEl, 'Ngày nhận phòng không được là ngày trong quá khứ.', 'danger');
        if (roomSelect) roomSelect.innerHTML = '<option value="">-- Ngày nhận phòng không hợp lệ --</option>';
        return;
      }
      if (checkOut <= checkIn) {
        showAlert(alertEl, 'Ngày trả phòng phải sau ngày nhận phòng.', 'danger');
        if (roomSelect) roomSelect.innerHTML = '<option value="">-- Ngày trả phòng phải sau ngày nhận --</option>';
        return;
      }

      try {
        const q = new URLSearchParams({ checkIn, checkOut, roomTypeId: rtId });
        const available = await apiFetch('/rooms/available?' + q.toString());
        if (!roomSelect) return;

        roomSelect.innerHTML = '';
        if (available.length === 0) {
          roomSelect.innerHTML = '<option value="">-- Hết phòng trống hạng này trong khoảng ngày đã chọn --</option>';
          showAlert(alertEl, 'Không còn phòng trống cho hạng phòng và khoảng ngày này. Vui lòng chọn ngày khác hoặc đổi hạng phòng.', 'warning');
          return;
        }

        available.forEach((r) => {
          const opt = document.createElement('option');
          opt.value = r.id;
          opt.textContent = `Phòng ${r.roomNumber} (${r.roomTypeName})`;
          if (String(r.id) === String(selectedRoomId)) opt.selected = true;
          roomSelect.appendChild(opt);
        });
        selectedRoomId = roomSelect.value;
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      }
    }

    typeSelect?.addEventListener('change', () => {
      selectedRoomTypeId = typeSelect.value;
      selectedRoomId = '';
      renderRoomTypeDetails(selectedRoomTypeId);
      loadAvailableRoomsForBooking();
    });

    checkInInput?.addEventListener('change', () => {
      if (checkInInput.value) {
        checkOutInput.min = addDaysIso(checkInInput.value, 1);
        if (checkOutInput.value <= checkInInput.value) {
          checkOutInput.value = addDaysIso(checkInInput.value, 1);
        }
      }
      loadAvailableRoomsForBooking();
    });

    checkOutInput?.addEventListener('change', loadAvailableRoomsForBooking);
    btnCheckAvail?.addEventListener('click', loadAvailableRoomsForBooking);

    btnSuggestDeposit?.addEventListener('click', () => {
      const total = updateSummary();
      if (depositInput && total > 0) {
        depositInput.value = Math.round(total * 0.3);
      }
    });

    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      hideAlert(alertEl);

      const roomId = roomSelect?.value;
      const checkIn = checkInInput?.value;
      const checkOut = checkOutInput?.value;
      const deposit = depositInput?.value ? Number(depositInput.value) : 0;

      if (!roomId) {
        showAlert(alertEl, 'Vui lòng chọn phòng còn trống để đặt phòng.', 'warning');
        return;
      }

      if (btnSubmit) {
        btnSubmit.disabled = true;
        btnSubmit.textContent = 'Đang xử lý...';
      }

      try {
        const created = await apiFetch('/bookings', {
          method: 'POST',
          body: JSON.stringify({
            roomId: Number(roomId),
            checkIn,
            checkOut,
            deposit
          })
        });

        // Cập nhật lại cache phòng sau khi đặt thành công
        cachedRooms = await apiFetch('/rooms');
        renderRoomTypeDetails(selectedRoomTypeId);
        await loadAvailableRoomsForBooking();

        alertEl.innerHTML = `
          <div><strong>Đặt phòng thành công!</strong> Mã đơn: <strong>#${created.id}</strong> (Phòng ${escapeHtml(created.roomNumber)}, ${formatDateVN(created.checkIn)} → ${formatDateVN(created.checkOut)}).</div>
          <div class="mt-2"><a href="bookings.html" class="btn btn-sm btn-success">Xem đơn đặt phòng của tôi</a></div>
        `;
        alertEl.className = 'alert alert-success';
        alertEl.classList.remove('d-none');
      } catch (err) {
        showAlert(alertEl, err.message, 'danger');
      } finally {
        if (btnSubmit) {
          btnSubmit.disabled = false;
          btnSubmit.textContent = 'Xác Nhận Đặt Phòng';
        }
      }
    });

    renderRoomTypeDetails(selectedRoomTypeId);
    await loadAvailableRoomsForBooking();
  }

  document.addEventListener('DOMContentLoaded', () => {
    initCustomerHome();
    initCustomerRoomsPage();
    initCustomerRoomSingle();
  });
})();

