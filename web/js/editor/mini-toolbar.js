(() => {
  'use strict';

  function insert(value){
    const e=document.getElementById('editor');
    if(!e || e.readOnly)return;

    const start=e.selectionStart;
    const end=e.selectionEnd;

    e.setRangeText(value,start,end,'end');
    e.dispatchEvent(new Event('input',{bubbles:true}));
    e.focus();
  }

  window.qfsMini={
    tab:()=>insert('    '),
    braces:()=>insert('{}'),
    parentheses:()=>insert('()'),
    semicolon:()=>insert(';'),
    angle:()=>insert('<>'),

    undo:()=>document.execCommand('undo'),
    redo:()=>document.execCommand('redo'),
    copy:()=>document.execCommand('copy'),
    paste:()=>document.execCommand('paste'),
    cut:()=>document.execCommand('cut'),

    left:()=>{
      const e=document.getElementById('editor');
      if(e)e.setSelectionRange(
        Math.max(0,e.selectionStart-1),
        Math.max(0,e.selectionStart-1)
      );
    },

    right:()=>{
      const e=document.getElementById('editor');
      if(e)e.setSelectionRange(
        Math.min(e.value.length,e.selectionEnd+1),
        Math.min(e.value.length,e.selectionEnd+1)
      );
    },

    up:()=>{
      const e=document.getElementById('editor');
      if(e)e.dispatchEvent(
        new KeyboardEvent('keydown',{key:'ArrowUp'})
      );
    },

    down:()=>{
      const e=document.getElementById('editor');
      if(e)e.dispatchEvent(
        new KeyboardEvent('keydown',{key:'ArrowDown'})
      );
    }
  };
})();
