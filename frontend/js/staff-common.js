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
