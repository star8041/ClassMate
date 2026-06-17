let currentTeacherId = null;

const API = {
  preview: "/api/v1/quizzes/preview",
  publish: "/api/v1/quizzes/generate",
  list: function (teacherId) {
    return teacherId
      ? "/api/v1/quizzes?teacherId=" + encodeURIComponent(teacherId)
      : "/api/v1/quizzes";
  },
  detail: function (quizId) {
    return "/api/v1/quizzes/" + encodeURIComponent(quizId);
  },
  remove: function (quizId) {
    return "/api/v1/quizzes/" + encodeURIComponent(quizId);
  },
  me: "/api/v1/users/me"
};

let previewQuiz = null;
let selectedQuizId = null;
let currentMode = "empty";

document.addEventListener("DOMContentLoaded", async function () {
  const generateBtn = document.getElementById("generateQuizBtn");
  const regenerateBtn = document.getElementById("regenerateQuizBtn");
  const publishBtn = document.getElementById("publishQuizBtn");
  const detailBtn = document.getElementById("detailQuizBtn");
  const deleteBtn = document.getElementById("deleteQuizBtn");
  const timeLimitInput = document.getElementById("timeLimitInput");
  const startDateTimeInput = document.getElementById("startDateTimeInput");

  currentTeacherId = await resolveTeacherId();

  if (typeof window.loadMaterialsForTeacher === "function") {
    await window.loadMaterialsForTeacher(currentTeacherId);
  }

  initializeStartDateTimeInput();
  initializeDefaultNumberInputs();

  generateBtn?.addEventListener("click", handleGeneratePreview);
  regenerateBtn?.addEventListener("click", handleGeneratePreview);
  publishBtn?.addEventListener("click", handlePublishQuiz);
  deleteBtn?.addEventListener("click", handleDeleteSelectedQuiz);

  detailBtn?.addEventListener("click", async function () {
    if (!selectedQuizId) {
      alert("상세보기 할 퀴즈를 먼저 선택해주세요.");
      return;
    }

    await showQuizDetail(selectedQuizId);
  });

  timeLimitInput?.addEventListener("input", function () {
    if (currentMode !== "detail") {
      document.getElementById("timeLimitStat").textContent = Number(timeLimitInput.value || 0);
    }

    if (currentMode === "preview" && previewQuiz) {
      const schedule = buildQuizSchedule();

      if (schedule) {
        previewQuiz.availableFrom = schedule.availableFrom;
        previewQuiz.availableUntil = schedule.availableUntil;
      }
    }
  });

  startDateTimeInput?.addEventListener("change", function () {
    if (currentMode === "preview" && previewQuiz) {
      const schedule = buildQuizSchedule();

      if (schedule) {
        previewQuiz.availableFrom = schedule.availableFrom;
        previewQuiz.availableUntil = schedule.availableUntil;
      }
    }
  });

  await loadSavedQuizList();
});

function authHeaders(extra = {}) {
  const token = localStorage.getItem("accessToken");

  return {
    ...extra,
    ...(token ? { Authorization: "Bearer " + token } : {})
  };
}

function unwrapResponse(body) {
  return body?.data || body?.result || body;
}

async function resolveTeacherId() {
  try {
    const response = await fetch(API.me, {
      headers: authHeaders()
    });

    if (response.ok) {
      const body = await response.json();
      const me = unwrapResponse(body);

      const teacherId = me.teacherId ?? me.teacher_id ?? me.userId ?? me.user_id ?? me.id;

      if (teacherId) {
        return Number(teacherId);
      }
    }
  } catch (error) {
    console.warn("교사 정보 조회 실패, 토큰에서 teacherId를 추출합니다.", error);
  }

  return getTeacherIdFromToken();
}

