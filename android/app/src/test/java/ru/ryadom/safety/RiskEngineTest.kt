package ru.ryadom.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskEngineTest {

    @Test
    fun directMurderThreatIsCritical() {
        val result = RiskEngine.analyze("я тебя убью")
        assertEquals(100, result.first)
        assertTrue(result.second.contains("Прямая угроза убийством"))
    }

    @Test
    fun directSuicideIntentIsCritical() {
        val result = RiskEngine.analyze("сегодня я убью себя")
        assertEquals(100, result.first)
    }

    @Test
    fun groomingSecrecyAndSexualRequestIsCritical() {
        val result = RiskEngine.analyze("никому не говори родителям, пришли голое фото")
        assertEquals(100, result.first)
    }

    @Test
    fun bankImpersonationAndSmsCodeIsCritical() {
        val result = RiskEngine.analyze("я сотрудник банка, сообщите код из смс")
        assertEquals(100, result.first)
    }

    @Test
    fun remoteAccessScamIsCritical() {
        val result = RiskEngine.analyze("установите AnyDesk и дайте удаленный доступ")
        assertEquals(100, result.first)
    }

    @Test
    fun mildProfanityAloneDoesNotBecomeCritical() {
        val result = RiskEngine.analyze("бля")
        assertTrue(result.first < 20)
    }

    @Test
    fun ordinaryMessageIsZero() {
        val result = RiskEngine.analyze("Привет, ты сделал домашку?")
        assertEquals(0, result.first)
    }
}
