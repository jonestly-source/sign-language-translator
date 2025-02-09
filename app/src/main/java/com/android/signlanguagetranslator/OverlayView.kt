package com.android.signlanguagetranslator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewParent
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import kotlin.math.max
import kotlin.math.min

private val ViewParent.context: Context
    get() {
        TODO("Not yet implemented")
    }

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: GestureRecognizerResult? = null
    private var linePaint = Paint()
    private var pointPaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var handCoordinate: Int? = null

    init {
        initPaints()
    }

    fun clear() {
        results = null
        linePaint.reset()
        pointPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        linePaint.color =
            ContextCompat.getColor(context!!, R.color.mp_bounding_box)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    private fun convertBoundingBox(landmarks: List<NormalizedLandmark>): GestureRecognizerHelper.BoundingBox {
        // Assuming the landmark contains information about the bounding box.
        // Adjust the indices based on your actual landmark structure.
        val left = landmarks.minByOrNull { it.x() }?.x() ?: 0f
        val top = landmarks.minByOrNull { it.y() }?.y() ?: 0f
        val right = landmarks.maxByOrNull { it.x() }?.x() ?: 0f
        val bottom = landmarks.maxByOrNull { it.y() }?.y() ?: 0f


        return GestureRecognizerHelper.BoundingBox(left, top, right, bottom)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if (handCoordinate == 1) {
            //bones
            results?.let { gestureRecognizerResult ->
                for (landmark in gestureRecognizerResult.landmarks()) {
                    for (normalizedLandmark in landmark) {
                        canvas.drawPoint(
                            normalizedLandmark.x() * imageWidth * scaleFactor,
                            normalizedLandmark.y() * imageHeight * scaleFactor,
                            pointPaint
                        )
                    }

                    HandLandmarker.HAND_CONNECTIONS.forEach {
                        canvas.drawLine(
                            gestureRecognizerResult.landmarks().get(0).get(it!!.start())
                                .x() * imageWidth * scaleFactor,
                            gestureRecognizerResult.landmarks().get(0).get(it.start())
                                .y() * imageHeight * scaleFactor,
                            gestureRecognizerResult.landmarks().get(0).get(it.end())
                                .x() * imageWidth * scaleFactor,
                            gestureRecognizerResult.landmarks().get(0).get(it.end())
                                .y() * imageHeight * scaleFactor,
                            linePaint
                        )
                    }
                }
            }
        }else {
            //bounding box
            results?.let { gestureRecognizerResult ->
                for (landmark in gestureRecognizerResult.landmarks()) {
                    val boundingBox = convertBoundingBox(landmark)
                    canvas.drawRect(
                        boundingBox.left * imageWidth * scaleFactor,
                        boundingBox.top * imageHeight * scaleFactor,
                        boundingBox.right * imageWidth * scaleFactor,
                        boundingBox.bottom * imageHeight * scaleFactor,
                        linePaint
                    )
                }
            }
        }
    }

    fun setResults(
        gestureRecognizerResult: GestureRecognizerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE,
        handCoordinateResult: Int?
    ) {
        results = gestureRecognizerResult
        handCoordinate = handCoordinateResult?: 0

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
    }
}