//package com.duke.orca.android.kotlin.lockscreencalendar.lockscreen.service
//
//import android.app.Service
//
//import android.app.*
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.os.Build
//import android.os.IBinder
//import android.provider.Settings
//import androidx.localbroadcastmanager.content.LocalBroadcastManager
//import com.duke.orca.android.kotlin.lockscreencalendar.PACKAGE_NAME
//import com.duke.orca.android.kotlin.lockscreencalendar.lockscreen.blindscreen.BlindScreenPresenter
//import com.duke.orca.android.kotlin.lockscreencalendar.main.views.MainActivity
//import com.duke.orca.android.kotlin.lockscreencalendar.permission.PermissionChecker
//import io.reactivex.android.schedulers.AndroidSchedulers
//import io.reactivex.disposables.Disposable
//import io.reactivex.schedulers.Schedulers
//import timber.log.Timber
//
//class LockScreenService : Service() {
//    object Action {
//        private const val PREFIX = "$PACKAGE_NAME.lockscreen.service.LockScreenService.Action"
//        const val HOME_KEY_PRESSED = "$PREFIX.HOME_KEY_PRESSED"
//        const val MAIN_ACTIVITY_DESTROYED = "$PREFIX.MAIN_ACTIVITY_DESTROYED"
//        const val RECENT_APPS_PRESSED = "$PREFIX.RECENT_APPS_PRESSED"
//        const val STOP_SELF = "$PREFIX.STOP_SELF"
//    }
//
//    private val blindScreenPresenter = BlindScreenPresenter(applicationContext)
//    private val localBroadcastManager: LocalBroadcastManager by lazy {
//        LocalBroadcastManager.getInstance(this)
//    }
//
//    private val notificationManager: NotificationManager by lazy {
//        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//    }
//
//    private var disposable: Disposable? = null
//
//    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            val showOnLockScreen = Preference.LockScreen.getShowOnLockScreen(context)
//            val displayAfterUnlocking = Preference.LockScreen.getShowAfterUnlocking(context)
//
//            when (intent.action) {
//                Action.MAIN_ACTIVITY_DESTROYED -> {
//                    if (Settings.canDrawOverlays(context).not()) {
//                        ManageOverlayPermissionNotificationBuilder.create(context).build().run {
//                            notificationManager.notify(ManageOverlayPermissionNotificationBuilder.ID, this)
//                        }
//                    }
//                }
//                Action.STOP_SELF -> {
//                    stopSelf()
//                    notificationManager.cancel(NotificationBuilder.ID)
//                }
//                Intent.ACTION_SCREEN_OFF -> {
//                    if (showOnLockScreen.not() || displayAfterUnlocking)
//                        return
//
//                    Intent(context, MainActivity::class.java).apply {
//                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
//                        startActivity(this)
//                    }
//                }
//                Intent.ACTION_USER_PRESENT -> {
//                    if (showOnLockScreen.not() || displayAfterUnlocking.not())
//                        return
//
//                    Intent(context, MainActivity::class.java).apply {
//                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
//                        startActivity(this)
//                    }
//                }
//                else -> {
//                    // pass
//                }
//            }
//        }
//    }
//
//    private val bottomNavigationBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent) {
//            when (intent.action) {
//                Action.HOME_KEY_PRESSED, Action.RECENT_APPS_PRESSED -> {
//                    if (Settings.canDrawOverlays(context)) {
//                        blindScreenPresenter.show()
//                    }
//                }
//                else -> {
//                    // pass
//                }
//            }
//        }
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//
//        localBroadcastManager.registerReceiver(bottomNavigationBroadcastReceiver, IntentFilter().apply {
//            addAction(Action.HOME_KEY_PRESSED)
//            addAction(Action.RECENT_APPS_PRESSED)
//        })
//
//        registerReceiver(
//            broadcastReceiver,
//            IntentFilter().apply {
//                addAction(Action.MAIN_ACTIVITY_DESTROYED)
//                addAction(Action.STOP_SELF)
//                addAction(Intent.ACTION_SCREEN_OFF)
//                addAction(Intent.ACTION_USER_PRESENT)
//            }
//        )
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        disposable = NotificationBuilder.single(this, notificationManager)
//            .observeOn(Schedulers.io())
//            .subscribeOn(AndroidSchedulers.mainThread())
//            .subscribe { it ->
//                startForeground(NotificationBuilder.ID, it.build())
//            }
//
//        return START_STICKY
//    }
//
//    override fun onDestroy() {
//        try {
//            blindScreenPresenter.hide()
//            localBroadcastManager.unregisterReceiver(bottomNavigationBroadcastReceiver)
//            unregisterReceiver(broadcastReceiver)
//        } catch (e: IllegalArgumentException) {
//            Timber.e(e)
//        } finally {
//            disposable?.dispose()
//        }
//
//        blindScreenPresenter.isBlindScreenVisible = false
//
//        super.onDestroy()
//    }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//}