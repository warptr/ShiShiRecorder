package com.yepgoryo.CaptureCap

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder

object QuadBuffers {
    val vertices = floatArrayOf(
        -1f, -1f, 0f, 1f,
        1f, -1f, 1f, 1f,
        -1f,  1f, 0f, 0f,
        1f,  1f, 1f, 0f,
    )

    val indices = shortArrayOf(
        0, 1, 2,
        2, 1, 3
    )

    private var vboId: Int = -1
    private var iboId: Int = -1

    fun create() {
        val buffers = IntArray(2)
        GLES20.glGenBuffers(buffers.size, buffers, 0)
        vboId = buffers[0]
        iboId = buffers[1]

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        val vertexBuf = ByteBuffer.allocateDirect(vertices.size * 4).apply {
            order(ByteOrder.nativeOrder())
            putFloats(vertices)
        }.flip()
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexBuf.capacity(), vertexBuf, GLES20.GL_STATIC_DRAW)

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, iboId)
        val indexBuf = ByteBuffer.allocateDirect(indices.size * 2).apply {
            order(ByteOrder.nativeOrder())
            putShorts(indices)
        }.flip()
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuf.capacity(), indexBuf, GLES20.GL_STATIC_DRAW)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun drawIndexed() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, iboId)

        val stride = 4 * 4
        GLES20.glEnableVertexAttribArray(0)
        GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, stride, 0)
        GLES20.glEnableVertexAttribArray(1)
        GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, stride, 2 * 4)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, 0)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun ByteBuffer.putFloats(arr: FloatArray): ByteBuffer {
        for (f in arr) putFloat(f)
        return this
    }

    private fun ByteBuffer.putShorts(arr: ShortArray): ByteBuffer {
        for (s in arr) putShort(s)
        return this
    }
}