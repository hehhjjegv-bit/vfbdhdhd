(() => {
  const editorState = {
    tabs: [],
    active: -1,
    history: [],
    future: [],
    readOnly: false,
    wordWrap: true,
    miniToolbar: true,
    fontSize: 15
  };

  const $ = s => document.querySelector(s);

  function editor() {
    return $('#editor');
  }

  function activeTab() {
    return editorState.tabs[editorState.active] || null;
  }

  function language(name = '') {
    const e = (name.split('.').pop() || '').toLowerCase();

    const map = {
      js:'JavaScript', mjs:'JavaScript', cjs:'JavaScript',
      ts:'TypeScript', html:'HTML', htm:'HTML',
      css:'CSS', scss:'SCSS', less:'LESS',
      json:'JSON', xml:'XML',
      py:'Python', java:'Java', kt:'Kotlin',
      c:'C', h:'C/C++', cpp:'C++', hpp:'C++',
      cs:'C#', go:'Go', rs:'Rust',
      php:'PHP', rb:'Ruby', swift:'Swift',
      sql:'SQL', sh:'Shell', bash:'Shell',
      md:'Markdown', yml:'YAML', yaml:'YAML',
      toml:'TOML', txt:'Text'
    };

    return map[e] || 'Text';
  }

  function updateStatus() {
    const tab = activeTab();
    const status = $('#editorMode');

    if (status) {
      status.textContent = tab
        ? `${language(tab.name)} • ${tab.dirty ? 'غير محفوظ' : 'محفوظ'}`
        : 'لا يوجد ملف';
    }

    if (editor()) {
      editor().readOnly = editorState.readOnly;
      editor().style.whiteSpace = editorState.wordWrap ? 'pre-wrap' : 'pre';
      editor().style.fontSize = editorState.fontSize + 'px';
    }
  }

  function pushHistory() {
    const e = editor();
    if (!e) return;

    editorState.history.push(e.value);

    if (editorState.history.length > 100) {
      editorState.history.shift();
    }

    editorState.future.length = 0;
  }

  function markDirty() {
    const tab = activeTab();
    if (!tab) return;

    tab.content = editor().value;
    tab.dirty = true;
    updateStatus();
  }

  function newTab(name = 'untitled.txt', content = '') {
    editorState.tabs.push({
      name,
      uri: null,
      content,
      dirty: false
    });

    editorState.active = editorState.tabs.length - 1;
    editor().value = content;

    editorState.history = [content];
    editorState.future = [];

    updateStatus();
    renderTabs();
  }

  function closeTab(index = editorState.active) {
    const tab = editorState.tabs[index];
    if (!tab) return;

    if (tab.dirty) {
      const save = confirm(`الملف "${tab.name}" يحتوي على تعديلات غير محفوظة.\nهل تريد حفظه؟`);

      if (save && window.saveEditor) {
        window.saveEditor().then(() => removeTab(index));
        return;
      }

      if (save) return;
    }

    removeTab(index);
  }

  function removeTab(index) {
    editorState.tabs.splice(index, 1);

    if (!editorState.tabs.length) {
      editorState.active = -1;
      editor().value = '';
    } else {
      editorState.active = Math.min(index, editorState.tabs.length - 1);
      const tab = activeTab();
      editor().value = tab.content;
    }

    renderTabs();
    updateStatus();
  }

  function activate(index) {
    const old = activeTab();

    if (old) {
      old.content = editor().value;
    }

    editorState.active = index;

    const tab = activeTab();
    if (!tab) return;

    editor().value = tab.content;
    editorState.history = [tab.content];
    editorState.future = [];

    $('#editorPath').textContent =
      tab.uri || tab.name || 'untitled.txt';

    updateStatus();
    renderTabs();
  }

  function renderTabs() {
    let bar = $('#editorTabs');

    if (!bar) {
      bar = document.createElement('div');
      bar.id = 'editorTabs';
      bar.className = 'editor-tabs';

      const ed = $('#editor');
      ed.parentNode.insertBefore(bar, ed);
    }

    bar.innerHTML = editorState.tabs.map((tab, i) => `
      <button class="editor-tab ${i === editorState.active ? 'active' : ''}"
              data-index="${i}">
        <span>${escapeHtml(tab.name)}</span>
        ${tab.dirty ? '<b>●</b>' : ''}
        <span class="tab-close" data-close="${i}">×</span>
      </button>
    `).join('') + `
      <button class="editor-tab-add" id="editorNewTab">＋</button>
    `;

    bar.querySelectorAll('.editor-tab').forEach(btn => {
      btn.onclick = e => {
        const close = e.target.closest('[data-close]');
        if (close) {
          e.stopPropagation();
          closeTab(Number(close.dataset.close));
          return;
        }

        activate(Number(btn.dataset.index));
      };
    });

    $('#editorNewTab').onclick = () => newTab();
  }

  function undo() {
    if (editorState.history.length <= 1) return;

    const current = editor().value;
    editorState.future.push(current);

    editorState.history.pop();

    editor().value =
      editorState.history[editorState.history.length - 1];

    markDirty();
  }

  function redo() {
    if (!editorState.future.length) return;

    const value = editorState.future.pop();

    editorState.history.push(value);
    editor().value = value;

    markDirty();
  }

  function selectAll() {
    editor().focus();
    editor().select();
  }

  function copy() {
    const text = editor().value.substring(
      editor().selectionStart,
      editor().selectionEnd
    );

    if (text) navigator.clipboard?.writeText(text);
  }

  function cut() {
    if (editorState.readOnly) return;

    copy();
    document.execCommand('delete');
    markDirty();
  }

  async function paste() {
    if (editorState.readOnly) return;

    try {
      const text = await navigator.clipboard.readText();
      const e = editor();

      e.setRangeText(
        text,
        e.selectionStart,
        e.selectionEnd,
        'end'
      );

      markDirty();
    } catch {
      document.execCommand('paste');
    }
  }

  function gotoLine() {
    const value = prompt('رقم السطر');

    if (!value) return;

    const line = Math.max(1, Number(value));
    const text = editor().value;

    const lines = text.split('\n');

    let pos = 0;

    for (let i = 0; i < line - 1 && i < lines.length; i++) {
      pos += lines[i].length + 1;
    }

    editor().focus();
    editor().setSelectionRange(pos, pos);
  }

  function statistics() {
    const text = editor().value;

    const words =
      text.trim() ? text.trim().split(/\s+/).length : 0;

    const chars = text.length;

    const noSpaces =
      text.replace(/\s/g, '').length;

    const lines =
      text ? text.split('\n').length : 0;

    const paragraphs =
      text.trim()
        ? text.trim().split(/\n\s*\n/).length
        : 0;

    alert(
      `إحصائيات الملف\n\n` +
      `الكلمات: ${words}\n` +
      `الأحرف: ${chars}\n` +
      `بدون مسافات: ${noSpaces}\n` +
      `الأسطر: ${lines}\n` +
      `الفقرات: ${paragraphs}`
    );
  }

  function searchReplace() {
    const search = prompt('النص المطلوب البحث عنه');
    if (search === null || search === '') return;

    const replacement = prompt('النص البديل', '');

    const text = editor().value;

    const escaped =
      search.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

    const re = new RegExp(escaped, 'g');

    const count = (text.match(re) || []).length;

    if (replacement !== null) {
      editor().value = text.replace(re, replacement);
      markDirty();
    }

    alert(`عدد المطابقات: ${count}`);
  }

  function toggleWrap() {
    editorState.wordWrap = !editorState.wordWrap;
    updateStatus();
  }

  function toggleReadOnly() {
    editorState.readOnly = !editorState.readOnly;
    updateStatus();
  }

  function changeFontSize() {
    const value = prompt(
      'حجم الخط',
      String(editorState.fontSize)
    );

    if (!value) return;

    const n = Number(value);

    if (!Number.isFinite(n) || n < 8 || n > 40) {
      alert('حجم الخط يجب أن يكون بين 8 و40');
      return;
    }

    editorState.fontSize = n;
    updateStatus();
  }

  function escapeHtml(v) {
    return String(v)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;');
  }

  function init() {
    const e = editor();
    if (!e) return;

    e.addEventListener('input', () => {
      pushHistory();
      markDirty();
    });

    e.addEventListener('keydown', ev => {
      const ctrl = ev.ctrlKey || ev.metaKey;

      if (ctrl && ev.key.toLowerCase() === 'z') {
        ev.preventDefault();
        undo();
      }

      if (ctrl && ev.key.toLowerCase() === 'y') {
        ev.preventDefault();
        redo();
      }

      if (ctrl && ev.key.toLowerCase() === 'a') {
        ev.preventDefault();
        selectAll();
      }

      if (ctrl && ev.key.toLowerCase() === 'c') {
        copy();
      }

      if (ctrl && ev.key.toLowerCase() === 'x') {
        cut();
      }

      if (ctrl && ev.key.toLowerCase() === 'v') {
        paste();
      }

      if (ctrl && ev.key.toLowerCase() === 'f') {
        ev.preventDefault();
        searchReplace();
      }

      if (ctrl && ev.key.toLowerCase() === 'g') {
        ev.preventDefault();
        gotoLine();
      }

      if (ctrl && ev.key.toLowerCase() === 'n') {
        ev.preventDefault();
        newTab();
      }

      if (ctrl && ev.key.toLowerCase() === 'w') {
        ev.preventDefault();
        closeTab();
      }

      if (ev.key === 'Tab' && !editorState.readOnly) {
        ev.preventDefault();

        const start = e.selectionStart;
        const end = e.selectionEnd;

        e.setRangeText(
          '    ',
          start,
          end,
          'end'
        );

        markDirty();
      }
    });

    if (!editorState.tabs.length) {
      newTab();
    }
  }

  window.QFSEditor = {
    newTab,
    closeTab,
    activate,
    undo,
    redo,
    selectAll,
    copy,
    cut,
    paste,
    gotoLine,
    statistics,
    searchReplace,
    toggleWrap,
    toggleReadOnly,
    changeFontSize,
    language,
    getActive: activeTab
  };

  window.addEventListener('DOMContentLoaded', init);
})();
