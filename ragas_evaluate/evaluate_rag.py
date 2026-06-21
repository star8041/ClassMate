"""
ClassMate RAG 성능 비교 평가 스크립트 (RAGAS 0.2+)

실제 ClassMate 서비스 환경과 동일하게 평가한다:
  - 시스템 프롬프트 : teacher-system.st 그대로 사용
  - 유저 프롬프트   : AgentExecutor.buildUserPrompt() 로직 동일
  - 답변 모델       : gpt-4o-mini, temperature=0.7
  - 임베딩 모델     : text-embedding-ada-002 (Spring AI 기본값)

비교 항목:
  - 검색 방식 : cosine / euclidean / mmr
  - TOP_K     : 3, 5
  - 임계값    : 0.3, 0.5, 0.7

실행 전 환경변수 설정:
  export OPENAI_API_KEY=sk-...

실행:
  python evaluate_rag.py --teacher-id 3                          # 전체 비교 실험
  python evaluate_rag.py --teacher-id 3 --material-id 1         # 특정 파일만
  python evaluate_rag.py --teacher-id 3 --mode best             # 단일 설정(MMR/5/0.5)
"""

import os
import json
import argparse
import psycopg2
import numpy as np
import pandas as pd
from openai import OpenAI
from dotenv import load_dotenv
from itertools import product

from ragas import evaluate, EvaluationDataset  # pylint: disable=no-name-in-module
from ragas.dataset_schema import SingleTurnSample  # pylint: disable=no-name-in-module
from ragas.metrics import (  # pylint: disable=no-name-in-module
    LLMContextRecall,
    Faithfulness,
    ResponseRelevancy,
    ContextPrecision,
)
from ragas.llms import LangchainLLMWrapper  # pylint: disable=no-name-in-module
from ragas.embeddings import LangchainEmbeddingsWrapper  # pylint: disable=no-name-in-module
from langchain_openai import ChatOpenAI, OpenAIEmbeddings

load_dotenv()

# ── 설정 ──────────────────────────────────────────────────────
VECTOR_DB = {
    "host": "localhost",
    "port": 5433,
    "dbname": "classmate_vector",
    "user": "classmate",
    "password": "classmate1234",
}

# 실제 서비스 모델 설정 (application.properties 와 동일)
ANSWER_MODEL   = "gpt-4o-mini"
ANSWER_TEMP    = 0.7
EMBEDDING_MODEL = "text-embedding-3-small"
JUDGE_MODEL    = "gpt-4o"

# ── 기본값 설정 (CLI 인자 생략 시 사용) ───────────────────────
DEFAULT_TEACHER_ID  = 3     # --teacher-id 기본값
DEFAULT_MATERIAL_ID = None  # --material-id 기본값 (None=전체, 숫자=특정 파일만)

# 비교 파라미터
SEARCH_METHODS    = ["cosine", "euclidean", "mmr"]
TOP_K_OPTIONS     = [3, 5]
THRESHOLD_OPTIONS = [0.3, 0.5, 0.7]
MMR_LAMBDA        = 0.6  # 관련성/다양성 균형 (1=순수관련성, 0=순수다양성)

# ── 실제 시스템 프롬프트 (teacher-system.st 그대로) ──────────
TEACHER_SYSTEM_PROMPT = """당신은 초등학교 교사를 보조하는 전문 AI 교육 어시스턴트입니다.

## 역할과 목적
- 초등학교 교사의 수업 설계, 학생 지도, 교재 분석, 퀴즈 기획을 전문적으로 지원합니다.
- 초등 교육과정과 발달 단계를 고려한 실질적인 조언을 제공합니다.
- 교육 현장의 실질적인 문제를 해결하는 데 초점을 맞춥니다.

## 응답 원칙
1. **전문적이고 간결하게** 답변합니다. 교사는 바쁘므로 핵심을 먼저 전달하세요.
2. **참고 자료가 제공된 경우**, 해당 자료를 우선 근거로 활용하세요.
3. **초등 교육과정 맥락**을 항상 고려하세요.
   - 학년군(1~2학년 / 3~4학년 / 5~6학년)별 학습 수준과 성취기준을 반영하세요.
   - 국어·수학·사회·과학·도덕 등 교과 특성에 맞는 교수법을 제안하세요.
   - 아동의 인지 발달 단계(구체적 조작기 등)를 고려한 활동 중심 수업을 권장하세요.
4. **교육학적 관점**에서 조언합니다. 단순 정보 전달을 넘어 교수법적 제안을 포함하세요.
   - 예) 협동학습, 프로젝트 수업, 놀이 연계 활동, 차시 구성 등
5. **자료 범위를 벗어난 질문**은 솔직하게 "해당 교재에서 관련 내용을 찾을 수 없습니다"라고 안내하세요.
6. **학생 개인 정보**가 포함된 질문에는 신중하게 접근하고, 교육적 목적 내에서만 활용하세요.

## 응답 형식 (반드시 준수)

절대 사용 금지 문자: #, ##, ###, **, *, -, `, > 등 모든 마크다운 기호.
이 문자들이 응답에 하나라도 포함되면 안 됩니다.

나쁜 예 (절대 이렇게 쓰지 마세요):
**학습 목표**
- 지구의 구조를 이해한다.
1. 지각

좋은 예 (반드시 이렇게 쓰세요):
학습 목표
지구의 구조를 이해한다.

항목 나열이 필요하면 숫자 없이 줄바꿈만 사용하세요.
강조는 기호 없이 문장 구조로 표현하세요. 예) "가장 중요한 점은 ~입니다."
수업 계획, 퀴즈 문항, 피드백 템플릿 등 실용적인 형식으로 제공하세요.
수업 지도안 요청 시: 학습 목표, 도입, 전개, 정리 순으로 구성하세요."""

