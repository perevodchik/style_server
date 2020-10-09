package com.perevodchik.utils

import io.micronaut.http.multipart.StreamingFileUpload
import io.reactivex.Single
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam


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
                        try {
                            compress("static/$newName", 0.5f)
                        } catch(ex: Exception) {}
                        newName
                    }.blockingGet()
        } catch (ex: Exception) {
            ex.printStackTrace()
            ""
        }
    }

    fun uploadFileTest(file: StreamingFileUpload, name: String, quality: Float): String {
        return try {
            Single.fromPublisher(file.transferTo(File("static/$name")))
                    .map {
                        compress("static/$name", quality)
                        name
                    }.blockingGet()
        } catch (ex: Exception) {
            ex.printStackTrace()
            ""
        }
    }

    private fun compress(name: String, quality: Float) {
        val file = File(name)
        val image = ImageIO.read(file)

        print("started file size ${getFileSizeKiloBytes(file)}")

        val compressedImageFile = File(file.path)
        val os: OutputStream = FileOutputStream(compressedImageFile)

        val writers = ImageIO.getImageWritersByFormatName("jpg")
        val writer = writers.next()

        val ios = ImageIO.createImageOutputStream(os)
        writer.output = ios
        val param = writer.defaultWriteParam

        param.compressionMode = ImageWriteParam.MODE_EXPLICIT
        param.compressionQuality = quality
        writer.write(null, IIOImage(image, null, null), param)

        println(", finished file size ${getFileSizeKiloBytes(compressedImageFile)}")
        os.close()
        ios.close()
        writer.dispose()
    }

    private fun getFileSizeMegaBytes(file: File): String? {
        return (file.length().toDouble() / (1024 * 1024)).toString() + " mb"
    }

    private fun getFileSizeKiloBytes(file: File): String? {
        return (file.length().toDouble() / 1024).toString() + "  kb"
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