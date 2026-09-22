'use strict';

window.QFSUpdater = (() => {

  const UPDATE_URL = '';

  let currentVersionCode = 120;
  let currentVersionName = '1.2.0';
  let checking = false;
  let installing = false;

  async function nativeCall(command, args = {}) {
    if (typeof window.call === 'function') {
      return window.call(command, args);
    }

    if (window.AndroidBridge &&
        typeof window.AndroidBridge.call === 'function') {

      return new Promise((resolve, reject) => {

        const id =
          'upd_' +
          Date.now() +
          '_' +
          Math.random()
            .toString(36)
            .slice(2);

        window.__qfsCallbacks ||= {};

        window.__qfsCallbacks[id] = {
          resolve,
          reject
        };

        try {
          window.AndroidBridge.call(
            JSON.stringify({
              id,
              command,
              args
            })
          );
        } catch (error) {
          delete window.__qfsCallbacks[id];
          reject(error);
        }
      });
    }

    throw new Error('Android bridge unavailable');
  }

  async function getAppInfo() {
    try {
      const info = await nativeCall('updateAppInfo');

      currentVersionCode =
        Number(info?.versionCode || 120);

      currentVersionName =
        info?.versionName || '1.2.0';

      return info;
    } catch (_) {
      return {
        versionCode: currentVersionCode,
        versionName: currentVersionName
      };
    }
  }

  async function check(url = UPDATE_URL) {

    if (!url || url.includes('YOUR-DOMAIN.example')) {
      return {
        available: false,
        configured: false
      };
    }

    if (checking) {
      return {
        available: false,
        busy: true
      };
    }

    checking = true;

    try {

      await getAppInfo();

      const result =
        await nativeCall(
          'updateCheck',
          { url }
        );

      return result;

    } catch (error) {

      console.warn(
        'Quick File Studio update check failed:',
        error
      );

      return {
        available: false,
        error: error?.message || String(error)
      };

    } finally {
      checking = false;
    }
  }

  async function install(latest) {

    if (installing) {
      return;
    }

    if (!latest || !latest.apkUrl) {
      throw new Error(
        'رابط تحديث APK غير متوفر'
      );
    }

    installing = true;

    try {

      const result =
        await nativeCall(
          'updateInstall',
          {
            apkUrl: latest.apkUrl,
            sha256: latest.sha256 || ''
          }
        );

      if (result?.needsInstallPermission) {

        const ok =
          window.confirm(
            'يحتاج Quick File Studio إلى السماح بالتثبيت من هذا المصدر. افتح الإعدادات الآن؟'
          );

        if (ok) {
          await nativeCall(
            'openInstallSettings'
          );
        }

        return result;
      }

      if (!result?.ok) {
        throw new Error(
          result?.message ||
          'تعذر بدء تثبيت التحديث'
        );
      }

      return result;

    } finally {
      installing = false;
    }
  }

  function show(data) {

    const existing =
      document.getElementById(
        'updateModal'
      );

    if (existing) {
      existing.remove();
    }

    const notes =
      Array.isArray(data.releaseNotes)
        ? data.releaseNotes
        : [];

    const modal =
      document.createElement('div');

    modal.id = 'updateModal';
    modal.className = 'modal-overlay';

    modal.innerHTML = `
      <div class="update-card">

        <div class="update-icon">↻</div>

        <h2>
          ${escapeHtml(
            data.title ||
            'يتوفر تحديث جديد'
          )}
        </h2>

        <p>
          ${escapeHtml(
            data.message ||
            'يتوفر إصدار جديد من Quick File Studio.'
          )}
        </p>

        <div class="version-box">

          <div>
            <small>الإصدار الحالي</small>
            <strong>
              ${escapeHtml(currentVersionName)}
            </strong>
          </div>

          <div class="version-arrow">→</div>

          <div>
            <small>الإصدار الجديد</small>
            <strong>
              ${escapeHtml(
                data.versionName || ''
              )}
            </strong>
          </div>

        </div>

        ${
          notes.length
            ? `
              <h3>ما الجديد</h3>
              <ul>
                ${notes
                  .map(
                    note =>
                      `<li>${escapeHtml(note)}</li>`
                  )
                  .join('')}
              </ul>
            `
            : ''
        }

        <div class="update-actions">

          <button
            id="updateLaterBtn"
            class="secondary-btn">
            لاحقًا
          </button>

          <button
            id="updateNowBtn"
            class="primary-btn">
            تحديث الآن
          </button>

        </div>

      </div>
    `;

    document.body.appendChild(modal);

    const later =
      document.getElementById(
        'updateLaterBtn'
      );

    const now =
      document.getElementById(
        'updateNowBtn'
      );

    if (later) {
      later.onclick = () => {
        modal.remove();
      };
    }

    if (now) {

      now.onclick = async () => {

        if (installing) {
          return;
        }

        now.disabled = true;
        now.textContent = 'جارٍ تنزيل التحديث...';

        try {

          const result =
            await install(data);

          if (result?.needsInstallPermission) {
            now.disabled = false;
            now.textContent = 'تحديث الآن';
            return;
          }

          modal.remove();

          if (typeof toast === 'function') {
            toast(
              'تم تنزيل التحديث وبدأ تثبيته'
            );
          }

        } catch (error) {

          now.disabled = false;
          now.textContent = 'تحديث الآن';

          alert(
            error?.message ||
            'تعذر تثبيت التحديث'
          );
        }
      };
    }
  }

  function escapeHtml(value) {
    return String(value ?? '')
      .replace(
        /[&<>"']/g,
        char => ({
          '&': '&amp;',
          '<': '&lt;',
          '>': '&gt;',
          '"': '&quot;',
          "'": '&#39;'
        }[char])
      );
  }

  return {
    check,
    show,
    install,
    getAppInfo,
    CURRENT_VERSION_CODE:
      currentVersionCode,
    CURRENT_VERSION_NAME:
      currentVersionName
  };

})();
