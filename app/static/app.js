/* ═══════════════ Latchi AI · الواجهة ═══════════════ */
const $ = s => document.querySelector(s);
const el = (t, c, h) => { const e = document.createElement(t); if (c) e.className = c; if (h !== undefined) e.innerHTML = h; return e; };

let CHAT_ID = null;
let STREAMING = false;
let ATTACHED = [];
let ABORT = null;

/* ════════ Markdown مبسّط وآمن ════════ */
function esc(s) { return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }

function md(src) {
  if (!src) return '';
  const blocks = [];
  let s = src.replace(/```(\w*)\n?([\s\S]*?)```/g, (_, lang, code) => {
    blocks.push({ lang, code });
    return `\u0000B${blocks.length - 1}\u0000`;
  });

  s = esc(s);

  // عناوين
  s = s.replace(/^###### (.*)$/gm, '<h3>$1</h3>')
       .replace(/^##### (.*)$/gm, '<h3>$1</h3>')
       .replace(/^#### (.*)$/gm, '<h3>$1</h3>')
       .replace(/^### (.*)$/gm, '<h3>$1</h3>')
       .replace(/^## (.*)$/gm, '<h2>$1</h2>')
       .replace(/^# (.*)$/gm, '<h1>$1</h1>');

  // اقتباس
  s = s.replace(/^&gt; (.*)$/gm, '<blockquote>$1</blockquote>');

  // قوائم
  s = s.replace(/^\s*[-*+] (.*)$/gm, '<li>$1</li>');
  s = s.replace(/^\s*\d+\. (.*)$/gm, '<li>$1</li>');
  s = s.replace(/(<li>[\s\S]*?<\/li>)(?!\s*<li>)/g, m => `<ul>${m}</ul>`);
  s = s.replace(/<\/ul>\s*<ul>/g, '');

  // hr
  s = s.replace(/^---+$/gm, '<hr>');

  // bold / italic / inline code / link
  s = s.replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>');
  s = s.replace(/(^|[^*])\*([^*\n]+)\*(?!\*)/g, '$1<em>$2</em>');
  s = s.replace(/`([^`\n]+)`/g, '<code>$1</code>');
  s = s.replace(/\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');

  // فقرات
  s = s.split(/\n{2,}/).map(p => {
    const t = p.trim();
    if (!t) return '';
    if (/^<(h\d|ul|ol|blockquote|hr|pre|table)/.test(t)) return t;
    return `<p>${t.replace(/\n/g, '<br>')}</p>`;
  }).join('');

  // رجّع الكود
  s = s.replace(/\u0000B(\d+)\u0000/g, (_, i) => {
    const b = blocks[+i];
    return `<pre><code>${esc(b.code.replace(/\n$/, ''))}</code></pre>`;
  });
  return s;
}

/* ════════ أدوات ════════ */
const shortModel = m => (m || '').split('/').pop();

async function api(path, opts = {}) {
  const r = await fetch(path, { headers: { 'Content-Type': 'application/json' }, ...opts });
  if (!r.ok) throw new Error((await r.text()).slice(0, 200));
  return r.json();
}

/* ════════ الحالة ════════ */
async function loadStatus() {
  try {
    const s = await api('/api/status');
    $('#statTxt').textContent = `${s.models_alive}/${s.models_total} موديل · ${s.keys} مفاتيح`;
    $('#dot').style.background = s.models_alive > 0 ? 'var(--ok)' : 'var(--err)';
  } catch { $('#statTxt').textContent = 'المحرّك ما يستجابش'; $('#dot').style.background = 'var(--err)'; }
}

async function loadChats() {
  const list = await api('/api/chats');
  const box = $('#chats');
  box.innerHTML = '';
  list.forEach(c => {
    const it = el('div', 'chat-item' + (c.id === CHAT_ID ? ' on' : ''));
    it.innerHTML = `<span class="tt">${esc(c.title)}</span><span class="del">✕</span>`;
    it.onclick = e => {
      if (e.target.classList.contains('del')) { delChat(c.id); return; }
      openChat(c.id);
      if (innerWidth <= 820) $('#side').classList.add('hide');
    };
    box.appendChild(it);
  });
}

async function delChat(id) {
  await api('/api/chats/' + id, { method: 'DELETE' });
  if (id === CHAT_ID) { CHAT_ID = null; newChat(); }
  else loadChats();
}

async function newChat() {
  ATTACHED = []; renderAttached();
  const c = await api('/api/chats', { method: 'POST', body: JSON.stringify({}) });
  CHAT_ID = c.id;
  $('#chatTitle').textContent = 'محادثة جديدة';
  clearMsgs();
  showEmpty(true);
  await loadChats();
  $('#inp').focus();
}

function clearMsgs() {
  $('#msgs').querySelectorAll('.msg').forEach(n => n.remove());
}

function showEmpty(on) { $('#empty').style.display = on ? 'flex' : 'none'; }

async function openChat(id) {
  CHAT_ID = id;
  const c = await api('/api/chats/' + id);
  $('#chatTitle').textContent = c.title || 'محادثة';
  clearMsgs();
  showEmpty(!c.messages.length);
  c.messages.forEach(m => addMsg(m.role, m.content, { model: m.model, image: m.image, raw: true }));
  scrollBottom(true);
  loadChats();
}

/* ════════ عرض الرسائل ════════ */
function addMsg(role, text, opts = {}) {
  const m = el('div', 'msg ' + (role === 'user' ? 'u' : 'a'));
  const av = el('div', 'av', role === 'user' ? 'أنا' : 'L');
  const bub = el('div', 'bub');
  const body = el('div', 'md');
  body.innerHTML = md(text || '');
  bub.appendChild(body);

  if (opts.image) {
    const w = el('div', 'img-out');
    w.innerHTML = `<img src="data:image/jpeg;base64,${opts.image}" alt="صورة مولّدة">`;
    bub.appendChild(w);
  }
  if (opts.model) {
    const meta = el('div', 'meta');
    meta.innerHTML = `<span class="tag">${esc(shortModel(opts.model))}</span>`;
    bub.appendChild(meta);
  }
  m.appendChild(av); m.appendChild(bub);
  $('#msgs').querySelector('.wrap').appendChild(m);
  return { root: m, bub, body };
}

function scrollBottom(force) {
  const box = $('#msgs');
  const near = box.scrollHeight - box.scrollTop - box.clientHeight < 220;
  if (force || near) box.scrollTop = box.scrollHeight;
}

/* ════════ الإرسال ════════ */
async function send() {
  if (STREAMING) return;
  const inp = $('#inp');
  const text = inp.value.trim();
  if (!text && !ATTACHED.length) return;
  if (!CHAT_ID) await newChat();

  showEmpty(false);
  addMsg('user', text || '(صورة)');
  inp.value = ''; inp.style.height = 'auto'; syncSend();

  const images = ATTACHED.map(a => ({ b64: a.b64, mime: a.mime }));
  ATTACHED = []; renderAttached();

  // فقاعة المساعد المؤقتة
  const A = addMsg('assistant', '');
  const thinking = el('div', 'thinking',
    '<div class="dots"><i></i><i></i><i></i></div><span class="st">كنفكّر…</span>');
  A.bub.insertBefore(thinking, A.bub.firstChild);
  const statusLine = el('div', 'status-line', '');
  let switchNote = null;
  let acc = '', curModel = '';

  STREAMING = true; syncSend();
  scrollBottom(true);

  try {
    const res = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ chat_id: CHAT_ID, text, images })
    });
    if (!res.ok) throw new Error(await res.text());

    const reader = res.body.getReader();
    const dec = new TextDecoder();
    let buf = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buf += dec.decode(value, { stream: true });
      const parts = buf.split('\n\n');
      buf = parts.pop();

      for (const p of parts) {
        const line = p.trim();
        if (!line.startsWith('data:')) continue;
        let ev; try { ev = JSON.parse(line.slice(5).trim()); } catch { continue; }

        if (ev.type === 'model') {
          curModel = ev.model;
          thinking.querySelector('.st').textContent = 'كنكتب…';
          statusLine.textContent = '';
        } else if (ev.type === 'switch') {
          switchNote = el('div', 'switch-note',
            `⚡ بدّل تلقائياً ← ${esc(shortModel(ev.to))}`);
          A.bub.insertBefore(switchNote, statusLine);
          curModel = ev.to;
          acc = ''; A.body.innerHTML = '';
        } else if (ev.type === 'reset') {
          acc = ''; A.body.innerHTML = '';
        } else if (ev.type === 'delta') {
          if (thinking.parentNode) thinking.remove();
          acc += ev.text;
          A.body.innerHTML = md(acc) + '<span class="cursor"></span>';
          scrollBottom();
        } else if (ev.type === 'status') {
          statusLine.textContent = ev.text;
        } else if (ev.type === 'vision_used') {
          statusLine.textContent = 'شفت الصورة ✓';
        } else if (ev.type === 'image') {
          if (thinking.parentNode) thinking.remove();
          const w = el('div', 'img-out');
          w.innerHTML = `<img src="data:${ev.mime || 'image/jpeg'};base64,${ev.b64}" alt="صورة">`;
          A.bub.insertBefore(w, statusLine);
          scrollBottom();
        } else if (ev.type === 'error') {
          if (thinking.parentNode) thinking.remove();
          A.body.innerHTML = `<p style="color:var(--err)">⚠️ ${esc(ev.message)}</p>`;
        }
      }
    }
  } catch (e) {
    if (thinking.parentNode) thinking.remove();
    A.body.innerHTML = `<p style="color:var(--err)">⚠️ ${esc(e.message)}</p>`;
  } finally {
    STREAMING = false; syncSend();
    if (thinking.parentNode) thinking.remove();
    statusLine.remove();
    A.body.innerHTML = md(acc);
    if (curModel) {
      const meta = el('div', 'meta', `<span class="tag">${esc(shortModel(curModel))}</span>`);
      A.bub.appendChild(meta);
      $('#chip').style.display = 'flex';
      $('#chipModel').textContent = shortModel(curModel);
    }
    // أزرار
    const act = el('div', 'act');
    const cp = el('button', '', '📋 نسخ');
    cp.onclick = () => { navigator.clipboard.writeText(acc); cp.textContent = '✓ تنسخ'; setTimeout(() => cp.textContent = '📋 نسخ', 1500); };
    act.appendChild(cp);
    A.bub.appendChild(act);
    scrollBottom();
    loadChats(); loadStatus();
  }
}

/* ════════ المرفقات ════════ */
function renderAttached() {
  const box = $('#attached');
  box.innerHTML = '';
  ATTACHED.forEach((a, i) => {
    const w = el('div', 'att');
    w.innerHTML = `<img src="data:${a.mime};base64,${a.b64}"><button>✕</button>`;
    w.querySelector('button').onclick = () => { ATTACHED.splice(i, 1); renderAttached(); syncSend(); };
    box.appendChild(w);
  });
}

$('#file').addEventListener('change', async e => {
  for (const f of e.target.files) {
    if (!f.type.startsWith('image/')) continue;
    const fd = new FormData(); fd.append('file', f);
    try {
      const r = await fetch('/api/upload', { method: 'POST', body: fd });
      if (!r.ok) { alert('ما قدرتش نرفع الصورة'); continue; }
      const j = await r.json();
      ATTACHED.push(j);
    } catch { alert('خطأ فـالرفع'); }
  }
  e.target.value = '';
  renderAttached(); syncSend();
});

/* ════════ الإدخال ════════ */
const inp = $('#inp');
function syncSend() { $('#send').disabled = STREAMING || (!inp.value.trim() && !ATTACHED.length); }

inp.addEventListener('input', () => {
  inp.style.height = 'auto';
  inp.style.height = Math.min(inp.scrollHeight, 180) + 'px';
  syncSend();
});
inp.addEventListener('keydown', e => {
  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); }
});

$('#send').onclick = send;
$('#attachBtn').onclick = () => $('#file').click();
$('#newChat').onclick = () => { newChat(); if (innerWidth <= 820) $('#side').classList.add('hide'); };
$('#toggleSide').onclick = () => $('#side').classList.toggle('hide');

document.querySelectorAll('.sg').forEach(b => b.onclick = () => {
  inp.value = b.dataset.q; syncSend(); inp.focus(); send();
});

/* ════════ البداية ════════ */
(async function init() {
  await loadStatus();
  const list = await api('/api/chats');
  if (list.length) { await openChat(list[0].id); }
  else { await newChat(); }
  setInterval(loadStatus, 45000);
})();
