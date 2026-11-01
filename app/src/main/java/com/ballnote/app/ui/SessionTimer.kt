@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.ballnote.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ballnote.app.AppModel
import com.ballnote.app.domain.*
import kotlinx.coroutines.delay

@Composable internal fun sessionTime(): Long {
    val now by produceState(System.currentTimeMillis()) {while(true) {delay(1000);value=System.currentTimeMillis()}}
    return now
}
internal fun timerStatus(timer: SessionTimer,now: Long): String = when {
    timer.pausedRemaining!=null -> "Paused · ${timerClock(timer.remaining(now))}"
    timer.remaining(now)==0L -> "Time ended"
    else -> "${timerClock(timer.remaining(now))} left"
}
@Composable internal fun TimerChip(label: String,timer: SessionTimer,open: ()->Unit) {
    val now=sessionTime()
    TextButton(onClick=open) {Text("$label · ${timerStatus(timer,now)}",color=if(timer.remaining(now)<=SessionTimer.TEN_MINUTES) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)}
}
@Composable internal fun TimerPanel(label: String,timer: SessionTimer?,busy: Boolean,scope: TimerScope,id: String,vm: AppModel) {
    var editing by rememberSaveable {mutableStateOf(false)}
    if(timer==null) TextButton(onClick={editing=true},enabled=!busy) {Text("Set $label")}
    else TimerChip(label,timer) {editing=true}
    if(editing) SessionTimerDialog(label,timer,busy,scope,id,vm) {editing=false}
}
@Composable internal fun SessionTimerDialog(label: String,timer: SessionTimer?,busy: Boolean,scope: TimerScope,id: String,vm: AppModel,dismiss: ()->Unit) {
    var minutes by rememberSaveable {mutableStateOf("60")}
    var warn by rememberSaveable {mutableStateOf(timer?.warnAtTenMinutes ?: true)}
    val now=sessionTime()
    val context=LocalContext.current
    var notifications by remember {mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {notifications=it}
    AlertDialog(onDismissRequest=dismiss,title={Text(label.replaceFirstChar {it.titlecase()})},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Booking time runs between innings and fixtures. You control when play ends.")
            timer?.let { current ->
                Text(timerStatus(current,now),style=MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(current.remaining(now)>0) OutlinedButton(onClick={vm.timerAction(scope,id,if(current.pausedRemaining==null) "Pause" else "Resume")},enabled=!busy) {Text(if(current.pausedRemaining==null) "Pause" else "Resume")}
                    OutlinedButton(onClick={vm.timerAction(scope,id,"Extend")},enabled=!busy && current.remaining(now)<=SessionTimer.MAX_DURATION-600_000L) {Text("+10 min")}
                }
                TextButton(onClick={vm.timerAction(scope,id,"Stop");dismiss()},enabled=!busy) {Text("Stop timer")}
                HorizontalDivider()
            }
            listOf(listOf(30,60),listOf(90,120)).forEach {row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {row.forEach {n->FilterChip(minutes==n.toString(),{minutes=n.toString()},label={Text("$n min")},modifier=Modifier.weight(1f))}}}
            OutlinedTextField(minutes,{if(it.length<=3 && it.all(Char::isDigit)) minutes=it},label={Text("Minutes (1–720)")},singleLine=true,keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())
            Row {Checkbox(warn,{warn=it});Text("Warn with 10 minutes left")}
            if(!notifications) TextButton(onClick={
                if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))
            }) {Text("Enable background notifications")}
            Text("Background notifications may be delayed by Android battery saving. The countdown stays accurate when you return.",style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={TextButton(onClick={vm.startTimer(scope,id,minutes.toInt(),warn);dismiss()},enabled=!busy && minutes.toIntOrNull() in 1..720) {Text(if(timer==null) "Start timer" else "Restart timer")}},dismissButton={TextButton(onClick=dismiss) {Text("Close")}})
}
