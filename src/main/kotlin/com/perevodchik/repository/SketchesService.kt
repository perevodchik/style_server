package com.perevodchik.repository

import com.perevodchik.domain.MinMax
import com.perevodchik.domain.Sketch
import com.perevodchik.domain.SketchFull
import com.perevodchik.domain.SketchPreview
import io.micronaut.http.multipart.StreamingFileUpload

interface SketchesService {
    fun getAllSketches(): List<SketchPreview>
    fun getList(page: Int, limit: Int, min: Int, max: Int, tags: String, isFavorite:Boolean): List<SketchPreview>
    fun getSketchesByMaster(id: Int): List<SketchPreview>
    fun getSketchById(id: Int): SketchFull?
    fun createSketch(sketch: Sketch): Sketch
    fun updateSketch(sketch: Sketch): Sketch
    fun createSketchImage(name: String, sketchId: Int): Boolean
    fun upload(sketchId: Int, fileName: String, upload: StreamingFileUpload): String
    fun isSketchLiked(sketchId: Int, userId: Int): Boolean
    fun likeSketch(sketchId: Int, userId: Int): Boolean
    fun unlikeSketch(sketchId: Int, userId: Int): Boolean
    fun getPrices(): MinMax
    fun deleteSketch(sketchId: Int): Boolean
}