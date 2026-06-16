package com.example.myapp.quiz.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.myapp.quiz.dto.QuestionUpdateRequest;
import com.example.myapp.quiz.dto.QuizUpdateRequest;
import com.example.myapp.quiz.domain.Quiz;
import com.example.myapp.quiz.domain.QuizQuestion;
import com.example.myapp.quiz.dto.QuizListResponse;
import org.apache.ibatis.annotations.Param;
import com.example.myapp.quiz.domain.QuizAnswer;
import com.example.myapp.quiz.domain.QuizAttempt;
import com.example.myapp.quiz.dto.QuizAnswerResultResponse;

@Mapper
public interface QuizMapper {

    void insertQuiz(Quiz quiz);

    void insertQuizQuestion(QuizQuestion question);

    List<QuizListResponse> findQuizList();

    Quiz findQuizById(Long quizId);

    List<QuizQuestion> findQuestionsByQuizId(Long quizId);
    
    void insertQuizAttempt(QuizAttempt attempt);

    QuizAttempt findAttemptById(
            @Param("quizId") Long quizId,
            @Param("attemptId") Long attemptId
    );

    void insertQuizAnswer(QuizAnswer answer);

    int updateAttemptResult(
            @Param("attemptId") Long attemptId,
            @Param("score") int score,
            @Param("totalCount") int totalCount,
            @Param("correctCount") int correctCount
    );

    List<QuizAnswerResultResponse> findAnswerResultsByAttemptId(Long attemptId);

    int updateQuiz(
            @Param("quizId") Long quizId,
            @Param("request") QuizUpdateRequest request
    );

    int deleteQuiz(Long quizId);

    int updateQuestion(
            @Param("quizId") Long quizId,
            @Param("questionId") Long questionId,
            @Param("request") QuestionUpdateRequest request
    );
}