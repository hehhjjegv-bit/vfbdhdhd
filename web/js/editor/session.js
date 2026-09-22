(() => {
  window.addEventListener('beforeunload',e=>{
    const dirty=QFSEditor?.tabs?.some(t=>t.dirty);

    if(dirty){
      e.preventDefault();
      e.returnValue='';
    }
  });

  document.addEventListener('visibilitychange',()=>{
    if(document.visibilityState==='hidden'){
      const data=QFSEditor.tabs.map(t=>({
        name:t.name,
        uri:t.uri,
        dirty:t.dirty
      }));

      localStorage.setItem(
        'qfs.openTabs',
        JSON.stringify(data)
      );
    }
  });
})();
