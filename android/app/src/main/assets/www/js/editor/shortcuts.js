(() => {
  document.addEventListener('keydown',async e=>{
    const ctrl=e.ctrlKey||e.metaKey;

    if(!ctrl)return;

    const key=e.key.toLowerCase();

    if(key==='s'){
      e.preventDefault();
      if(window.saveCurrentTab)
        await saveCurrentTab();
    }

    if(key==='n'){
      e.preventDefault();
      createEditorTab();
    }

    if(key==='w'){
      e.preventDefault();
      closeCurrentTab();
    }

    if(key==='f'){
      e.preventDefault();
      qfsEditorCore?.find();
    }

    if(key==='h'){
      e.preventDefault();
      qfsEditorCore?.replace();
    }

    if(key==='g'){
      e.preventDefault();
      qfsEditorCore?.gotoLine();
    }

    if(key==='a'){
      const e2=document.getElementById('editor');
      if(e2)e2.select();
    }
  });
})();
