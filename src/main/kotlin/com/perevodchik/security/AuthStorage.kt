package com.perevodchik.security

import com.perevodchik.domain.Phone
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap

class AuthStorage {
    companion object {
        private val map = HashMap<String, MutableList<String>>()
        private val random = Random()

        fun isContainsCode(phone: Phone, code: String): Boolean {
//            return map[phone.phone]?.contains(code) ?: false
            return code == "1234"
        }

        fun removeCode(phone: Phone, code: String): Boolean {
            return map[phone.phone]?.remove(code) ?: false
        }

        fun getCodeByPhone(phone: Phone): String {
            if(!map.containsKey(phone.phone)) return ""
            val codes = map[phone.phone]!!
            try {
                val i = codes.iterator()
                if(i.hasNext()) {
                    val code = i.next()
                    i.remove()
                    return code
                }
            } catch(ex: Exception) {
                ex.printStackTrace()
                return ""
            }
            return ""
        }

        fun createCode(phone: Phone) {
            random.setSeed(Calendar.getInstance().timeInMillis)
            var code = ""
            for(i in 0..4)
                code = code.plus(random.nextInt(10))
            println("new code $code")
            if(map.containsKey(phone.phone)) {
                map[phone.phone]?.add(code)
            } else {
                val codes = mutableListOf(code)
                map[phone.phone] = codes
            }
        }
    }
}