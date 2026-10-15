package com.example.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraController(
    private val context: Context,
    private val previewView: PreviewView
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var cameraExecutor: ExecutorService? = null

    var isFrontCamera: Boolean = false
        private set

    var isTorchEnabled: Boolean = false
        private set

    private fun getExecutor(): ExecutorService {
        val exec = cameraExecutor
        return if (exec == null || exec.isShutdown || exec.isTerminated) {
            val newExec = Executors.newSingleThreadExecutor()
            cameraExecutor = newExec
            newExec
        } else {
            exec
        }
    }

    fun startCamera(
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        useFront: Boolean = false,
        onReady: () -> Unit = {}
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Camera permission not granted yet")
            return
        }

        isFrontCamera = useFront
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                onReady()
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun toggleCamera(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        startCamera(lifecycleOwner, !isFrontCamera)
    }

    fun toggleTorch(onComplete: (Boolean) -> Unit = {}) {
        val cam = camera ?: run {
            onComplete(false)
            return
        }
        if (cam.cameraInfo.hasFlashUnit()) {
            isTorchEnabled = !isTorchEnabled
            cam.cameraControl.enableTorch(isTorchEnabled)
            onComplete(isTorchEnabled)
        } else {
            onComplete(false)
        }
    }

    fun capturePhoto(onSuccess: (Bitmap) -> Unit, onError: (Throwable) -> Unit) {
        val capture = imageCapture ?: run {
            onError(IllegalStateException("Camera capture not initialized"))
            return
        }

        capture.takePicture(
            getExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    try {
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        val bitmap = imageProxy.toBitmap()
                        val rotatedBitmap = if (rotationDegrees != 0) {
                            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        } else {
                            bitmap
                        }
                        imageProxy.close()
                        ContextCompat.getMainExecutor(context).execute {
                            onSuccess(rotatedBitmap)
                        }
                    } catch (e: Exception) {
                        imageProxy.close()
                        ContextCompat.getMainExecutor(context).execute {
                            onError(e)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    ContextCompat.getMainExecutor(context).execute {
                        onError(exception)
                    }
                }
            }
        )
    }

    fun unbind() {
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor?.shutdown()
            cameraExecutor = null
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "CameraController"
    }
}

@Composable
fun CameraPreviewContainer(
    modifier: Modifier = Modifier,
    onControllerReady: (CameraController) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Using ImplementationMode.COMPATIBLE (TextureView) avoids SurfaceView
    // BufferQueue abandonment and tear-down crashes in Compose hierarchies
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val controller = remember { CameraController(context, previewView) }

    LaunchedEffect(lifecycleOwner) {
        controller.startCamera(lifecycleOwner) {
            onControllerReady(controller)
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            controller.unbind()
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
