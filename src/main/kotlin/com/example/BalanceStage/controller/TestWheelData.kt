package com.example.BalanceStage.controller

import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.concurrent.fixedRateTimer

class TestWheelData {
    companion object {
        private val random = Random()
        private var started = false

        fun start() {
            if (started) return
            started = true
            println("TestWheelData 시작됨")

            fixedRateTimer("SendFakeData", daemon = true, initialDelay = 2000, period = 1000) {
                val firstWheel = random.nextDouble() * 180
                val secondWheel = random.nextDouble() * 180

                val json = """{"firstWheel":$firstWheel,"secondWheel":$secondWheel}"""
                try {
                    val url = URL("http://localhost:8080/api/positions")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")

                    conn.outputStream.use { it.write(json.toByteArray()) }

                    val responseCode = conn.responseCode
                    val responseText = conn.inputStream.bufferedReader().readText()

                    println("보냄: 1st=$firstWheel°, 2nd=$secondWheel°")
                    println("응답 코드: $responseCode")
                    println("응답 내용: $responseText")
                } catch (e: Exception) {
                    println("전송 실패: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
}
