package com.nj.parentguard.notification
import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class ChildNotificationListenerService:NotificationListenerService(){
    override fun onNotificationPosted(sbn:StatusBarNotification){
        if(sbn.packageName==packageName)return
        val e=sbn.notification.extras
        NotificationEventStore.save(
            sbn.packageName,
            e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            e.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            sbn.postTime
        )
    }
}
object NotificationEventStore{
    data class Event(val packageName:String,val title:String,val text:String,val postedAtEpochMs:Long)
    private val events=mutableListOf<Event>()
    fun save(packageName:String,title:String,text:String,postedAtEpochMs:Long){ synchronized(events){ events+=Event(packageName,title,text,postedAtEpochMs); if(events.size>1000)events.removeAt(0) } }
    fun all():List<Event> = synchronized(events){events.toList()}
}