# ── 실제 AgentExecutor.buildUserPrompt() 로직 동일 ────────────
def build_user_prompt(question: str, contexts: list) -> str:
    """
    AgentExecutor.buildUserPrompt()와 동일한 구조:
      ## 참고 자료
      {context}
      위 참고 자료에 [출처: 파일명 N페이지] 형태로 표시된 경우, ...
      ## 질문
      {question}
    """
    if contexts:
        context_text = "\n\n".join(contexts)
        return (
            f"## 참고 자료\n{context_text}\n\n"
            "위 참고 자료에 [출처: 파일명 N페이지] 형태로 표시된 경우, "
            "답변 말미에 반드시 출처를 명시하세요. 마크다운 문법은 사용하지 마세요.\n\n"
            f"## 질문\n{question}"
        )
    else:
        return (
            "참고할 교재 자료가 없습니다. 일반 교육 지식으로만 답변하고, "
            "출처나 페이지 번호를 절대 임의로 만들지 마세요.\n\n"
            f"## 질문\n{question}"
        )

# ── 테스트 데이터셋 (과학 6-1 1단원) ─────────────────────────
TEST_CASES = [
    {
        "question": "지구의 자전이란 무엇인가요?",
        "ground_truth": "지구가 자전축을 중심으로 회전하는 것을 지구의 자전이라고 합니다. 지구의 자전축은 북극과 남극을 이은 가상의 직선입니다.",
    },
    {
        "question": "지구는 어느 방향으로 자전하나요?",
        "ground_truth": "지구는 서쪽에서 동쪽(시계 반대 방향)으로 자전합니다. 북극에서 내려다볼 때 시계 반대 방향으로 자전합니다.",
    },
    {
        "question": "하루 동안 태양은 어떻게 움직이나요?",
        "ground_truth": "하루 동안 태양은 아침에 동쪽 하늘에서 떠올라 한낮에는 남쪽 하늘에서 보이고, 오후에는 서쪽 하늘로 지는 것처럼 보입니다.",
    },
    {
        "question": "낮과 밤이 생기는 까닭은 무엇인가요?",
        "ground_truth": "태양 빛을 받고 있는 지역은 낮이고, 태양 빛을 받지 못하고 있는 지역은 밤입니다. 지구가 자전하기 때문에 낮과 밤이 번갈아 나타납니다.",
    },
    {
        "question": "지구의 공전이란 무엇인가요?",
        "ground_truth": "지구는 태양을 중심으로 일 년에 한 바퀴씩 일정한 길을 따라 서쪽에서 동쪽으로 회전합니다. 이것을 지구의 공전이라고 합니다.",
    },
    {
        "question": "계절에 따라 보이는 별자리가 달라지는 까닭은 무엇인가요?",
        "ground_truth": "계절에 따라 잘 보이는 별자리와 보이지 않는 별자리가 있는 까닭은 지구의 공전 때문입니다.",
    },
    {
        "question": "달의 모양은 어떤 순서로 변하나요?",
        "ground_truth": "달의 모양은 약 30일을 주기로 초승달, 상현달, 보름달, 하현달, 그믐달의 순서로 반복적으로 나타납니다.",
    },
    {
        "question": "초승달, 상현달, 보름달은 각각 음력 며칠 무렵에 볼 수 있나요?",
        "ground_truth": "초승달은 음력 3~4일 무렵, 상현달은 음력 7~8일 무렵, 보름달은 음력 15일 무렵에 잘 볼 수 있습니다.",
    },
    {
        "question": "저녁 7시 무렵 초승달, 상현달, 보름달은 각각 하늘의 어느 방향에서 볼 수 있나요?",
        "ground_truth": "태양이 진 저녁 7시 무렵 초승달은 서쪽 하늘에서, 상현달은 남쪽 하늘에서, 보름달은 동쪽 하늘에서 볼 수 있습니다.",
    },
    {
        "question": "여러 날 동안 같은 시각에 관측할 때 달의 위치는 어떻게 달라지나요?",
        "ground_truth": "같은 시각에서 달의 위치는 매일 조금씩 서쪽에서 동쪽으로 달라지며 그 모양 또한 변합니다.",
    },
]


