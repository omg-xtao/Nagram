package xyz.nextalone.nagram.helper.livephoto

/**
 * Interface for Live Photo creators for different device manufacturers.
 */
interface LivePhotoCreator {
    /**
     * Creates a Live Photo by combining a JPEG image and a video.
     * @param jpegPath The path to the source JPEG image.
     * @param videoPath The path to the source MP4 video.
     * @param outputPath The path where the combined Live Photo will be saved.
     * @param presentationTimestampUs The presentation timestamp of the primary frame in microseconds.
     * @return true if successful, false otherwise.
     */
    fun create(
        jpegPath: String,
        videoPath: String,
        outputPath: String,
        presentationTimestampUs: Long = 0
    ): Boolean
}
