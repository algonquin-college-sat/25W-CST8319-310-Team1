package algonquin.cst8319.enigmatic


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors
import boofcv.alg.feature.detect.edge.CannyEdge
import boofcv.android.ConvertBitmap
import boofcv.struct.image.GrayU8
import boofcv.struct.image.GrayS16
import com.google.mlkit.vision.common.InputImage

class ImagePreprocessor {
    fun processImage(image: InputImage): InputImage {
        var bitmap: Bitmap? = null // Declare outside
        try {
            bitmap = inputImageToBitmap(image)
            if (bitmap == null) {
                throw IllegalArgumentException("Failed to convert InputImage to Bitmap")
            }

            // Simplest way to convert Bitmap to GrayU8
            val grayImage = ConvertBitmap.bitmapToGray(bitmap, null as GrayU8?, null)

            val edgeImage = detectEdges(grayImage)
            val processedBitmap = ConvertBitmap.grayToBitmap(edgeImage, null)

            return InputImage.fromBitmap(processedBitmap, 0)
        } catch (e: Exception) {
            throw IllegalStateException("Error processing image: ${e.message}")
        }finally {
            bitmap?.recycle()
        }
    }


    fun inputImageToBitmap(image: InputImage): Bitmap? {
        return try {
            if (image.bitmapInternal != null) {
                return image.bitmapInternal
            }
            val buffer = image.byteBuffer
            val bytes = ByteArray(buffer!!.remaining())
            buffer!!.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e("BoofCV", "Failed to convert InputImage to Bitmap: ${e.message}")
            null
        }
    }



    private fun detectEdges(grayImage: GrayU8): GrayU8 {
        val edgeImage = GrayU8(grayImage.width, grayImage.height) // Allocate correct size

        val canny = FactoryEdgeDetectors.canny(
            2,
            true,
            true,
            GrayU8::class.java,
            GrayS16::class.java
        )
        // Try adjusting these values
        canny.process(grayImage, 0.1f, 0.3f, edgeImage) // Lower thresholds


        return edgeImage
    }



}