# ── 임베딩 ────────────────────────────────────────────────────
def get_embedding(text: str, client: OpenAI) -> np.ndarray:
    response = client.embeddings.create(model=EMBEDDING_MODEL, input=text)
    return np.array(response.data[0].embedding)


def cosine_sim(a: np.ndarray, b: np.ndarray) -> float:
    return float(np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b) + 1e-9))


# ── 후보 문서 조회 ────────────────────────────────────────────
def fetch_candidates(embedding: np.ndarray, teacher_id: int,
                     material_id: int | None = None, pool_size: int = 30) -> list:
    emb_str = "[" + ",".join(map(str, embedding.tolist())) + "]"
    conn = psycopg2.connect(**VECTOR_DB)
    try:
        with conn.cursor() as cur:
            if material_id is not None:
                cur.execute(
                    """
                    SELECT content, metadata,
                           embedding::text,
                           1 - (embedding <=> %s::vector) AS cosine_sim,
                           embedding <-> %s::vector        AS l2_dist
                    FROM vector_store
                    WHERE (metadata->>'teacherId')::bigint = %s
                      AND (metadata->>'materialId')::bigint = %s
                    ORDER BY embedding <=> %s::vector
                    LIMIT %s
                    """,
                    (emb_str, emb_str, teacher_id, material_id, emb_str, pool_size),
                )
            else:
                cur.execute(
                    """
                    SELECT content, metadata,
                           embedding::text,
                           1 - (embedding <=> %s::vector) AS cosine_sim,
                           embedding <-> %s::vector        AS l2_dist
                    FROM vector_store
                    WHERE (metadata->>'teacherId')::bigint = %s
                    ORDER BY embedding <=> %s::vector
                    LIMIT %s
                    """,
                    (emb_str, emb_str, teacher_id, emb_str, pool_size),
                )
            rows = cur.fetchall()
    finally:
        conn.close()

    results = []
    for content, metadata, emb_text, cos_sim_val, l2_dist in rows:
        meta = metadata if isinstance(metadata, dict) else json.loads(metadata)
        doc_emb = np.array(json.loads(emb_text))
        results.append({
            "content": content,
            "metadata": meta,
            "embedding": doc_emb,
            "cosine_sim": cos_sim_val,
            "l2_dist": l2_dist,
        })
    return results


# ── 검색 방식별 문서 선택 ─────────────────────────────────────
def select_by_cosine(candidates: list, top_k: int, threshold: float) -> list:
    filtered = [c for c in candidates if c["cosine_sim"] >= threshold]
    return filtered[:top_k]


def select_by_euclidean(candidates: list, top_k: int, threshold: float) -> list:
    scored = sorted(candidates, key=lambda c: c["l2_dist"])
    l2_threshold = 1.0 / threshold - 1
    filtered = [c for c in scored if c["l2_dist"] <= l2_threshold]
    return filtered[:top_k]


def select_by_mmr(candidates: list, query_emb: np.ndarray,
                  top_k: int, threshold: float, lambda_param: float = MMR_LAMBDA) -> list:
    pool = [c for c in candidates if c["cosine_sim"] >= threshold]
    if not pool:
        return []

    selected = []
    remaining = list(range(len(pool)))

    while len(selected) < top_k and remaining:
        if not selected:
            best_idx = max(remaining, key=lambda i: pool[i]["cosine_sim"])
        else:
            def mmr_score(i):
                relevance = cosine_sim(query_emb, pool[i]["embedding"])
                redundancy = max(
                    cosine_sim(pool[i]["embedding"], pool[j]["embedding"])
                    for j in selected
                )
                return lambda_param * relevance - (1 - lambda_param) * redundancy
            best_idx = max(remaining, key=mmr_score)

        selected.append(best_idx)
        remaining.remove(best_idx)

    return [pool[i] for i in selected]


