// Tiện ích dùng chung cho Feature 2: Room & Booking Management
window.F2 = (function () {
  const ROOM_STATUS_META = {
    AVAILABLE: { label: 'Phòng trống', badge: 'badge-success', color: '#16a34a', bg: '#dcfce7' },
    BOOKED: { label: 'Đã đặt', badge: 'badge-info', color: '#0284c7', bg: '#e0f2fe' },
    OCCUPIED: { label: 'Đang có khách', badge: 'badge-primary', color: '#8d703b', bg: '#f3efe6' },
    CLEANING: { label: 'Đang dọn dẹp', badge: 'badge-warning', color: '#b45309', bg: '#fef3c7' },
    MAINTENANCE: { label: 'Đang bảo trì', badge: 'badge-danger', color: '#dc2626', bg: '#fee2e2' }
  };

  const BOOKING_STATUS_META = {
    PENDING: { label: 'Chờ xác nhận', badge: 'badge-warning', color: '#b45309', bg: '#fef3c7' },
    CONFIRMED: { label: 'Đã xác nhận', badge: 'badge-info', color: '#0284c7', bg: '#e0f2fe' },
    CHECKED_IN: { label: 'Đã nhận phòng', badge: 'badge-primary', color: '#8d703b', bg: '#f3efe6' },
    CHECKED_OUT: { label: 'Đã trả phòng', badge: 'badge-secondary', color: '#475569', bg: '#e2e8f0' },
    CANCELLED: { label: 'Đã hủy', badge: 'badge-danger', color: '#dc2626', bg: '#fee2e2' }
  };

  function escapeHtml(val) {
    if (val === null || val === undefined) return '';
    return String(val)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function formatVND(amount) {
    return Number(amount || 0).toLocaleString('vi-VN') + ' VNĐ';
  }

  function formatDateVN(isoDate) {
    if (!isoDate) return '-';
    const parts = String(isoDate).split('T')[0].split('-');
    if (parts.length !== 3) return isoDate;
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }

  function formatDateTimeVN(isoDateTime) {
    if (!isoDateTime) return '-';
    const d = new Date(isoDateTime);
    if (isNaN(d.getTime())) return isoDateTime;
    return d.toLocaleDateString('vi-VN') + ' ' + d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  }

  function todayIso() {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  function addDaysIso(isoDate, days) {
    const parts = String(isoDate || todayIso()).split('-').map(Number);
    const d = new Date(parts[0], parts[1] - 1, parts[2]);
    d.setDate(d.getDate() + days);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  function parseDateToIso(val) {
    if (!val) return '';
    const trimmed = String(val).trim();
    if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return trimmed;
    const slash = trimmed.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
    if (slash) {
      // bootstrap-datepicker dùng định dạng m/d/yyyy
      const m = slash[1].padStart(2, '0');
      const d = slash[2].padStart(2, '0');
      return `${slash[3]}-${m}-${d}`;
    }
    return '';
  }

  function nightsBetween(checkInIso, checkOutIso) {
    if (!checkInIso || !checkOutIso) return 0;
    const a = new Date(checkInIso + 'T00:00:00');
    const b = new Date(checkOutIso + 'T00:00:00');
    const diff = Math.round((b - a) / 86400000);
    return diff > 0 ? diff : 0;
  }

  function roomImageForType(roomTypeId, prefix = '../../') {
    const idx = ((Number(roomTypeId || 1) - 1) % 6) + 1;
    return `${prefix}images/room-${idx}.jpg`;
  }

  function renderRoomStatusBadge(status) {
    const m = ROOM_STATUS_META[status] || { label: status || '-', color: '#475569', bg: '#e2e8f0' };
    return `<span class="badge px-2 py-1" style="background:${m.bg};color:${m.color};font-weight:600;border-radius:14px;font-size:12px;">${escapeHtml(m.label)}</span>`;
  }

  function renderBookingStatusBadge(status) {
    const m = BOOKING_STATUS_META[status] || { label: status || '-', color: '#475569', bg: '#e2e8f0' };
    return `<span class="badge px-2 py-1" style="background:${m.bg};color:${m.color};font-weight:600;border-radius:14px;font-size:12px;">${escapeHtml(m.label)}</span>`;
  }

  function showAlert(el, msg, type = 'danger') {
    if (!el) return;
    el.textContent = msg;
    el.className = `alert alert-${type}`;
    el.classList.remove('d-none');
  }

  function hideAlert(el) {
    if (!el) return;
    el.className = 'alert d-none';
    el.textContent = '';
  }

  return {
    ROOM_STATUS_META,
    BOOKING_STATUS_META,
    escapeHtml,
    formatVND,
    formatDateVN,
    formatDateTimeVN,
    todayIso,
    addDaysIso,
    parseDateToIso,
    nightsBetween,
    roomImageForType,
    renderRoomStatusBadge,
    renderBookingStatusBadge,
    showAlert,
    hideAlert
  };
})();

