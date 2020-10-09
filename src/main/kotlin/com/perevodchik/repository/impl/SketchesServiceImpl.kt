package com.perevodchik.repository.impl

import com.perevodchik.domain.*
import com.perevodchik.repository.SketchesService
import com.perevodchik.utils.FileUtils
import io.micronaut.http.multipart.StreamingFileUpload
import javax.inject.Inject

class SketchesServiceImpl: SketchesService {
    @Inject
    lateinit var pool: io.reactiverse.reactivex.pgclient.PgPool

    override fun getAllSketches(): List<SketchPreview> {
        val r = pool.rxQuery("SELECT sketches.id, sketches.owner_id, sketches.price, string_agg(distinct sketch_photos.name, ',') as photos FROM sketches LEFT JOIN sketch_photos ON sketches.id = sketch_photos.sketch_id GROUP BY sketches.id ORDER BY id DESC;").blockingGet()
        val i = r.iterator()
        val sketches = mutableListOf<SketchPreview>()
        while(i.hasNext()) {
            val row = i.next()
            val sketchPreview = SketchPreview(
                    row.getInteger("id"),
                    row.getInteger("owner_id"),
                    row.getInteger("price"),
                    row.getString("photos") ?: ""
            )
            sketches.add(sketchPreview)
        }
        return sketches
    }

    override fun getList(userId: Int, page: Int, limit: Int, min: Int, max: Int, tags: String, isFavorite:Boolean): List<SketchPreview> {
        val filters = mutableListOf<String>()
        if(min > 0)
            filters.add("s.price >= $min")
        if(max > 0)
            filters.add("s.price <= $max")
        if(isFavorite)
            filters.add("(SELECT COUNT(sketch_id) FROM user_favorite_sketches f WHERE f.sketch_id = s.id AND user_id = $userId) > 0")
        if(tags.isNotEmpty() && tags.isNotBlank())
            filters.add("ARRAY[$tags] && (SELECT STRING_TO_ARRAY(tags, ','))")

        println("tags is $tags")

        val q = "SELECT s.id, s.owner_id, s.price, " +
                "string_agg(distinct sketch_photos.name, ',') as photos " +
                "FROM sketches s " +
                "LEFT JOIN sketch_photos ON s.id = sketch_photos.sketch_id " +
                (if(filters.isNotEmpty()) "WHERE ${filters.joinToString(" AND ")} " else "") +
                "GROUP BY s.id ORDER BY id DESC  OFFSET $page LIMIT $limit;"
        print(q)
        val r = pool.rxQuery(q).blockingGet()
        val i = r.iterator()
        val sketches = mutableListOf<SketchPreview>()
        while(i.hasNext()) {
            val row = i.next()
            val sketchPreview = SketchPreview(
                    row.getInteger("id"),
                    row.getInteger("owner_id"),
                    row.getInteger("price"),
                    row.getString("photos") ?: ""
            )
            sketches.add(sketchPreview)
        }
        return sketches
    }

    override fun getSketchesByMaster(id: Int): List<SketchPreview> {
        val r = pool.rxQuery("SELECT sketches.id, sketches.owner_id, sketches.price, string_agg(distinct sketch_photos.name, ',') as photos FROM sketches LEFT JOIN sketch_photos ON sketches.id = sketch_photos.sketch_id WHERE owner_id = $id GROUP BY sketches.id ORDER BY id DESC;").blockingGet()
        val i = r.iterator()
        val sketches = mutableListOf<SketchPreview>()
        while(i.hasNext()) {
            val row = i.next()
            val sketchPreview = SketchPreview(
                    row.getInteger("id"),
                    row.getInteger("owner_id"),
                    row.getInteger("price"),
                    row.getString("photos") ?: ""
            )
            sketches.add(sketchPreview)
        }
        return sketches
    }

