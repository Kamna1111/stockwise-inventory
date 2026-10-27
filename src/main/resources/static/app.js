const state = { products: [], restock: [], activity: [], view: 'overview', lowOnly: false, search: '', category: 'all', toastTimer: null };
const $ = (selector) => document.querySelector(selector);
const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
const number = new Intl.NumberFormat('en-US');
const icons = { 'Electronics': ['🎧', 'blue'], 'Home & Kitchen': ['⌂', 'gold'], 'Apparel': ['✦', 'rose'], 'Grocery': ['◉', 'green'], 'Other': ['▦', 'purple'] };

function safe(value) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);
}

async function request(url, options = {}) {
  const response = await fetch(url, { ...options, headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(options.headers || {}) } });
  const text = await response.text();
  let body = null;
  try { body = text ? JSON.parse(text) : null; } catch { body = { message: text }; }
  if (!response.ok) throw new Error(body?.detail || body?.error || body?.message || `Request failed (${response.status})`);
  return body;
}

async function loadData() {
  $('#error-notice').hidden = true;
  try {
    const [products, restock, activity] = await Promise.all([
      request('/api/products'), request('/api/products/restock'), request('/api/products/activity')
    ]);
    state.products = products;
    state.restock = restock;
    state.activity = activity;
    updateMetrics();
    updateCategoryOptions();
    renderCurrentView();
  } catch (error) {
    $('#error-notice').textContent = `Could not load inventory. Make sure the Spring Boot app and database are running. ${error.message}`;
    $('#error-notice').hidden = false;
    $('#product-rows').innerHTML = '<tr><td class="empty-cell" colspan="8">Inventory could not be loaded.</td></tr>';
    $('#table-count').textContent = 'Connection error';
  }
}

function updateMetrics() {
  const units = state.products.reduce((sum, product) => sum + product.quantity, 0);
  const stockValue = state.products.reduce((sum, product) => sum + product.quantity * Number(product.price), 0);
  $('#metric-products').textContent = number.format(state.products.length);
  $('#metric-units').textContent = number.format(units);
  $('#metric-low').textContent = number.format(state.restock.length);
  $('#metric-value').textContent = usd.format(stockValue);
  $('#nav-count').textContent = state.products.length;
  $('#restock-dot').classList.toggle('visible', state.restock.length > 0);
  $('#metric-low-label').textContent = state.restock.length ? 'Action recommended' : 'All stocked up';
}

function updateCategoryOptions() {
  const select = $('#category-filter');
  const previous = state.category;
  const categories = [...new Set(state.products.map(p => p.category || 'General'))].sort();
  select.innerHTML = '<option value="all">All categories</option>' + categories.map(category => `<option value="${safe(category)}">${safe(category)}</option>`).join('');
  if (categories.includes(previous)) select.value = previous;
  else { state.category = 'all'; select.value = 'all'; }
}

function renderCurrentView() {
  const isActivity = state.view === 'activity';
  $('#inventory-panel').hidden = isActivity;
  $('#activity-panel').hidden = !isActivity;
  const labels = { overview: ['Overview', 'Inventory', 'Manage your products, quantities, and locations.'], inventory: ['Inventory', 'Inventory', 'Search and manage every product in your catalog.'], restock: ['Restock queue', 'Restock queue', 'These items are at or below their reorder point.'], activity: ['Activity', 'Recent activity', 'Stock movements recorded in your inventory.'] };
  const [page, heading, description] = labels[state.view];
  $('#crumb-current').textContent = page;
  $('#list-heading').textContent = heading;
  $('#list-description').textContent = description;
  $('#product-rows').innerHTML = '';
  if (isActivity) renderActivity();
  else renderProducts();
}

function visibleProducts() {
  const base = state.view === 'restock' || state.lowOnly ? state.restock : state.products;
  const query = state.search.trim().toLowerCase();
  return base.filter(product => {
    const matchesQuery = !query || [product.name, product.sku, product.category, product.location].some(value => String(value || '').toLowerCase().includes(query));
    const matchesCategory = state.category === 'all' || product.category === state.category;
    return matchesQuery && matchesCategory;
  });
}

