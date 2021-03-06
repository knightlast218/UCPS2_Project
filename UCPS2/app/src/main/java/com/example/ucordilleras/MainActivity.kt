package com.example.ucordilleras

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.imageclassificationdemo.R
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class MainActivity : AppCompatActivity() {
    protected var tflite: Interpreter? = null
    private val tfliteModel: MappedByteBuffer? = null
    private var inputImageBuffer: TensorImage? = null
    private var imageSizeX = 0
    private var imageSizeY = 0
    private var outputProbabilityBuffer: TensorBuffer? = null
    private var probabilityProcessor: TensorProcessor? = null
    private var bitmap: Bitmap? = null
    private var labels: List<String>? = null
    var imageView: ImageView? = null
    var imageuri: Uri? = null
    var buclassify: Button? = null
    var classitext: TextView? = null
    var bushowDetails: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = findViewById<View>(R.id.image) as ImageView
        buclassify = findViewById<View>(R.id.classify) as Button
        classitext = findViewById<View>(R.id.classifytext) as TextView
        bushowDetails = findViewById<View>(R.id.showDetails) as Button
        imageView!!.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 12)
        }
        try {
            tflite = Interpreter(loadmodelfile(this))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        buclassify!!.setOnClickListener {
            val imageTensorIndex = 0
            val imageShape = tflite!!.getInputTensor(imageTensorIndex).shape() // {1, height, width, 3}
            imageSizeY = imageShape[1]
            imageSizeX = imageShape[2]
            val imageDataType = tflite!!.getInputTensor(imageTensorIndex).dataType()
            val probabilityTensorIndex = 0
            val probabilityShape = tflite!!.getOutputTensor(probabilityTensorIndex).shape() // {1, NUM_CLASSES}
            val probabilityDataType = tflite!!.getOutputTensor(probabilityTensorIndex).dataType()
            inputImageBuffer = TensorImage(imageDataType)
            outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)
            probabilityProcessor = TensorProcessor.Builder().add(postprocessNormalizeOp).build()
            inputImageBuffer = loadImage(bitmap)
            tflite!!.run(inputImageBuffer!!.buffer, outputProbabilityBuffer!!.buffer.rewind())
            showresult()
        }
    }

    private fun loadImage(bitmap: Bitmap?): TensorImage {
        // Loads bitmap into a TensorImage.
        inputImageBuffer!!.load(bitmap!!)

        // Creates processor for the TensorImage.
        val cropSize = Math.min(bitmap.width, bitmap.height)
        // TODO(b/143564309): Fuse ops inside ImageProcessor.
        val imageProcessor = ImageProcessor.Builder()
                .add(ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .add(preprocessNormalizeOp)
                .build()
        return imageProcessor.process(inputImageBuffer)
    }

    @Throws(IOException::class)
    private fun loadmodelfile(activity: Activity): MappedByteBuffer {
        val fileDescriptor = activity.assets.openFd("model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startoffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startoffset, declaredLength)
    }

    private val preprocessNormalizeOp: TensorOperator
        private get() = NormalizeOp(IMAGE_MEAN, IMAGE_STD)

    private val postprocessNormalizeOp: TensorOperator
        private get() = NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)

    private fun showresult() {
        try {
            labels = FileUtil.loadLabels(this, "dict.txt")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val labeledProbability = TensorLabel(labels!!, probabilityProcessor!!.process(outputProbabilityBuffer))
                .mapWithFloatValue
        val maxValueInMap = Collections.max(labeledProbability.values)
        for ((key, value) in labeledProbability) {
            if (value == maxValueInMap) {
                classitext!!.text = key

                bushowDetails!!.setOnClickListener {
                    if (key=="leafspot"){
                        val callleafspot = Intent(this, leafspot_disease::class.java)
                        startActivity(callleafspot)}
                    if (key=="graymold"){
                        val callgraymold = Intent(this, graymold_disease::class.java)
                        startActivity(callgraymold)}
                    if (key=="leafblight"){
                        val callleafblight = Intent(this, leafblight::class.java)
                        startActivity(callleafblight)}
                    if (key=="mycosphaerella_fragariae"){
                        val callmycosphaerella_fragariae = Intent(this, leafspot_disease::class.java)
                        startActivity(callmycosphaerella_fragariae)}
                    if (key=="anthracnose"){
                        val callanthracnose = Intent(this, anthracnose_disease::class.java)
                        startActivity(callanthracnose)}
                    if (key=="leafscorch"){
                        val callleafscorch = Intent(this, leafscorch_diseases::class.java)
                        startActivity(callleafscorch)}
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 12 && resultCode == Activity.RESULT_OK && data != null) {
            imageuri = data.data
            try {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageuri)
                imageView!!.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val IMAGE_MEAN = 0.0f
        private const val IMAGE_STD = 1.0f
        private const val PROBABILITY_MEAN = 0.0f
        private const val PROBABILITY_STD = 255.0f
    }
}