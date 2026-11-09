// Quản lý chức năng Thanh toán phòng (Room Payment) cho Khách hàng đã đăng nhập
(function () {
  const STORAGE_KEY = 'deluxe_customer_room_bookings';

  // Dữ liệu đặt phòng mẫu ban đầu (sẵn sàng kết nối API /api/bookings/my hoặc /api/invoices/my sau này)
  const DEFAULT_BOOKINGS = [
    {
      id: 'BK-202601',
      roomNumber: '204',
      roomType: 'Phòng Deluxe Hướng Biển',
      checkIn: '15/11/2026',
      checkOut: '18/11/2026',
      nights: 3,
      pricePerNight: 1500000,
      roomTotal: 4500000,
      foodTotal: 450000,
      depositPaid: 1000000,
      status: 'UNPAID', // UNPAID | DEPOSIT_PAID | PAID
      paymentMethod: '',
      paidAt: ''
    },
    {
      id: 'BK-202602',
      roomNumber: '301',
      roomType: 'Phòng Suite Cao Cấp',
      checkIn: '20/11/2026',
      checkOut: '22/11/2026',
      nights: 2,
      pricePerNight: 2800000,
      roomTotal: 5600000,
      foodTotal: 0,
      depositPaid: 0,
      status: 'UNPAID',
      paymentMethod: '',
      paidAt: ''
    },
    {
      id: 'BK-202598',
      roomNumber: '105',
      roomType: 'Phòng Family Gia Đình',
      checkIn: '01/11/2026',
      checkOut: '03/11/2026',
      nights: 2,
      pricePerNight: 1200000,
      roomTotal: 2400000,
      foodTotal: 320000,
      depositPaid: 2720000,
      status: 'PAID',
      paymentMethod: 'Chuyển khoản VietQR',
      paidAt: '03/11/2026 10:30'
    }
  ];

  function formatVND(amount) {
    return Number(amount || 0).toLocaleString('vi-VN') + ' VNĐ';
  }

  function getBookings() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(DEFAULT_BOOKINGS));
        return JSON.parse(JSON.stringify(DEFAULT_BOOKINGS));
      }
      return JSON.parse(raw);
    } catch (e) {
      return JSON.parse(JSON.stringify(DEFAULT_BOOKINGS));
    }
  }

  function saveBookings(list) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
  }

  function getRemainingAmount(b) {
    if (b.status === 'PAID') return 0;
    const total = (b.roomTotal || 0) + (b.foodTotal || 0);
    return Math.max(0, total - (b.depositPaid || 0));
  }

  function getStatusBadge(status) {
    if (status === 'PAID') {
      return '<span class="badge badge-success px-3 py-2" style="border-radius:20px;font-weight:500;">Đã thanh toán</span>';
    }
    if (status === 'DEPOSIT_PAID') {
      return '<span class="badge badge-info px-3 py-2" style="border-radius:20px;font-weight:500;">Đã đặt cọc</span>';
    }
    return '<span class="badge badge-warning px-3 py-2" style="border-radius:20px;font-weight:500;color:#fff;background:#d97706;">Chờ thanh toán</span>';
  }

  // Hiển thị khối Thanh toán phòng ở màn Home nếu user đã đăng nhập
  function initHomePaymentSection() {
    const section = document.getElementById('home-room-payment-section');
    if (!section) return;

    const user = typeof getUser === 'function' ? getUser() : null;
    const isCustomerPortal = window.location.pathname.includes('/portal/customer/');

    if (!user && !isCustomerPortal) {
      section.classList.add('d-none');
      return;
    }

    section.classList.remove('d-none');
    renderHomePaymentTable();
  }

  function renderHomePaymentTable() {
    const tbody = document.getElementById('home-payment-tbody');
    if (!tbody) return;

    const bookings = getBookings();
    const unpaidCountEl = document.getElementById('home-unpaid-count');
    const unpaidTotalEl = document.getElementById('home-unpaid-total');

    let unpaidCount = 0;
    let unpaidTotal = 0;

    tbody.innerHTML = '';

    bookings.forEach((b) => {
      const total = (b.roomTotal || 0) + (b.foodTotal || 0);
      const remaining = getRemainingAmount(b);
      if (b.status !== 'PAID') {
        unpaidCount++;
        unpaidTotal += remaining;
      }

      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td class="align-middle font-weight-bold" style="color:#8d703b;">#${b.id}</td>
        <td class="align-middle">
          <div class="font-weight-bold text-dark">${b.roomType}</div>
          <small class="text-muted">Phòng ${b.roomNumber} &bull; ${b.nights} đêm</small>
        </td>
        <td class="align-middle small">
          <div>Nhận: <strong>${b.checkIn}</strong></div>
          <div>Trả: <strong>${b.checkOut}</strong></div>
        </td>
        <td class="align-middle">${formatVND(total)}</td>
        <td class="align-middle text-success">${formatVND(b.depositPaid)}</td>
        <td class="align-middle font-weight-bold text-danger">${formatVND(remaining)}</td>
        <td class="align-middle">${getStatusBadge(b.status)}</td>
        <td class="align-middle text-right">
          ${
            b.status !== 'PAID'
              ? `<button type="button" class="btn btn-sm px-3 py-2 btn-open-pay-modal" data-id="${b.id}" style="background:#8d703b;color:#fff;border-radius:6px;font-weight:500;">
                   Thanh toán ngay
                 </button>`
              : `<span class="text-muted small"><span class="icon-check text-success mr-1"></span>Hoàn tất</span>`
          }
        </td>
      `;
      tbody.appendChild(tr);
    });

    if (unpaidCountEl) unpaidCountEl.textContent = unpaidCount;
    if (unpaidTotalEl) unpaidTotalEl.textContent = formatVND(unpaidTotal);

    document.querySelectorAll('.btn-open-pay-modal').forEach((btn) => {
      btn.addEventListener('click', () => {
        openPaymentModal(btn.getAttribute('data-id'));
      });
    });
  }

  let selectedBookingId = null;

  function openPaymentModal(bookingId) {
    const bookings = getBookings();
    const booking = bookings.find((x) => x.id === bookingId);
    if (!booking) return;

    selectedBookingId = booking.id;
    const total = (booking.roomTotal || 0) + (booking.foodTotal || 0);
    const remaining = getRemainingAmount(booking);

    const setElText = (id, val) => {
      const el = document.getElementById(id);
      if (el) el.textContent = val;
    };

    setElText('modal-pay-booking-id', '#' + booking.id);
    setElText('modal-pay-room-name', booking.roomType + ' (Phòng ' + booking.roomNumber + ')');
    setElText('modal-pay-dates', booking.checkIn + ' → ' + booking.checkOut + ' (' + booking.nights + ' đêm)');
    setElText('modal-pay-room-total', formatVND(booking.roomTotal));
    setElText('modal-pay-food-total', formatVND(booking.foodTotal));
    setElText('modal-pay-deposit', formatVND(booking.depositPaid));
    setElText('modal-pay-remaining', formatVND(remaining));
    setElText('modal-qr-Memo', 'DELUXE ' + booking.id);

    const alertEl = document.getElementById('modal-pay-alert');
    if (alertEl) alertEl.className = 'alert d-none';

    if (window.$ && $('#roomPaymentModal').length) {
      $('#roomPaymentModal').modal('show');
    }
  }

  function setupModalEvents() {
    // Chuyển đổi phương thức thanh toán trong Modal hoặc trang Payment
    document.querySelectorAll('input[name="payMethod"]').forEach((radio) => {
      radio.addEventListener('change', (e) => {
        const val = e.target.value;
        const qrBox = document.getElementById('pay-method-qr-box');
        const cardBox = document.getElementById('pay-method-card-box');
        const walletBox = document.getElementById('pay-method-wallet-box');
        if (qrBox) qrBox.classList.toggle('d-none', val !== 'VIETQR');
        if (cardBox) cardBox.classList.toggle('d-none', val !== 'CARD');
        if (walletBox) walletBox.classList.toggle('d-none', val !== 'WALLET');
      });
    });

    const btnConfirm = document.getElementById('btn-confirm-room-payment');
    if (btnConfirm) {
      btnConfirm.addEventListener('click', async () => {
        if (!selectedBookingId) return;
        const bookings = getBookings();
        const idx = bookings.findIndex((x) => x.id === selectedBookingId);
        if (idx === -1) return;

        const methodRadio = document.querySelector('input[name="payMethod"]:checked');
        const methodVal = methodRadio ? methodRadio.value : 'VIETQR';
        const methodLabel =
          methodVal === 'CARD'
            ? 'Thẻ tín dụng / Ghi nợ'
            : methodVal === 'WALLET'
            ? 'Ví điện tử VNPay / MoMo'
            : 'Chuyển khoản VietQR';

        const b = bookings[idx];
        const total = (b.roomTotal || 0) + (b.foodTotal || 0);

        // Thử gọi API backend nếu sau này đã có endpoint
        try {
          await apiFetch('/invoices/pay-booking', {
            method: 'POST',
            body: JSON.stringify({
              bookingCode: b.id,
              amount: getRemainingAmount(b),
              method: methodVal
            })
          });
        } catch (err) {
          // Chưa có API backend thì lưu local để hoạt động mượt mà
        }

        b.depositPaid = total;
        b.status = 'PAID';
        b.paymentMethod = methodLabel;
        const now = new Date();
        b.paidAt =
          now.toLocaleDateString('vi-VN') +
          ' ' +
          now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

        saveBookings(bookings);
        renderHomePaymentTable();
        renderDedicatedPaymentPage();

        const alertEl = document.getElementById('modal-pay-alert');
        if (alertEl) {
          alertEl.textContent =
            'Thanh toán thành công đơn phòng #' + b.id + ' qua ' + methodLabel + '!';
          alertEl.className = 'alert alert-success';
        }

        setTimeout(() => {
          if (window.$ && $('#roomPaymentModal').length) {
            $('#roomPaymentModal').modal('hide');
          }
        }, 1200);
      });
    }
  }

  // Logic cho trang chuyên biệt portal/customer/payment.html
  function renderDedicatedPaymentPage() {
    const pageContainer = document.getElementById('dedicated-payment-page');
    if (!pageContainer) return;

    const bookings = getBookings();
    const listContainer = document.getElementById('payment-booking-list');
    if (!listContainer) return;

    listContainer.innerHTML = '';

    // Nếu chưa chọn đơn nào thì mặc định chọn đơn UNPAID đầu tiên
    if (!selectedBookingId) {
      const firstUnpaid = bookings.find((x) => x.status !== 'PAID') || bookings[0];
      if (firstUnpaid) selectedBookingId = firstUnpaid.id;
    }

    bookings.forEach((b) => {
      const total = (b.roomTotal || 0) + (b.foodTotal || 0);
      const remaining = getRemainingAmount(b);
      const isSelected = b.id === selectedBookingId;

      const card = document.createElement('div');
      card.className = 'p-3 mb-3 rounded bg-white shadow-sm';
      card.style.border = isSelected ? '2px solid #8d703b' : '1px solid #eee';
      card.style.cursor = 'pointer';
      card.innerHTML = `
        <div class="d-flex justify-content-between align-items-center mb-2">
          <div>
            <span class="font-weight-bold" style="color:#8d703b;font-size:16px;">#${b.id}</span>
            <span class="ml-2 font-weight-bold text-dark">${b.roomType} (P.${b.roomNumber})</span>
          </div>
          <div>${getStatusBadge(b.status)}</div>
        </div>
        <div class="row small text-muted mb-2">
          <div class="col-sm-6">Nhận phòng: <strong>${b.checkIn}</strong> &bull; Trả phòng: <strong>${b.checkOut}</strong> (${b.nights} đêm)</div>
          <div class="col-sm-6 text-sm-right">Tổng cộng: <strong>${formatVND(total)}</strong> | Đã trả: <strong class="text-success">${formatVND(b.depositPaid)}</strong></div>
        </div>
        <div class="d-flex justify-content-between align-items-center pt-2 border-top">
          <div>
            <span class="small text-muted">Cần thanh toán: </span>
            <span class="font-weight-bold text-danger" style="font-size:16px;">${formatVND(remaining)}</span>
          </div>
          <div>
            ${
              b.status !== 'PAID'
                ? `<button type="button" class="btn btn-sm px-3 py-1" style="background:#8d703b;color:#fff;border-radius:6px;">Chọn thanh toán</button>`
                : `<span class="small text-success"><span class="icon-check mr-1"></span>Đã thanh toán (${b.paymentMethod || 'Trực tuyến'})</span>`
            }
          </div>
        </div>
      `;
      card.addEventListener('click', () => {
        selectedBookingId = b.id;
        renderDedicatedPaymentPage();
      });
      listContainer.appendChild(card);
    });

    // Cập nhật cột bên phải
    const current = bookings.find((x) => x.id === selectedBookingId);
    if (current) {
      const total = (current.roomTotal || 0) + (current.foodTotal || 0);
      const remaining = getRemainingAmount(current);

      const setElText = (id, val) => {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
      };

      setElText('modal-pay-booking-id', '#' + current.id);
      setElText('modal-pay-room-name', current.roomType + ' (Phòng ' + current.roomNumber + ')');
      setElText('modal-pay-dates', current.checkIn + ' → ' + current.checkOut + ' (' + current.nights + ' đêm)');
      setElText('modal-pay-room-total', formatVND(current.roomTotal));
      setElText('modal-pay-food-total', formatVND(current.foodTotal));
      setElText('modal-pay-deposit', formatVND(current.depositPaid));
      setElText('modal-pay-remaining', formatVND(remaining));
      setElText('modal-qr-Memo', 'DELUXE ' + current.id);

      const btnConfirm = document.getElementById('btn-confirm-room-payment');
      if (btnConfirm) {
        if (current.status === 'PAID') {
          btnConfirm.disabled = true;
          btnConfirm.textContent = 'Đơn phòng này đã được thanh toán';
        } else {
          btnConfirm.disabled = false;
          btnConfirm.textContent = 'Xác Nhận Thanh Toán (' + formatVND(remaining) + ')';
        }
      }
    }

    // Nút khôi phục dữ liệu mẫu để test lại
    const btnResetDemo = document.getElementById('btn-reset-demo-bookings');
    if (btnResetDemo && !btnResetDemo.dataset.bound) {
      btnResetDemo.dataset.bound = '1';
      btnResetDemo.addEventListener('click', () => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(DEFAULT_BOOKINGS));
        selectedBookingId = DEFAULT_BOOKINGS[0].id;
        renderDedicatedPaymentPage();
        renderHomePaymentTable();
      });
    }
  }

  document.addEventListener('DOMContentLoaded', () => {
    initHomePaymentSection();
    renderDedicatedPaymentPage();
    setupModalEvents();
  });
})();
