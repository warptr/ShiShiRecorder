/*
 * This code has been co-authored by an AI.
 * Model name: Qwen 3 Coder Next
 */

package com.yepgoryo.CaptureCap

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.util.Log

class EglCore private constructor(
    val display: EGLDisplay,
    val config: EGLConfig,
    val context: EGLContext
) {

    init {
        if (display == EGL14.EGL_NO_DISPLAY) throw IllegalStateException("EGL_NO_DISPLAY")
    }

    companion object {
        fun create(shareContext: EGLContext? = EGL14.EGL_NO_CONTEXT): EglCore {
            var ctx = shareContext

            if (shareContext == null) {
                ctx = EGL14.EGL_NO_CONTEXT
            }

            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                ?: throw IllegalStateException("Failed to get EGL display")

            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1))
                throw IllegalStateException("EGL initialization failed: ${EGL14.eglGetError()}")

            val attribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE
            )

            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(display, attribs, 0, configs, 0, configs.size, numConfigs, 0) ||
                numConfigs[0] == 0
            ) throw IllegalStateException("No EGL config found: ${EGL14.eglGetError()}")

            val ctxAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )

            val context = EGL14.eglCreateContext(display, configs[0]!!, ctx, ctxAttribs, 0)
                ?: throw IllegalStateException("Failed to create ES2 context: ${EGL14.eglGetError()}")

            return EglCore(display, configs[0]!!, context)
        }
    }

    fun createContext(share: Boolean = false): EGLContext =
        if (share) {
            val attribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            EGL14.eglCreateContext(display, config, context, attribs, 0)
                ?: throw IllegalStateException()
        } else context

    fun makeCurrent() {
        if (!EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, context))
            throw IllegalStateException("eglMakeCurrent failed: ${EGL14.eglGetError()}")
    }

    fun makeCurrent(draw: EGLSurface, read: EGLSurface? = draw) {
        if (!EGL14.eglMakeCurrent(display, draw, read, context))
            throw IllegalStateException("eglMakeCurrent failed: ${EGL14.eglGetError()}")
    }

    fun swapBuffers(): Boolean =
        EGL14.eglSwapBuffers(display, EGL14.eglGetCurrentSurface(EGL14.EGL_READ))
                || Log.w("EglCore", "swapBuffers false: ${EGL14.eglGetError()}") == null

    fun swapBuffers(surface: EGLSurface): Boolean =
        EGL14.eglSwapBuffers(display, surface)
                || Log.w("EglCore", "swapBuffers false: ${EGL14.eglGetError()}") == null


    fun releaseSurface() {
        val defaultSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ)
        if (defaultSurface != null && defaultSurface != EGL14.EGL_NO_SURFACE)
            EGL14.eglDestroySurface(display, defaultSurface)
    }

    fun releaseSurface(surface: EGLSurface?) {
        if (surface != null && surface != EGL14.EGL_NO_SURFACE)
            EGL14.eglDestroySurface(display, surface)
    }

    fun destroy() {
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroyContext(display, context)
        EGL14.eglTerminate(display)
    }
}