/**
 * layout.js – ClassMate 공통 레이아웃 스크립트
 */

/* ── 알림 배지 업데이트 ── */
function updateNotiBadge(count) {
  const badge = document.getElementById('notiBadge');
  if (!badge) return;
  if (count > 0) {
    badge.textContent = count > 99 ? '99+' : count;
    badge.style.display = 'inline-block';
  } else {
    badge.style.display = 'none';
  }
}

/* ── 모바일 사이드바 토글 ── */
function toggleSidebar() {
  document.getElementById('sidebar')?.classList.toggle('open');
}

document.addEventListener('DOMContentLoaded', () => {
  document.addEventListener('click', (e) => {
    const sidebar = document.getElementById('sidebar');
    if (
      window.innerWidth <= 768 &&
      sidebar?.classList.contains('open') &&
      !sidebar.contains(e.target) &&
      !e.target.closest('[onclick="toggleSidebar()"]')
    ) {
      sidebar.classList.remove('open');
    }
  });
});

/* ── 초대코드 ── */
let inviteTimerInterval = null;

// 페이지 로드 시 서버에 유효한 코드가 있으면 복원 (새로 생성하지 않음)
document.addEventListener('DOMContentLoaded', async function () {
  const body = document.getElementById('inviteBody');
  if (!body) return;

  const token = localStorage.getItem('accessToken');
  if (!token) return;

  try {
    const res = await fetch('/api/v1/teachers/me/invite-code', {
      headers: { 'Authorization': 'Bearer ' + token }
    });
    if (res.ok) {
      const data = await res.json();
      showInviteCode(data.code, data.expiresAt);
    }
    // 204 또는 오류면 버튼 그대로 유지
  } catch (e) {
    // 네트워크 오류 시 버튼 그대로 유지
  }
});

async function generateInvite() {
  const t = localStorage.getItem('accessToken');
  const body = document.getElementById('inviteBody');
  if (!t || !body) return;
  body.innerHTML = '<span style="font-size:13px;color:#888">생성 중…</span>';
  try {
    const res = await fetch('/api/v1/teachers/me/invite-code', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + t }
    });
    if (!res.ok) {
      body.innerHTML = '<span class="invite-expired">생성 실패</span>'
        + '<button class="invite-gen-btn" onclick="generateInvite()">다시 시도</button>';
      return;
    }
    const data = await res.json();
    showInviteCode(data.code, data.expiresAt);
  } catch (e) {
    body.innerHTML = '<span class="invite-expired">오류가 발생했습니다</span>';
  }
}

function showInviteCode(code, expiresAt) {
  const body = document.getElementById('inviteBody');
  if (!body) return;
  body.className = 'invite-wrapper';
  body.innerHTML =
    '<span class="invite-code">' + code + '</span>' +
    '<span class="invite-timer" id="inviteTimer"></span>' +
    '<button class="invite-icon-btn" title="복사" onclick="copyInvite(\'' + code + '\', this)">' +
      '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>' +
    '</button>' +
    '<button class="invite-icon-btn" title="재생성" onclick="generateInvite()">' +
      '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>' +
    '</button>';
  startInviteTimer(new Date(expiresAt).getTime());
}

function startInviteTimer(expiryMs) {
  if (inviteTimerInterval) clearInterval(inviteTimerInterval);
  function tick() {
    const timerEl = document.getElementById('inviteTimer');
    const body = document.getElementById('inviteBody');
    const remain = Math.floor((expiryMs - Date.now()) / 1000);
    if (remain <= 0) {
      clearInterval(inviteTimerInterval);
      if (body) {
        body.className = '';
        body.innerHTML = '<button class="invite-gen-btn" onclick="generateInvite()">코드 생성</button>';
      }
      return;
    }
    const m = String(Math.floor(remain / 60)).padStart(2, '0');
    const s = String(remain % 60).padStart(2, '0');
    if (timerEl) timerEl.textContent = '⏱ ' + m + ':' + s;
  }
  tick();
  inviteTimerInterval = setInterval(tick, 1000);
}

function copyInvite(code, btn) {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(code).then(function () {
      if (btn) { btn.style.color = '#1f9d55'; setTimeout(function () { btn.style.color = '#3b5bdb'; }, 1200); }
    });
  }
}