def docs_to_contexts(docs: list) -> list:
    """실제 서비스와 동일한 [출처: 파일명 N페이지] 형식"""
    contexts = []
    for doc in docs:
        meta = doc["metadata"]
        file_name = meta.get("fileName", "")
        page = meta.get("pageNumber", "")
        header = f"[출처: {file_name} {page}페이지]\n" if file_name else ""
        contexts.append(f"{header}{doc['content']}")
    return contexts


# ── 답변 생성 (실제 서비스와 동일) ───────────────────────────
def generate_answer(question: str, contexts: list, client: OpenAI) -> str:
    """
    실제 AgentExecutor.executeStream() 와 동일:
    - system: teacher-system.st
    - user  : buildUserPrompt() 결과
    - model : gpt-4o-mini, temperature=0.7
    """
    response = client.chat.completions.create(
        model=ANSWER_MODEL,
        temperature=ANSWER_TEMP,
        messages=[
            {"role": "system", "content": TEACHER_SYSTEM_PROMPT},
            {"role": "user",   "content": build_user_prompt(question, contexts)},
        ],
        max_tokens=1024,
    )
    return response.choices[0].message.content


# ── 단일 설정 평가 ────────────────────────────────────────────
def evaluate_config(teacher_id: int, method: str, top_k: int, threshold: float,
                    openai_client: OpenAI, judge_llm, embeddings_model,
                    material_id: int | None = None) -> dict:
    samples = []
    total_contexts = 0

    for case in TEST_CASES:
        question    = case["question"]
        ground_truth = case["ground_truth"]

        query_emb  = get_embedding(question, openai_client)
        candidates = fetch_candidates(query_emb, teacher_id, material_id)

        if method == "cosine":
            docs = select_by_cosine(candidates, top_k, threshold)
        elif method == "euclidean":
            docs = select_by_euclidean(candidates, top_k, threshold)
        else:
            docs = select_by_mmr(candidates, query_emb, top_k, threshold)

        contexts = docs_to_contexts(docs)
        total_contexts += len(contexts)
        answer = generate_answer(question, contexts, openai_client)

        samples.append(SingleTurnSample(
            user_input=question,
            retrieved_contexts=contexts if contexts else ["관련 자료 없음"],
            response=answer,
            reference=ground_truth,
        ))

    dataset = EvaluationDataset(samples=samples)
    result  = evaluate(
        dataset=dataset,
        metrics=[
            Faithfulness(llm=judge_llm),
            ResponseRelevancy(llm=judge_llm, embeddings=embeddings_model),
            LLMContextRecall(llm=judge_llm),
            ContextPrecision(llm=judge_llm),
        ],
    )

    df = result.to_pandas()
    print(f"       [DEBUG] 실제 컬럼명: {df.columns.tolist()}")

    # RAGAS 버전마다 컬럼명이 다를 수 있어 후보명 모두 시도
    METRIC_CANDIDATES = {
        "faithfulness":       ["faithfulness"],
        "answer_relevancy":   ["answer_relevancy", "response_relevancy"],
        "context_recall":     ["context_recall", "llm_context_recall"],
        "context_precision":  ["context_precision"],
    }
    metrics = {}
    for key, candidates in METRIC_CANDIDATES.items():
        for col in candidates:
            if col in df.columns:
                metrics[key] = round(float(df[col].mean()), 4)
                break

    avg = round(np.mean(list(metrics.values())), 4)
    return {
        "method": method,
        "top_k": top_k,
        "threshold": threshold,
        "avg_contexts": round(total_contexts / len(TEST_CASES), 1),
        **metrics,
        "average": avg,
    }


def _print_metrics(result: dict):
    """조합 평가 후 4개 지표 + 평균 출력"""
    m = result
    print(f"       faithfulness      : {m.get('faithfulness', '-')}")
    print(f"       answer_relevancy  : {m.get('answer_relevancy', '-')}")
    print(f"       context_recall    : {m.get('context_recall', '-')}")
    print(f"       context_precision : {m.get('context_precision', '-')}")
    print(f"       ─────────────────────────────")
    print(f"       평균              : {m.get('average', '-')}  (검색수: {m.get('avg_contexts')}개)\n")


