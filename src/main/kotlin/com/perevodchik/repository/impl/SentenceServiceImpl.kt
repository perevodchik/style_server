package com.perevodchik.repository.impl

import com.perevodchik.domain.Sentence
import com.perevodchik.domain.SentenceComment
import com.perevodchik.domain.SentenceCommentShort
import com.perevodchik.domain.SentenceFull
import com.perevodchik.repository.SentenceService
import com.perevodchik.utils.DateTimeUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentenceServiceImpl: SentenceService {

    @Inject
    lateinit var pool: io.reactiverse.reactivex.pgclient.PgPool

    override fun test(id: Int) {
        val r = pool.rxQuery("SELECT json_build_object('id', o.id, 'name', o.name, 'sentences', (SELECT json_agg(json_build_object('id', s.id, 'message', s.message)) FROM sentences s WHERE o.id = s.order_id)) json FROM orders o;").blockingGet()
        val i = r.iterator()
        println("begin...")
        while(i.hasNext()) {
            val row = i.next()
            val j = row.getJson("json")
            println(j.value())
        }
        println("end...")
    }

    override fun getSentencesByOrder(id: Int): List<SentenceFull> {
        val sentences = mutableListOf<SentenceFull>()
        val r = pool.rxQuery("SELECT sentences.id, sentences.order_id, sentences.master_id, sentences.price, sentences.message, users.name as master_name, users.surname as master_surname, users.avatar as master_avatar, COUNT(COALESCE(sentence_comments.id)) as comments_count, sentences.created_at FROM sentences JOIN users ON users.id = sentences.master_id LEFT JOIN sentence_comments ON sentences.id = sentence_comments.sentence_id WHERE sentences.order_id = $id GROUP BY sentences.id, users.name, users.surname, users.avatar ORDER BY id DESC;").blockingGet()
        val i = r.iterator()
        while(i.hasNext()) {
            val row = i.next()
            val sentence = SentenceFull(
                    id = row.getInteger("id"),
                    orderId = row.getInteger("order_id"),
                    masterId = row.getInteger("master_id"),
                    price = row.getInteger("price"),
                    commentsCount = row.getInteger("comments_count"),
                    message = row.getString("message"),
                    masterName = row.getString("master_name"),
                    masterSurname = row.getString("master_surname"),
                    masterAvatar = row.getString("master_avatar"),
                    createdAt = row.getString("created_at")
            )

            sentences.add(sentence)
        }
        return sentences
    }

    override fun create(sentence: Sentence): Sentence {
        val r = pool.rxQuery("INSERT INTO sentences (order_id, master_id, price, message, created_at) VALUES (${sentence.orderId}, ${sentence.masterId}, ${sentence.price}, '${sentence.message}', '${DateTimeUtil.timestamp()}') RETURNING sentences.id;").blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val row = i.next()
            sentence.id = row.getInteger("id")
            return sentence
        }
        return sentence
    }

    override fun sentenceCommentById(sentenceCommentId: Int): SentenceComment? {
        val r = pool.rxQuery("SELECT c.id, c.sentence_id, c.user_id, u.name, u.surname, u.avatar, c.message, c.created_at FROM sentence_comments c INNER JOIN users u ON u.id = c.user_id WHERE c.id = $sentenceCommentId;").blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val row = i.next()
            return SentenceComment(
                    id = row.getInteger("id"),
                    sentenceId = row.getInteger("sentence_id"),
                    userId = row.getInteger("user_id"),
                    userName = row.getString("name"),
                    userSurname = row.getString("surname"),
                    userAvatar = row.getString("avatar"),
                    message = row.getString("message"),
                    createAt = row.getString("created_at")
            )
        }
        return null
    }

    override fun createSentenceComment(userId: Int, sentenceComment: SentenceCommentShort): SentenceComment? {
        val r = pool.rxQuery("INSERT INTO sentence_comments (sentence_id, user_id, message, created_at) VALUES (${sentenceComment.sentenceId}, $userId, '${sentenceComment.message}', '${DateTimeUtil.timestamp()}') RETURNING sentence_comments.id;").blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val row = i.next()
            return sentenceCommentById(row.getInteger("id"))
        }
        return null
    }

    override fun getComments(sentenceId: Int): List<SentenceComment> {
        val comments = mutableListOf<SentenceComment>()
        val r = pool.rxQuery("SELECT c.id, c.sentence_id, c.user_id, u.name, u.surname, u.avatar, c.message, c.created_at FROM sentence_comments c INNER JOIN users u ON u.id = c.user_id WHERE c.sentence_id = $sentenceId;").blockingGet()
        val i = r.iterator()
        while(i.hasNext()) {
            val row = i.next()
            val comment = SentenceComment(
                    id = row.getInteger("id"),
                    sentenceId = row.getInteger("sentence_id"),
                    userId = row.getInteger("user_id"),
                    userName = row.getString("name"),
                    userSurname = row.getString("surname"),
                    userAvatar = row.getString("avatar"),
                    message = row.getString("message"),
                    createAt = row.getString("created_at")
            )
            comments.add(comment)
        }
        return comments
    }
}