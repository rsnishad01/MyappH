package com.example.data.storage

sealed class UploadStatus {
    object Idle : UploadStatus()
    data class Progress(val progressPercent: Float, val statusMessage: String = "Uploading...") : UploadStatus()
    data class Success(
        val downloadUrl: String,
        val publicId: String = "",
        val resourceType: String = "",
        val format: String = "",
        val bytes: Long = 0L,
        val width: Int = 0,
        val height: Int = 0,
        val originalFilename: String = "",
        val folder: String = ""
    ) : UploadStatus()
    data class Error(val errorMessage: String) : UploadStatus()
}

data class UploadTaskInfo(
    val taskId: String,
    val mediaType: String,
    val status: UploadStatus,
    val progress: Float = 0f
)
