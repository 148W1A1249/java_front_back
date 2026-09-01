const API = '/api/v1';

function getToken() {
  return localStorage.getItem('accessToken');
}

function getRefreshToken() {
  return localStorage.getItem('refreshToken');
}

function saveAuth(data) {
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  localStorage.setItem('username', data.username);
  localStorage.setItem('role', data.role);
}

function clearAuth() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('username');
  localStorage.removeItem('role');
}

function currentUser() {
  const username = localStorage.getItem('username');
  if (!username) return null;
  return {
    username,
    role: localStorage.getItem('role'),
    isAdmin: localStorage.getItem('role') === 'ROLE_ADMIN'
  };
}

async function api(path, options = {}) {
  const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  let res = await fetch(`${API}${path}`, { ...options, headers });

  if (res.status === 401 && getRefreshToken()) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      headers.Authorization = `Bearer ${getToken()}`;
      res = await fetch(`${API}${path}`, { ...options, headers });
    }
  }

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const message = data?.message || data?.error || 'Request failed';
    throw new Error(message);
  }
  return data;
}

async function tryRefresh() {
  try {
    const res = await fetch(`${API}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: getRefreshToken() })
    });
    if (!res.ok) {
      clearAuth();
      return false;
    }
    const data = await res.json();
    saveAuth(data);
    return true;
  } catch {
    clearAuth();
    return false;
  }
}

function formatMoney(n) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0
  }).format(Number(n || 0));
}

function showToast(message) {
  let el = document.getElementById('toast');
  if (!el) {
    el = document.createElement('div');
    el.id = 'toast';
    el.className = 'toast';
    document.body.appendChild(el);
  }
  el.textContent = message;
  el.classList.add('show');
  setTimeout(() => el.classList.remove('show'), 2200);
}

function renderNav(active) {
  const user = currentUser();
  const cartBadge = `<span class="badge" id="cart-badge" hidden>0</span>`;
  const authLinks = user
    ? `<a href="/orders.html">Orders</a>
       ${user.isAdmin ? '<a href="/admin.html">Admin</a>' : ''}
       <a href="#" id="logout-link">Hi ${user.username} · Logout</a>`
    : `<a href="/login.html">Login</a><a href="/register.html">Register</a>`;

  return `
    <header class="site-header">
      <nav class="nav">
        <a class="brand" href="/">Nova<span>Mart</span></a>
        <div class="nav-links">
          <a href="/" ${active === 'shop' ? 'style="color:var(--moss)"' : ''}>Shop</a>
          <a href="/cart.html" ${active === 'cart' ? 'style="color:var(--moss)"' : ''}>Cart${cartBadge}</a>
          ${authLinks}
        </div>
      </nav>
    </header>`;
}

async function refreshCartBadge() {
  const badge = document.getElementById('cart-badge');
  if (!badge || !currentUser()) return;
  try {
    const cart = await api('/cart');
    if (cart.totalItems > 0) {
      badge.hidden = false;
      badge.textContent = cart.totalItems;
    }
  } catch {
    /* ignore */
  }
}

function wireLogout() {
  const link = document.getElementById('logout-link');
  if (!link) return;
  link.addEventListener('click', (e) => {
    e.preventDefault();
    clearAuth();
    location.href = '/';
  });
}

document.addEventListener('DOMContentLoaded', () => {
  wireLogout();
  refreshCartBadge();
});
