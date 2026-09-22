(() => {
  'use strict';

  window.createEditorTab = function(){
    const existing=QFSEditor.tabs.findIndex(
      t=>t.name==='untitled.txt' && !t.uri && !t.dirty
    );

    if(existing>=0){
      QFSEditor.activate(existing);
      return;
    }

    QFSEditor.newTab();
    loadEditorTab(QFSEditor.current());
  };

  window.loadEditorTab=function(tab){
    const editor=document.getElementById('editor');
    if(!editor)return;

    editor.value=tab.content||'';
    editor.readOnly=QFSEditor.settings.readOnly;

    const name=document.getElementById('editorPath');
    if(name){
      name.textContent=tab.uri || tab.name;
    }

    if(window.qfsEditorCore){
      qfsEditorCore.updateStats();
    }
  };

  window.saveCurrentTab=async function(){
    const tab=QFSEditor.current();
    const editor=document.getElementById('editor');

    if(!tab || !editor)return;

    tab.content=editor.value;

    if(!tab.uri){
      if(typeof saveAs==='function'){
        await saveAs();
      }
      return;
    }

    try{
      await call('write',{
        uri:tab.uri,
        content:tab.content
      });

      tab.originalContent=tab.content;
      tab.dirty=false;

      if(typeof toast==='function')
        toast('تم حفظ الملف');

      QFSEditor.render();

    }catch(error){
      tab.dirty=true;

      if(typeof toast==='function')
        toast('فشل الحفظ: '+error.message);
    }
  };

  window.closeCurrentTab=function(){
    QFSEditor.close();
  };

  window.newFileFromEditor=function(){
    createEditorTab();
  };
})();

/* =========================================================
   QFS 1.3.0 - LIVE DIRTY STATE
   ========================================================= */
(() => {
  'use strict';

  function bindLiveDirtyState() {
    const editor = document.getElementById('editor');

    if (!editor || editor.dataset.qfsDirtyBound === '1') {
      return;
    }

    editor.dataset.qfsDirtyBound = '1';

    editor.addEventListener('input', () => {
      const tab = QFSEditor.current();
      if (!tab) return;

      tab.content = editor.value;

      const dirty = tab.content !== tab.originalContent;

      if (tab.dirty !== dirty) {
        tab.dirty = dirty;
        QFSEditor.render();
      }

      if (typeof window.QFSUpdateCaretStatus === 'function') {
        window.QFSUpdateCaretStatus();
      }
    });
  }

  window.QFSBindLiveDirtyState = bindLiveDirtyState;

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', bindLiveDirtyState, { once: true });
  } else {
    bindLiveDirtyState();
  }
})();

/* QFS 1.3.0 - activate live dirty tracking */
if (typeof window.QFSBindLiveDirtyState === 'function') {
  window.QFSBindLiveDirtyState();
}
