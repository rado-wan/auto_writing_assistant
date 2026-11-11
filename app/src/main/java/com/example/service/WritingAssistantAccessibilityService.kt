package com.example.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.data.local.AppDatabase
import com.example.data.model.SavedText
import com.example.data.preferences.AppPreferences
import com.example.util.FormatEngine
import com.example.util.FormattingHelper
import com.example.util.VariableParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.hypot

class WritingAssistantAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var windowManager: WindowManager
    private lateinit var preferences: AppPreferences
    private lateinit var database: AppDatabase

    private var buttonView: FrameLayout? = null
    private var buttonParams: WindowManager.LayoutParams? = null
    private var isButtonAttached = false

    private var menuCardView: LinearLayout? = null
    private var menuParams: WindowManager.LayoutParams? = null
    private var isMenuAttached = false

    private var cachedSavedTexts: List<SavedText> = emptyList()
    private var savedTextsJob: Job? = null

    // Debounce runner for hide events (400 ms debounce to prevent keyboard flicker closures)
    private var hideDebounceRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        preferences = AppPreferences.getInstance(this)
        database = AppDatabase.getDatabase(this)

        savedTextsJob = serviceScope.launch(Dispatchers.IO) {
            database.savedTextDao().getAllFlow().collectLatest { list ->
                withContext(Dispatchers.Main) {
                    cachedSavedTexts = list
                    if (isMenuAttached) {
                        refreshMenuContents()
                        updateMenuPosition()
                    }
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // 1. Ignore accessibility events from the app's own package so overlay/menu never closes on self-events
        val eventPackage = event.packageName?.toString() ?: ""
        if (eventPackage == packageName) {
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> handleFocusEvent(event)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleWindowStateChanged(event)
        }
    }

    private fun handleFocusEvent(event: AccessibilityEvent) {
        try {
            val packageName = event.packageName?.toString() ?: ""
            val settings = preferences.currentSnapshot

            if (!settings.isMasterEnabled || !settings.isAppAllowed(packageName)) {
                scheduleDebouncedHide()
                return
            }

            val source = event.source
            try {
                if (source != null && source.isEditable && !source.isPassword) {
                    cancelDebouncedHide()
                    showFloatingButton()
                    if (isMenuAttached) {
                        updateMenuPosition()
                    }
                } else {
                    scheduleDebouncedHide()
                }
            } finally {
                try { source?.recycle() } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            scheduleDebouncedHide()
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        try {
            val packageName = event.packageName?.toString() ?: ""
            val settings = preferences.currentSnapshot

            if (!settings.isMasterEnabled || !settings.isAppAllowed(packageName)) {
                scheduleDebouncedHide()
                return
            }

            // Recalculate menu position when keyboard shows or hides
            if (isMenuAttached) {
                updateMenuPosition()
            }

            // Check if any field is currently focused in active window
            var root: AccessibilityNodeInfo? = null
            var focused: AccessibilityNodeInfo? = null
            try {
                root = rootInActiveWindow
                if (root != null) {
                    focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    if (focused != null && focused.isEditable && !focused.isPassword) {
                        cancelDebouncedHide()
                        showFloatingButton()
                        return
                    }
                }
                scheduleDebouncedHide()
            } finally {
                try { focused?.recycle() } catch (_: Exception) {}
                try { root?.recycle() } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            scheduleDebouncedHide()
        }
    }

    /**
     * Debounce keyboard visible/hidden and focus transitions by ~400 ms.
     * Prevents brief IME transitions or touch events from abruptly hiding button or menu.
     */
    private fun scheduleDebouncedHide() {
        if (isMenuAttached) {
            // Never close overlay while user is interacting with open menu
            return
        }
        cancelDebouncedHide()
        val runnable = Runnable {
            if (!isMenuAttached && !isAnyFieldFocused()) {
                hideFloatingButton()
            }
        }
        hideDebounceRunnable = runnable
        mainHandler.postDelayed(runnable, 400L)
    }

    private fun cancelDebouncedHide() {
        hideDebounceRunnable?.let {
            mainHandler.removeCallbacks(it)
            hideDebounceRunnable = null
        }
    }

    private fun isAnyFieldFocused(): Boolean {
        var root: AccessibilityNodeInfo? = null
        var focused: AccessibilityNodeInfo? = null
        try {
            root = rootInActiveWindow ?: return false
            focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            return focused != null && focused.isEditable && !focused.isPassword
        } catch (_: Exception) {
            return false
        } finally {
            try { focused?.recycle() } catch (_: Exception) {}
            try { root?.recycle() } catch (_: Exception) {}
        }
    }

    private fun showFloatingButton() {
        cancelDebouncedHide()
        if (isButtonAttached) {
            buttonView?.visibility = View.VISIBLE
            return
        }

        initButtonView()
        val params = getButtonLayoutParams()
        try {
            windowManager.addView(buttonView, params)
            isButtonAttached = true
            buttonView?.visibility = View.VISIBLE
        } catch (_: Exception) {
            isButtonAttached = false
        }
    }

    private fun hideFloatingButton() {
        cancelDebouncedHide()
        closeMenu(animated = false)
        if (isButtonAttached && buttonView != null) {
            buttonView?.visibility = View.GONE
        }
    }

    private fun removeOverlayViews() {
        cancelDebouncedHide()
        closeMenu(animated = false)
        if (isButtonAttached && buttonView != null) {
            try {
                windowManager.removeView(buttonView)
            } catch (_: Exception) {}
            isButtonAttached = false
            buttonView = null
        }
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
    }

    /**
     * Determines visible screen height above the keyboard if present.
     */
    private fun getVisibleScreenHeight(): Int {
        val displayHeight = resources.displayMetrics.heightPixels
        try {
            val root = rootInActiveWindow
            if (root != null) {
                val bounds = Rect()
                root.getBoundsInScreen(bounds)
                try { root.recycle() } catch (_: Exception) {}
                if (bounds.bottom in (displayHeight / 3)..displayHeight) {
                    return bounds.bottom
                }
            }
        } catch (_: Exception) {}
        return displayHeight
    }

    /**
     * Non-focusable button window so it never steals input focus from keyboard or host field.
     */
    private fun getButtonLayoutParams(): WindowManager.LayoutParams {
        val settings = preferences.currentSnapshot
        val sizePx = dpToPx(settings.buttonSizeDp.toFloat())
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val initialY = (screenHeight * settings.buttonPositionRatioY).toInt()
            .coerceIn(dpToPx(50f), screenHeight - dpToPx(100f))
        val initialX = if (settings.buttonSide == "left") dpToPx(16f) else screenWidth - sizePx - dpToPx(16f)

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }
        buttonParams = params
        return params
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initButtonView() {
        val settings = preferences.currentSnapshot
        val sizePx = dpToPx(settings.buttonSizeDp.toFloat())

        val button = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb((settings.buttonOpacity * 255).toInt().coerceIn(50, 255), 30, 96, 145))
                setStroke(dpToPx(1.5f), Color.argb(180, 255, 255, 255))
            }
            background = bg
            elevation = dpToPx(8f).toFloat()

            val icon = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_edit)
                setColorFilter(Color.WHITE)
                val pad = (sizePx * 0.25f).toInt()
                setPadding(pad, pad, pad, pad)
            }
            addView(icon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        buttonView = button

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        button.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var initialParamX = 0
            private var initialParamY = 0
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val params = buttonParams ?: return false
                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        initialParamX = params.x
                        initialParamY = params.y
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY
                        if (!isDragging && hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                            isDragging = true
                        }
                        if (isDragging) {
                            params.x = (initialParamX + dx).toInt().coerceIn(0, screenWidth - sizePx)
                            params.y = (initialParamY + dy).toInt().coerceIn(
                                dpToPx(40f),
                                screenHeight - dpToPx(100f)
                            )
                            if (isButtonAttached && buttonView != null) {
                                windowManager.updateViewLayout(buttonView, params)
                            }
                            // Recalculate menu position dynamically as button is dragged
                            if (isMenuAttached) {
                                updateMenuPosition()
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            toggleMenu()
                        } else {
                            snapButtonToEdge(params)
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun snapButtonToEdge(params: WindowManager.LayoutParams) {
        val settings = preferences.currentSnapshot
        val sizePx = dpToPx(settings.buttonSizeDp.toFloat())
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val snapLeft = params.x + (sizePx / 2) < (screenWidth / 2)
        val finalX = if (snapLeft) dpToPx(16f) else screenWidth - sizePx - dpToPx(16f)
        params.x = finalX

        val ratioY = (params.y.toFloat() / screenHeight.coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
        val side = if (snapLeft) "left" else "right"

        if (isButtonAttached && buttonView != null) {
            windowManager.updateViewLayout(buttonView, params)
        }

        // Recalculate menu position after snap
        if (isMenuAttached) {
            updateMenuPosition()
        }

        serviceScope.launch(Dispatchers.IO) {
            preferences.setButtonPositionRatioY(ratioY)
            preferences.setButtonSide(side)
        }
    }

    private fun toggleMenu() {
        if (isMenuAttached) {
            closeMenu(animated = true)
        } else {
            openMenu()
        }
    }

    private data class MenuPositionResult(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val openBelow: Boolean,
        val isRight: Boolean
    )

    /**
     * Anchors the menu directly next to the button's current on-screen position:
     * - Gap: 8dp
     * - Vertical: if more room below, menuTop = buttonBottom + 8dp. Otherwise menuBottom = buttonTop - 8dp.
     * - Horizontal: align right edges if button on right; align left edges if button on left. Clamped with 8dp screen margin.
     * - Height reduced to available space if menu is taller than space.
     */
    private fun calculateMenuPosition(menuView: View): MenuPositionResult {
        val btnParams = buttonParams ?: return MenuPositionResult(0, 0, dpToPx(270f), WindowManager.LayoutParams.WRAP_CONTENT, true, true)
        val settings = preferences.currentSnapshot

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = getVisibleScreenHeight()

        val gapPx = dpToPx(8f)
        val marginPx = dpToPx(8f)
        val menuWidth = dpToPx(270f).coerceAtMost(screenWidth - (marginPx * 2))

        val buttonX = btnParams.x
        val buttonY = btnParams.y
        val buttonWidth = buttonView?.width?.takeIf { it > 0 } ?: btnParams.width
        val buttonHeight = buttonView?.height?.takeIf { it > 0 } ?: btnParams.height
        val buttonBottom = buttonY + buttonHeight
        val buttonTop = buttonY

        // Vertical calculation
        val spaceBelow = screenHeight - (buttonBottom + gapPx) - marginPx
        val spaceAbove = (buttonTop - gapPx) - marginPx

        val openBelow = spaceBelow >= spaceAbove
        val maxAvailableHeight = if (openBelow) spaceBelow else spaceAbove
        val userMaxHeight = (screenHeight * (settings.menuMaxHeightPercent / 100f)).toInt()
        val maxAllowedHeight = maxAvailableHeight.coerceAtMost(userMaxHeight).coerceAtLeast(dpToPx(120f))

        // Measure menu to find desired height
        menuView.measure(
            View.MeasureSpec.makeMeasureSpec(menuWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(maxAllowedHeight, View.MeasureSpec.AT_MOST)
        )
        val measuredHeight = menuView.measuredHeight.coerceAtMost(maxAllowedHeight)

        val targetY = if (openBelow) {
            buttonBottom + gapPx
        } else {
            (buttonTop - gapPx - measuredHeight).coerceAtLeast(marginPx)
        }

        // Horizontal calculation
        val isRight = (buttonX + buttonWidth / 2) > (screenWidth / 2)
        val targetX = if (isRight) {
            val idealX = (buttonX + buttonWidth) - menuWidth
            idealX.coerceIn(marginPx, screenWidth - menuWidth - marginPx)
        } else {
            val idealX = buttonX
            idealX.coerceIn(marginPx, screenWidth - menuWidth - marginPx)
        }

        return MenuPositionResult(
            x = targetX,
            y = targetY,
            width = menuWidth,
            height = measuredHeight,
            openBelow = openBelow,
            isRight = isRight
        )
    }

    private fun updateMenuPosition() {
        val menu = menuCardView ?: return
        if (!isMenuAttached) return
        val pos = calculateMenuPosition(menu)
        val params = menuParams ?: return

        params.x = pos.x
        params.y = pos.y
        params.width = pos.width
        params.height = pos.height

        // Update pivot so animations anchor to the button corner
        menu.pivotX = if (pos.isRight) pos.width.toFloat() else 0f
        menu.pivotY = if (pos.openBelow) 0f else pos.height.toFloat()

        try {
            windowManager.updateViewLayout(menu, params)
        } catch (_: Exception) {}
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun openMenu() {
        if (isMenuAttached) return
        buttonParams ?: return

        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(18f).toFloat()
                setColor(Color.argb(250, 15, 23, 42))
                setStroke(dpToPx(1f), Color.argb(60, 255, 255, 255))
            }
            background = bg
            elevation = dpToPx(16f).toFloat()
            setPadding(dpToPx(12f), dpToPx(12f), dpToPx(12f), dpToPx(12f))
        }
        menuCardView = menu

        refreshMenuContents()

        // Calculate position anchored right next to the button
        val pos = calculateMenuPosition(menu)

        // Menu is non-focusable to prevent keyboard closure and IME flicker
        val params = WindowManager.LayoutParams(
            pos.width,
            pos.height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = pos.x
            y = pos.y
        }
        menuParams = params

        menu.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                closeMenu(animated = true)
                true
            } else false
        }

        // Set pivot point to the corner closest to the button
        menu.pivotX = if (pos.isRight) pos.width.toFloat() else 0f
        menu.pivotY = if (pos.openBelow) 0f else pos.height.toFloat()

        // Initial scale and fade values for attached launch animation
        menu.alpha = 0f
        menu.scaleX = 0.8f
        menu.scaleY = 0.8f

        try {
            windowManager.addView(menu, params)
            isMenuAttached = true

            menu.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(160L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } catch (_: Exception) {
            isMenuAttached = false
        }
    }

    private fun closeMenu(animated: Boolean = true) {
        val menu = menuCardView ?: return
        if (!isMenuAttached) return
        isMenuAttached = false
        menuCardView = null
        menuParams = null

        if (animated) {
            menu.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(120L)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    try {
                        windowManager.removeView(menu)
                    } catch (_: Exception) {}
                }
                .start()
        } else {
            try {
                windowManager.removeView(menu)
            } catch (_: Exception) {}
        }
    }

    private fun refreshMenuContents() {
        val menu = menuCardView ?: return
        menu.removeAllViews()

        val settings = preferences.currentSnapshot
        val cal = Calendar.getInstance()

        // Available vertical space for the inner scrollview
        val btnParams = buttonParams
        val screenHeight = getVisibleScreenHeight()
        val gapPx = dpToPx(8f)
        val marginPx = dpToPx(8f)
        val buttonY = btnParams?.y ?: (screenHeight / 2)
        val buttonHeight = buttonView?.height?.takeIf { it > 0 } ?: (btnParams?.height ?: dpToPx(40f))
        val buttonBottom = buttonY + buttonHeight
        val spaceBelow = screenHeight - (buttonBottom + gapPx) - marginPx
        val spaceAbove = (buttonY - gapPx) - marginPx
        val maxAvailableHeight = if (spaceBelow >= spaceAbove) spaceBelow else spaceAbove
        val userMaxHeight = (screenHeight * (settings.menuMaxHeightPercent / 100f)).toInt()
        val maxAllowedHeight = maxAvailableHeight.coerceAtMost(userMaxHeight).coerceAtLeast(dpToPx(120f))
        val maxListHeight = (maxAllowedHeight - dpToPx(160f)).coerceAtLeast(dpToPx(60f))

        // 1. Fixed Top Items: Date, Time, Day with per-preset bold and underline styling
        val dateFormatted = FormatEngine.formatWithStyle(
            pattern = settings.defaultDateFormat,
            calendar = cal,
            options = settings.getDateFormatOptions(),
            boldPresets = settings.formatBoldPresets,
            underlinePresets = settings.formatUnderlinePresets
        )
        val isDateStyled = settings.isPresetBold(settings.defaultDateFormat) ||
                settings.isPresetUnderline(settings.defaultDateFormat) ||
                dateFormatted.contains("<b>") || dateFormatted.contains("<u>")
        val datePreview = FormattingHelper.stripHtml(dateFormatted)
        menu.addView(createMenuItem("📅 Date", datePreview) {
            val clip = FormattingHelper.createClipData(
                label = "Writing Assistant Date",
                bodyWithVariables = dateFormatted,
                isFormattingEnabled = isDateStyled,
                formattingStyle = settings.formattingStyle
            )
            insertClipDataIntoField(clip)
        })

        val timeFormatted = FormatEngine.formatWithStyle(
            pattern = settings.defaultTimeFormat,
            calendar = cal,
            options = settings.getTimeFormatOptions(),
            boldPresets = settings.formatBoldPresets,
            underlinePresets = settings.formatUnderlinePresets
        )
        val isTimeStyled = settings.isPresetBold(settings.defaultTimeFormat) ||
                settings.isPresetUnderline(settings.defaultTimeFormat) ||
                timeFormatted.contains("<b>") || timeFormatted.contains("<u>")
        val timePreview = FormattingHelper.stripHtml(timeFormatted)
        menu.addView(createMenuItem("⏰ Time", timePreview) {
            val clip = FormattingHelper.createClipData(
                label = "Writing Assistant Time",
                bodyWithVariables = timeFormatted,
                isFormattingEnabled = isTimeStyled,
                formattingStyle = settings.formattingStyle
            )
            insertClipDataIntoField(clip)
        })

        val dayFormatted = FormatEngine.formatWithStyle(
            pattern = settings.defaultDayFormat,
            calendar = cal,
            options = settings.getDayFormatOptions(),
            boldPresets = settings.formatBoldPresets,
            underlinePresets = settings.formatUnderlinePresets
        )
        val isDayStyled = settings.isPresetBold(settings.defaultDayFormat) ||
                settings.isPresetUnderline(settings.defaultDayFormat) ||
                dayFormatted.contains("<b>") || dayFormatted.contains("<u>")
        val dayPreview = FormattingHelper.stripHtml(dayFormatted)
        menu.addView(createMenuItem("🗓️ Day", dayPreview) {
            val clip = FormattingHelper.createClipData(
                label = "Writing Assistant Day",
                bodyWithVariables = dayFormatted,
                isFormattingEnabled = isDayStyled,
                formattingStyle = settings.formattingStyle
            )
            insertClipDataIntoField(clip)
        })

        // 2. Collapsible Header Row for Saved Texts
        val isCollapsed = settings.isSavedTextsCollapsed
        val savedHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8f), dpToPx(8f), dpToPx(8f), dpToPx(8f))

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8f).toFloat()
                setColor(Color.argb(25, 255, 255, 255))
            }
            background = bg
            isClickable = true
        }

        val headerTitle = TextView(this).apply {
            text = "Saved texts (${cachedSavedTexts.size})"
            setTextColor(Color.argb(220, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val arrowIndicator = TextView(this).apply {
            text = if (isCollapsed) "▼" else "▲"
            setTextColor(Color.argb(180, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        savedHeader.addView(headerTitle)
        savedHeader.addView(arrowIndicator)

        val headerMargin = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, dpToPx(6f), 0, dpToPx(4f))
        }
        savedHeader.layoutParams = headerMargin

        savedHeader.setOnClickListener {
            val nextState = !isCollapsed
            serviceScope.launch(Dispatchers.IO) {
                preferences.setSavedTextsCollapsed(nextState)
            }
            refreshMenuContents()
            updateMenuPosition()
        }
        menu.addView(savedHeader)

        // 3. Scrollable List of Saved Texts (when expanded)
        if (!isCollapsed && cachedSavedTexts.isNotEmpty()) {
            val scrollView = object : ScrollView(this) {
                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                    val customHeightSpec = MeasureSpec.makeMeasureSpec(maxListHeight, MeasureSpec.AT_MOST)
                    super.onMeasure(widthMeasureSpec, customHeightSpec)
                }
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                isVerticalScrollBarEnabled = true
            }

            val listContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            for (item in cachedSavedTexts) {
                val itemView = createSavedTextMenuItem(item) {
                    val parsed = VariableParser.parse(
                        template = item.body,
                        defaultDateFormat = settings.defaultDateFormat,
                        defaultDayFormat = settings.defaultDayFormat,
                        defaultTimeFormat = settings.defaultTimeFormat,
                        dateOptions = settings.getDateFormatOptions(),
                        dayOptions = settings.getDayFormatOptions(),
                        timeOptions = settings.getTimeFormatOptions(),
                        calendar = Calendar.getInstance(),
                        boldPresets = settings.formatBoldPresets,
                        underlinePresets = settings.formatUnderlinePresets
                    )
                    val isStyled = item.isFormattingEnabled ||
                            parsed.text.contains("<b>") ||
                            parsed.text.contains("<u>")
                    val clip = FormattingHelper.createClipData(
                        label = item.title,
                        bodyWithVariables = parsed.text,
                        isFormattingEnabled = isStyled,
                        formattingStyle = settings.formattingStyle
                    )
                    insertClipDataIntoField(clip)
                }
                listContainer.addView(itemView)
            }

            scrollView.addView(listContainer)
            menu.addView(scrollView)
        } else if (!isCollapsed && cachedSavedTexts.isEmpty()) {
            val emptyNotice = TextView(this).apply {
                text = "No saved texts yet"
                setTextColor(Color.argb(120, 255, 255, 255))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setPadding(dpToPx(8f), dpToPx(8f), dpToPx(8f), dpToPx(8f))
            }
            menu.addView(emptyNotice)
        }
    }

    private fun createMenuItem(label: String, value: String, onClick: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8f), dpToPx(7f), dpToPx(8f), dpToPx(7f))

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10f).toFloat()
                setColor(Color.argb(30, 255, 255, 255))
            }
            background = bg

            setOnClickListener {
                closeMenu(animated = true)
                onClick()
            }
        }

        val labelView = TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val valueView = TextView(this).apply {
            text = value
            setTextColor(Color.argb(220, 82, 182, 154))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }

        row.addView(labelView)
        row.addView(valueView)

        val marginParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, dpToPx(2.5f), 0, dpToPx(2.5f))
        }
        row.layoutParams = marginParams
        return row
    }

    private fun createSavedTextMenuItem(item: SavedText, onClick: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(8f), dpToPx(6f), dpToPx(8f), dpToPx(6f))

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8f).toFloat()
                setColor(Color.argb(20, 255, 255, 255))
            }
            background = bg

            setOnClickListener {
                closeMenu(animated = true)
                onClick()
            }
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleView = TextView(this).apply {
            text = item.title
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 1
        }
        headerRow.addView(titleView)

        if (item.isFormattingEnabled) {
            val formatBadge = TextView(this).apply {
                text = "B/U"
                setTextColor(Color.argb(200, 82, 182, 154))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
                setPadding(dpToPx(4f), dpToPx(1f), dpToPx(4f), dpToPx(1f))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(4f).toFloat()
                    setColor(Color.argb(40, 82, 182, 154))
                }
            }
            headerRow.addView(formatBadge)
        }

        val plainSnippet = FormattingHelper.stripHtml(item.body).replace("\n", " ")
        val snippetView = TextView(this).apply {
            text = plainSnippet
            setTextColor(Color.argb(150, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            maxLines = 2
        }

        row.addView(headerRow)
        row.addView(snippetView)

        val marginParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, dpToPx(2f), 0, dpToPx(2f))
        }
        row.layoutParams = marginParams
        return row
    }

    /**
     * Real paste mechanism:
     * - Puts clip on clipboard (HTML rich text or Unicode styled or plain text)
     * - Finds the focused input with findFocus(FOCUS_INPUT)
     * - Calls node.performAction(ACTION_PASTE) at cursor
     * - Restores previous clipboard after 300ms
     * - Fallback: toast "Copied, paste manually"
     */
    private fun insertClipDataIntoField(clip: ClipData) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val previousClip: ClipData? = try {
            clipboard.primaryClip
        } catch (_: Exception) {
            null
        }

        var rootNode: AccessibilityNodeInfo? = null
        var focusedNode: AccessibilityNodeInfo? = null
        var pasteSucceeded = false

        try {
            clipboard.setPrimaryClip(clip)

            rootNode = try { rootInActiveWindow } catch (_: Exception) { null }
            focusedNode = try { rootNode?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) } catch (_: Exception) { null }

            if (focusedNode != null && focusedNode.isEditable && !focusedNode.isPassword) {
                pasteSucceeded = try {
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                } catch (_: Exception) {
                    false
                }

                if (!pasteSucceeded) {
                    val pkg = focusedNode.packageName?.toString() ?: ""
                    val cls = focusedNode.className?.toString() ?: ""
                    val isKeep = pkg.contains("keep", ignoreCase = true)
                    val isPlainEditText = cls == "android.widget.EditText"

                    if (!isKeep && isPlainEditText) {
                        try {
                            val textToInsert = clip.getItemAt(0)?.text?.toString() ?: ""
                            val rawText = focusedNode.text?.toString() ?: ""
                            val isHint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                try { focusedNode.isShowingHintText } catch (_: Exception) { false }
                            } else false
                            val existingText = if (isHint) "" else rawText
                            var selStart = if (isHint) 0 else try { focusedNode.textSelectionStart } catch (_: Exception) { -1 }
                            var selEnd = if (isHint) 0 else try { focusedNode.textSelectionEnd } catch (_: Exception) { -1 }
                            if (selStart < 0 || selEnd < 0 || selStart > existingText.length || selEnd > existingText.length || selStart > selEnd) {
                                selStart = existingText.length
                                selEnd = existingText.length
                            }
                            val finalText = existingText.substring(0, selStart) + textToInsert + existingText.substring(selEnd)
                            val setArgs = Bundle().apply {
                                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, finalText)
                            }
                            pasteSucceeded = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setArgs)
                            if (pasteSucceeded) {
                                val newCursor = selStart + textToInsert.length
                                val selArgs = Bundle().apply {
                                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newCursor)
                                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newCursor)
                                }
                                try { focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs) } catch (_: Exception) {}
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            if (!pasteSucceeded) {
                showToast("Copied, paste manually")
            } else {
                if (previousClip != null) {
                    mainHandler.postDelayed({
                        try {
                            clipboard.setPrimaryClip(previousClip)
                        } catch (_: Exception) {}
                    }, 300)
                }
            }
        } catch (_: Exception) {
            showToast("Copied, paste manually")
        } finally {
            try { focusedNode?.recycle() } catch (_: Exception) {}
            try { rootNode?.recycle() } catch (_: Exception) {}
        }
    }

    private fun showToast(msg: String) {
        mainHandler.post {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val params = buttonParams ?: return
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val settings = preferences.currentSnapshot
        val sizePx = dpToPx(settings.buttonSizeDp.toFloat())

        params.x = (if (settings.buttonSide == "left") dpToPx(16f) else screenWidth - sizePx - dpToPx(16f))
        params.y = (screenHeight * settings.buttonPositionRatioY).toInt().coerceIn(dpToPx(40f), screenHeight - dpToPx(100f))

        if (isButtonAttached && buttonView != null) {
            windowManager.updateViewLayout(buttonView, params)
        }
        if (isMenuAttached) {
            updateMenuPosition()
        }
    }

    override fun onInterrupt() {
        hideFloatingButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlayViews()
        savedTextsJob?.cancel()
        serviceScope.cancel()
    }
}
