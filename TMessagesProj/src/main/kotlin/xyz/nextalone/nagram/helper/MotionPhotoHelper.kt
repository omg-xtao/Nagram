package xyz.nextalone.nagram.helper

import org.telegram.messenger.FileLoader
import org.telegram.messenger.FileLog
import org.telegram.messenger.MessageObject

import xyz.nextalone.nagram.helper.livephoto.MotionPhotoWriter

import java.io.File

object MotionPhotoHelper {

    private const val TAG = "MotionPhotoHelper"

    sealed class MergeResult {
        data class Success(val outputPath: String) : MergeResult()
        data class Error(val errorMessage: String, val exception: Exception? = null) : MergeResult()
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    private fun validateFilePath(path: String): ValidationResult {
        if (path.isBlank()) {
            return ValidationResult(false, "文件路径不能为空")
        }

        val file = File(path)

        if (!file.exists()) {
            return ValidationResult(false, "文件不存在: $path")
        }

        if (!file.isFile) {
            return ValidationResult(false, "路径不是有效的文件: $path")
        }

        if (!file.canRead()) {
            return ValidationResult(false, "无法读取文件: $path")
        }

        if (file.length() == 0L) {
            return ValidationResult(false, "文件为空: $path")
        }

        return ValidationResult(true)
    }

    private fun validateImageFile(path: String): ValidationResult {
        val baseValidation = validateFilePath(path)
        if (!baseValidation.isValid) {
            return baseValidation
        }

        val extension = path.substringAfterLast('.', "").lowercase()
        val supportedImageFormats = listOf("jpg", "jpeg", "png", "webp")

        if (extension !in supportedImageFormats) {
            return ValidationResult(
                false,
                "不支持的图片格式: $extension。支持格式: ${supportedImageFormats.joinToString()}"
            )
        }

        return ValidationResult(true)
    }

    private fun validateVideoFile(path: String): ValidationResult {
        val baseValidation = validateFilePath(path)
        if (!baseValidation.isValid) {
            return baseValidation
        }

        val extension = path.substringAfterLast('.', "").lowercase()
        val supportedVideoFormats = listOf("mp4", "webm", "mkv", "3gp")

        if (extension !in supportedVideoFormats) {
            return ValidationResult(
                false,
                "不支持的视频格式: $extension。支持格式: ${supportedVideoFormats.joinToString()}"
            )
        }

        return ValidationResult(true)
    }

    fun createMotionPhoto(currentAccount: Int, messageObject: MessageObject): MergeResult {
        if (!messageObject.isLivePhoto) {
            return MergeResult.Error("不是有效的 Live Photo messageObject")
        }
        val imageFile = FileLoader.getInstance(currentAccount).getPathToMessage(messageObject.messageOwner, true, true)
        val video = messageObject.document
        val videoFile = FileLoader.getInstance(currentAccount).getPathToAttach(video)
        // 遍历 document attributes 获取视频 duration
        var duration = 0.0
        for (attribute in video.attributes) {
            if (attribute.duration > 0) {
                duration = attribute.duration
                break
            }
        }
        val durationLong = (duration * 1_000_000 / 2).toLong()
        return createMotionPhoto(imageFile.path, videoFile.path, durationLong)
    }

    fun createMotionPhoto(
        imagePath: String,
        videoPath: String,
        presentationTimestampUs: Long,
    ): MergeResult {
        val imageValidation = validateImageFile(imagePath)
        if (!imageValidation.isValid) {
            FileLog.e("图片文件验证失败: ${imageValidation.errorMessage}")
            return MergeResult.Error(imageValidation.errorMessage ?: "图片文件验证失败")
        }

        val videoValidation = validateVideoFile(videoPath)
        if (!videoValidation.isValid) {
            FileLog.e("视频文件验证失败: ${videoValidation.errorMessage}")
            return MergeResult.Error(videoValidation.errorMessage ?: "视频文件验证失败")
        }

        val imageFile = File(imagePath)
        val videoFile = File(videoPath)
        val cacheDir = FileLoader.checkDirectory(FileLoader.MEDIA_DIR_CACHE)
        val targetDirectory = File(cacheDir, "MotionPhoto")

        if (!targetDirectory.exists()) {
            if (!targetDirectory.mkdirs()) {
                return MergeResult.Error("无法创建输出目录: ${targetDirectory.absolutePath}")
            }
        }

        val baseName = imageFile.nameWithoutExtension
        val outputFileName = "${baseName}.jpg"
        val outputFile = File(targetDirectory, outputFileName)

        try {
            val data = embedMotionPhotoMetadata(imageFile, videoFile, outputFile, presentationTimestampUs)
            if (data) {
                if (!outputFile.exists()) {
                    return MergeResult.Error("创建MotionPhoto失败")
                }
            }
            FileLog.d("MotionPhoto创建成功: ${outputFile.absolutePath}")
            return MergeResult.Success(outputFile.absolutePath)
        } catch (e: Exception) {
            FileLog.e("创建MotionPhoto失败", e)
            return MergeResult.Error("创建MotionPhoto失败: ${e.message}", e)
        }
    }

    private fun embedMotionPhotoMetadata(
        sourceImage: File,
        sourceVideo: File,
        outputImage: File,
        presentationTimestampUs: Long,
    ): Boolean {
        return MotionPhotoWriter.write(sourceImage.absolutePath, sourceVideo.absolutePath, outputImage.absolutePath, presentationTimestampUs)
    }
}