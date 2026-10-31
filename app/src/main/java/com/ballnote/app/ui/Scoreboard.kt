package com.ballnote.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ballnote.app.domain.*

@Composable internal fun LargeScoreboard(g: Game,s: Score,close:()->Unit) {
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        val view=LocalView.current
        DisposableEffect(view) {view.keepScreenOn=true;onDispose {view.keepScreenOn=false}}
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp,Alignment.CenterVertically)) {
                val inning=if(g.chaseStarted) s.second else s.first
                Text(if(g.chaseStarted) g.nameB else g.nameA,style=MaterialTheme.typography.headlineMedium)
                BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
                    Text("${inning.runs}/${inning.wickets}",fontSize=(maxWidth.value/("${inning.runs}/${inning.wickets}".length)*1.3f).coerceIn(36f,140f).sp,fontWeight=FontWeight.Bold)
                }
                Text("${inning.overs} / ${g.rules.overs} overs",style=MaterialTheme.typography.headlineMedium)
                Text(s.result,style=MaterialTheme.typography.headlineSmall)
                if(g.chaseStarted) Text("Target ${s.target}",style=MaterialTheme.typography.titleLarge)
                Button(onClick=close) {Text("Back to scoring")}
            }
        }
    }
}
internal fun competitionImage(context: Context,text: String): Uri {
    val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {textSize=28f;color=Color.rgb(28,38,65)}
    val lines=text.lines().flatMap { line ->
        val result=mutableListOf<String>();var rest=line
        while(rest.length>0 && paint.measureText(rest)>936) { var n=rest.length;while(n>1 && paint.measureText(rest.take(n))>936) n--;result+=rest.take(n);rest=rest.drop(n) }
        result+rest
    }
    val bitmap=androidx.core.graphics.createBitmap(1000,lines.size*42+96)
    val canvas=Canvas(bitmap);canvas.drawColor(Color.rgb(250,248,255))
    lines.forEachIndexed {i,line->canvas.drawText(line,32f,56f+i*42,paint)}
    val directory=File(context.cacheDir,"results").apply {mkdirs()};val file=File(directory,"competition.png")
    try {file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}} finally {bitmap.recycle()}
    return FileProvider.getUriForFile(context,"${context.packageName}.files",file)
}
