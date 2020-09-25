package com.perevodchik.utils

import io.micronaut.http.multipart.StreamingFileUpload
import io.reactivex.Single
import java.io.File

class FileUtils {

    fun uploadFile(file: StreamingFileUpload, name: String): Boolean {
        return try {
            Single.fromPublisher(file.transferTo(File(name)))
                    .map { success -> success
                    }.blockingGet()
        } catch (ex: Exception) {
            false
        }
    }

    fun deleteFile(fileName: String) {
        println("delete [$fileName]")
    }

}