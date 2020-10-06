package com.perevodchik.utils

import io.micronaut.http.multipart.StreamingFileUpload
import io.reactivex.Single
import java.io.File
import java.util.*

class FileUtils {

    fun uploadFile(file: StreamingFileUpload, name: String): String {
        val names = name.split("_")
        println("name is $name")
        if(names.size != 3) {
            return ""
        }
        val file0 = File("static/${names[0]}")
        println("static/${names[0]} => [${file0.exists()}] [${file0.isDirectory}]")
        if(!file0.exists() || !file0.isDirectory) {
            val file0IsMake = file0.mkdirs()
            println("file0IsMake [$file0IsMake]")
        }

        val file1 = File("static/${names[0]}/${names[1]}")
        println("static/${names[0]}/${names[1]} => [${file1.exists()}] [${file1.isDirectory}]")
        if(!file1.exists() || !file1.isDirectory) {
            val file1IsMake = file1.mkdirs()
            println("file1IsMake [$file1IsMake]")
        }

        val newName = "${names[0]}/${names[1]}/${names[1]}_${Calendar.getInstance().timeInMillis}_${names[2]}"
        println("newName file =>  $newName")

        return try {
            Single.fromPublisher(file.transferTo(File("static/$newName")))
                    .map {
                        newName
                    }.blockingGet()
        } catch (ex: Exception) {
            ex.printStackTrace()
            ""
        }
    }

    fun deleteFile(fileName: String) {
        val f = File(fileName)
        if(f.exists())
            f.delete()
    }

    fun generateFilePathAndName(prefix: String, username: String, uploadFileName: String): String {
        return "$prefix/${username.hashCode()}_${DateTimeUtil.day()}_${uploadFileName.length.hashCode()}"
                .replace("-", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(":", "")
                .plus(".jpeg")
    }

}