    override fun getSketchById(id: Int): SketchFull? {
        val r = pool.rxQuery("SELECT " +
                "sketches.id, sketches.owner_id, users.name, users.surname, users.avatar, sketches.tags, " +
                "sketches.description, sketches.price, sketches.time, " +
                "sketches.height, sketches.width, sketches.height, sketches.is_colored, " +
                "string_agg(sketch_photos.name, ',') as photos, " +
                "sketches.style_id, styles.name as style_name, sketches.position_id, " +
                "positions.name as position_name " +
                "FROM sketches " +
                "LEFT JOIN styles ON sketches.style_id = styles.id " +
                "LEFT JOIN positions ON sketches.position_id = positions.id " +
                "LEFT JOIN sketch_photos ON sketches.id = sketch_photos.sketch_id " +
                "INNER JOIN users ON sketches.owner_id = users.id " +
                "WHERE sketches.id = $id " +
                "GROUP BY sketches.id, styles.name, positions.name, users.name, users.surname, users.avatar;").blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val row = i.next()
            return SketchFull(
                    row.getInteger("id"),
                    row.getInteger("owner_id"),
                    row.getInteger("price"),
                    row.getInteger("time"),
                    row.getInteger("width"),
                    row.getInteger("height"),
                    "${row.getString("name")} ${row.getString("surname") }",
                    row.getString("avatar"),
                    row.getString("tags"),
                    row.getString("description"),
                    row.getString("photos") ?: "",
                    row.getBoolean("is_colored") ?: true,
                    false,
                    Position(row.getInteger("position_id"), row.getString("position_name")),
                    Style(row.getInteger("style_id"), row.getString("style_name"))
            )
        }
        return null
    }

    override fun createSketch(sketch: Sketch): Sketch {
        val r = pool.rxQuery("INSERT INTO sketches (owner_id, position_id, style_id, price, time, width, height, tags, description, is_colored) VALUES (${sketch.ownerId}, ${sketch.positionId}, ${sketch.styleId}, ${sketch.price}, ${sketch.time}, ${sketch.width}, ${sketch.height}, '${sketch.tags}', '${sketch.description}', ${sketch.isColored}) RETURNING sketches.id;").blockingGet()
        val i = r.iterator()
        if(i.hasNext())
            sketch.id = i.next().getInteger("id")
        println("$sketch")
        return sketch
    }

    override fun updateSketch(sketch: Sketch): Sketch {
        pool.rxQuery("UPDATE sketches SET position_id = ${sketch.positionId}, style_id = ${sketch.styleId}, price = ${sketch.price}, time = ${sketch.time}, tags = '${sketch.tags}', description = '${sketch.description}' WHERE id = ${sketch.id};").blockingGet()
        return sketch
    }

    override fun createSketchImage(name: String, sketchId: Int): Boolean {
        val r = pool.rxQuery("INSERT INTO sketch_photos (name, sketch_id) VALUES ('$name', $sketchId);").blockingGet()
        return r.rowCount() > 0
    }

    override fun upload(sketchId: Int, fileName: String, upload: StreamingFileUpload): String {
        try {
            val uploadResult = FileUtils().uploadFile(upload, fileName)
            if(uploadResult.isNotEmpty() && uploadResult.isNotBlank()) {
                val r = pool.rxQuery("INSERT INTO sketch_photos (name, sketch_id) VALUES ('$uploadResult', $sketchId);").blockingGet()
                if(r.rowCount() == 0) {
                    FileUtils().deleteFile("static/$uploadResult")
                    return ""
                }
                return uploadResult
            }
            return ""
        } catch(ex: Exception) {
            println(ex.localizedMessage)
            return ""
        }
    }

    override fun isSketchLiked(sketchId: Int, userId: Int): Boolean {
        val r = pool
                .rxQuery("SELECT COUNT(sketch_id) count FROM user_favorite_sketches WHERE user_id = $userId AND sketch_id = $sketchId;")
                .blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val likes = i.next().getInteger("count")
            return likes > 0
        }
        return false
    }

    override fun likeSketch(sketchId: Int, userId: Int): Boolean {
        val r = pool
                .rxQuery("INSERT INTO user_favorite_sketches VALUES($userId, $sketchId);")
                .blockingGet()
        return r.rowCount() > 0
    }

    override fun unlikeSketch(sketchId: Int, userId: Int): Boolean {
        val r = pool
                .rxQuery("DELETE FROM user_favorite_sketches WHERE user_id = $userId AND sketch_id = $sketchId;")
                .blockingGet()
        return r.rowCount() > 0
    }

    override fun getPrices(): MinMax {
        val r = pool.rxQuery("").blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val row = i.next()
            return MinMax(
                    min = row.getInteger("min") ?: 0,
                    max = row.getInteger("max") ?: 1
            )
        }
        return MinMax(0, 1)
    }

    override fun deleteSketch(sketchId: Int): Boolean {
        val r = pool.rxQuery("DELETE FROM sketches WHERE id = $sketchId").blockingGet()
        return r.rowCount() > 0
    }
}