package xyz.nextalone.nagram.helper.livephoto

import org.telegram.messenger.FileLog
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

/**
 * Live Photo creator using Google's Motion Photo format (XMP-based).
 */
class GoogleLivePhotoCreator : LivePhotoCreator {
    private val TAG = "GoogleLivePhotoCreator"

    override fun create(
        jpegPath: String,
        videoPath: String,
        outputPath: String,
        presentationTimestampUs: Long
    ): Boolean {
        try {
            val jpegFile = File(jpegPath)
            val videoFile = File(videoPath)
            val videoLength = videoFile.length()

            // 1. Generate XMP Metadata
            val xmpData = buildMotionPhotoXmp(videoLength, presentationTimestampUs)

            FileOutputStream(outputPath).use { output ->
                FileInputStream(jpegPath).use { jpegInput ->
                    // 2. Inject XMP and write JPEG content
                    if (!injectXmpToStream(jpegInput, output, xmpData)) {
                        FileLog.e("Failed to inject XMP")
                        return false
                    }
                }

                // 3. Append MP4 video
                FileInputStream(videoPath).use { videoInput ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (videoInput.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            return true
        } catch (e: Exception) {
            FileLog.e("Failed to create Google Motion Photo", e)
            return false
        }
    }

    private fun injectXmpToStream(input: FileInputStream, output: FileOutputStream, xmpData: ByteArray): Boolean {
        val bis = BufferedInputStream(input)

        // 1. Verify SOI (FF D8)
        val b1 = bis.read()
        val b2 = bis.read()
        if (b1 != 0xFF || b2 != 0xD8) {
            FileLog.e("Invalid JPEG: missing SOI marker")
            return false
        }
        output.write(b1)
        output.write(b2)

        // 2. Inject XMP APP1 segment
        val xmpNamespace = "http://ns.adobe.com/xap/1.0/\u0000"
        val namespaceBytes = xmpNamespace.toByteArray(StandardCharsets.UTF_8)
        val segmentLength = 2 + namespaceBytes.size + xmpData.size

        if (segmentLength > 65535) {
            FileLog.e("XMP data too large")
            return false
        }

        output.write(0xFF)
        output.write(0xE1)
        output.write((segmentLength shr 8) and 0xFF)
        output.write(segmentLength and 0xFF)
        output.write(namespaceBytes)
        output.write(xmpData)

        // 3. Copy remaining JPEG data
        val buffer = ByteArray(8192)
        var len: Int
        while (bis.read(buffer).also { len = it } != -1) {
            output.write(buffer, 0, len)
        }
        return true
    }

    private fun buildMotionPhotoXmp(videoLength: Long, presentationTimestampUs: Long): ByteArray {
        val xmp = """
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description
                        xmlns:Camera="http://ns.google.com/photos/1.0/camera/"
                        xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
                        xmlns:OpCamera="http://ns.oppo.com/photo/1.0/camera/"
                        Camera:MotionPhoto="1"
                        Camera:MotionPhotoVersion="1"
                        Camera:MotionPhotoPresentationTimestampUs="$presentationTimestampUs"
                        GCamera:MotionPhoto="1"
                        GCamera:MotionPhotoVersion="1"
                        GCamera:MotionPhotoPresentationTimestampUs="$presentationTimestampUs"
                        GCamera:MicroVideo="1"
                        GCamera:MicroVideoVersion="1"
                        GCamera:MicroVideoOffset="$videoLength"
                        OpCamera:MotionPhotoPrimaryPresentationTimestampUs="$presentationTimestampUs"
                        OpCamera:MotionPhotoOwner="PhotonCamera"
                        OpCamera:OLivePhotoVersion="1"
                        OpCamera:VideoLength="$videoLength"/>
                    <rdf:Description
                        xmlns:Container="http://ns.google.com/photos/1.0/container/"
                        xmlns:Item="http://ns.google.com/photos/1.0/container/item/">
                        <Container:Directory>
                            <rdf:Seq>
                                <rdf:li rdf:parseType="Resource">
                                    <Container:Item
                                        Item:Mime="image/jpeg"
                                        Item:Semantic="Primary"
                                        Item:Length="0"
                                        Item:Padding="0"/>
                                </rdf:li>
                                <rdf:li rdf:parseType="Resource">
                                    <Container:Item
                                        Item:Mime="video/mp4"
                                        Item:Semantic="MotionPhoto"
                                        Item:Length="$videoLength"/>
                                </rdf:li>
                            </rdf:Seq>
                        </Container:Directory>
                    </rdf:Description>
                </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()
        return xmp.toByteArray(StandardCharsets.UTF_8)
    }
}
