package com.perevodchik.repository

import com.perevodchik.domain.Sentence
import com.perevodchik.domain.SentenceComment
import com.perevodchik.domain.SentenceCommentShort
import com.perevodchik.domain.SentenceFull

interface SentenceService {
    fun test(id: Int)
    fun getSentencesByOrder(id: Int): List<SentenceFull>
    fun create(sentence: Sentence): Sentence
    fun sentenceCommentById(sentenceCommentId: Int): SentenceComment?
    fun createSentenceComment(userId: Int, sentenceComment: SentenceCommentShort): SentenceComment?
    fun getComments(sentenceId: Int): List<SentenceComment>
}