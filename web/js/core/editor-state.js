(() => {
  'use strict';

  window.QFSEditor = {
    tabs: [],
    active: -1,
    settings: {
      fontSize: 15,
      wordWrap: true,
      readOnly: false,
      lineNumbers: true,
      autoIndent: true,
      miniToolbar: true,
      encoding: 'UTF-8',
      theme: 'dark'
    },

    newTab(name='untitled.txt', uri=null, content='') {
      const tab = {
        id: 'tab_' + Date.now() + '_' + Math.random().toString(36).slice(2),
        name,
        uri,
        content,
        originalContent: content,
        dirty: false,
        encoding: 'UTF-8',
        language: 'Plain Text'
      };

      this.tabs.push(tab);
      this.active = this.tabs.length - 1;
      this.render();
      return tab;
    },

    current() {
      return this.tabs[this.active] || null;
    },

    markDirty(value=true) {
      const tab=this.current();
      if(!tab)return;

      tab.dirty=value;
      this.render();
    },

    close(index=this.active) {
      if(index<0 || index>=this.tabs.length)return;

      const tab=this.tabs[index];

      if(tab.dirty){
        const result=confirm(
          `الملف "${tab.name}" يحتوي على تعديلات غير محفوظة.\n\n`+
          `موافق = إغلاق\nإلغاء = الاحتفاظ به`
        );

        if(!result)return false;
      }

      this.tabs.splice(index,1);

      if(this.tabs.length===0){
        this.active=-1;
      }else{
        this.active=Math.min(index,this.tabs.length-1);
      }

      this.render();
      return true;
    },

    closeOthers() {
      const current=this.current();
      if(!current)return;

      if(current.dirty && !confirm('الملف الحالي غير محفوظ. المتابعة؟'))
        return;

      this.tabs=[current];
      this.active=0;
      this.render();
    },

    closeAll() {
      const dirty=this.tabs.some(t=>t.dirty);

      if(dirty && !confirm('توجد ملفات غير محفوظة. إغلاق الكل؟'))
        return;

      this.tabs=[];
      this.active=-1;
      this.render();
    },

    activate(index) {
      if(index<0 || index>=this.tabs.length)return;
      this.active=index;
      this.render();

      if(typeof window.loadEditorTab==='function')
        window.loadEditorTab(this.tabs[index]);
    },

    render() {
      const box=document.getElementById('qfsTabs');
      if(!box)return;

      box.innerHTML='';

      this.tabs.forEach((tab,index)=>{
        const el=document.createElement('button');

        el.className='qfs-tab'+
          (index===this.active?' active':'')+
          (tab.dirty?' dirty':'');

        el.innerHTML=
          `<span>${escapeHtml(tab.name)}</span>`+
          `${tab.dirty?'●':''}`+
          `<b>×</b>`;

        el.onclick=(event)=>{
          if(event.target.tagName==='B'){
            event.stopPropagation();
            this.close(index);
          }else{
            this.activate(index);
          }
        };

        box.appendChild(el);
      });

      const plus=document.createElement('button');
      plus.className='qfs-tab-add';
      plus.textContent='+';
      plus.onclick=()=>{
        if(typeof window.createEditorTab==='function')
          window.createEditorTab();
      };

      box.appendChild(plus);
    }
  };

  function escapeHtml(value){
    return String(value)
      .replace(/&/g,'&amp;')
      .replace(/</g,'&lt;')
      .replace(/>/g,'&gt;')
      .replace(/"/g,'&quot;');
  }
})();
