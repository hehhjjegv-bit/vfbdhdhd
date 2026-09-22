(() => {
  'use strict';

  const $ = id => document.getElementById(id);

  const editorState = {
    fontSize: Number(localStorage.getItem('qfs.editor.fontSize') || 15),
    wrap: localStorage.getItem('qfs.editor.wrap') !== 'false',
    readonly: false
  };

  function editor(){
    return $('editor');
  }

  function status(){
    return $('editorStats');
  }

  function updateStats(){
    const e = editor();
    if(!e) return;

    const text = e.value || '';
    const pos = e.selectionStart || 0;
    const before = text.slice(0,pos);
    const line = before.split('\n').length;
    const last = before.lastIndexOf('\n');
    const column = pos - last;

    const lines = text ? text.split('\n').length : 1;
    const chars = text.length;
    const words = text.trim() ? text.trim().split(/\s+/).length : 0;

    if(status()){
      status().textContent =
        `السطر ${line} | العمود ${column} | ${lines} سطر | ${words} كلمة | ${chars} حرف`;
    }

    const mode = $('editorMode');
    if(mode){
      mode.textContent = detectLanguage(state.editorName || '');
    }
  }

  function detectLanguage(name){
    const ext = (name.split('.').pop() || '').toLowerCase();

    const map = {
      js:'JavaScript',
      mjs:'JavaScript',
      cjs:'JavaScript',
      ts:'TypeScript',
      jsx:'JSX',
      tsx:'TSX',
      html:'HTML',
      htm:'HTML',
      css:'CSS',
      scss:'SCSS',
      less:'LESS',
      xml:'XML',
      json:'JSON',
      yaml:'YAML',
      yml:'YAML',
      toml:'TOML',
      py:'Python',
      pyw:'Python',
      java:'Java',
      kt:'Kotlin',
      kts:'Kotlin',
      c:'C',
      h:'C/C++ Header',
      cpp:'C++',
      cc:'C++',
      cxx:'C++',
      hpp:'C++ Header',
      cs:'C#',
      go:'Go',
      rs:'Rust',
      php:'PHP',
      rb:'Ruby',
      pl:'Perl',
      lua:'Lua',
      r:'R',
      swift:'Swift',
      m:'Objective-C',
      mm:'Objective-C++',
      dart:'Dart',
      sql:'SQL',
      sh:'Shell',
      bash:'Bash',
      zsh:'Zsh',
      ps1:'PowerShell',
      bat:'Batch',
      cmd:'Batch',
      md:'Markdown',
      tex:'LaTeX',
      vue:'Vue',
      svelte:'Svelte'
    };

    return map[ext] || 'Plain Text';
  }

  function indent(){
    const e=editor();
    if(!e)return;

    const start=e.selectionStart;
    const end=e.selectionEnd;
    const selected=e.value.slice(start,end);

    if(!selected){
      e.setRangeText('    ',start,end,'end');
      return;
    }

    const changed=selected
      .split('\n')
      .map(line=>'    '+line)
      .join('\n');

    e.setRangeText(changed,start,end,'select');
    updateStats();
  }

  function unindent(){
    const e=editor();
    if(!e)return;

    const start=e.selectionStart;
    const end=e.selectionEnd;
    const selected=e.value.slice(start,end);

    if(!selected){
      const lineStart=e.value.lastIndexOf('\n',start-1)+1;
      const part=e.value.slice(lineStart,start);
      const remove=part.startsWith('    ')?4:part.startsWith('\t')?1:0;

      if(remove){
        e.setRangeText('',lineStart,lineStart+remove,'end');
      }
      return;
    }

    const changed=selected
      .split('\n')
      .map(line=>line.replace(/^ {1,4}|\t/,''))
      .join('\n');

    e.setRangeText(changed,start,end,'select');
    updateStats();
  }

  function toggleWrap(){
    editorState.wrap=!editorState.wrap;
    localStorage.setItem('qfs.editor.wrap',editorState.wrap);
    apply();
  }

  function toggleReadonly(){
    editorState.readonly=!editorState.readonly;
    apply();
  }

  function font(delta){
    editorState.fontSize=Math.max(
      10,
      Math.min(32,editorState.fontSize+delta)
    );

    localStorage.setItem(
      'qfs.editor.fontSize',
      editorState.fontSize
    );

    apply();
  }

  function apply(){
    const e=editor();
    if(!e)return;

    e.style.fontSize=editorState.fontSize+'px';
    e.style.whiteSpace=editorState.wrap?'pre-wrap':'pre';
    e.style.overflowX=editorState.wrap?'hidden':'auto';
    e.readOnly=editorState.readonly;

    const mode=$('editorMode');
    if(mode){
      const flags=[];
      if(editorState.readonly) flags.push('قراءة فقط');
      if(editorState.wrap) flags.push('Word Wrap');

      mode.textContent =
        detectLanguage(state.editorName||'') +
        (flags.length?' · '+flags.join(' · '):'');
    }
  }

  function find(){
    const e=editor();
    if(!e)return;

    const q=prompt('بحث عن:');
    if(!q)return;

    const text=e.value;
    const start=e.selectionEnd;
    let index=text.indexOf(q,start);

    if(index<0) index=text.indexOf(q,0);

    if(index>=0){
      e.focus();
      e.setSelectionRange(index,index+q.length);
    }else{
      alert('لم يتم العثور على النص');
    }
  }

  function replace(){
    const e=editor();
    if(!e)return;

    const search=prompt('النص المطلوب استبداله:');
    if(search===null || search==='')return;

    const replacement=prompt('استبدال بـ:');
    if(replacement===null)return;

    const count=(e.value.match(
      new RegExp(search.replace(/[.*+?^${}()|[\]\\]/g,'\\$&'),'g')
    )||[]).length;

    e.value=e.value.split(search).join(replacement);
    state.editorDirty=true;
    updateStats();

    alert(`تم استبدال ${count} نتيجة`);
  }

  function gotoLine(){
    const e=editor();
    if(!e)return;

    const line=Number(prompt('رقم السطر:'));
    if(!Number.isInteger(line) || line<1)return;

    const lines=e.value.split('\n');
    if(line>lines.length){
      alert('رقم السطر غير موجود');
      return;
    }

    let pos=0;
    for(let i=0;i<line-1;i++){
      pos+=lines[i].length+1;
    }

    e.focus();
    e.setSelectionRange(pos,pos);
  }

  function statistics(){
    const e=editor();
    if(!e)return;

    const text=e.value||'';
    const words=text.trim()?text.trim().split(/\s+/).length:0;
    const lines=text?text.split('\n').length:1;

    alert(
      `إحصائيات الملف\n\n`+
      `الأسطر: ${lines}\n`+
      `الكلمات: ${words}\n`+
      `الأحرف: ${text.length}\n`+
      `البايتات UTF-8 تقريبًا: ${new TextEncoder().encode(text).length}`
    );
  }

  function preview(){
    const e=editor();
    if(!e)return;

    const name=state.editorName||'';
    if(!/\.(html?|svg)$/i.test(name)){
      alert('المعاينة متاحة حاليًا لملفات HTML وSVG');
      return;
    }

    const blob=new Blob(
      [e.value],
      {type:name.toLowerCase().endsWith('svg')
        ?'image/svg+xml'
        :'text/html'}
    );

    const url=URL.createObjectURL(blob);
    window.open(url,'_blank');
  }


  /* ==============================
     QFS_SYNTAX_V1
     ============================== */

  const QFS_SYNTAX_V1 = true;

  function syntaxLanguage(){
    try{
      return detectLanguage(
        typeof state !== 'undefined' ? (state.editorName || '') : ''
      );
    }catch(_){
      return 'Plain Text';
    }
  }

  function escapeSyntax(value){
    return String(value)
      .replace(/&/g,'&amp;')
      .replace(/</g,'&lt;')
      .replace(/>/g,'&gt;');
  }

  function highlightCode(source){
    const lang = syntaxLanguage();
    const text = String(source || '');

    if(!text) return '';

    const escaped = escapeSyntax(text);

    const codeLanguages = [
      'JavaScript','TypeScript','JSX','TSX',
      'Python','Java','Kotlin','C','C/C++ Header',
      'C++','C#','Go','Rust','PHP','Ruby','Perl',
      'Lua','R','Swift','Objective-C','Objective-C++',
      'Dart','SQL','Shell','Bash','Zsh','PowerShell',
      'Batch','Vue','Svelte'
    ];

    if(codeLanguages.includes(lang)){
      return highlightCodeLanguage(escaped,lang);
    }

    if(['HTML','XML','Vue','Svelte'].includes(lang)){
      return highlightMarkup(escaped);
    }

    if(lang === 'CSS' || lang === 'SCSS' || lang === 'LESS'){
      return highlightCss(escaped);
    }

    if(lang === 'JSON'){
      return highlightJson(escaped);
    }

    if(lang === 'Markdown'){
      return highlightMarkdown(escaped);
    }

    if(['YAML','TOML'].includes(lang)){
      return highlightConfig(escaped);
    }

    return escaped;
  }

  function tokenSpan(cls,text){
    return `<span class="qfs-token-${cls}">${text}</span>`;
  }

  function highlightCodeLanguage(text,lang){
    const keywords = {
      JavaScript:
        'const let var function return if else for while do switch case break continue new class extends import export from async await try catch finally throw typeof instanceof in of this true false null undefined yield delete',
      TypeScript:
        'const let var function return if else for while switch case break continue new class extends interface type enum public private protected readonly implements import export from async await try catch finally throw typeof instanceof in of this true false null undefined',
      Python:
        'and as assert async await break case class continue def del elif else except False finally for from global if import in is lambda match None nonlocal not or pass raise return True try while with yield',
      Java:
        'abstract assert boolean break byte case catch char class const continue default do double else enum extends final finally float for if implements import instanceof int interface long native new package private protected public return short static strictfp super switch synchronized this throw throws transient try void volatile while true false null',
      Kotlin:
        'as break class continue do else false for fun if in interface is null object package return super this throw true try typealias typeof val var when while',
      C:
        'auto break case char const continue default do double else enum extern float for goto if int long register return short signed sizeof static struct switch typedef union unsigned void volatile while',
      'C++':
        'alignas alignof and asm auto bool break case catch char class const constexpr continue default delete do double else enum explicit export extern false float for friend if inline int long mutable namespace new nullptr operator private protected public register reinterpret_cast return short signed sizeof static struct switch template this throw true try typedef typename union unsigned using virtual void volatile while',
      'C#':
        'abstract as base bool break byte case catch char checked class const continue decimal default delegate do double else enum event explicit extern false finally fixed float for foreach goto if implicit in int interface internal is lock long namespace new null object operator out override params private protected public readonly ref return sbyte sealed short sizeof stackalloc static string struct switch this throw true try typeof uint ulong unchecked unsafe ushort using virtual void volatile while async await var',
      Go:
        'break default func interface select case defer go map struct chan else goto package switch const fallthrough if range type continue for import return var',
      Rust:
        'as break const continue crate else enum extern false fn for if impl in let loop match mod move mut pub ref return self Self static struct super trait true type unsafe use where while async await dyn',
      PHP:
        'and or xor __FILE__ __LINE__ array as break case class const continue declare default die do echo else elseif empty enddeclare endfor endforeach endif endswitch endwhile eval exit extends final for foreach function global if include include_once instanceof insteadof interface isset list namespace new print private protected public require require_once return static switch throw trait try unset use var while yield true false null',
      Ruby:
        'BEGIN END alias and begin break case class def defined do else elsif end ensure false for if in module next nil not or redo rescue retry return self super then true undef unless until when while yield',
      SQL:
        'select from where and or insert into update delete create alter drop table join inner left right full outer on as group by order having limit offset union all distinct values set null is not like between exists case when then else end asc desc primary key foreign references index',
      Shell:
        'if then else elif fi for while in do done case esac function select time until',
      Bash:
        'if then else elif fi for while in do done case esac function select time until',
      PowerShell:
        'function param begin process end if else elseif foreach for while do until switch return break continue try catch finally throw class enum filter',
      Dart:
        'abstract as assert async await break case catch class const continue covariant default deferred do dynamic else enum export extends extension external factory false final finally for Function get hide if implements import in interface is late library mixin new null on operator part required rethrow return set show static super switch sync this throw true try typedef var void while with yield',
      Swift:
        'associatedtype class deinit enum extension fileprivate func import init in internal let open operator private protocol public static struct subscript typealias var break case continue default defer do else fallthrough for guard if in repeat return switch where while as Any catch false is nil rethrows super self Self throw throws true try',
      Lua:
        'and break do else elseif end false for function goto if in local nil not or repeat return then true until while',
      R:
        'if else repeat while function for in next break TRUE FALSE NULL Inf NaN NA',
      Perl:
        'my our local state sub if else elsif unless while until for foreach return package use require eval die last next redo',
      'Objective-C':
        'if else for while return class interface implementation protocol property synthesize import include define',
      'Objective-C++':
        'if else for while return class interface implementation protocol property synthesize import include define',
      Svelte:
        'if else each await then catch const let function'
    };

    const words = keywords[lang] || keywords.JavaScript;

    const keywordSet = new Set(words.split(/\s+/));

    const parts = text.split(/(\s+|\/\/[^\n]*|\/\*[\s\S]*?\*\/|#[^\n]*|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|`(?:\\.|[^`\\])*`|\b\d+(?:\.\d+)?\b)/g);

    return parts.map(part=>{
      if(!part) return '';

      if(/^(\s+)$/.test(part)) return part;

      if(/^(\/\/|\/\*|#)/.test(part))
        return tokenSpan('comment',part);

      if(/^["'`]/.test(part))
        return tokenSpan('string',part);

      if(/^\d/.test(part))
        return tokenSpan('number',part);

      if(/^(true|false|null|undefined|None|True|False|nil|NULL)$/.test(part))
        return tokenSpan('boolean',part);

      if(keywordSet.has(part))
        return tokenSpan('keyword',part);

      if(/^[+\-*\/%=!<>|&?:]+$/.test(part))
        return tokenSpan('operator',part);

      return part;
    }).join('');
  }

  function highlightMarkup(text){
    return text
      .replace(
        /(&lt;\/?)([A-Za-z][\w:-]*)([^&]*?)(\/?&gt;)/g,
        (_,open,name,attrs,close)=>{
          let rendered = tokenSpan('operator',open);
          rendered += tokenSpan('tag',name);

          rendered += attrs.replace(
            /([A-Za-z_:][\w:.-]*)(=)(&quot;.*?&quot;|&#39;.*?&#39;)/g,
            (_,a,eq,v)=>
              tokenSpan('attribute',a) +
              tokenSpan('operator',eq) +
              tokenSpan('string',v)
          );

          rendered += tokenSpan('operator',close);
          return rendered;
        }
      );
  }

  function highlightCss(text){
    return text
      .replace(/(\/\*[\s\S]*?\*\/)/g, tokenSpan('comment','$1'))
      .replace(
        /([.#]?[A-Za-z][\w-]*)(\s*\{)/g,
        (_,name,brace)=>tokenSpan('property',name)+brace
      )
      .replace(
        /([A-Za-z-]+)(\s*:)/g,
        (_,name,col)=>tokenSpan('attribute',name)+col
      )
      .replace(
        /(["'])(?:\\.|(?!\1)[^\\])*\1/g,
        m=>tokenSpan('string',m)
      );
  }

  function highlightJson(text){
    return text
      .replace(
        /("(?:\\.|[^"\\])*")(\s*:)/g,
        (_,key,col)=>tokenSpan('property',key)+col
      )
      .replace(
        /"(?:\\.|[^"\\])*"/g,
        m=>tokenSpan('string',m)
      )
      .replace(
        /\b(true|false|null)\b/g,
        m=>tokenSpan('boolean',m)
      )
      .replace(
        /-?\b\d+(?:\.\d+)?\b/g,
        m=>tokenSpan('number',m)
      );
  }

  function highlightMarkdown(text){
    return text
      .replace(
        /^(#{1,6})\s+(.*)$/gm,
        (_,hash,title)=>tokenSpan('keyword',hash+' '+title)
      )
      .replace(
        /(`+)(.+?)\1/g,
        (_,ticks,code)=>tokenSpan('string',ticks+code+ticks)
      )
      .replace(
        /(\*\*|__)(.+?)\1/g,
        (_,mark,value)=>tokenSpan('function',mark+value+mark)
      )
      .replace(
        /(^|\s)(https?:\/\/[^\s]+)/g,
        (_,space,url)=>space+tokenSpan('property',url)
      );
  }

  function highlightConfig(text){
    return text
      .replace(
        /^(\s*)([A-Za-z_][\w.-]*)(\s*:|\s*=)/gm,
        (_,space,key,sep)=>
          space+tokenSpan('property',key)+sep
      )
      .replace(
        /(#.*)$/gm,
        m=>tokenSpan('comment',m)
      )
      .replace(
        /"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'/g,
        m=>tokenSpan('string',m)
      );
  }

  function updateHighlight(){
    const e = editor();
    const layer = $('qfsHighlight');

    if(!e || !layer) return;

    layer.innerHTML =
      highlightCode(e.value || '') + '\n';

    layer.scrollTop = e.scrollTop;
    layer.scrollLeft = e.scrollLeft;
  }


  /* ==============================
     QFS_PRO_EDITOR_V1
     ============================== */

  function setupVisualEditor(){
    const e = editor();
    if(!e) return;

    let layout = e.parentElement;
    let gutter = document.getElementById('qfsLineNumbers');

    if(!layout || !layout.classList.contains('qfs-editor-layout')){
      layout = document.createElement('div');
      layout.className = 'qfs-editor-layout';

      const wrap = document.createElement('div');
      wrap.className = 'qfs-editor-wrap';

      e.parentNode.insertBefore(layout, e);
      wrap.appendChild(e);
      layout.appendChild(wrap);
    }

    gutter = document.getElementById('qfsLineNumbers');

    if(!gutter){
      gutter = document.createElement('div');
      gutter.id = 'qfsLineNumbers';
      gutter.className = 'qfs-line-numbers';
      gutter.setAttribute('aria-hidden','true');

      layout.insertBefore(gutter, layout.firstChild);
    }

    updateLineNumbers();

    if(!e.dataset.qfsVisualBound){
      e.dataset.qfsVisualBound = '1';

      e.addEventListener('scroll', syncEditorScroll);

      e.addEventListener('input', () => {
        updateLineNumbers();
        updateCurrentLine();
        updateHighlight();
      });

      e.addEventListener('scroll', updateHighlight);

      e.addEventListener('click', updateCurrentLine);
      e.addEventListener('keyup', updateCurrentLine);
      e.addEventListener('select', updateCurrentLine);
    }
  }

  function updateLineNumbers(){
    const e = editor();
    const gutter = document.getElementById('qfsLineNumbers');

    if(!e || !gutter) return;

    const count = Math.max(
      1,
      (e.value.match(/\n/g) || []).length + 1
    );

    let html = '';

    for(let i = 1; i <= count; i++){
      html += `<div data-line="${i}">${i}</div>`;
    }

    gutter.innerHTML = html;

    syncEditorScroll();
    updateCurrentLine();
  }

  function syncEditorScroll(){
    const e = editor();
    const gutter = document.getElementById('qfsLineNumbers');

    if(!e || !gutter) return;

    gutter.scrollTop = e.scrollTop;
  }

  function currentLineNumber(){
    const e = editor();
    if(!e) return 1;

    return e.value.slice(0,e.selectionStart).split('\n').length;
  }

  function updateCurrentLine(){
    const gutter = document.getElementById('qfsLineNumbers');
    if(!gutter) return;

    gutter.querySelectorAll('.current-line')
      .forEach(x => x.classList.remove('current-line'));

    const line = gutter.querySelector(
      `[data-line="${currentLineNumber()}"]`
    );

    if(line){
      line.classList.add('current-line');
    }
  }

  function lineIndentBeforeCursor(){
    const e = editor();
    if(!e) return '';

    const pos = e.selectionStart;
    const lineStart = e.value.lastIndexOf('\n',pos - 1) + 1;
    const line = e.value.slice(lineStart,pos);

    const m = line.match(/^[ \t]*/);
    return m ? m[0] : '';
  }

  function smartEnter(e){
    if(editorState.readonly) return false;

    const start = e.selectionStart;
    const end = e.selectionEnd;
    const value = e.value;

    const before = value.slice(0,start);
    const after = value.slice(end);

    const indent = lineIndentBeforeCursor();

    const previous = before.trimEnd().slice(-1);
    const next = after.trimStart().charAt(0);

    let extra = '';

    if(['{','[','('].includes(previous)){
      extra = '    ';
    }

    if(
      ['}',']',')'].includes(next) &&
      ['{','[','('].includes(previous)
    ){
      const text = '\n' + indent + '    ' + '\n' + indent;

      e.setRangeText(
        text,
        start,
        end,
        'end'
      );

      const cursor = start + indent.length + 5;

      e.setSelectionRange(cursor,cursor);
      updateLineNumbers();
      return true;
    }

    e.setRangeText(
      '\n' + indent + extra,
      start,
      end,
      'end'
    );

    updateLineNumbers();
    return true;
  }

  function autoPair(e,key){
    if(editorState.readonly) return false;

    const pairs = {
      '(': ')',
      '[': ']',
      '{': '}',
      '"': '"',
      "'": "'",
      '`': '`'
    };

    const close = pairs[key];

    if(!close) return false;

    const start = e.selectionStart;
    const end = e.selectionEnd;
    const value = e.value;

    if(start !== end){
      const selected = value.slice(start,end);

      e.setRangeText(
        key + selected + close,
        start,
        end,
        'end'
      );

      e.setSelectionRange(
        start + 1,
        start + 1 + selected.length
      );

      return true;
    }

    if(
      key === close &&
      value.charAt(start) === close
    ){
      e.setSelectionRange(start + 1,start + 1);
      return true;
    }

    e.setRangeText(
      key + close,
      start,
      end,
      'end'
    );

    e.setSelectionRange(
      start + 1,
      start + 1
    );

    return true;
  }

  function smartBackspace(e){
    if(editorState.readonly) return false;

    const pos = e.selectionStart;

    if(pos !== e.selectionEnd || pos === 0)
      return false;

    const left = e.value.charAt(pos - 1);
    const right = e.value.charAt(pos);

    const pairs = {
      '(': ')',
      '[': ']',
      '{': '}',
      '"': '"',
      "'": "'",
      '`': '`'
    };

    if(pairs[left] === right){
      e.setRangeText('',pos - 1,pos + 1,'end');
      return true;
    }

    return false;
  }

  function advancedFind(){
    const e = editor();
    if(!e) return;

    const q = prompt(
      'البحث\n\nاكتب النص أو Regex بين / /'
    );

    if(q === null || q === '') return;

    let regex = null;

    if(q.length >= 2 && q.startsWith('/') && q.lastIndexOf('/') > 0){
      const last = q.lastIndexOf('/');
      const pattern = q.slice(1,last);
      const flags = q.slice(last + 1) || 'g';

      try{
        regex = new RegExp(pattern,flags.includes('g') ? flags : flags + 'g');
      }catch(err){
        alert('Regex غير صالح: ' + err.message);
        return;
      }
    }

    const text = e.value;
    const start = e.selectionEnd;

    let index = -1;
    let length = 0;

    if(regex){
      regex.lastIndex = start;
      let match = regex.exec(text);

      if(!match){
        regex.lastIndex = 0;
        match = regex.exec(text);
      }

      if(match){
        index = match.index;
        length = match[0].length;
      }
    }else{
      index = text.indexOf(q,start);

      if(index < 0){
        index = text.indexOf(q,0);
      }

      if(index >= 0){
        length = q.length;
      }
    }

    if(index < 0){
      alert('لم يتم العثور على النص');
      return;
    }

    e.focus();
    e.setSelectionRange(index,index + length);
    updateCurrentLine();
  }

  function advancedReplace(){
    const e = editor();
    if(!e || editorState.readonly) return;

    const search = prompt(
      'البحث\n\nلـ Regex استخدم /النمط/gi'
    );

    if(search === null || search === '') return;

    const replacement = prompt('استبدال بـ:');

    if(replacement === null) return;

    let regex;

    if(
      search.startsWith('/') &&
      search.lastIndexOf('/') > 0
    ){
      const last = search.lastIndexOf('/');
      const pattern = search.slice(1,last);
      let flags = search.slice(last + 1);

      if(!flags.includes('g')) flags += 'g';

      try{
        regex = new RegExp(pattern,flags);
      }catch(err){
        alert('Regex غير صالح: ' + err.message);
        return;
      }
    }else{
      const escaped =
        search.replace(/[.*+?^${}()|[\]\\]/g,'\\$&');

      regex = new RegExp(escaped,'g');
    }

    const oldValue = e.value;
    const matches = oldValue.match(regex);

    if(!matches || !matches.length){
      alert('لم يتم العثور على أي نتيجة');
      return;
    }

    e.value = oldValue.replace(regex,replacement);

    state.editorDirty = true;

    updateStats();
    updateLineNumbers();

    alert(`تم استبدال ${matches.length} نتيجة`);
  }



  function markEditorDirtySafe(){
    try{
      if(typeof state !== 'undefined'){
        state.editorDirty = true;
      }
    }catch(_){}

    try{
      updateStats();
    }catch(_){}

    try{
      updateLineNumbers();
      updateCurrentLine();
    }catch(_){}
  }

  function bind(){
    const e=editor();
    if(!e)return;

    [
      'input',
      'keyup',
      'click',
      'select'
    ].forEach(ev=>{
      e.addEventListener(ev,updateStats);
    });

    e.addEventListener('keydown',ev=>{
      const ctrl=ev.ctrlKey||ev.metaKey;

      if(ev.key === 'Enter' && !ctrl && !ev.altKey){
        ev.preventDefault();
        smartEnter(e);
        markEditorDirtySafe();
        return;
      }

      if(ev.key === 'Backspace'){
        if(smartBackspace(e)){
          ev.preventDefault();
          markEditorDirtySafe();
          updateLineNumbers();
          return;
        }
      }

      if(!ctrl && !ev.altKey && !ev.shiftKey){
        if(['(', '[', '{', '"', "'", '`'].includes(ev.key)){
          ev.preventDefault();
          autoPair(e,ev.key);
          markEditorDirtySafe();
          updateLineNumbers();
          return;
        }
      }

      if(ctrl && ev.key.toLowerCase()==='f'){
        ev.preventDefault();
        advancedFind();
        return;
      }

      if(ctrl && ev.key.toLowerCase()==='h'){
        ev.preventDefault();
        advancedReplace();
        return;
      }

      if(ctrl && ev.key.toLowerCase()==='g'){
        ev.preventDefault();
        gotoLine();
        return;
      }

      if(ctrl && ev.key.toLowerCase()==='s'){
        ev.preventDefault();
        if(typeof saveEditor==='function') saveEditor();
        return;
      }

      if(ctrl && ev.key.toLowerCase()==='a'){
        return;
      }

      if(ev.key==='Tab'){
        ev.preventDefault();

        if(ev.shiftKey){
          unindent();
        }else{
          indent();
        }

        updateStats();
      }
    });

    setupVisualEditor();
    apply();
    updateStats();
    updateLineNumbers();
    updateCurrentLine();
    updateHighlight();
  }

  window.qfsEditorCore={
    find,
    replace,
    gotoLine,
    statistics,
    preview,
    toggleWrap,
    toggleReadonly,
    font,
    indent,
    unindent,
    updateStats,
    advancedFind,
    advancedReplace,
    updateLineNumbers,
    updateCurrentLine,
    updateHighlight
  };

  document.addEventListener('DOMContentLoaded',bind);
})();


/* =========================================================
   QFS 1.3.0 - EDITOR CARET STATUS
   ========================================================= */

(function(){
  function updateCaretStatus(){
    const e = document.getElementById('editor');
    const stats = document.getElementById('editorStats');

    if(!e || !stats) return;

    const pos = Number.isInteger(e.selectionStart)
      ? e.selectionStart
      : 0;

    const end = Number.isInteger(e.selectionEnd)
      ? e.selectionEnd
      : pos;

    const before = e.value.slice(0, pos);
    const line = before.split('\n').length;
    const lastBreak = before.lastIndexOf('\n');
    const column = pos - lastBreak;

    const selected = Math.max(0, end - pos);

    const lineText = e.value.split('\n')[line - 1] || '';
    const lines = e.value ? e.value.split('\n').length : 1;

    let text =
      `السطر ${line} | العمود ${column} | ${lines} سطر`;

    if(selected > 0){
      text += ` | محدد: ${selected}`;
    }

    stats.textContent = text;

    const gutter = document.querySelector('.qfs-line-numbers');

    if(gutter){
      gutter.querySelectorAll('.current-line')
        .forEach(el => el.classList.remove('current-line'));

      const current = gutter.querySelector(
        `[data-line="${line}"]`
      );

      if(current){
        current.classList.add('current-line');
      }
    }

    void lineText;
  }

  function bindCaretStatus(){
    const e = document.getElementById('editor');

    if(!e || e.dataset.qfsCaretStatus === '1'){
      return;
    }

    e.dataset.qfsCaretStatus = '1';

    [
      'click',
      'keyup',
      'select',
      'input',
      'focus'
    ].forEach(eventName => {
      e.addEventListener(eventName, updateCaretStatus);
    });

    e.addEventListener('select', updateCaretStatus);

    if(typeof requestAnimationFrame === 'function'){
      requestAnimationFrame(updateCaretStatus);
    }else{
      updateCaretStatus();
    }
  }

  window.QFSUpdateCaretStatus = updateCaretStatus;
  window.QFSBindCaretStatus = bindCaretStatus;

  if(document.readyState === 'loading'){
    document.addEventListener('DOMContentLoaded', bindCaretStatus, {once:true});
  }else{
    bindCaretStatus();
  }
})();

