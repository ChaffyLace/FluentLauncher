package com.example.fluentui.ui.system

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.example.fluentui.GlobalActionCenterActivity

/**
 * Service tournant en arrière-plan qui place une zone invisible de 70px en haut de l'écran.
 * Intercepte les "Swipe Down" pour lancer notre Action Center par-dessus n'importe quelle app.
 */
class GlobalTriggerService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var triggerView: View
    private lateinit var gestureDetector: GestureDetector

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 1. La zone invisible (transparente)
        triggerView = View(this).apply { setBackgroundColor(0x00000000) }

        // 2. Détection du Swipe Down
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                // Si on a bien un geste vers le bas assez rapide
                if (e1 != null && e2.y - e1.y > 50 && velocityY > 100) {
                    // SWIPE DÉTECTÉ -> On lance notre Action Center par-dessus l'app actuelle
                    val intent = Intent(this@GlobalTriggerService, GlobalActionCenterActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    }
                    startActivity(intent)
                    return true // On consomme le geste (Samsung ne le verra pas)
                }
                return false
            }
        })

        // On attache le détecteur à la vue
        triggerView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event); true }

        // 3. Configuration de la fenêtre système (Tout en haut, au-dessus de tout)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            70, // Hauteur de la zone d'interception (environ la taille d'une status bar native)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL }

        windowManager.addView(triggerView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::triggerView.isInitialized) {
            windowManager.removeView(triggerView)
        }
    }
}