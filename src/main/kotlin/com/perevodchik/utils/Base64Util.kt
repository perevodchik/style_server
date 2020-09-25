package com.perevodchik.utils

import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

class Base64Util {

    fun base64ToImage(base64: String) {
        val data = base64.split(",")
        for(s in data) println(s)
        val bytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(data[1])
        val i = ImageIO.read(ByteArrayInputStream(bytes))
        ImageIO.write(i, "png", File("dfdsfd"))
    }
}