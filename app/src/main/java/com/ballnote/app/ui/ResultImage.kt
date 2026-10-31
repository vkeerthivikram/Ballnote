package com.ballnote.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import com.ballnote.app.domain.*
import java.io.File

internal fun resultFile(context: Context, g: Game, s: Score): Uri {
    val bitmap = createBitmap(1080, 1080)
    try {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(16, 29, 60))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans-serif", Typeface.BOLD) }
        fun text(value: String, x: Float, y: Float, size: Float, color: Int, maxWidth: Float = 920f) {
            paint.color = color; paint.textSize = size
            while (paint.measureText(value) > maxWidth && paint.textSize > 18f) paint.textSize -= 1f
            canvas.drawText(value, x, y, paint)
        }
        fun panel(top: Float, name: String, innings: InningsScore, winner: Boolean, played: Boolean) {
            paint.color = if (winner) Color.rgb(211, 246, 233) else Color.rgb(225, 233, 255)
            canvas.drawRoundRect(64f, top, 1016f, top + 220f, 28f, 28f, paint)
            val ink = Color.rgb(16, 36, 79)
            text(name, 96f, top + 62f, 42f, ink, if (winner) 700f else 850f)
            if (winner) text("WINNER", 858f, top + 58f, 24f, Color.rgb(0, 89, 70), 130f)
            if (played) {
                text("${innings.runs}/${innings.wickets}", 96f, top + 164f, 82f, ink, 540f)
                text("${innings.overs} overs", 720f, top + 156f, 34f, ink, 260f)
            } else text("Did not bat", 96f, top + 158f, 44f, ink)
        }
        text("Ballnote", 64f, 108f, 52f, Color.WHITE)
        text(date(g.created), 64f, 164f, 30f, Color.rgb(180, 197, 255))
        panel(220f, g.nameA, s.first, s.winner == g.teamA, true)
        panel(464f, g.nameB, s.second, s.winner == g.teamB, g.chaseStarted)
        paint.color = when {
            s.status == Status.ABANDONED -> Color.rgb(56, 65, 82)
            !s.finished -> Color.rgb(98, 68, 0)
            s.winner == null -> Color.rgb(82, 52, 112)
            else -> Color.rgb(48, 69, 118)
        }
        canvas.drawRoundRect(64f, 736f, 1016f, 958f, 28f, 28f, paint)
        text(statusLabel(s).uppercase(), 96f, 784f, 24f, Color.rgb(225, 233, 255))
        val resultPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 42f; typeface = paint.typeface }
        val layout = StaticLayout.Builder.obtain(s.result, 0, s.result.length, resultPaint, 856)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).setMaxLines(3)
            .setEllipsize(TextUtils.TruncateAt.END).build()
        canvas.withTranslation(96f, 812f) { layout.draw(this) }
        text("Tap. Score. Play.", 64f, 1032f, 28f, Color.rgb(180, 197, 255))
        val dir = File(context.cacheDir, "results").apply { mkdirs() }
        val file = File(dir, "${g.id}.png")
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    } finally { bitmap.recycle() }
}
