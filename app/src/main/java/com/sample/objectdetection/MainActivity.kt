package com.sample.objectdetection

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.huawei.hmf.tasks.Task
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.objects.MLObject
import com.huawei.hms.mlsdk.objects.MLObjectAnalyzer
import com.huawei.hms.mlsdk.objects.MLObjectAnalyzerSetting
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val IMAGE_REQUEST_CODE = 30
        private const val TAG = "ML_MainActivity"
    }

    private var bitmap: Bitmap? = null
    private var analyzer: MLObjectAnalyzer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createObjectAnalyzer()
        fabPhoto.setOnClickListener { getImage() }
    }

    private fun getImage() {
        Intent(Intent.ACTION_GET_CONTENT).also {
            it.type = "image/*"
            startActivityForResult(it, IMAGE_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.also {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)

                if (bitmap != null) {
                    analyze(bitmap!!)
                }
            }
        }

    }

    private fun analyze(bitmap: Bitmap) {
        // Create an MLFrame object using the bitmap, which is the image data in bitmap format.
        val frame: MLFrame = MLFrame.fromBitmap(bitmap)

        // Create a task to process the result returned by the object detector.
        val task: Task<MutableList<MLObject>>? = analyzer?.asyncAnalyseFrame(frame)

        // Asynchronously process the result returned by the object detector.
        task?.addOnSuccessListener {
            val processedBitmap = drawItems(it, bitmap)
            imageView.setImageBitmap(processedBitmap)
        }
        task?.addOnFailureListener {
            Log.e(TAG, "Object detection failed with exception -> $it")
        }
    }

    // Use MLObjectAnalyzerSetting.TYPE_PICTURE for static image detection.
    private fun createObjectAnalyzer() {
        val setting = MLObjectAnalyzerSetting.Factory()
            .setAnalyzerType(MLObjectAnalyzerSetting.TYPE_PICTURE)
            .allowMultiResults()
            .allowClassification()
            .create()

        analyzer = MLAnalyzerFactory.getInstance().getLocalObjectAnalyzer(setting)
    }

    /*
     *  Draws yellow rectangle around MLObjects and
     *  draws text about objects type in the upper left corner of the rectangle
     */
    private fun drawItems(items: List<MLObject>, bitmap: Bitmap): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        for (item in items) {

            val type = when(item.typeIdentity) {
                MLObject.TYPE_OTHER -> "Other"
                MLObject.TYPE_FACE -> "Face"
                MLObject.TYPE_FOOD -> "Food"
                MLObject.TYPE_FURNITURE -> "Furniture"
                MLObject.TYPE_PLACE -> "Place"
                MLObject.TYPE_PLANT -> "Plant"
                else -> "No match"
            }
            Log.i(TAG, "Border: ${item.border}\nType Identity:$type\nType Possibility:${item.typePossibility}")

            //Draw a yellow rectangle around the object.
            Paint().also {
                it.color = Color.YELLOW
                it.style = Paint.Style.STROKE
                it.strokeWidth = 4F
                canvas.drawRect(item.border, it)
            }

            //Draw a text in the top-left corner writing object's type.
            Paint().also {
                it.color = Color.BLACK
                it.style = Paint.Style.FILL_AND_STROKE
                it.textSize = 16F
                canvas.drawText(type, (item.border.left+20).toFloat(), (item.border.top+20).toFloat(), it)
            }
        }

        return mutableBitmap
    }

    override fun onPause() {
        super.onPause()
        //After the detection is complete, stop the analyzer to release detection resources.
        analyzer?.stop()
    }
}
