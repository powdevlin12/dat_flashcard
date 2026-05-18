package com.dttrn.datfs.core.domain.common

/**
 * Sealed class wrapper cho tất cả kết quả trả về từ UseCases.
 * UI layer chỉ làm việc với Result, không trực tiếp xử lý exception.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: AppException) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isLoading get() = this is Loading
    val isSuccess get() = this is Success
    val isError get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): AppException? = (this as? Error)?.exception
}

/** Convenience extension để map kết quả thành công */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}
