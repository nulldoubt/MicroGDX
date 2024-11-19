package me.nulldoubt.micro.graphics;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class VertexAttributes implements Iterable<VertexAttribute>, Comparable<VertexAttributes> {
	
	public static final class Usage {
		
		public static final int Position = 1;
		public static final int ColorUnpacked = 2;
		public static final int ColorPacked = 4;
		public static final int Normal = 8;
		public static final int TextureCoordinates = 16;
		public static final int Generic = 32;
		public static final int BoneWeight = 64;
		public static final int Tangent = 128;
		public static final int BiNormal = 256;
		
	}
	
	private final VertexAttribute[] attributes;
	
	public final int vertexSize;
	
	private long mask = -1;
	
	private int boneWeightUnits = -1;
	
	private int textureCoordinates = -1;
	
	private ReadonlyIterable<VertexAttribute> iterable;
	
	public VertexAttributes(VertexAttribute... attributes) {
		if (attributes.length == 0)
			throw new IllegalArgumentException("attributes must be >= 1");
		
		VertexAttribute[] list = new VertexAttribute[attributes.length];
		System.arraycopy(attributes, 0, list, 0, attributes.length);
		
		this.attributes = list;
		vertexSize = calculateOffsets();
	}
	
	public int getOffset(int usage, int defaultIfNotFound) {
		VertexAttribute vertexAttribute = findByUsage(usage);
		if (vertexAttribute == null)
			return defaultIfNotFound;
		return vertexAttribute.offset / 4;
	}
	
	public int getOffset(int usage) {
		return getOffset(usage, 0);
	}
	
	public VertexAttribute findByUsage(int usage) {
		int len = size();
		for (int i = 0; i < len; i++)
			if (get(i).usage == usage)
				return get(i);
		return null;
	}
	
	private int calculateOffsets() {
		int count = 0;
		for (VertexAttribute attribute : attributes) {
			attribute.offset = count;
			count += attribute.getSizeInBytes();
		}
		return count;
	}
	
	public int size() {
		return attributes.length;
	}
	
	public VertexAttribute get(int index) {
		return attributes[index];
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		for (VertexAttribute attribute : attributes) {
			builder.append("(");
			builder.append(attribute.alias);
			builder.append(", ");
			builder.append(attribute.usage);
			builder.append(", ");
			builder.append(attribute.numComponents);
			builder.append(", ");
			builder.append(attribute.offset);
			builder.append(")");
			builder.append("\n");
		}
		builder.append("]");
		return builder.toString();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof VertexAttributes other))
			return false;
		if (this.attributes.length != other.attributes.length)
			return false;
		for (int i = 0; i < attributes.length; i++) {
			if (!attributes[i].equals(other.attributes[i]))
				return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		long result = 61L * attributes.length;
		for (VertexAttribute attribute : attributes)
			result = result * 61L + attribute.hashCode();
		return (int) (result ^ (result >> 32));
	}
	
	public long getMask() {
		if (mask == -1) {
			long result = 0;
			for (VertexAttribute attribute : attributes)
				result |= attribute.usage;
			mask = result;
		}
		return mask;
	}
	
	public long getMaskWithSizePacked() {
		return getMask() | ((long) attributes.length << 32);
	}
	
	public int getBoneWeights() {
		if (boneWeightUnits < 0) {
			boneWeightUnits = 0;
			for (VertexAttribute a : attributes) {
				if (a.usage == Usage.BoneWeight) {
					boneWeightUnits = Math.max(boneWeightUnits, a.unit + 1);
				}
			}
		}
		return boneWeightUnits;
	}
	
	public int getTextureCoordinates() {
		if (textureCoordinates < 0) {
			textureCoordinates = 0;
			for (VertexAttribute a : attributes) {
				if (a.usage == Usage.TextureCoordinates)
					textureCoordinates = Math.max(textureCoordinates, a.unit + 1);
			}
		}
		return textureCoordinates;
	}
	
	@Override
	public int compareTo(VertexAttributes o) {
		if (attributes.length != o.attributes.length)
			return attributes.length - o.attributes.length;
		final long m1 = getMask();
		final long m2 = o.getMask();
		if (m1 != m2)
			return m1 < m2 ? -1 : 1;
		for (int i = attributes.length - 1; i >= 0; --i) {
			final VertexAttribute va0 = attributes[i];
			final VertexAttribute va1 = o.attributes[i];
			if (va0.usage != va1.usage)
				return va0.usage - va1.usage;
			if (va0.unit != va1.unit)
				return va0.unit - va1.unit;
			if (va0.numComponents != va1.numComponents)
				return va0.numComponents - va1.numComponents;
			if (va0.normalized != va1.normalized)
				return va0.normalized ? 1 : -1;
			if (va0.type != va1.type)
				return va0.type - va1.type;
		}
		return 0;
	}
	
	@Override
	public Iterator<VertexAttribute> iterator() {
		if (iterable == null)
			iterable = new ReadonlyIterable<>(attributes);
		return iterable.iterator();
	}
	
	private static class ReadonlyIterator<T> implements Iterator<T>, Iterable<T> {
		
		private final T[] array;
		int index;
		boolean valid = true;
		
		public ReadonlyIterator(T[] array) {
			this.array = array;
		}
		
		@Override
		public boolean hasNext() {
			if (!valid)
				throw new MicroRuntimeException("#iterator() cannot be used nested.");
			return index < array.length;
		}
		
		@Override
		public T next() {
			if (index >= array.length)
				throw new NoSuchElementException(String.valueOf(index));
			if (!valid)
				throw new MicroRuntimeException("#iterator() cannot be used nested.");
			return array[index++];
		}
		
		@Override
		public void remove() {
			throw new MicroRuntimeException("Remove not allowed.");
		}
		
		public void reset() {
			index = 0;
		}
		
		@Override
		public Iterator<T> iterator() {
			return this;
		}
		
	}
	
	private record ReadonlyIterable<T>(T[] array) implements Iterable<T> {
		
		@Override
		public Iterator<T> iterator() {
			return new ReadonlyIterator(array);
		}
		
	}
	
}
