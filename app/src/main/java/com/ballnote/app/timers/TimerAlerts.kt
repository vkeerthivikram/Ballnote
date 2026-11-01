package com.ballnote.app.timers

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ballnote.app.MainActivity
import com.ballnote.app.R
import com.ballnote.app.data.Repository
import com.ballnote.app.data.sessionTimers
import com.ballnote.app.domain.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Inexact background reminders; the persisted deadline drives the accurate in-app clock. */
object TimerAlerts {
    private const val CHANNEL="turf_sessions"
    private fun pending(context: Context,ref: TimerRef,phase: String): PendingIntent {
        val intent=Intent(context,TimerReceiver::class.java).apply {
            data="ballnote://timer/${ref.scope.name}/${ref.ownerId}/$phase".toUri()
            putExtra("scope",ref.scope.name);putExtra("owner",ref.ownerId);putExtra("revision",ref.timer.id)
        }
        return PendingIntent.getBroadcast(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    fun sync(context: Context,before: List<TimerRef>,after: List<TimerRef>) {
        val alarms=context.getSystemService(AlarmManager::class.java)
        val now=System.currentTimeMillis()
        before.filter {old->after.none {it==old}}.forEach {ref->listOf("warning","expiry").forEach {alarms.cancel(pending(context,ref,it))};if(after.none {it.key==ref.key && it.timer.id==ref.timer.id}) NotificationManagerCompat.from(context).cancel(ref.key,0)}
        after.filter {ref->ref !in before && ref.timer.pausedRemaining==null}.forEach {ref->
            if(!ref.timer.expiryDelivered) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,maxOf(now,ref.timer.endsAt),pending(context,ref,"expiry"))
            if(ref.timer.warnAtTenMinutes && !ref.timer.warningDelivered && ref.timer.endsAt>now) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,maxOf(now,ref.timer.endsAt-SessionTimer.TEN_MINUTES),pending(context,ref,"warning"))
        }
    }
    fun notify(context: Context,ref: TimerRef,alert: TimerAlert) {
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,"Turf session timers",NotificationManager.IMPORTANCE_DEFAULT))
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return
        val content=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(context,CHANNEL).setSmallIcon(R.drawable.ic_ballnote)
            .setContentTitle(if(alert==TimerAlert.EXPIRED) "Turf session ended" else "10 minutes of turf time left")
            .setContentText(ref.label).setStyle(NotificationCompat.BigTextStyle().bigText(ref.message(alert)))
            .setContentIntent(content).setAutoCancel(true).build()
        NotificationManagerCompat.from(context).notify(ref.key,0,notification)
    }
    suspend fun deliver(context: Context,repo: Repository,ref: TimerRef,now: Long): String? {
        val alert=repo.claimTimerAlert(ref,now) ?: return null
        runCatching {notify(context,ref,alert)}.onFailure {android.util.Log.w("BallnoteTimer","Could not post timer notification",it)}
        return ref.message(alert)
    }
}
class TimerReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        val pending=goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val repo=Repository(context.applicationContext)
            try {
                val refs=repo.snapshot().sessionTimers()
                if(intent.action==Intent.ACTION_BOOT_COMPLETED) TimerAlerts.sync(context,emptyList(),refs)
                else refs.find {it.scope.name==intent.getStringExtra("scope") && it.ownerId==intent.getStringExtra("owner") && it.timer.id==intent.getStringExtra("revision")}?.let {TimerAlerts.deliver(context,repo,it,System.currentTimeMillis())}
            } catch(e: Exception) {android.util.Log.w("BallnoteTimer","Could not process timer reminder",e)}
            finally {repo.close();pending.finish()}
        }
    }
}
