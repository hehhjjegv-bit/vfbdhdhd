'use strict';

window.QFSi18n = (() => {

  const supported = {
    ar: 'العربية',
    en: 'English',
    ru: 'Русский',
    tr: 'Türkçe'
  };

  let lang = localStorage.getItem('qfs.lang') || 'ar';
  let dict = {};

  async function load(code) {
    if (!supported[code]) code = 'ar';

    try {
      if (typeof window.qfsNativeCall === 'function') {
        const result = await window.qfsNativeCall('readAsset', {
          path: `locales/${code}.json`
        });

        const content = result?.content || '';
        if (!content.trim()) {
          throw new Error('Translation file is empty');
        }

        dict = JSON.parse(content);
      } else {
        const response = await fetch(`locales/${code}.json`);
        if (!response.ok) {
          throw new Error('Translation file not found');
        }

        dict = await response.json();
      }

      lang = code;

      localStorage.setItem('qfs.lang', code);
      document.documentElement.lang = code;
      document.documentElement.dir =
        code === 'ar' || code === 'fa' || code === 'ur'
          ? 'rtl'
          : 'ltr';

      apply();
    } catch (error) {
      console.error('Translation load failed:', error);
    }
  }

  function t(key) {
    return dict[key] || key;
  }

  function apply() {

    document.querySelectorAll('[data-i18n]')
      .forEach(element => {

        const key = element.dataset.i18n;

        element.textContent = t(key);
      });

    document.querySelectorAll('[data-i18n-placeholder]')
      .forEach(element => {

        element.placeholder =
          t(element.dataset.i18nPlaceholder);
      });
  }

  return {
    load,
    t,
    apply,
    languages: supported,
    get current() {
      return lang;
    }
  };

})();
