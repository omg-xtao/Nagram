package xyz.nextalone.nagram.helper.livephoto

import androidx.exifinterface.media.ExifInterface
import org.telegram.messenger.FileLog
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * Live Photo creator for Oppo/Realme/OnePlus devices (ColorOS "Olive" format).
 * Requirements:
 * 1. EXIF UserComment set to "oplus_8388608".
 * 2. XMP metadata with specific namespaces and structure.
 * 3. MPF (Multi-Picture Format) APP2 segment.
 * 4. Appended video data.
 */
class OppoLivePhotoCreator : LivePhotoCreator {
    private val TAG = "OppoLivePhotoCreator"

    override fun create(
        jpegPath: String,
        videoPath: String,
        outputPath: String,
        presentationTimestampUs: Long
    ): Boolean {
        try {
            val videoFile = File(videoPath)
            val videoLength = videoFile.length()
            val originalJpegFile = File(jpegPath)
            val originalJpegLength = originalJpegFile.length()

            // 1. Prepare XMP Data
            val xmpData = buildOppoXmp(videoLength, presentationTimestampUs)

            // 2. Prepare MPF Data
            val mpfData = buildMpfSegment(originalJpegLength.toInt())

            // 3. Write modified JPEG
            FileOutputStream(outputPath).use { output ->
                FileInputStream(jpegPath).use { jpegInput ->
                    if (!injectSegments(jpegInput, output, xmpData, mpfData)) {
                        return false
                    }
                }
                // 4. Append Video
                FileInputStream(videoPath).use { videoInput ->
                    videoInput.copyTo(output)
                }
            }

            // 5. Set EXIF UserComment
            try {
                val exif = ExifInterface(outputPath)
                exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "oplus_8388608")
                exif.saveAttributes()
            } catch (e: Exception) {
                FileLog.e("Failed to set EXIF", e)
            }

            return true
        } catch (e: Exception) {
            FileLog.e("Failed to create Oppo Live Photo", e)
            return false
        }
    }

    private fun injectSegments(
        input: FileInputStream,
        output: FileOutputStream,
        xmpData: ByteArray,
        mpfData: ByteArray
    ): Boolean {
        val bis = BufferedInputStream(input)
        val b1 = bis.read()
        val b2 = bis.read()
        if (b1 != 0xFF || b2 != 0xD8) return false
        output.write(b1)
        output.write(b2)

        // Inject XMP (APP1)
        val xmpNamespace = "http://ns.adobe.com/xap/1.0/\u0000"
        val namespaceBytes = xmpNamespace.toByteArray(StandardCharsets.UTF_8)
        val xmpTotalLen = 2 + namespaceBytes.size + xmpData.size
        output.write(0xFF)
        output.write(0xE1)
        output.write((xmpTotalLen shr 8) and 0xFF)
        output.write(xmpTotalLen and 0xFF)
        output.write(namespaceBytes)
        output.write(xmpData)

        // Inject MPF (APP2)
        val mpfTotalLen = 2 + mpfData.size
        output.write(0xFF)
        output.write(0xE2)
        output.write((mpfTotalLen shr 8) and 0xFF)
        output.write(mpfTotalLen and 0xFF)
        output.write(mpfData)

        bis.copyTo(output)
        return true
    }

    private fun buildOppoXmp(videoLength: Long, presentationTimestampUs: Long): ByteArray {
        val xmp = """
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
             <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
                xmlns:OpCamera="http://ns.oplus.com/photos/1.0/camera/"
                xmlns:Container="http://ns.google.com/photos/1.0/container/"
                xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
                GCamera:MotionPhoto="1"
                GCamera:MotionPhotoVersion="1"
                GCamera:MotionPhotoPresentationTimestampUs="$presentationTimestampUs"
                OpCamera:MotionPhotoPrimaryPresentationTimestampUs="$presentationTimestampUs"
                OpCamera:MotionPhotoOwner="PhotonCamera"
                OpCamera:OLivePhotoVersion="1"
                OpCamera:VideoLength="$videoLength">
               <Container:Directory>
                <rdf:Seq>
                 <rdf:li rdf:parseType="Resource">
                  <Container:Item Item:Mime="image/jpeg" Item:Semantic="Primary"/>
                 </rdf:li>
                 <rdf:li rdf:parseType="Resource">
                  <Container:Item Item:Mime="video/mp4" Item:Semantic="MotionPhoto" Item:Length="$videoLength"/>
                 </rdf:li>
                </rdf:Seq>
               </Container:Directory>
              </rdf:Description>
             </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()
        return xmp.toByteArray(StandardCharsets.UTF_8)
    }

    private fun buildMpfSegment(jpegSize: Int): ByteArray {
        val baos = ByteArrayOutputStream()
        val order = ByteOrder.BIG_ENDIAN

        // 1. Signature
        baos.write("MPF\u0000".toByteArray(StandardCharsets.US_ASCII))

        // 2. Header
        baos.write("MM\u0000\u002a".toByteArray(StandardCharsets.US_ASCII)) // Big Endian TIFF
        baos.write(ByteBuffer.allocate(4).order(order).putInt(8).array()) // Offset to first IFD

        // 3. Index IFD
        baos.write(ByteBuffer.allocate(2).order(order).putShort(3).array()) // 3 entries

        // Entry 1: Version (0xB000)
        baos.write(ByteBuffer.allocate(12).order(order)
            .putShort(0xB000.toShort())
            .putShort(7) // UNDEFINED
            .putInt(4)   // 4 bytes
            .put("0100".toByteArray(StandardCharsets.US_ASCII))
            .array())

        // Entry 2: Number of Images (0xB001)
        baos.write(ByteBuffer.allocate(12).order(order)
            .putShort(0xB001.toShort())
            .putShort(4) // LONG
            .putInt(1)   // 1 count
            .putInt(1)   // 1 image
            .array())

        // Entry 3: MPEntry (0xB002)
        baos.write(ByteBuffer.allocate(12).order(order)
            .putShort(0xB002.toShort())
            .putShort(7) // UNDEFINED
            .putInt(16)  // 1 entry * 16 bytes
            .putInt(50)  // Offset to entry data (8 + 2 + 12*3 + 4 = 50)
            .array())

        baos.write(ByteBuffer.allocate(4).order(order).putInt(0).array()) // Next IFD offset

        // 4. MPEntry values (Offset 50)
        // Entry 0: TypeCode(4 bytes) | Size(4) | Offset(4) | Dep1(2) | Dep2(2)
        val entry0 = ByteBuffer.allocate(16).order(order)
        entry0.put(byteArrayOf(0, 3, 0, 0)) // Type Code: Baseline Primary Image
        entry0.putInt(jpegSize + 4) // Roughly the jpeg size
        entry0.putInt(0) // Offset
        entry0.putShort(0)
        entry0.putShort(0)
        baos.write(entry0.array())

        return baos.toByteArray()
    }
}
