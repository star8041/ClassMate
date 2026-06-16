/**
 * archive.js – 강의자료 관리 페이지 스크립트
 */

/* ── 파일 업로드 트리거 ── */
function triggerUpload() {
    document.getElementById('fileInput')?.click();
}

/* ── 파일 선택 시 ── */
function onFileSelected(e) {
    const files = Array.from(e.target.files);
    files.forEach(f => addFileCard(f));
    e.target.value = ''; // 동일 파일 재선택 허용
}

/* ── 드래그앤드롭 ── */
function onDragOver(e) {
    e.preventDefault();
    document.getElementById('uploadZone').classList.add('drag-over');
}

function onDragLeave(e) {
    document.getElementById('uploadZone').classList.remove('drag-over');
}

function onDrop(e) {
    e.preventDefault();
    document.getElementById('uploadZone').classList.remove('drag-over');
    const files = Array.from(e.dataTransfer.files).filter(f => f.type === 'application/pdf');
    if (files.length === 0) {
        alert('PDF 파일만 업로드할 수 있습니다.');
        return;
    }
    files.forEach(f => addFileCard(f));
}

/* ── 파일 카드 동적 추가 ── */
let cardCount = 5; // 기존 카드 수

function addFileCard(file) {
    cardCount++;
    const sizeMB = (file.size / 1024 / 1024).toFixed(1) + 'MB';
    const now = new Date();
    const dateStr = `${now.getFullYear()}.${String(now.getMonth()+1).padStart(2,'0')}.${String(now.getDate()).padStart(2,'0')} `
                  + `${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`;

    // 마지막 카드의 --full 클래스 제거
    const grid = document.getElementById('fileGrid');
    const lastFull = grid.querySelector('.arc-file-card--full');
    if (lastFull) lastFull.classList.remove('arc-file-card--full');

    const card = document.createElement('div');
    card.className = 'arc-file-card' + (cardCount % 2 === 1 ? ' arc-file-card--full' : '');
    card.innerHTML = `
        <div class="arc-file-thumb">
            <span class="arc-thumb-num">${String(cardCount).padStart(2,'0')}</span>
            <span class="arc-thumb-label">${file.name.replace('.pdf','').replace(/_/g,' ')}</span>
        </div>
        <div class="arc-file-info">
            <div class="arc-file-top">
                <div class="arc-file-name-row">
                    <span class="arc-pdf-icon">
                        <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24"
                             fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                            <polyline points="14 2 14 8 20 8"/>
                        </svg>
                        PDF
                    </span>
                    <span class="arc-file-name">${file.name}</span>
                </div>
                <button class="arc-more-btn" onclick="toggleMenu(this)" aria-label="더보기">⋮</button>
            </div>
            <div class="arc-file-meta">
                <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24"
                     fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                    <line x1="16" y1="2" x2="16" y2="6"/>
                    <line x1="8" y1="2" x2="8" y2="6"/>
                    <line x1="3" y1="10" x2="21" y2="10"/>
                </svg>
                ${dateStr} <span class="arc-dot">·</span> ${sizeMB}
            </div>
            <button class="arc-download-btn" onclick="downloadFile('${file.name}')">
                <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24"
                     fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="7 10 12 15 17 10"/>
                    <line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
                다운로드
            </button>
        </div>
        <div class="arc-context-menu" style="display:none;">
            <button onclick="renameFile(this)">이름 변경</button>
            <button onclick="deleteFile(this)" class="danger">삭제</button>
        </div>`;
    grid.appendChild(card);
    updateCount();
}

/* ── 더보기 메뉴 토글 ── */
function toggleMenu(btn) {
    const card = btn.closest('.arc-file-card');
    const menu = card.querySelector('.arc-context-menu');
    const isOpen = menu.style.display !== 'none';
    // 다른 열린 메뉴 닫기
    document.querySelectorAll('.arc-context-menu').forEach(m => m.style.display = 'none');
    menu.style.display = isOpen ? 'none' : 'block';
}

/* 외부 클릭 시 메뉴 닫기 */
document.addEventListener('click', (e) => {
    if (!e.target.closest('.arc-more-btn') && !e.target.closest('.arc-context-menu')) {
        document.querySelectorAll('.arc-context-menu').forEach(m => m.style.display = 'none');
    }
});

/* ── 파일 삭제 ── */
function deleteFile(btn) {
    if (!confirm('이 파일을 삭제하시겠습니까?')) return;
    btn.closest('.arc-file-card').remove();
    cardCount--;
    updateCount();
    reorderCards();
}

/* ── 이름 변경 (간단 프롬프트) ── */
function renameFile(btn) {
    const card = btn.closest('.arc-file-card');
    const nameEl = card.querySelector('.arc-file-name');
    const labelEl = card.querySelector('.arc-thumb-label');
    const newName = prompt('새 파일명을 입력하세요:', nameEl.textContent);
    if (newName && newName.trim()) {
        nameEl.textContent = newName.trim();
        labelEl.textContent = newName.trim().replace('.pdf','').replace(/_/g,' ');
    }
    card.querySelector('.arc-context-menu').style.display = 'none';
}

/* ── 단일 파일 다운로드 ── */
function downloadFile(filename) {
    // 실제 구현: th:href 로 서버 엔드포인트 연결
    console.log('다운로드:', filename);
    alert(`"${filename}" 다운로드가 시작됩니다.`);
}

/* ── 전체 다운로드 ── */
function downloadAll() {
    console.log('전체 다운로드');
    alert('전체 파일을 ZIP으로 다운로드합니다.');
}

/* ── 파일 수 뱃지 업데이트 ── */
function updateCount() {
    const count = document.querySelectorAll('.arc-file-card').length;
    const badge = document.getElementById('fileCount');
    if (badge) badge.textContent = count + '개';
}

/* ── 카드 번호 재정렬 ── */
function reorderCards() {
    document.querySelectorAll('.arc-file-card').forEach((card, i) => {
        const numEl = card.querySelector('.arc-thumb-num');
        if (numEl && /^\d+$/.test(numEl.textContent)) {
            numEl.textContent = String(i + 1).padStart(2, '0');
        }
        // 마지막 홀수 카드 --full 처리
        card.classList.remove('arc-file-card--full');
    });
    const cards = document.querySelectorAll('.arc-file-card');
    if (cards.length % 2 === 1) {
        cards[cards.length - 1].classList.add('arc-file-card--full');
    }
    cardCount = cards.length;
}

/* ── 정렬 ── */
function sortFiles(value) {
    const grid = document.getElementById('fileGrid');
    const cards = Array.from(grid.querySelectorAll('.arc-file-card'));

    cards.sort((a, b) => {
        if (value === 'name') {
            const nameA = a.querySelector('.arc-file-name')?.textContent || '';
            const nameB = b.querySelector('.arc-file-name')?.textContent || '';
            return nameA.localeCompare(nameB, 'ko');
        }
        if (value === 'size') {
            const sizeA = parseFloat(a.querySelector('.arc-file-meta')?.textContent.match(/[\d.]+MB/)?.[0] || '0');
            const sizeB = parseFloat(b.querySelector('.arc-file-meta')?.textContent.match(/[\d.]+MB/)?.[0] || '0');
            return sizeB - sizeA;
        }
        // recent (기본): DOM 순서 유지
        return 0;
    });

    cards.forEach(c => grid.appendChild(c));
    reorderCards();
}
