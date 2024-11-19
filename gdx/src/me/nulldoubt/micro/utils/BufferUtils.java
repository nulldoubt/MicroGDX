package me.nulldoubt.micro.utils;

import me.nulldoubt.micro.math.Matrix3;
import me.nulldoubt.micro.math.Matrix4;

import java.nio.*;

public final class BufferUtils {
	
	private BufferUtils() {}
	
	static final Array<ByteBuffer> unsafeBuffers = new Array<>();
	static int allocatedUnsafe = 0;
	
	public static void copy(float[] src, Buffer dst, int numFloats, int offset) {
		if (dst instanceof ByteBuffer)
			dst.limit(numFloats << 2);
		else if (dst instanceof FloatBuffer)
			dst.limit(numFloats);
		
		copyJni(src, dst, numFloats, offset);
		dst.position(0);
	}
	
	public static void copy(byte[] src, int srcOffset, Buffer dst, int numElements) {
		dst.limit(dst.position() + bytesToElements(dst, numElements));
		copyJni(src, srcOffset, dst, positionInBytes(dst), numElements);
	}
	
	public static void copy(short[] src, int srcOffset, Buffer dst, int numElements) {
		dst.limit(dst.position() + bytesToElements(dst, numElements << 1));
		copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 1);
	}
	
	public static void copy(char[] src, int srcOffset, int numElements, Buffer dst) {
		copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 1);
	}
	
	public static void copy(int[] src, int srcOffset, int numElements, Buffer dst) {
		copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2);
	}
	
	public static void copy(long[] src, int srcOffset, int numElements, Buffer dst) {
		copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 3);
	}
	
	public static void copy(float[] src, int srcOffset, int numElements, Buffer dst) {
		copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2);
	}
	
	public static void copy(double[] src, int srcOffset, int numElements, Buffer dst) {
		copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 3);
	}
	
	public static void copy(char[] src, int srcOffset, Buffer dst, int numElements) {
		dst.limit(dst.position() + bytesToElements(dst, numElements << 1));
		copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 1);
	}
	
	public static void copy(int[] src, int srcOffset, Buffer dst, int numElements) {
		dst.limit(dst.position() + bytesToElements(dst, numElements << 2));
		copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2);
	}
	
	public static void copy(long[] src, int srcOffset, Buffer dst, int numElements) {
		dst.limit(dst.position() + bytesToElements(dst, numElements << 3));
		copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 3);
	}
	
	public static void copy(float[] src, int srcOffset, Buffer dst, int numElements) {
		dst.limit(dst.position() + bytesToElements(dst, numElements << 2));
		copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 2);
	}
	
	public static void copy(double[] src, int srcOffset, Buffer dst, int numElements) {
		dst.limit(dst.position() + bytesToElements(dst, numElements << 3));
		copyJni(src, srcOffset, dst, positionInBytes(dst), numElements << 3);
	}
	
	public static void copy(Buffer src, Buffer dst, int numElements) {
		int numBytes = elementsToBytes(src, numElements);
		dst.limit(dst.position() + bytesToElements(dst, numBytes));
		copyJni(src, positionInBytes(src), dst, positionInBytes(dst), numBytes);
	}
	
	public static void transform(Buffer data, int dimensions, int strideInBytes, int count, Matrix4 matrix) {
		transform(data, dimensions, strideInBytes, count, matrix, 0);
	}
	
	/**
	 * Multiply float vector components within the buffer with the specified matrix. The {@link Buffer#position()} is used as the
	 * offset.
	 *
	 * @param data          The buffer to transform.
	 * @param dimensions    The number of components of the vector (2 for xy, 3 for xyz or 4 for xyzw)
	 * @param strideInBytes The offset between the first and the second vector to transform
	 * @param count         The number of vectors to transform
	 * @param matrix        The matrix to multiply the vector with
	 */
	public static void transform(float[] data, int dimensions, int strideInBytes, int count, Matrix4 matrix) {
		transform(data, dimensions, strideInBytes, count, matrix, 0);
	}
	
	/**
	 * Multiply float vector components within the buffer with the specified matrix. The specified offset value is added to the
	 * {@link Buffer#position()} and used as the offset.
	 *
	 * @param data          The buffer to transform.
	 * @param dimensions    The number of components of the vector (2 for xy, 3 for xyz or 4 for xyzw)
	 * @param strideInBytes The offset between the first and the second vector to transform
	 * @param count         The number of vectors to transform
	 * @param matrix        The matrix to multiply the vector with
	 * @param offset        The offset within the buffer (in bytes relative to the current position) to the vector
	 */
	public static void transform(Buffer data, int dimensions, int strideInBytes, int count, Matrix4 matrix, int offset) {
		switch (dimensions) {
			case 4:
				transformV4M4Jni(data, strideInBytes, count, matrix.val, positionInBytes(data) + offset);
				break;
			case 3:
				transformV3M4Jni(data, strideInBytes, count, matrix.val, positionInBytes(data) + offset);
				break;
			case 2:
				transformV2M4Jni(data, strideInBytes, count, matrix.val, positionInBytes(data) + offset);
				break;
			default:
				throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Multiply float vector components within the buffer with the specified matrix. The specified offset value is added to the
	 * {@link Buffer#position()} and used as the offset.
	 *
	 * @param data          The buffer to transform.
	 * @param dimensions    The number of components of the vector (2 for xy, 3 for xyz or 4 for xyzw)
	 * @param strideInBytes The offset between the first and the second vector to transform
	 * @param count         The number of vectors to transform
	 * @param matrix        The matrix to multiply the vector with
	 * @param offset        The offset within the buffer (in bytes relative to the current position) to the vector
	 */
	public static void transform(float[] data, int dimensions, int strideInBytes, int count, Matrix4 matrix, int offset) {
		switch (dimensions) {
			case 4:
				transformV4M4Jni(data, strideInBytes, count, matrix.val, offset);
				break;
			case 3:
				transformV3M4Jni(data, strideInBytes, count, matrix.val, offset);
				break;
			case 2:
				transformV2M4Jni(data, strideInBytes, count, matrix.val, offset);
				break;
			default:
				throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Multiply float vector components within the buffer with the specified matrix. The {@link Buffer#position()} is used as the
	 * offset.
	 *
	 * @param data          The buffer to transform.
	 * @param dimensions    The number of components (x, y, z) of the vector (2 for xy or 3 for xyz)
	 * @param strideInBytes The offset between the first and the second vector to transform
	 * @param count         The number of vectors to transform
	 * @param matrix        The matrix to multiply the vector with
	 */
	public static void transform(Buffer data, int dimensions, int strideInBytes, int count, Matrix3 matrix) {
		transform(data, dimensions, strideInBytes, count, matrix, 0);
	}
	
	/**
	 * Multiply float vector components within the buffer with the specified matrix. The {@link Buffer#position()} is used as the
	 * offset.
	 *
	 * @param data          The buffer to transform.
	 * @param dimensions    The number of components (x, y, z) of the vector (2 for xy or 3 for xyz)
	 * @param strideInBytes The offset between the first and the second vector to transform
	 * @param count         The number of vectors to transform
	 * @param matrix        The matrix to multiply the vector with
	 */
	public static void transform(float[] data, int dimensions, int strideInBytes, int count, Matrix3 matrix) {
		transform(data, dimensions, strideInBytes, count, matrix, 0);
	}
	
	/**
	 * Multiply float vector components within the buffer with the specified matrix. The specified offset value is added to the
	 * {@link Buffer#position()} and used as the offset.
	 *
	 * @param data          The buffer to transform.
	 * @param dimensions    The number of components (x, y, z) of the vector (2 for xy or 3 for xyz)
	 * @param strideInBytes The offset between the first and the second vector to transform
	 * @param count         The number of vectors to transform
	 * @param matrix        The matrix to multiply the vector with,
	 * @param offset        The offset within the buffer (in bytes relative to the current position) to the vector
	 */
	public static void transform(Buffer data, int dimensions, int strideInBytes, int count, Matrix3 matrix, int offset) {
		switch (dimensions) {
			case 3:
				transformV3M3Jni(data, strideInBytes, count, matrix.val, positionInBytes(data) + offset);
				break;
			case 2:
				transformV2M3Jni(data, strideInBytes, count, matrix.val, positionInBytes(data) + offset);
				break;
			default:
				throw new IllegalArgumentException();
		}
	}
	
	/**
	 * Multiply float vector components within the buffer with the specified matrix. The specified offset value is added to the
	 * {@link Buffer#position()} and used as the offset.
	 *
	 * @param data          The buffer to transform.
	 * @param dimensions    The number of components (x, y, z) of the vector (2 for xy or 3 for xyz)
	 * @param strideInBytes The offset between the first and the second vector to transform
	 * @param count         The number of vectors to transform
	 * @param matrix        The matrix to multiply the vector with,
	 * @param offset        The offset within the buffer (in bytes relative to the current position) to the vector
	 */
	public static void transform(float[] data, int dimensions, int strideInBytes, int count, Matrix3 matrix, int offset) {
		switch (dimensions) {
			case 3:
				transformV3M3Jni(data, strideInBytes, count, matrix.val, offset);
				break;
			case 2:
				transformV2M3Jni(data, strideInBytes, count, matrix.val, offset);
				break;
			default:
				throw new IllegalArgumentException();
		}
	}
	
	public static long findFloats(Buffer vertex, int strideInBytes, Buffer vertices, int numVertices) {
		return find(vertex, positionInBytes(vertex), strideInBytes, vertices, positionInBytes(vertices), numVertices);
	}
	
	public static long findFloats(float[] vertex, int strideInBytes, Buffer vertices, int numVertices) {
		return find(vertex, 0, strideInBytes, vertices, positionInBytes(vertices), numVertices);
	}
	
	public static long findFloats(Buffer vertex, int strideInBytes, float[] vertices, int numVertices) {
		return find(vertex, positionInBytes(vertex), strideInBytes, vertices, 0, numVertices);
	}
	
	public static long findFloats(float[] vertex, int strideInBytes, float[] vertices, int numVertices) {
		return find(vertex, 0, strideInBytes, vertices, 0, numVertices);
	}
	
	public static long findFloats(Buffer vertex, int strideInBytes, Buffer vertices, int numVertices, float epsilon) {
		return find(vertex, positionInBytes(vertex), strideInBytes, vertices, positionInBytes(vertices), numVertices, epsilon);
	}
	
	public static long findFloats(float[] vertex, int strideInBytes, Buffer vertices, int numVertices, float epsilon) {
		return find(vertex, 0, strideInBytes, vertices, positionInBytes(vertices), numVertices, epsilon);
	}
	
	public static long findFloats(Buffer vertex, int strideInBytes, float[] vertices, int numVertices, float epsilon) {
		return find(vertex, positionInBytes(vertex), strideInBytes, vertices, 0, numVertices, epsilon);
	}
	
	public static long findFloats(float[] vertex, int strideInBytes, float[] vertices, int numVertices, float epsilon) {
		return find(vertex, 0, strideInBytes, vertices, 0, numVertices, epsilon);
	}
	
	private static int positionInBytes(final Buffer destination) {
		return switch (destination) {
			case ByteBuffer _ -> destination.position();
			case ShortBuffer _, CharBuffer _ -> destination.position() << 1;
			case IntBuffer _ -> destination.position() << 2;
			case LongBuffer _ -> destination.position() << 3;
			case FloatBuffer _ -> destination.position() << 2;
			case DoubleBuffer _ -> destination.position() << 3;
			case null, default -> throw new MicroRuntimeException("Can't copy to buffer instance");
		};
	}
	
	private static int bytesToElements(Buffer dst, int bytes) {
		return switch (dst) {
			case ByteBuffer _ -> bytes;
			case ShortBuffer _, CharBuffer _ -> bytes >>> 1;
			case IntBuffer _ -> bytes >>> 2;
			case LongBuffer _ -> bytes >>> 3;
			case FloatBuffer _ -> bytes >>> 2;
			case DoubleBuffer _ -> bytes >>> 3;
			case null, default -> throw new MicroRuntimeException("Can't copy to buffer instance");
		};
	}
	
	private static int elementsToBytes(Buffer dst, int elements) {
		return switch (dst) {
			case ByteBuffer _ -> elements;
			case ShortBuffer _, CharBuffer _ -> elements << 1;
			case IntBuffer _ -> elements << 2;
			case LongBuffer _ -> elements << 3;
			case FloatBuffer _ -> elements << 2;
			case DoubleBuffer _ -> elements << 3;
			case null, default -> throw new MicroRuntimeException("Can't copy to buffer instance");
		};
	}
	
	public static FloatBuffer newFloatBuffer(int numFloats) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(numFloats * 4);
		buffer.order(ByteOrder.nativeOrder());
		return buffer.asFloatBuffer();
	}
	
	public static DoubleBuffer newDoubleBuffer(int numDoubles) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(numDoubles * 8);
		buffer.order(ByteOrder.nativeOrder());
		return buffer.asDoubleBuffer();
	}
	
	public static ByteBuffer newByteBuffer(int numBytes) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(numBytes);
		buffer.order(ByteOrder.nativeOrder());
		return buffer;
	}
	
	public static ShortBuffer newShortBuffer(int numShorts) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(numShorts * 2);
		buffer.order(ByteOrder.nativeOrder());
		return buffer.asShortBuffer();
	}
	
	public static CharBuffer newCharBuffer(int numChars) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(numChars * 2);
		buffer.order(ByteOrder.nativeOrder());
		return buffer.asCharBuffer();
	}
	
	public static IntBuffer newIntBuffer(int numInts) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(numInts * 4);
		buffer.order(ByteOrder.nativeOrder());
		return buffer.asIntBuffer();
	}
	
	public static LongBuffer newLongBuffer(int numLongs) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(numLongs * 8);
		buffer.order(ByteOrder.nativeOrder());
		return buffer.asLongBuffer();
	}
	
	public static void disposeUnsafeByteBuffer(ByteBuffer buffer) {
		int size = buffer.capacity();
		synchronized (unsafeBuffers) {
			if (!unsafeBuffers.removeValue(buffer, true))
				throw new IllegalArgumentException("buffer not allocated with newUnsafeByteBuffer or already disposed");
		}
		allocatedUnsafe -= size;
		freeMemory(buffer);
	}
	
	public static boolean isUnsafeByteBuffer(ByteBuffer buffer) {
		synchronized (unsafeBuffers) {
			return unsafeBuffers.contains(buffer, true);
		}
	}
	
	public static ByteBuffer newUnsafeByteBuffer(int numBytes) {
		ByteBuffer buffer = newDisposableByteBuffer(numBytes);
		buffer.order(ByteOrder.nativeOrder());
		allocatedUnsafe += numBytes;
		synchronized (unsafeBuffers) {
			unsafeBuffers.add(buffer);
		}
		return buffer;
	}
	
	public static long getUnsafeBufferAddress(Buffer buffer) {
		return getBufferAddress(buffer) + buffer.position();
	}
	
	public static ByteBuffer newUnsafeByteBuffer(ByteBuffer buffer) {
		allocatedUnsafe += buffer.capacity();
		synchronized (unsafeBuffers) {
			unsafeBuffers.add(buffer);
		}
		return buffer;
	}
	
	public static int getAllocatedBytesUnsafe() {
		return allocatedUnsafe;
	}
	
	// @off
	/*JNI 
	#include <stdio.h>
	#include <stdlib.h>
	#include <string.h>
	*/
	
	private static native void freeMemory(ByteBuffer buffer); /*
		free(buffer);
	 */
	
	private static native ByteBuffer newDisposableByteBuffer(int numBytes); /*
		return env->NewDirectByteBuffer((char*)malloc(numBytes), numBytes);
	*/
	
	private static native long getBufferAddress(Buffer buffer); /*
	    return (jlong) buffer;
	*/
	
	public static native void clear(ByteBuffer buffer, int numBytes); /*
		memset(buffer, 0, numBytes);
	*/
	
	private native static void copyJni(float[] src, Buffer dst, int numFloats, int offset); /*
		memcpy(dst, src + offset, numFloats << 2 );
	*/
	
	private native static void copyJni(byte[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
	
	private native static void copyJni(char[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
	
	private native static void copyJni(short[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	 */
	
	private native static void copyJni(int[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
	
	private native static void copyJni(long[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
	
	private native static void copyJni(float[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
	
	private native static void copyJni(double[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
	
	private native static void copyJni(Buffer src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
	
	/*JNI
	template<size_t n1, size_t n2> void transform(float * const &src, float * const &m, float * const &dst) {}
	
	template<> inline void transform<4, 4>(float * const &src, float * const &m, float * const &dst) {
		const float x = src[0], y = src[1], z = src[2], w = src[3];
		dst[0] = x * m[ 0] + y * m[ 4] + z * m[ 8] + w * m[12]; 
		dst[1] = x * m[ 1] + y * m[ 5] + z * m[ 9] + w * m[13];
		dst[2] = x * m[ 2] + y * m[ 6] + z * m[10] + w * m[14];
		dst[3] = x * m[ 3] + y * m[ 7] + z * m[11] + w * m[15]; 
	}
	
	template<> inline void transform<3, 4>(float * const &src, float * const &m, float * const &dst) {
		const float x = src[0], y = src[1], z = src[2];
		dst[0] = x * m[ 0] + y * m[ 4] + z * m[ 8] + m[12]; 
		dst[1] = x * m[ 1] + y * m[ 5] + z * m[ 9] + m[13];
		dst[2] = x * m[ 2] + y * m[ 6] + z * m[10] + m[14]; 
	}
	
	template<> inline void transform<2, 4>(float * const &src, float * const &m, float * const &dst) {
		const float x = src[0], y = src[1];
		dst[0] = x * m[ 0] + y * m[ 4] + m[12]; 
		dst[1] = x * m[ 1] + y * m[ 5] + m[13]; 
	}
	
	template<> inline void transform<3, 3>(float * const &src, float * const &m, float * const &dst) {
		const float x = src[0], y = src[1], z = src[2];
		dst[0] = x * m[0] + y * m[3] + z * m[6]; 
		dst[1] = x * m[1] + y * m[4] + z * m[7];
		dst[2] = x * m[2] + y * m[5] + z * m[8]; 
	}
	
	template<> inline void transform<2, 3>(float * const &src, float * const &m, float * const &dst) {
		const float x = src[0], y = src[1];
		dst[0] = x * m[0] + y * m[3] + m[6]; 
		dst[1] = x * m[1] + y * m[4] + m[7]; 
	}
	
	template<size_t n1, size_t n2> void transform(float * const &v, int const &stride, int const &count, float * const &m, int offset) {
		for (int i = 0; i < count; i++) {
			transform<n1, n2>(&v[offset], m, &v[offset]);
			offset += stride;
		}
	}
	
	template<size_t n1, size_t n2> void transform(float * const &v, int const &stride, unsigned short * const &indices, int const &count, float * const &m, int offset) {
		for (int i = 0; i < count; i++) {
			transform<n1, n2>(&v[offset], m, &v[offset]);
			offset += stride;
		}
	}
	
	inline bool compare(float * const &lhs, float * const & rhs, const unsigned int &size, const float &epsilon) {
   	for (unsigned int i = 0; i < size; i++)
   		if ((*(unsigned int*)&lhs[i] != *(unsigned int*)&rhs[i]) && ((lhs[i] > rhs[i] ? lhs[i] - rhs[i] : rhs[i] - lhs[i]) > epsilon))
         	return false;
		return true;
	}
	
	long find(float * const &vertex, const unsigned int &size, float * const &vertices, const unsigned int &count, const float &epsilon) {
		for (unsigned int i = 0; i < count; i++)
			if (compare(&vertices[i*size], vertex, size, epsilon))
				return (long)i;
		return -1;
	}

	inline bool compare(float * const &lhs, float * const & rhs, const unsigned int &size) {
   	for (unsigned int i = 0; i < size; i++)
      	if ((*(unsigned int*)&lhs[i] != *(unsigned int*)&rhs[i]) && lhs[i] != rhs[i])
         	return false;
		return true;
	}
	
	long find(float * const &vertex, const unsigned int &size, float * const &vertices, const unsigned int &count) {
		for (unsigned int i = 0; i < count; i++)
			if (compare(&vertices[i*size], vertex, size))
				return (long)i;
		return -1;
	}

	inline unsigned int calcHash(float * const &vertex, const unsigned int &size) {
		unsigned int result = 0;
		for (unsigned int i = 0; i < size; ++i)
			result += ((*((unsigned int *)&vertex[i])) & 0xffffff80) >> (i & 0x7);
		return result & 0x7fffffff;
	}
	
	long find(float * const &vertex, const unsigned int &size, float * const &vertices, unsigned int * const &hashes, const unsigned int &count) {
		const unsigned int hash = calcHash(vertex, size);
		for (unsigned int i = 0; i < count; i++)
			if (hashes[i] == hash && compare(&vertices[i*size], vertex, size))
				return (long)i;
		return -1;
	}
	*/
	
	private native static void transformV4M4Jni(Buffer data, int strideInBytes, int count, float[] matrix, int offsetInBytes); /*
		transform<4, 4>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);  
	*/
	
	private native static void transformV4M4Jni(float[] data, int strideInBytes, int count, float[] matrix, int offsetInBytes); /*
		transform<4, 4>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);  
	*/
	
	private native static void transformV3M4Jni(Buffer data, int strideInBytes, int count, float[] matrix, int offsetInBytes); /*
		transform<3, 4>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
	
	private native static void transformV3M4Jni(float[] data, int strideInBytes, int count, float[] matrix, int offsetInBytes); /*
		transform<3, 4>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
	
	private native static void transformV2M4Jni(Buffer data, int strideInBytes, int count, float[] matrix, int offsetInBytes); /*
		transform<2, 4>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
	
	private native static void transformV2M4Jni(float[] data, int strideInBytes, int count, float[] matrix, int offsetInBytes); /*
		transform<2, 4>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
	
	private native static void transformV3M3Jni(Buffer data, int strideInBytes, int count, float[] matrix, int offsetInBytes); /*
		transform<3, 3>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
	
	private native static void transformV3M3Jni(float[] data, int strideInBytes, int count, float[] matrix, int offsetInBytes); /*
		transform<3, 3>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
	
	private native static void transformV2M3Jni(Buffer data, int strideInBytes, int count, float[] matrix, int offsetInBytes); /*
		transform<2, 3>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
	
	private native static void transformV2M3Jni(float[] data, int strideInBytes, int count, float[] matrix, int offsetInBytes); /*
		transform<2, 3>((float*)data, strideInBytes / 4, count, (float*)matrix, offsetInBytes / 4);
	*/
	
	private native static long find(Buffer vertex, int vertexOffsetInBytes, int strideInBytes, Buffer vertices, int verticesOffsetInBytes, int numVertices); /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices);
	*/
	
	private native static long find(float[] vertex, int vertexOffsetInBytes, int strideInBytes, Buffer vertices, int verticesOffsetInBytes, int numVertices); /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices);
	*/
	
	private native static long find(Buffer vertex, int vertexOffsetInBytes, int strideInBytes, float[] vertices, int verticesOffsetInBytes, int numVertices); /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices);
	*/
	
	private native static long find(float[] vertex, int vertexOffsetInBytes, int strideInBytes, float[] vertices, int verticesOffsetInBytes, int numVertices); /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices);
	*/
	
	private native static long find(Buffer vertex, int vertexOffsetInBytes, int strideInBytes, Buffer vertices, int verticesOffsetInBytes, int numVertices, float epsilon); /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices, epsilon);
	*/
	
	private native static long find(float[] vertex, int vertexOffsetInBytes, int strideInBytes, Buffer vertices, int verticesOffsetInBytes, int numVertices, float epsilon); /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices, epsilon);
	*/
	
	private native static long find(Buffer vertex, int vertexOffsetInBytes, int strideInBytes, float[] vertices, int verticesOffsetInBytes, int numVertices, float epsilon); /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices, epsilon);
	*/
	
	private native static long find(float[] vertex, int vertexOffsetInBytes, int strideInBytes, float[] vertices, int verticesOffsetInBytes, int numVertices, float epsilon); /*
		return find((float *)&vertex[vertexOffsetInBytes / 4], (unsigned int)(strideInBytes / 4), (float*)&vertices[verticesOffsetInBytes / 4], (unsigned int)numVertices, epsilon);
	*/
	
}
