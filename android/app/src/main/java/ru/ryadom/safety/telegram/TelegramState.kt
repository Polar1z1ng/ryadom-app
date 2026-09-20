package ru.ryadom.safety.telegram

sealed interface TelegramState {
    data object Starting : TelegramState
    data object NeedCredentials : TelegramState
    data object NeedPhone : TelegramState
    data object NeedCode : TelegramState
    data class NeedPassword(val hint: String) : TelegramState
    data object NeedEmail : TelegramState
    data object NeedEmailCode : TelegramState
    data class ConfirmOnOtherDevice(val link: String) : TelegramState
    data object Ready : TelegramState
    data class Error(val message: String) : TelegramState
}
