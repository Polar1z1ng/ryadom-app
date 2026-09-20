package ru.ryadom.safety.vk

sealed interface VkState {
    data object Disconnected : VkState
    data object Connecting : VkState
    data object Ready : VkState
    data class Error(val message: String) : VkState
}
