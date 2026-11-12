// Dung chung cho cac trang nhan vien (admin/, reception/, housekeeping/, accountant/, waiter/)
(async function () {
  const roles = (document.body.dataset.roles || '').split(',');
  // ROLE_ADMIN duoc vao moi khu vuc
  const user = await requireRole(roles.concat('ROLE_ADMIN'));
  if (!user) return;
  const name = document.getElementById('user-name');
  if (name) name.textContent = user.fullName + ' (' + ROLE_LABEL[user.role] + ')';
  const out = document.getElementById('btn-logout');
  if (out) out.addEventListener('click', (e) => { e.preventDefault(); logout(); });
})();

function ensureDeluxeDialogModal() {
  let modalEl = document.getElementById('deluxeDialogModal');
  if (modalEl) return modalEl;

  const wrapper = document.createElement('div');
  wrapper.innerHTML = `
    <div class="modal fade" id="deluxeDialogModal" tabindex="-1" role="dialog" aria-hidden="true" style="z-index: 1060;">
      <div class="modal-dialog modal-dialog-centered" role="document">
        <div class="modal-content shadow border-0">
          <div class="modal-header bg-dark text-white py-3">
            <h5 class="modal-title font-weight-bold text-white mb-0" id="deluxeDialogTitle">Deluxe</h5>
            <button type="button" class="close text-white" data-dismiss="modal" aria-label="Đóng">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
          <div class="modal-body p-4 text-dark" id="deluxeDialogMessage" style="font-size: 15px;"></div>
          <div class="modal-footer bg-light py-2">
            <button type="button" class="btn btn-secondary btn-sm px-3" id="deluxeDialogCancel" data-dismiss="modal">Hủy</button>
            <button type="button" class="btn btn-primary btn-sm font-weight-bold px-3" id="deluxeDialogOk">OK</button>
          </div>
        </div>
      </div>
    </div>
  `;
  document.body.appendChild(wrapper.firstElementChild);
  return document.getElementById('deluxeDialogModal');
}

function showDeluxeDialog(message, title, isConfirm) {
  if (!window.$ || !$.fn || !$.fn.modal) {
    return Promise.resolve(isConfirm ? window.confirm(message) : (window.alert(message), true));
  }

  const modalEl = ensureDeluxeDialogModal();
  const titleEl = document.getElementById('deluxeDialogTitle');
  const msgEl = document.getElementById('deluxeDialogMessage');
  const okBtn = document.getElementById('deluxeDialogOk');
  const cancelBtn = document.getElementById('deluxeDialogCancel');

  if (titleEl) titleEl.textContent = title || 'Deluxe';
  if (msgEl) msgEl.textContent = message || '';
  if (cancelBtn) cancelBtn.style.display = isConfirm ? 'inline-block' : 'none';

  return new Promise(resolve => {
    let settled = false;
    const $modal = $(modalEl);

    const cleanup = () => {
      okBtn.removeEventListener('click', onOk);
      $modal.off('hidden.bs.modal', onHidden);
    };

    const onOk = () => {
      settled = true;
      cleanup();
      $modal.modal('hide');
      resolve(true);
    };

    const onHidden = () => {
      if (!settled) {
        settled = true;
        cleanup();
        resolve(isConfirm ? false : true);
      }
    };

    okBtn.addEventListener('click', onOk);
    $modal.on('hidden.bs.modal', onHidden);
    $modal.modal('show');
  });
}

function deluxeConfirm(message, title = 'Deluxe') {
  return showDeluxeDialog(message, title, true);
}

function deluxeAlert(message, title = 'Deluxe') {
  return showDeluxeDialog(message, title, false).then(() => {});
}