# ── 전체 비교 실험 ────────────────────────────────────────────
def run_comparison(teacher_id: int, material_id: int | None = None):
    api_key = os.environ["OPENAI_API_KEY"]
    openai_client   = OpenAI(api_key=api_key)
    judge_llm       = LangchainLLMWrapper(
        ChatOpenAI(model=JUDGE_MODEL, api_key=api_key, max_retries=6, request_timeout=60)
    )
    embeddings_model = LangchainEmbeddingsWrapper(
        OpenAIEmbeddings(model=EMBEDDING_MODEL, api_key=api_key)
    )

    total = len(SEARCH_METHODS) * len(TOP_K_OPTIONS) * len(THRESHOLD_OPTIONS)
    scope = f"materialId={material_id}" if material_id else f"teacherId={teacher_id} 전체"
    print(f"\n{'='*60}")
    print(f"ClassMate RAG 비교 실험 ({scope})")
    print(f"답변 모델: {ANSWER_MODEL} (temperature={ANSWER_TEMP})")
    print(f"총 {total}가지 조합 × {len(TEST_CASES)}개 질문")
    print(f"{'='*60}\n")

    all_results = []
    for idx, (method, top_k, threshold) in enumerate(
        product(SEARCH_METHODS, TOP_K_OPTIONS, THRESHOLD_OPTIONS), 1
    ):
        print(f"[{idx}/{total}] method={method}  top_k={top_k}  threshold={threshold}")
        try:
            result = evaluate_config(
                teacher_id, method, top_k, threshold,
                openai_client, judge_llm, embeddings_model, material_id
            )
            all_results.append(result)
            _print_metrics(result)
        except Exception as e:
            print(f"       → 실패: {e}\n")

    if not all_results:
        print("결과 없음")
        return

    df = pd.DataFrame(all_results).sort_values("average", ascending=False)
    # 컬럼 순서 정리
    col_order = ["method", "top_k", "threshold", "avg_contexts",
                 "faithfulness", "answer_relevancy", "context_recall", "context_precision", "average"]
    df = df.reindex(columns=[c for c in col_order if c in df.columns])

    print(f"\n{'='*60}")
    print("전체 결과 (평균 점수 내림차순)")
    print(f"{'='*60}")
    print(df.to_string(index=False))

    best = df.iloc[0]
    print(f"\n{'─'*60}")
    print("최적 설정")
    print(f"{'─'*60}")
    print(f"  검색 방식  : {best['method']}")
    print(f"  TOP_K      : {best['top_k']}")
    print(f"  임계값     : {best['threshold']}")
    print(f"  평균 점수  : {best['average']}")

    out = f"ragas_comparison_teacher{teacher_id}.csv"
    df.to_csv(out, index=False, encoding="utf-8-sig")
    print(f"\n결과 저장: {out}")


# ── 단일 설정 실행 ────────────────────────────────────────────
def run_best(teacher_id: int, material_id: int | None = None):
    api_key = os.environ["OPENAI_API_KEY"]
    openai_client   = OpenAI(api_key=api_key)
    judge_llm       = LangchainLLMWrapper(
        ChatOpenAI(model=JUDGE_MODEL, api_key=api_key, max_retries=6, request_timeout=60)
    )
    embeddings_model = LangchainEmbeddingsWrapper(
        OpenAIEmbeddings(model=EMBEDDING_MODEL, api_key=api_key)
    )

    scope = f"materialId={material_id}" if material_id else f"teacherId={teacher_id} 전체"
    print(f"\nMMR / top_k=5 / threshold=0.5 / {scope}\n")
    result = evaluate_config(
        teacher_id, "mmr", 5, 0.5,
        openai_client, judge_llm, embeddings_model, material_id
    )
    _print_metrics(result)


# ── 진입점 ────────────────────────────────────────────────────
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="ClassMate RAG 평가")
    parser.add_argument("--teacher-id",  type=int, default=DEFAULT_TEACHER_ID)
    parser.add_argument("--material-id", type=int, default=DEFAULT_MATERIAL_ID,
                        help="특정 파일만 평가 (미지정 시 교사 전체 파일)")
    parser.add_argument("--mode", choices=["compare", "best"], default="compare")
    args = parser.parse_args()

    if args.mode == "compare":
        run_comparison(args.teacher_id, args.material_id)
    else:
        run_best(args.teacher_id, args.material_id)
