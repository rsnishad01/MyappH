package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.data.PostEntity
import com.example.data.ReelVideo
import com.example.data.StoryEntity
import com.example.data.UserData

object ShareManager {
    private const val TAG = "ShareManager"

    fun copyToClipboard(context: Context, text: String, label: String = "HundredGram Link", toastMessage: String = "Link copied to clipboard! (लिंक कॉपी कर लिया गया)") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error copying to clipboard: ${e.message}", e)
            Toast.makeText(context, "Link: $text", Toast.LENGTH_LONG).show()
        }
    }

    fun sharePost(context: Context, post: PostEntity) {
        val shareLink = "https://hundredgram.app/p/${post.id}"
        val creatorName = post.authorDisplayName.ifBlank { post.authorUsername }
        val captionSnippet = if (post.caption.isNotBlank()) "\n${post.caption}\n" else ""
        val shareText = "$creatorName$captionSnippet\n$shareLink"
        
        // Also copy to clipboard for convenience
        copyToClipboard(context, shareLink, "Post Link", "Link copied! Opening share options... (लिंक कॉपी हुआ)")
        
        launchSystemShare(
            context = context,
            subject = "Post by $creatorName",
            text = shareText,
            chooserTitle = "Share Post via (पोस्ट शेयर करें)"
        )
    }

    fun shareReel(context: Context, reel: ReelVideo) {
        val shareLink = "https://hundredgram.app/reel/${reel.id}"
        val creatorName = reel.authorDisplayName.ifBlank { reel.authorUsername }
        val captionSnippet = if (reel.caption.isNotBlank()) "\n${reel.caption}\n" else ""
        val shareText = "$creatorName$captionSnippet\n$shareLink"
        
        copyToClipboard(context, shareLink, "Reel Link", "Link copied! Opening share options... (लिंक कॉपी हुआ)")

        launchSystemShare(
            context = context,
            subject = "Reel by $creatorName",
            text = shareText,
            chooserTitle = "Share Reel via (रील शेयर करें)"
        )
    }

    fun shareProfile(context: Context, user: UserData) {
        val name = user.displayName.ifBlank { user.fullName.ifBlank { user.username } }
        val shareLink = "https://hundredgram.app/u/${user.username}"
        val bioSnippet = if (user.bio.isNotBlank()) "\n${user.bio}\n" else ""
        val shareText = "$name$bioSnippet\n$shareLink"

        copyToClipboard(context, shareLink, "Profile Link", "Profile link copied! (प्रोफाइल लिंक कॉपी हो गया)")

        launchSystemShare(
            context = context,
            subject = "Profile - $name",
            text = shareText,
            chooserTitle = "Share Profile via (प्रोफाइल शेयर करें)"
        )
    }

    fun shareStory(context: Context, story: StoryEntity) {
        val shareLink = "https://hundredgram.app/story/${story.id}"
        val captionSnippet = if (story.caption.isNotBlank()) "\n${story.caption}\n" else ""
        val shareText = "${story.username}$captionSnippet\n$shareLink"

        copyToClipboard(context, shareLink, "Story Link", "Story link copied! (स्टोरी लिंक कॉपी हो गया)")

        launchSystemShare(
            context = context,
            subject = "Story by ${story.username}",
            text = shareText,
            chooserTitle = "Share Story via (स्टोरी शेयर करें)"
        )
    }

    private fun launchSystemShare(context: Context, subject: String, text: String, chooserTitle: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooserIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch share chooser: ${e.message}", e)
            Toast.makeText(context, "Unable to open share sheet: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