function renderProducts() {
  const products = visibleProducts();
  $('#product-rows').innerHTML = products.length ? products.map(product => {
    const [icon, tone] = icons[product.category] || ['▦', 'purple'];
    const low = product.quantity <= product.reorderPoint;
    return `<tr>
      <td><div class="product-cell"><span class="product-thumb ${tone}">${icon}</span><span><span class="product-name">${safe(product.name)}</span><span class="product-sub">Reorder at ${number.format(product.reorderPoint)} units</span></span></div></td>
      <td><span class="sku-text">${safe(product.sku)}</span></td>
      <td><span class="category-pill">${safe(product.category)}</span></td>
      <td><span class="location-cell">⌖ &nbsp;${safe(product.location)}</span></td>
      <td><span class="price-cell">${usd.format(Number(product.price))}</span></td>
      <td><span class="quantity-cell">${number.format(product.quantity)}</span></td>
      <td><span class="status-pill ${low ? 'status-low' : 'status-healthy'}">${low ? 'Low stock' : 'In stock'}</span></td>
      <td><div class="action-group"><button class="row-action" data-action="stock" data-id="${product.id}">Adjust</button><button class="row-action" data-action="edit" data-id="${product.id}" aria-label="Edit ${safe(product.name)}">Edit</button><button class="row-action delete" data-action="delete" data-id="${product.id}" aria-label="Delete ${safe(product.name)}">×</button></div></td>
    </tr>`;
  }).join('') : `<tr><td class="empty-cell" colspan="8">${state.products.length ? 'No products match these filters.' : 'No products yet. Add your first product to get started.'}</td></tr>`;
  $('#table-count').textContent = `Showing ${products.length} of ${state.view === 'restock' || state.lowOnly ? state.restock.length : state.products.length} products`;
}

function renderActivity() {
  const container = $('#activity-list');
  if (!state.activity.length) {
    container.innerHTML = '<div class="empty-cell">No stock activity yet. Use “Adjust” on a product to record your first change.</div>';
    return;
  }
  container.innerHTML = state.activity.map(item => {
    const added = item.change > 0;
    const time = new Date(item.createdAt).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });
    return `<div class="activity-row"><span class="activity-icon">${added ? '↗' : '↙'}</span><div class="activity-copy"><strong>${safe(item.productName)} <span class="sku-text">· ${safe(item.sku)}</span></strong><p>${safe(item.reason)} · ${number.format(item.quantityAfter)} units now on hand</p></div><span class="trend ${added ? 'positive' : 'warning'}">${added ? '+' : ''}${number.format(item.change)}</span><time class="activity-time">${safe(time)}</time></div>`;
  }).join('');
}

function showToast(message) {
  const toast = $('#toast');
  toast.textContent = message;
  toast.classList.add('show');
  clearTimeout(state.toastTimer);
  state.toastTimer = setTimeout(() => toast.classList.remove('show'), 2600);
}

function openProduct(product = null) {
  $('#product-form').reset();
  $('#form-error').hidden = true;
  $('#product-id').value = product?.id || '';
  $('#product-modal-title').textContent = product ? 'Edit product' : 'Add a product';
  $('#save-product-button').textContent = product ? 'Save changes' : 'Save product';
  $('#field-name').value = product?.name || '';
  $('#field-sku').value = product?.sku || '';
  $('#field-category').value = product?.category || 'Electronics';
  $('#field-price').value = product?.price ?? '';
  $('#field-quantity').value = product?.quantity ?? '';
  $('#field-reorder').value = product?.reorderPoint ?? 10;
  $('#field-location').value = product?.location || '';
  $('#product-modal').showModal();
  $('#field-name').focus();
}

function showFormError(id, message) {
  const element = $(id);
  element.textContent = message;
  element.hidden = false;
}

