package xyz.nextalone.nagram.helper.livephoto

import android.os.Build
import java.io.File

/**
 * Motion Photo 文件合成器
 *
 * 将 JPEG 静态图片和 MP4 视频合成为符合特定厂商规范或 Android Motion Photo 1.0 规范的文件。
 *
 * https://github.com/bjzhou/PhotonCamera/blob/main/app/src/main/java/com/hinnka/mycamera/livephoto/MotionPhotoWriter.kt
 */
object MotionPhotoWriter {
    /**
     * 获取适合当前设备的创建器
     */
    fun getCreator(): LivePhotoCreator {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") ->
                OppoLivePhotoCreator()
            manufacturer.contains("vivo") ->
                VivoLivePhotoCreator()
            manufacturer.contains("huawei") || manufacturer.contains("honor") ->
                LegacyLivePhotoCreator()
            else -> GoogleLivePhotoCreator()
        }
    }

    /**
     * 合成 Motion Photo 文件
     *
     * @param jpegPath 静态 JPEG 图片路径
     * @param videoPath MP4 视频路径
     * @param outputPath 输出 Motion Photo 路径
     * @param presentationTimestampUs 主要帧的显示时间戳（微秒）
     * @return 是否成功
     */
    fun write(
        jpegPath: String,
        videoPath: String,
        outputPath: String,
        presentationTimestampUs: Long = 0,
    ): Boolean {
        val jpegFile = File(jpegPath)
        val videoFile = File(videoPath)

        if (!jpegFile.exists()) return false
        if (!videoFile.exists()) return false

        val creator = getCreator()

        return creator.create(jpegPath, videoPath, outputPath, presentationTimestampUs)
    }
}
