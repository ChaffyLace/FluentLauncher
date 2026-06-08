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
import com.example.fluentui.MainActivity

class SwipeDownService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var triggerView: View
    private lateinit var gestureDetector: GestureDetector

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 1. Création de la zone invisible
        triggerView = View(this).apply {
            setBackgroundColor(0x00000000) // 100% Transparent
        }

        // 2. Détection du Swipe Down
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null && e2.y - e1.y > 100 && velocityY > 100) {
                    // SWIPE DOWN DÉTECTÉ PARTOUT SUR LE TÉLÉPHONE !
                    openActionCenter()
                    return true
                }
                return false
            }
        })

        // On attache le détecteur à la zone invisible
        triggerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            // Retourner true consomme l'événement, false laisse passer le clic (si c'est juste un tap)
            true
        }

        // 3. Configuration de la fenêtre système
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            100, // Hauteur de la zone de détection (en pixels) en haut de l'écran
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }

        // Ajout de la vue à l'écran
        windowManager.addView(triggerView, params)
    }

    private fun openActionCenter() {
        // Lancer l'Activity de ton Action Center
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            // On ajoute une info pour dire à MainActivity d'ouvrir directement l'Action Center
            putExtra("OPEN_ACTION_CENTER", true)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::triggerView.isInitialized) {
            windowManager.removeView(triggerView)
        }
    }
}