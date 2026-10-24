(function () {
  const box = document.getElementById('alert-box');

  function show(msg, type) {
    box.textContent = msg;
    box.className = 'alert alert-' + type;
  }

  document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      const user = await apiFetch('/auth/login', {
        method: 'POST',
        body: JSON.stringify({
          username: document.getElementById('login-username').value.trim(),
          password: document.getElementById('login-password').value
        })
      });
      localStorage.setItem('user', JSON.stringify(user));
      show('Dang nhap thanh cong, xin chao ' + user.username + '!', 'success');
      setTimeout(() => { window.location.href = homeFor(user.role); }, 800);
    } catch (err) {
      show(err.message, 'danger');
    }
  });

  document.getElementById('register-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await apiFetch('/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          username: document.getElementById('reg-username').value.trim(),
          fullName: document.getElementById('reg-fullname').value.trim(),
          email: document.getElementById('reg-email').value.trim(),
          password: document.getElementById('reg-password').value
        })
      });
      show('Dang ky thanh cong! Hay dang nhap.', 'success');
      $('a[href="#tab-login"]').tab('show');
    } catch (err) {
      show(err.message, 'danger');
    }
  });
})();

