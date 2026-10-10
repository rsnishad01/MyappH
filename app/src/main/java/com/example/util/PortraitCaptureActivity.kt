package com.example.util

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import kotlin.concurrent.thread

/**
 * Custom CaptureActivity configured to display barcode/QR scanner in portrait orientation,
 * with an integrated UI overlay including a Gallery Image Picker button, Flashlight toggle,
 * and Close button.
 */
class PortraitCaptureActivity : CaptureActivity() {

    private var barcodeView: DecoratedBarcodeView? = null
    private var isTorchOn = false
    private var loadingOverlay: FrameLayout? = null

    companion object {
        private const val REQUEST_PICK_QR_IMAGE = 4096
    }

    override fun initializeContent(): DecoratedBarcodeView {
        val view = super.initializeContent()
        barcodeView = view
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupOverlayUI()
    }

    private fun setupOverlayUI() {
        val root = findViewById<ViewGroup>(android.R.id.content) ?: return
        val density = resources.displayMetrics.density

        fun dp(value: Int): Int = (value * density).toInt()

        val overlayContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // 1. Top Bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(44), dp(16), dp(16))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP
            }
        }

        // Close Button
        val closeButton = TextView(this).apply {
            text = "✕"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#80000000"))
            }
            setOnClickListener {
                finish()
            }
        }

        // Title
        val titleText = TextView(this).apply {
            text = "Scan QR Code"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            paint.isFakeBoldText = true
        }

        // Flash/Torch Button
        val flashButton = TextView(this).apply {
            text = "⚡"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#80000000"))
            }
            setOnClickListener {
                isTorchOn = !isTorchOn
                if (isTorchOn) {
                    barcodeView?.setTorchOn()
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#E1306C"))
                    }
                } else {
                    barcodeView?.setTorchOff()
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#80000000"))
                    }
                }
            }
        }

        topBar.addView(closeButton)
        topBar.addView(titleText)
        topBar.addView(flashButton)
        overlayContainer.addView(topBar)

        // 2. Bottom Controls Container
        val bottomContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(16), dp(24), dp(48))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
            }
        }

        // Subtitle instructions
        val instructionText = TextView(this).apply {
            text = "Point camera at a 100Gram QR code\nor select an image from gallery"
            textSize = 13f
            setTextColor(Color.parseColor("#D0D0D0"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }
        bottomContainer.addView(instructionText)

        // Gallery Upload Button
        val galleryButton = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(14), dp(24), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(28).toFloat()
                setColor(Color.parseColor("#E1306C")) // 100Gram Accent Pink
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                openGalleryPicker()
            }
        }

        val galleryIcon = TextView(this).apply {
            text = "🖼️"
            textSize = 18f
            setPadding(0, 0, dp(8), 0)
        }

        val galleryLabel = TextView(this).apply {
            text = "Upload from Gallery (गैलरी से QR चुनें)"
            textSize = 15f
            setTextColor(Color.WHITE)
            paint.isFakeBoldText = true
        }

        galleryButton.addView(galleryIcon)
        galleryButton.addView(galleryLabel)
        bottomContainer.addView(galleryButton)

        overlayContainer.addView(bottomContainer)

        // 3. Loading Indicator Overlay (hidden by default)
        loadingOverlay = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#99000000"))
            visibility = View.GONE

            val loadingBox = LinearLayout(this@PortraitCaptureActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
            }

            val spinner = ProgressBar(this@PortraitCaptureActivity).apply {
                isIndeterminate = true
            }

            val loadingText = TextView(this@PortraitCaptureActivity).apply {
                text = "Scanning image from gallery...\n(इमेज से QR कोड पढ़ा जा रहा है...)"
                textSize = 14f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, 0)
            }

            loadingBox.addView(spinner)
            loadingBox.addView(loadingText)
            addView(loadingBox)
        }

        loadingOverlay?.let { overlayContainer.addView(it) }

        root.addView(overlayContainer)
    }

    private fun openGalleryPicker() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = "image/*"
            }
        } else {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
        }
        startActivityForResult(
            Intent.createChooser(intent, "Select QR Code Image (QR इमेज चुनें)"),
            REQUEST_PICK_QR_IMAGE
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_QR_IMAGE && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                processPickedUri(uri)
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processPickedUri(uri: Uri) {
        loadingOverlay?.visibility = View.VISIBLE

        thread {
            val decodedContent = QrCodeDecoder.decodeQrFromUri(this, uri)
            runOnUiThread {
                loadingOverlay?.visibility = View.GONE
                if (!decodedContent.isNullOrBlank()) {
                    Toast.makeText(this, "QR Code scanned successfully!", Toast.LENGTH_SHORT).show()
                    val resultIntent = Intent().apply {
                        putExtra(com.google.zxing.client.android.Intents.Scan.RESULT, decodedContent)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "No QR code found in selected image (चुनी गई इमेज में QR कोड नहीं मिला)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
