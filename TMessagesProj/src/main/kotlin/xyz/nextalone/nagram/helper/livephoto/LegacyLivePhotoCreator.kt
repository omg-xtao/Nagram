package xyz.nextalone.nagram.helper.livephoto

import org.telegram.messenger.FileLog
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

/**
 * Legacy Live Photo creator that appends a "LIVE_" marker at the end of the file.
 * Common in some older implementations or specific brands like Samsung/Huawei.
 */
class LegacyLivePhotoCreator : LivePhotoCreator {
    private val TAG = "LegacyLivePhotoCreator"

    override fun create(
        jpegPath: String,
        videoPath: String,
        outputPath: String,
        presentationTimestampUs: Long
    ): Boolean {
        try {
            val videoLength = File(videoPath).length()

            FileOutputStream(outputPath).use { fos ->
                val bos = BufferedOutputStream(fos)
                val dos = DataOutputStream(bos)

                // 1. Copy image
                FileInputStream(jpegPath).use { fis ->
                    fis.copyTo(dos)
                }

                // 2. Copy video
                FileInputStream(videoPath).use { fis ->
                    fis.copyTo(dos)
                }

                // 3. Append Marker
                val markerstr1 = "500:1046"
                val markerStr2 = "LIVE_$videoLength"
                val paddedMarker = markerstr1.padEnd(20) + markerStr2.padEnd(20)
                dos.write(paddedMarker.toByteArray(StandardCharsets.UTF_8))

                dos.flush()
            }
            return true
        } catch (e: Exception) {
            FileLog.e("Failed to create Legacy Live Photo", e)
            return false
        }
    }
}
