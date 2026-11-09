(function () {
  const box = document.getElementById('alert-box');

  function show(msg, type) {
    if (!box) return;
    box.textContent = msg;
    box.className = 'alert alert-' + type;
    box.classList.remove('d-none');
  }

  // Xử lý Form Đăng Nhập
  const loginForm = document.getElementById('login-form');
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      try {
        const usernameInput = document.getElementById('login-username');
        const passwordInput = document.getElementById('login-password');
        
        const user = await apiFetch('/auth/login', {
          method: 'POST',
          body: JSON.stringify({
            username: usernameInput.value.trim(),
            password: passwordInput.value
          })
        });
        localStorage.setItem('user', JSON.stringify(user));
        show('Đăng nhập thành công! Xin chào ' + (user.fullName || user.username) + '.', 'success');
        setTimeout(() => { 
          window.location.href = homeFor(user.role); 
        }, 600);
      } catch (err) {
        show(err.message || 'Tên đăng nhập hoặc mật khẩu không chính xác', 'danger');
      }
    });
  }

  // Xử lý Form Đăng Ký
  const registerForm = document.getElementById('register-form');
  if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
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
        show('Đăng ký tài khoản thành công! Đang chuyển hướng sang Đăng nhập...', 'success');
        setTimeout(() => {
          window.location.href = 'login.html';
        }, 1200);
      } catch (err) {
        show(err.message || 'Đăng ký không thành công. Vui lòng thử lại.', 'danger');
      }
    });
  }

  // Xử lý Quên Mật Khẩu
  const btnForgot = document.getElementById('btn-forgot-password');
  if (btnForgot) {
    btnForgot.addEventListener('click', (e) => {
      e.preventDefault();
      if (window.$ && $('#forgotModal').length) {
        $('#forgotModal').modal('show');
      } else {
        alert('Vui lòng liên hệ Lễ tân Resort qua Hotline: +84 987 654 321 để được cấp lại mật khẩu.');
      }
    });
  }
})();