function getTeacherIdFromToken() {
  const token = localStorage.getItem("accessToken");

  if (!token) {
    alert("로그인이 필요합니다.");
    location.href = "/login";
    return null;
  }

  try {
    const payloadBase64Url = token.split(".")[1];
    const payloadBase64 = payloadBase64Url
      .replace(/-/g, "+")
      .replace(/_/g, "/");

    const payloadJson = decodeURIComponent(
      atob(payloadBase64)
        .split("")
        .map(function (char) {
          return "%" + ("00" + char.charCodeAt(0).toString(16)).slice(-2);
        })
        .join("")
    );

    const payload = JSON.parse(payloadJson);

    const role = payload.role || payload.authority || "";

    if (role && role !== "TEACHER" && role !== "ROLE_TEACHER" && role !== "ADMIN" && role !== "ROLE_ADMIN") {
      console.warn("교사 권한 토큰이 아닐 수 있습니다.", payload);
    }

    return Number(payload.sub);
  } catch (error) {
    console.error(error);
    alert("로그인 정보가 올바르지 않습니다. 다시 로그인해주세요.");
    location.href = "/login";
    return null;
  }
}

function initializeDefaultNumberInputs() {
  const questionCountInput = document.getElementById("questionCountInput");
  const timeLimitInput = document.getElementById("timeLimitInput");

  if (questionCountInput && !questionCountInput.value) {
    questionCountInput.value = 3;
  }

  if (timeLimitInput && !timeLimitInput.value) {
    timeLimitInput.value = 15;
  }

  document.getElementById("timeLimitStat").textContent = Number(timeLimitInput?.value || 15);
}

