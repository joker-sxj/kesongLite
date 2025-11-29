package com.example.kesonglite.ui.transition

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.transition.ChangeBounds
import android.transition.TransitionValues
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.example.kesonglite.R

class HeroImageTransition : ChangeBounds {
    
    constructor() : super()
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    
    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues,
        endValues: TransitionValues
    ): Animator? {
        val animator = super.createAnimator(sceneRoot, startValues, endValues)
        
        val view = endValues.view ?: return animator
        
        // 创建放大动画
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.8f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.8f, 1.0f)
        val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.0f, 1.0f)
        
        // 组合动画
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, alpha, animator)
        animatorSet.duration = 300
        
        return animatorSet
    }
}