async function submitProduct(event) {
  event.preventDefault();
  const id = $('#product-id').value;
  const payload = {
    name: $('#field-name').value.trim(), sku: $('#field-sku').value.trim(), category: $('#field-category').value,
    price: Number($('#field-price').value), quantity: Number($('#field-quantity').value),
    reorderPoint: Number($('#field-reorder').value), location: $('#field-location').value.trim() || 'Main warehouse'
  };
  try {
    await request(id ? `/api/products/${id}` : '/api/products', { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
    $('#product-modal').close();
    showToast(id ? 'Product updated' : 'Product added');
    await loadData();
  } catch (error) { showFormError('#form-error', error.message); }
}

function openStock(product) {
  $('#stock-form').reset();
  $('#stock-error').hidden = true;
  $('#stock-product-id').value = product.id;
  $('#stock-product-label').textContent = `${product.name} · ${product.quantity} units currently on hand`;
  $('#stock-modal').showModal();
  $('#field-stock-change').focus();
}

async function submitStock(event) {
  event.preventDefault();
  const id = $('#stock-product-id').value;
  const payload = { quantityChange: Number($('#field-stock-change').value), reason: $('#field-stock-reason').value.trim() || 'Manual adjustment' };
  try {
    await request(`/api/products/${id}/stock`, { method: 'POST', body: JSON.stringify(payload) });
    $('#stock-modal').close();
    showToast('Stock adjustment recorded');
    await loadData();
  } catch (error) { showFormError('#stock-error', error.message); }
}

async function handleRowAction(event) {
  const button = event.target.closest('[data-action]');
  if (!button) return;
  const product = state.products.find(item => String(item.id) === button.dataset.id);
  if (!product) return;
  if (button.dataset.action === 'edit') openProduct(product);
  if (button.dataset.action === 'stock') openStock(product);
  if (button.dataset.action === 'delete') {
    if (!window.confirm(`Delete ${product.name}? Products with stock history cannot be deleted.`)) return;
    try {
      await request(`/api/products/${product.id}`, { method: 'DELETE' });
      showToast('Product deleted');
      await loadData();
    } catch (error) { showToast(error.message); }
  }
}

function setView(view) {
  state.view = view;
  state.lowOnly = false;
  document.querySelectorAll('.nav-item').forEach(item => item.classList.toggle('is-active', item.dataset.view === view));
  $('#low-filter-button').classList.remove('is-active-filter');
  renderCurrentView();
}

function exportCsv() {
  const rows = visibleProducts();
  const columns = ['name', 'sku', 'category', 'location', 'price', 'quantity', 'reorderPoint'];
  const csv = [columns.join(','), ...rows.map(product => columns.map(key => `"${String(product[key] ?? '').replace(/"/g, '""')}"`).join(','))].join('\n');
  const link = document.createElement('a');
  link.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  link.download = 'stockwise-inventory.csv';
  link.click();
  URL.revokeObjectURL(link.href);
  showToast('Inventory CSV exported');
}

function addChatMessage(message, isUser = false) {
  const thread = $('#assistant-thread');
  const row = document.createElement('div');
  row.className = `chat-message ${isUser ? 'user-message' : 'assistant-message'}`;
  if (!isUser) {
    const icon = document.createElement('span'); icon.className = 'chat-icon'; icon.textContent = '✳'; row.append(icon);
  }
  const paragraph = document.createElement('p'); paragraph.textContent = message; row.append(paragraph); thread.append(row); thread.scrollTop = thread.scrollHeight;
  return row;
}

async function askAssistant(event) {
  event.preventDefault();
  const field = $('#assistant-question');
  const question = field.value.trim();
  if (!question) return;
  field.value = '';
  addChatMessage(question, true);
  const pending = addChatMessage('Checking your current inventory…');
  try {
    const answer = await request('/api/assistant/inventory', { method: 'POST', body: JSON.stringify({ question }) });
    pending.querySelector('p').textContent = answer.answer;
  } catch (error) {
    pending.querySelector('p').textContent = error.message.includes('503') || error.message.toLowerCase().includes('unavailable')
      ? 'The assistant is not configured yet. Add your LLM_API_KEY environment variable and restart the app.'
      : error.message;
  }
}

document.querySelectorAll('.nav-item').forEach(button => button.addEventListener('click', () => setView(button.dataset.view)));
$('#add-product-button').addEventListener('click', () => openProduct());
$('#product-form').addEventListener('submit', submitProduct);
$('#stock-form').addEventListener('submit', submitStock);
$('#product-rows').addEventListener('click', handleRowAction);
$('#search-input').addEventListener('input', event => { state.search = event.target.value; renderProducts(); });
$('#category-filter').addEventListener('change', event => { state.category = event.target.value; renderProducts(); });
$('#low-filter-button').addEventListener('click', event => {
  state.lowOnly = !state.lowOnly;
  event.currentTarget.classList.toggle('is-active-filter', state.lowOnly);
  renderProducts();
});
$('#export-button').addEventListener('click', exportCsv);
$('#refresh-button').addEventListener('click', loadData);
$('#open-assistant').addEventListener('click', () => $('#assistant-modal').showModal());
$('#assistant-form').addEventListener('submit', askAssistant);
document.querySelectorAll('.assistant-prompt').forEach(button => button.addEventListener('click', () => {
  $('#assistant-question').value = button.textContent.trim();
  $('#assistant-form').requestSubmit();
}));
document.querySelectorAll('.close-modal').forEach(button => button.addEventListener('click', () => button.closest('dialog').close()));
document.querySelectorAll('dialog.modal').forEach(dialog => dialog.addEventListener('click', event => { if (event.target === dialog) dialog.close(); }));
$('#assistant-modal').addEventListener('click', event => { if (event.target === $('#assistant-modal')) $('#assistant-modal').close(); });
loadData();