async function handleGeneratePreview() {
  const generateBtn = document.getElementById("generateQuizBtn");

  try {
    if (generateBtn) {
      generateBtn.disabled = true;
    }

    const requestBody = createQuizRequestBody();

    if (!requestBody) {
      return;
    }

    const response = await fetch(API.preview, {
      method: "POST",
      headers: authHeaders({
        "Content-Type": "application/json"
      }),
      body: JSON.stringify(requestBody)
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error(errorText);
      alert("퀴즈 미리보기 생성에 실패했습니다.");
      return;
    }

    const body = await response.json();
    const quiz = unwrapResponse(body);

    previewQuiz = {
      ...requestBody,
      ...quiz,
      teacherId: currentTeacherId,
      materialId: requestBody.materialId,
      questions: quiz.questions || []
    };

    selectedQuizId = null;
    currentMode = "preview";

    renderQuizPreview(previewQuiz);
    setPublishButtonEnabled(true);
    setDetailButtonEnabled(false);
    setDeleteButtonEnabled(false);
  } catch (error) {
    console.error(error);
    alert("퀴즈 미리보기 생성 중 오류가 발생했습니다.");
  } finally {
    if (generateBtn) {
      generateBtn.disabled = false;
    }
  }
}

async function handlePublishQuiz() {
  if (!previewQuiz) {
    alert("먼저 퀴즈를 생성해주세요.");
    return;
  }

  const publishBtn = document.getElementById("publishQuizBtn");

  try {
    if (publishBtn) {
      publishBtn.disabled = true;
    }

    const finalQuiz = collectEditedPreviewQuiz();

    if (!finalQuiz) {
      setPublishButtonEnabled(true);
      return;
    }

    if (!finalQuiz.title.trim()) {
      alert("퀴즈 이름을 입력해주세요.");
      setPublishButtonEnabled(true);
      return;
    }

    const response = await fetch(API.publish, {
      method: "POST",
      headers: authHeaders({
        "Content-Type": "application/json"
      }),
      body: JSON.stringify(finalQuiz)
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error(errorText);
      alert("학생 배포에 실패했습니다.");
      setPublishButtonEnabled(true);
      return;
    }

    const body = await safeJson(response);
    const savedQuiz = unwrapResponse(body);

    alert("학생 배포가 완료되었습니다.");

    previewQuiz = null;
    currentMode = "detail";

    await loadSavedQuizList(savedQuiz?.quizId);

    if (savedQuiz?.quizId) {
      selectedQuizId = savedQuiz.quizId;
      await showQuizDetail(savedQuiz.quizId);
      setDeleteButtonEnabled(true);
    } else {
      clearPreviewAreaAfterPublish();
      setDeleteButtonEnabled(false);
    }
  } catch (error) {
    console.error(error);
    alert("학생 배포 중 오류가 발생했습니다.");
    setPublishButtonEnabled(true);
  }
}

async function handleDeleteSelectedQuiz() {
  if (!selectedQuizId) {
    alert("삭제할 퀴즈를 먼저 선택해주세요.");
    return;
  }

  const confirmed = confirm("선택한 퀴즈를 삭제하시겠습니까? 삭제 후에는 되돌릴 수 없습니다.");

  if (!confirmed) {
    return;
  }

  const deleteBtn = document.getElementById("deleteQuizBtn");

  try {
    if (deleteBtn) {
      deleteBtn.disabled = true;
    }

    const response = await fetch(API.remove(selectedQuizId), {
      method: "DELETE",
      headers: authHeaders()
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error(errorText);
      alert("퀴즈 삭제에 실패했습니다.");
      setDeleteButtonEnabled(true);
      return;
    }

    alert("퀴즈가 삭제되었습니다.");

    selectedQuizId = null;
    previewQuiz = null;
    currentMode = "empty";

    setDetailButtonEnabled(false);
    setDeleteButtonEnabled(false);
    setPublishButtonEnabled(false);

    clearPreviewAreaAfterDelete();
    await loadSavedQuizList();
  } catch (error) {
    console.error(error);
    alert("퀴즈 삭제 중 오류가 발생했습니다.");
    setDeleteButtonEnabled(true);
  }
}

async function loadSavedQuizList(selectedAfterLoad = null) {
  const listContainer = document.getElementById("savedQuizList");

  try {
    const response = await fetch(API.list(currentTeacherId), {
      headers: authHeaders()
    });

    if (!response.ok) {
      if (listContainer) {
        listContainer.innerHTML = `
          <div class="saved-quiz-empty">
            퀴즈 목록을 불러오지 못했습니다.
          </div>
        `;
      }
      return;
    }

    const body = await response.json();
    const data = unwrapResponse(body);

    const quizzes = Array.isArray(data)
      ? data
      : (data.quizzes || []);

    renderSavedQuizList(quizzes);

    if (selectedAfterLoad) {
      selectQuizInList(selectedAfterLoad);
    }
  } catch (error) {
    console.error(error);

    if (listContainer) {
      listContainer.innerHTML = `
        <div class="saved-quiz-empty">
          퀴즈 목록을 불러오는 중 오류가 발생했습니다.
        </div>
      `;
    }
  }
}

function renderSavedQuizList(quizzes) {
  const listContainer = document.getElementById("savedQuizList");

  if (!listContainer) {
    return;
  }

  if (!quizzes.length) {
    listContainer.innerHTML = `
      <div class="saved-quiz-empty">
        아직 배포된 퀴즈가 없습니다.
      </div>
    `;

    selectedQuizId = null;
    setDetailButtonEnabled(false);
    setDeleteButtonEnabled(false);
    return;
  }

  listContainer.innerHTML = quizzes.map(quiz => {
    const questionCount = quiz.questionCount ?? quiz.questions?.length ?? 0;
    const difficulty = quiz.difficulty || "-";
    const createdAt = formatDateTime(quiz.createdAt);
    const availableFrom = formatDateTime(quiz.availableFrom);
    const availableUntil = formatDateTime(quiz.availableUntil);

    return `
      <div class="saved-quiz-item" data-quiz-id="${quiz.quizId}">
        <div class="saved-quiz-title">${escapeHtml(quiz.title)}</div>
        <div class="saved-quiz-meta">
          ${escapeHtml(difficulty)} · ${questionCount}문항
        </div>
        <div class="saved-quiz-meta">
          시작 ${availableFrom || "-"} · 종료 ${availableUntil || "-"}
        </div>
        <div class="saved-quiz-meta">
          생성 ${createdAt || "-"}
        </div>
        <span class="badge-published">배포됨</span>
      </div>
    `;
  }).join("");

  document.querySelectorAll(".saved-quiz-item").forEach(item => {
    item.addEventListener("click", () => {
      const quizId = Number(item.dataset.quizId);

      selectedQuizId = quizId;
      selectQuizInList(quizId);
      setDetailButtonEnabled(true);
      setDeleteButtonEnabled(true);
    });
  });
}

function selectQuizInList(quizId) {
  document.querySelectorAll(".saved-quiz-item").forEach(item => {
    const itemQuizId = Number(item.dataset.quizId);
    item.classList.toggle("selected", itemQuizId === Number(quizId));
  });

  selectedQuizId = Number(quizId);
  setDetailButtonEnabled(true);
  setDeleteButtonEnabled(true);
}

async function showQuizDetail(quizId) {
  try {
    const response = await fetch(API.detail(quizId), {
      headers: authHeaders()
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error(errorText);
      alert("퀴즈 상세 정보를 불러오지 못했습니다.");
      return;
    }

    const body = await response.json();
    const quiz = unwrapResponse(body);

    currentMode = "detail";
    previewQuiz = null;

    renderQuizDetail(quiz);
    setPublishButtonEnabled(false);
    setDeleteButtonEnabled(true);
  } catch (error) {
    console.error(error);
    alert("퀴즈 상세보기 중 오류가 발생했습니다.");
  }
}

function createQuizRequestBody() {
  if (!currentTeacherId) {
    alert("교사 정보를 불러오지 못했습니다. 다시 로그인해주세요.");
    return null;
  }

  const selectedMaterial = typeof window.getSelectedMaterial === "function"
    ? window.getSelectedMaterial()
    : null;

  const materialId = typeof window.getSelectedMaterialId === "function"
    ? window.getSelectedMaterialId()
    : null;

  const materialTotalPages = typeof window.getSelectedMaterialTotalPages === "function"
    ? window.getSelectedMaterialTotalPages()
    : 0;

  if (!selectedMaterial || !materialId) {
    alert("강의자료를 선택해주세요.");
    return null;
  }

  if (typeof window.validatePageRange === "function" && !window.validatePageRange()) {
    return null;
  }

  const difficulty = document.getElementById("difficultySelect").value;
  const questionCount = getQuestionCount();
  const unitValue = document.getElementById("unitSelect").value;
  const [startPage, endPage] = unitValue.split("-").map(Number);
  const schedule = buildQuizSchedule();

  if (!questionCount) {
    return null;
  }

  if (!schedule) {
    return null;
  }

  if (materialTotalPages > 0 && endPage > materialTotalPages) {
    alert("선택한 강의자료는 총 " + materialTotalPages + "쪽입니다. 끝 쪽을 다시 입력해주세요.");
    return null;
  }

  const subject = getMaterialSubject(selectedMaterial);
  const fileName = getMaterialFileName(selectedMaterial);

  return {
    teacherId: currentTeacherId,
    materialId: Number(materialId),
    title: subject ? subject + " 퀴즈" : fileName + " 퀴즈",
    difficulty,
    startPage,
    endPage,
    questionCount,
    availableFrom: schedule.availableFrom,
    availableUntil: schedule.availableUntil
  };
}

function getQuestionCount() {
  const input = document.getElementById("questionCountInput");
  const value = Number(input?.value);

  if (!Number.isInteger(value) || value < 1) {
    alert("문제 수는 1 이상의 숫자로 입력해주세요.");
    return null;
  }

  if (value > 50) {
    alert("문제 수는 최대 50문항까지 입력할 수 있습니다.");
    return null;
  }

  return value;
}

function collectEditedPreviewQuiz() {
  const cards = document.querySelectorAll(".preview-question-card");
  const titleInput = document.getElementById("previewQuizTitleInput");
  const schedule = buildQuizSchedule();

  if (!schedule) {
    return null;
  }

  const quizTitle = titleInput?.value.trim() || previewQuiz.title || "제목 없는 퀴즈";

  const questions = Array.from(cards).map((card, index) => {
    const questionText = card.querySelector(".question-text-input")?.value.trim() || "";
    const explanation = card.querySelector(".explanation-input")?.value.trim() || "";
    const answerText = card.querySelector(".answer-select")?.value || "";
    const optionInputs = card.querySelectorAll(".option-text-input");

    const options = Array.from(optionInputs).map((input, optionIndex) => {
      return {
        number: Number(input.dataset.optionNumber || optionIndex + 1),
        text: input.value.trim()
      };
    });

    return {
      questionType: card.dataset.questionType || "MULTIPLE_CHOICE",
      questionText,
      options: JSON.stringify(options),
      answerText,
      explanation,
      questionOrder: index + 1
    };
  });

  return {
    ...previewQuiz,
    teacherId: currentTeacherId,
    materialId: previewQuiz.materialId,
    title: quizTitle,
    availableFrom: schedule.availableFrom,
    availableUntil: schedule.availableUntil,
    questions
  };
}

function renderQuizPreview(quiz) {
  const questions = quiz.questions || [];
  const container = document.getElementById("quizPreviewContainer");
  const scheduleText = createScheduleText(quiz.availableFrom, quiz.availableUntil);

  document.getElementById("previewTitleText").textContent = "생성된 퀴즈 미리보기";
  document.getElementById("previewCountText").textContent =
    `총 ${questions.length}문항이 생성되었습니다. ${scheduleText}`;

  updateStats(questions);

  if (!container) {
    return;
  }

  if (!questions.length) {
    container.innerHTML = `
      <div class="question-card">
        <div class="q-text">생성된 문제가 없습니다.</div>
      </div>
    `;
    return;
  }

  const quizTitleEditHtml = `
    <div class="question-card">
      <div class="q-col-title">퀴즈 이름</div>
      <input
        id="previewQuizTitleInput"
        type="text"
        value="${escapeAttribute(quiz.title || "제목 없는 퀴즈")}"
        style="width:100%; height:38px; border:1px solid #ddd; border-radius:8px; padding:0 10px; font-size:14px;"
      >
      <div class="q-col-desc">
        ${escapeHtml(scheduleText)}
      </div>
    </div>
  `;

  const questionCardsHtml = questions.map((question, index) => {
    const options = parseOptions(question.options);

    const optionInputsHtml = options.map(option => {
      return `
        <label class="choice" style="display:flex; align-items:center; gap:6px;">
          <span>${option.number}.</span>
          <input
            class="option-text-input"
            data-question-index="${index}"
            data-option-number="${option.number}"
            value="${escapeAttribute(option.text)}"
            style="width:130px; height:30px; border:1px solid #ddd; border-radius:6px; padding:0 8px; font-size:13px;"
          >
        </label>
      `;
    }).join("");

    const answerOptionsHtml = options.map(option => {
      const selected = String(option.number) === String(question.answerText) ? "selected" : "";

      return `
        <option value="${option.number}" ${selected}>
          ${option.number}. ${escapeHtml(option.text)}
        </option>
      `;
    }).join("");

    return `
      <div class="question-card preview-question-card"
           data-question-index="${index}"
           data-question-type="${escapeAttribute(question.questionType || "MULTIPLE_CHOICE")}">
        <div class="q-header">
          <span class="q-num">${index + 1}</span>
          <div style="flex:1">
            <span class="q-badge">${formatQuestionType(question.questionType)}</span>
            <textarea
              class="question-text-input"
              style="width:100%; min-height:42px; border:1px solid #ddd; border-radius:8px; padding:8px 10px; font-size:14px; resize:vertical;"
            >${escapeHtml(question.questionText)}</textarea>
          </div>
        </div>

        <div class="q-choices">
          ${optionInputsHtml}
        </div>

        <div class="q-cols">
          <div>
            <div class="q-col-title">정답</div>
            <select
              class="answer-select"
              data-question-index="${index}"
              style="height:32px; border:1px solid #ddd; border-radius:6px; padding:0 8px;"
            >
              ${answerOptionsHtml}
            </select>
          </div>

          <div>
            <div class="q-col-title">해설</div>
            <textarea
              class="explanation-input"
              style="width:100%; min-height:60px; border:1px solid #ddd; border-radius:8px; padding:8px 10px; font-size:12px; resize:vertical;"
            >${escapeHtml(question.explanation || "")}</textarea>
          </div>

          <div>
            <div class="q-col-title">피드백</div>
            <div class="q-col-desc">학생이 문제를 푼 뒤 결과를 확인할 수 있습니다.</div>
          </div>
        </div>
      </div>
    `;
  }).join("");

  container.innerHTML = quizTitleEditHtml + questionCardsHtml;

  bindOptionInputEvents();
}

function bindOptionInputEvents() {
  const optionInputs = document.querySelectorAll(".option-text-input");

  optionInputs.forEach(input => {
    input.addEventListener("input", () => {
      const questionIndex = input.dataset.questionIndex;
      updateAnswerSelectOptions(questionIndex);
    });
  });
}

function updateAnswerSelectOptions(questionIndex) {
  const questionCard = document.querySelector(
    `.preview-question-card[data-question-index="${questionIndex}"]`
  );

  if (!questionCard) {
    return;
  }

  const optionInputs = questionCard.querySelectorAll(".option-text-input");
  const answerSelect = questionCard.querySelector(".answer-select");

  if (!answerSelect) {
    return;
  }

  const currentAnswer = answerSelect.value;

  answerSelect.innerHTML = Array.from(optionInputs).map(input => {
    const optionNumber = input.dataset.optionNumber;
    const optionText = input.value.trim();
    const selected = String(optionNumber) === String(currentAnswer) ? "selected" : "";

    return `
      <option value="${optionNumber}" ${selected}>
        ${optionNumber}. ${escapeHtml(optionText)}
      </option>
    `;
  }).join("");
}

function renderQuizDetail(quiz) {
  const questions = quiz.questions || [];
  const container = document.getElementById("quizPreviewContainer");
  const scheduleText = createScheduleText(quiz.availableFrom, quiz.availableUntil);

  document.getElementById("previewTitleText").textContent = "퀴즈 상세보기";
  document.getElementById("previewCountText").textContent =
    `${quiz.title || "선택한 퀴즈"} · 총 ${questions.length}문항 · ${scheduleText}`;

  updateStats(questions, getTimeLimitFromDates(quiz.availableFrom, quiz.availableUntil));

  if (!container) {
    return;
  }

  if (!questions.length) {
    container.innerHTML = `
      <div class="question-card">
        <div class="q-text">표시할 문제가 없습니다.</div>
      </div>
    `;
    return;
  }

  container.innerHTML = questions.map((question, index) => {
    const options = parseOptions(question.options);

    const choicesHtml = options.map(option => {
      const isCorrect = String(option.number) === String(question.answerText);

      return `
        <span class="choice ${isCorrect ? "correct" : ""}">
          ${option.number}. ${escapeHtml(option.text)}
        </span>
      `;
    }).join("");

    return `
      <div class="question-card">
        <div class="q-header">
          <span class="q-num">${index + 1}</span>
          <div style="flex:1">
            <span class="q-badge">${formatQuestionType(question.questionType)}</span>
            <span class="q-text">${escapeHtml(question.questionText)}</span>
          </div>
        </div>

        <div class="q-choices">
          ${choicesHtml}
        </div>

        <div class="q-cols">
          <div>
            <div class="q-col-title">정답</div>
            <div class="q-col-val correct">${escapeHtml(formatAnswer(question))}</div>
          </div>

          <div>
            <div class="q-col-title">해설</div>
            <div class="q-col-desc">${escapeHtml(question.explanation || "")}</div>
          </div>

          <div>
            <div class="q-col-title">일정</div>
            <div class="q-col-desc">${escapeHtml(scheduleText)}</div>
          </div>
        </div>
      </div>
    `;
  }).join("");
}

function updateStats(questions, timeLimitOverride = null) {
  const totalCount = questions.length;
  const multipleChoiceCount = questions.filter(question =>
    !question.questionType || question.questionType === "MULTIPLE_CHOICE"
  ).length;

  const ratio = totalCount === 0
    ? 0
    : Math.round((multipleChoiceCount / totalCount) * 100);

  const timeLimit = timeLimitOverride ?? Number(document.getElementById("timeLimitInput")?.value || 15);

  document.getElementById("totalQuestionCount").textContent = totalCount;
  document.getElementById("timeLimitStat").textContent = timeLimit || 0;
  document.getElementById("multipleChoiceRatio").textContent = `${ratio}%`;
  document.getElementById("multipleChoiceCount").textContent = `(${multipleChoiceCount}/${totalCount})`;
}

function clearPreviewAreaAfterPublish() {
  document.getElementById("previewTitleText").textContent = "퀴즈 상세보기";
  document.getElementById("previewCountText").textContent = "배포된 퀴즈를 목록에서 선택해주세요.";

  document.getElementById("quizPreviewContainer").innerHTML = `
    <div class="question-card">
      <div class="q-text">오른쪽 목록에서 퀴즈를 선택한 뒤 상세 보기를 눌러주세요.</div>
    </div>
  `;

  updateStats([]);
}

function clearPreviewAreaAfterDelete() {
  document.getElementById("previewTitleText").textContent = "생성된 퀴즈 미리보기";
  document.getElementById("previewCountText").textContent = "퀴즈 생성 전입니다.";

  document.getElementById("quizPreviewContainer").innerHTML = `
    <div class="question-card">
      <div class="q-text">퀴즈 생성 버튼을 누르면 문제가 표시됩니다.</div>
    </div>
  `;

  updateStats([]);
}

function setPublishButtonEnabled(enabled) {
  const publishBtn = document.getElementById("publishQuizBtn");

  if (publishBtn) {
    publishBtn.disabled = !enabled;
  }
}

function setDetailButtonEnabled(enabled) {
  const detailBtn = document.getElementById("detailQuizBtn");

  if (detailBtn) {
    detailBtn.disabled = !enabled;
  }
}

function setDeleteButtonEnabled(enabled) {
  const deleteBtn = document.getElementById("deleteQuizBtn");

  if (deleteBtn) {
    deleteBtn.disabled = !enabled;
  }
}

function initializeStartDateTimeInput() {
  const input = document.getElementById("startDateTimeInput");

  if (!input) {
    return;
  }

  const now = new Date();
  now.setSeconds(0, 0);

  if (!input.value) {
    input.value = toDatetimeLocalValue(now);
  }
}

function buildQuizSchedule() {
  const startInput = document.getElementById("startDateTimeInput");
  const timeLimitInput = document.getElementById("timeLimitInput");

  const startValue = startInput?.value;
  const timeLimit = Number(timeLimitInput?.value);

  if (!startValue) {
    alert("퀴즈 시작 시간을 선택해주세요.");
    return null;
  }

  const startDate = new Date(startValue);

  if (Number.isNaN(startDate.getTime())) {
    alert("퀴즈 시작 시간이 올바르지 않습니다.");
    return null;
  }

  if (!Number.isInteger(timeLimit) || timeLimit <= 0) {
    alert("시간 제한은 1분 이상의 숫자로 입력해주세요.");
    return null;
  }

  if (timeLimit > 180) {
    alert("시간 제한은 최대 180분까지 입력할 수 있습니다.");
    return null;
  }

  const endDate = new Date(startDate.getTime() + timeLimit * 60 * 1000);

  return {
    timeLimit,
    availableFrom: toLocalDateTime(startDate),
    availableUntil: toLocalDateTime(endDate)
  };
}

function createScheduleText(availableFrom, availableUntil) {
  const startText = formatDateTime(availableFrom);
  const endText = formatDateTime(availableUntil);
  const timeLimit = getTimeLimitFromDates(availableFrom, availableUntil);

  if (!startText || !endText) {
    return "시작/종료 시간이 설정되지 않았습니다.";
  }

  return `시작 ${startText} · 종료 ${endText} · 제한 ${timeLimit}분`;
}

function getTimeLimitFromDates(availableFrom, availableUntil) {
  if (!availableFrom || !availableUntil) {
    return 0;
  }

  const startDate = new Date(availableFrom);
  const endDate = new Date(availableUntil);

  if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime())) {
    return 0;
  }

  const diffMs = endDate.getTime() - startDate.getTime();
  const diffMin = Math.round(diffMs / 1000 / 60);

  return diffMin > 0 ? diffMin : 0;
}

function getMaterialSubject(material) {
  return material.subject || material.subjectName || "";
}

function getMaterialFileName(material) {
  return material.fileName ?? material.file_name ?? material.title ?? "강의자료";
}

function parseOptions(options) {
  if (Array.isArray(options)) {
    return options;
  }

  if (!options) {
    return [];
  }

  try {
    return JSON.parse(options);
  } catch (error) {
    console.error("선택지 JSON 파싱 실패:", options);
    return [];
  }
}

function formatQuestionType(questionType) {
  if (questionType === "MULTIPLE_CHOICE") {
    return "객관식";
  }

  if (questionType === "SHORT_ANSWER") {
    return "주관식";
  }

  return "문제";
}

function formatAnswer(question) {
  const options = parseOptions(question.options);
  const matchedOption = options.find(option =>
    String(option.number) === String(question.answerText)
  );

  if (matchedOption) {
    return `${matchedOption.number}. ${matchedOption.text}`;
  }

  return question.answerText || "";
}

function formatDateTime(value) {
  if (!value) {
    return "";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "";
  }

  return date.toLocaleString("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function toLocalDateTime(date) {
  const offsetDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return offsetDate.toISOString().slice(0, 19);
}

function toDatetimeLocalValue(date) {
  const offsetDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return offsetDate.toISOString().slice(0, 16);
}

async function safeJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

function escapeAttribute(value) {
  return escapeHtml(value)
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}