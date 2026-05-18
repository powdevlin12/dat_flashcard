package com.dttrn.datfs.core.domain.common

/**
 * Hierarchy AppException cho tất cả lỗi business logic trong app.
 * Giúp UI phân biệt loại lỗi để hiển thị thông báo phù hợp.
 */
sealed class AppException(override val message: String?) : Exception(message) {

    /** Lỗi liên quan đến database operation */
    class DatabaseException(message: String?) : AppException(message)

    /** Lỗi khi parse/validate file Excel import */
    class ImportException(
        message: String?,
        val rowNumber: Int? = null,
        val errors: List<String> = emptyList()
    ) : AppException(message)

    /** Lỗi khi export file */
    class ExportException(message: String?) : AppException(message)

    /** Lỗi validation input người dùng */
    class ValidationException(message: String?) : AppException(message)

    /** File không tìm thấy hoặc không đọc được */
    class FileNotFoundException(message: String?) : AppException(message)

    /** Lỗi backup/restore */
    class BackupException(message: String?) : AppException(message)

    /** Lỗi không xác định */
    class UnknownException(message: String?, cause: Throwable? = null) : AppException(message)

    companion object {
        fun from(throwable: Throwable): AppException = when (throwable) {
            is AppException -> throwable
            else -> UnknownException(throwable.message, throwable)
        }
    }
}
