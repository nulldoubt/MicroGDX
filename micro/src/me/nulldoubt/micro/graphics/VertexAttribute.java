package me.nulldoubt.micro.graphics;

import me.nulldoubt.micro.graphics.VertexAttributes.Usage;
import me.nulldoubt.micro.graphics.glutils.Shader;

public final class VertexAttribute {
	
	public final int usage;
	public final int numComponents;
	public final boolean normalized;
	public final int type;
	public int offset;
	public String alias;
	public int unit;
	private final int usageIndex;
	
	public VertexAttribute(int usage, int numComponents, String alias) {
		this(usage, numComponents, alias, 0);
	}
	
	public VertexAttribute(int usage, int numComponents, String alias, int unit) {
		this(usage, numComponents, usage == Usage.ColorPacked ? GL20.GL_UNSIGNED_BYTE : GL20.GL_FLOAT, usage == Usage.ColorPacked,
				alias, unit);
	}
	
	public VertexAttribute(int usage, int numComponents, int type, boolean normalized, String alias) {
		this(usage, numComponents, type, normalized, alias, 0);
	}
	
	public VertexAttribute(int usage, int numComponents, int type, boolean normalized, String alias, int unit) {
		this.usage = usage;
		this.numComponents = numComponents;
		this.type = type;
		this.normalized = normalized;
		this.alias = alias;
		this.unit = unit;
		this.usageIndex = Integer.numberOfTrailingZeros(usage);
	}
	
	public VertexAttribute copy() {
		return new VertexAttribute(usage, numComponents, type, normalized, alias, unit);
	}
	
	public static VertexAttribute position() {
		return new VertexAttribute(Usage.Position, 3, Shader.POSITION_ATTRIBUTE);
	}
	
	public static VertexAttribute texCoords(int unit) {
		return new VertexAttribute(Usage.TextureCoordinates, 2, Shader.TEXCOORD_ATTRIBUTE + unit, unit);
	}
	
	public static VertexAttribute normal() {
		return new VertexAttribute(Usage.Normal, 3, Shader.NORMAL_ATTRIBUTE);
	}
	
	public static VertexAttribute colorPacked() {
		return new VertexAttribute(Usage.ColorPacked, 4, GL20.GL_UNSIGNED_BYTE, true, Shader.COLOR_ATTRIBUTE);
	}
	
	public static VertexAttribute colorUnpacked() {
		return new VertexAttribute(Usage.ColorUnpacked, 4, GL20.GL_FLOAT, false, Shader.COLOR_ATTRIBUTE);
	}
	
	public static VertexAttribute tangent() {
		return new VertexAttribute(Usage.Tangent, 3, Shader.TANGENT_ATTRIBUTE);
	}
	
	public static VertexAttribute binormal() {
		return new VertexAttribute(Usage.BiNormal, 3, Shader.BINORMAL_ATTRIBUTE);
	}
	
	public static VertexAttribute boneWeight(int unit) {
		return new VertexAttribute(Usage.BoneWeight, 2, Shader.BONEWEIGHT_ATTRIBUTE + unit, unit);
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof VertexAttribute vertexAttribute))
			return false;
		return equals(vertexAttribute);
	}
	
	public boolean equals(final VertexAttribute other) {
		return other != null && usage == other.usage && numComponents == other.numComponents && type == other.type && normalized == other.normalized && alias.equals(other.alias) && unit == other.unit;
	}
	
	public int getKey() {
		return (usageIndex << 8) + (unit & 0xFF);
	}
	
	public int getSizeInBytes() {
		return switch (type) {
			case GL20.GL_FLOAT, GL20.GL_FIXED -> 4 * numComponents;
			case GL20.GL_UNSIGNED_BYTE, GL20.GL_BYTE -> numComponents;
			case GL20.GL_UNSIGNED_SHORT, GL20.GL_SHORT -> 2 * numComponents;
			default -> 0;
		};
	}
	
	@Override
	public int hashCode() {
		int result = getKey();
		result = 541 * result + numComponents;
		result = 541 * result + alias.hashCode();
		return result;
	}
	
}
