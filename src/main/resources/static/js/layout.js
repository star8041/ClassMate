/**
 * layout.js – ClassMate 공통 레이아웃 스크립트
 * header.html + sidebar.html 에서 사용
 */

/* ── 프로필 드롭다운 ── */
function toggleProfileMenu() {
  const menu = document.getElementById('profileMenu');
  const btn  = document.querySelector('.header-profile-btn');
  if (!menu) return;
  const isOpen = menu.style.display !== 'none';
  menu.style.display = isOpen ? 'none' : 'block';
  btn?.setAttribute('aria-expanded', String(!isOpen));
}

/* 바깥 클릭 시 드롭다운 닫기 */
document.addEventListener('click', (e) => {
  const profile = document.getElementById('profileDropdown');
  const menu    = document.getElementById('profileMenu');
  const btn     = document.querySelector('.header-profile-btn');
  if (!profile || !menu) return;
  if (!profile.contains(e.target)) {
    menu.style.display = 'none';
    btn?.setAttribute('aria-expanded', 'false');
  }
});

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

/**
 * 알림 수 서버 폴링 (선택 사용)
 * 사용법: pollNotifications(30000)  ← 30초마다
 * 서버: GET /notifications/count → { "count": 3 }
 */
function pollNotifications(intervalMs = 60000) {
  const fetch$ = () =>
    fetch('/notifications/count')
      .then(r => r.json())
      .then(d => updateNotiBadge(d.count ?? 0))
      .catch(() => {});
  fetch$();
  setInterval(fetch$, intervalMs);
}

/* ── 모바일 사이드바 토글 ── */
function toggleSidebar() {
  document.getElementById('sidebar')?.classList.toggle('open');
}

/* 모바일 – 사이드바 외부 클릭 시 닫기 */
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
