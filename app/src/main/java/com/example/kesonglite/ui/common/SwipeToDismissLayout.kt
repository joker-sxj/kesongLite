package com.example.kesonglite.ui.common

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import androidx.fragment.app.FragmentActivity

class SwipeToDismissLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val viewDragHelper: ViewDragHelper
    private var mainContent: View? = null
    private var backgroundView: View? = null
    private val minVelocity: Float
    private val threshold = 0.4f // 拖动阈值，超过40%的高度时触发退出
    private var dragListener: OnDragListener? = null

    init {
        viewDragHelper = ViewDragHelper.create(this, 1.0f, DragHelperCallback())
        minVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity.toFloat()
    }

    fun setBackgroundView(view: View) {
        this.backgroundView = view
    }
    
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (childCount > 0) {
            // 第一个子视图作为背景
            backgroundView = getChildAt(0)
            backgroundView?.layout(0, 0, right - left, bottom - top)
            
            // 第二个子视图作为主内容
            if (childCount > 1) {
                mainContent = getChildAt(1)
                mainContent?.layout(0, 0, right - left, bottom - top)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
            val childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            child.measure(childWidthSpec, childHeightSpec)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return viewDragHelper.shouldInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        viewDragHelper.processTouchEvent(event)
        return true
    }

    fun setDragListener(listener: OnDragListener) {
        this.dragListener = listener
    }

    private inner class DragHelperCallback : ViewDragHelper.Callback() {
        private var initialY = 0

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            // 只捕获主内容视图
            return child == mainContent
        }

        override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
            initialY = capturedChild.top
            dragListener?.onDragStart()
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            // 限制只能向下拖动
            return if (top < 0) 0 else top
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return height
        }

        override fun onViewPositionChanged(
            changedView: View,
            left: Int,
            top: Int,
            dx: Int,
            dy: Int
        ) {
            if (changedView != mainContent) return

            val dragRatio = top.toFloat() / height
            val scaleRatio = 1 - dragRatio * 0.1f // 拖动时缩小视图
            
            // 设置位移和缩放
            mainContent?.translationY = top.toFloat()
            mainContent?.scaleX = scaleRatio
            mainContent?.scaleY = scaleRatio

            // 通知监听器拖动进度
            dragListener?.onDrag(dragRatio)

            // 调整背景透明度 - 随着拖动，背景逐渐显示（双列页面从暗淡转明亮）
            val alpha = dragRatio
            backgroundView?.alpha = alpha
            
            // 调整主内容的透明度，随着拖动逐渐变暗
            mainContent?.alpha = 1 - dragRatio * 0.3f
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val top = releasedChild.top
            val shouldFinish = top > height * threshold || yvel > minVelocity

            if (shouldFinish) {
                // 超过阈值，退出Activity
                finishActivityTransition()
            } else {
                // 未超过阈值，回弹到原位
                viewDragHelper.settleCapturedViewAt(0, 0)
                invalidate()
                resetViewTransforms()
                dragListener?.onDragEnd(false)
            }
        }
    }

    private fun finishActivityTransition() {
        try {
            // 通知监听器拖动结束并需要退出
            dragListener?.onDragEnd(true)
            // 延迟一点时间，让动画更平滑
            postDelayed({
                val fragmentActivity = context as? FragmentActivity
                if (fragmentActivity != null && !fragmentActivity.isFinishing) {
                    fragmentActivity.supportFinishAfterTransition()
                } else {
                    // 如果不是FragmentActivity，尝试普通finish
                    (context as? Activity)?.finish()
                }
            }, 100)
        } catch (e: Exception) {
            Log.e("SwipeToDismissLayout", "Error during finish transition: ${e.message}")
            // 如果出错，尝试使用普通的finish方法
            (context as? Activity)?.finish()
        }
    }

    private fun resetViewTransforms() {
        try {
            mainContent?.translationY = 0f
            mainContent?.scaleX = 1f
            mainContent?.scaleY = 1f
            backgroundView?.alpha = 1f
        } catch (e: Exception) {
            Log.e("SwipeToDismissLayout", "Error resetting view transforms: ${e.message}")
        }
    }

    override fun computeScroll() {
        if (viewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    interface OnDragListener {
        fun onDragStart()
        fun onDrag(progress: Float) // progress: 0-1，表示拖动进度
        fun onDragEnd(shouldFinish: Boolean) // shouldFinish: 是否应该结束Activity
    }
}