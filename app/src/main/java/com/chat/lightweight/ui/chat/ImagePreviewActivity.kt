package com.chat.lightweight.ui.chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.chat.lightweight.databinding.ActivityImagePreviewBinding

/**
 * 图片预览Activity
 */
class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePreviewBinding

    companion object {
        const val EXTRA_IMAGE_URL = "image_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)
        if (imageUrl != null) {
            binding.imageView.load(imageUrl) {
                crossfade(true)
            }
        } else {
            finish()
        }

        binding.root.setOnClickListener {
            finish()
        }
    }
}
