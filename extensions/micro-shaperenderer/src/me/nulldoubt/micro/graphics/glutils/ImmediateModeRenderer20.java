package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.graphics.Mesh;
import me.nulldoubt.micro.graphics.VertexAttribute;
import me.nulldoubt.micro.graphics.VertexAttributes.Usage;
import me.nulldoubt.micro.math.Matrix4;
import me.nulldoubt.micro.utils.collections.Array;

public class ImmediateModeRenderer20 implements ImmediateModeRenderer {
	
	private int primitiveType;
	private int vertexIdx;
	private int numSetTexCoords;
	private final int maxVertices;
	private int numVertices;
	
	private final Mesh mesh;
	private Shader shader;
	private boolean ownsShader;
	private final int numTexCoords;
	private final int vertexSize;
	private final int normalOffset;
	private final int colorOffset;
	private final int texCoordOffset;
	private final Matrix4 projModelView = new Matrix4();
	private final float[] vertices;
	private final String[] shaderUniformNames;
	
	public ImmediateModeRenderer20(boolean hasNormals, boolean hasColors, int numTexCoords) {
		this(5000, hasNormals, hasColors, numTexCoords, createDefaultShader(hasNormals, hasColors, numTexCoords));
		ownsShader = true;
	}
	
	public ImmediateModeRenderer20(int maxVertices, boolean hasNormals, boolean hasColors, int numTexCoords) {
		this(maxVertices, hasNormals, hasColors, numTexCoords, createDefaultShader(hasNormals, hasColors, numTexCoords));
		ownsShader = true;
	}
	
	public ImmediateModeRenderer20(int maxVertices, boolean hasNormals, boolean hasColors, int numTexCoords, Shader shader) {
		this.maxVertices = maxVertices;
		this.numTexCoords = numTexCoords;
		this.shader = shader;
		
		VertexAttribute[] attribs = buildVertexAttributes(hasNormals, hasColors, numTexCoords);
		mesh = new Mesh(false, maxVertices, 0, attribs);
		
		vertices = new float[maxVertices * (mesh.getVertexAttributes().vertexSize / 4)];
		vertexSize = mesh.getVertexAttributes().vertexSize / 4;
		normalOffset = mesh.getVertexAttribute(Usage.Normal) != null ? mesh.getVertexAttribute(Usage.Normal).offset / 4 : 0;
		colorOffset = mesh.getVertexAttribute(Usage.ColorPacked) != null ? mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
		texCoordOffset = mesh.getVertexAttribute(Usage.TextureCoordinates) != null ? mesh.getVertexAttribute(Usage.TextureCoordinates).offset / 4
				: 0;
		
		shaderUniformNames = new String[numTexCoords];
		for (int i = 0; i < numTexCoords; i++) {
			shaderUniformNames[i] = "u_sampler" + i;
		}
	}
	
	private VertexAttribute[] buildVertexAttributes(boolean hasNormals, boolean hasColor, int numTexCoords) {
		final Array<VertexAttribute> attributes = new Array<>();
		attributes.add(new VertexAttribute(Usage.Position, 3, Shader.POSITION_ATTRIBUTE));
		if (hasNormals)
			attributes.add(new VertexAttribute(Usage.Normal, 3, Shader.NORMAL_ATTRIBUTE));
		if (hasColor)
			attributes.add(new VertexAttribute(Usage.ColorPacked, 4, Shader.COLOR_ATTRIBUTE));
		for (int i = 0; i < numTexCoords; i++)
			attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, Shader.TEXCOORD_ATTRIBUTE + i));
		final VertexAttribute[] array = new VertexAttribute[attributes.size];
		for (int i = 0; i < attributes.size; i++)
			array[i] = attributes.get(i);
		return array;
	}
	
	public void setShader(Shader shader) {
		if (ownsShader)
			this.shader.dispose();
		this.shader = shader;
		ownsShader = false;
	}
	
	public Shader getShader() {
		return shader;
	}
	
	public void begin(Matrix4 projModelView, int primitiveType) {
		this.projModelView.set(projModelView);
		this.primitiveType = primitiveType;
	}
	
	public void color(Color color) {
		vertices[vertexIdx + colorOffset] = color.toFloatBits();
	}
	
	public void color(float r, float g, float b, float a) {
		vertices[vertexIdx + colorOffset] = Color.toFloatBits(r, g, b, a);
	}
	
	public void color(float colorBits) {
		vertices[vertexIdx + colorOffset] = colorBits;
	}
	
	public void texCoord(float u, float v) {
		final int idx = vertexIdx + texCoordOffset;
		vertices[idx + numSetTexCoords] = u;
		vertices[idx + numSetTexCoords + 1] = v;
		numSetTexCoords += 2;
	}
	
	public void normal(float x, float y, float z) {
		final int idx = vertexIdx + normalOffset;
		vertices[idx] = x;
		vertices[idx + 1] = y;
		vertices[idx + 2] = z;
	}
	
	public void vertex(float x, float y, float z) {
		final int idx = vertexIdx;
		vertices[idx] = x;
		vertices[idx + 1] = y;
		vertices[idx + 2] = z;
		
		numSetTexCoords = 0;
		vertexIdx += vertexSize;
		numVertices++;
	}
	
	public void flush() {
		if (numVertices == 0)
			return;
		shader.bind();
		shader.setUniformMatrix("u_projModelView", projModelView);
		for (int i = 0; i < numTexCoords; i++)
			shader.setUniformi(shaderUniformNames[i], i);
		mesh.setVertices(vertices, 0, vertexIdx);
		mesh.render(shader, primitiveType);
		
		numSetTexCoords = 0;
		vertexIdx = 0;
		numVertices = 0;
	}
	
	public void end() {
		flush();
	}
	
	public int getNumVertices() {
		return numVertices;
	}
	
	@Override
	public int getMaxVertices() {
		return maxVertices;
	}
	
	public void dispose() {
		if (ownsShader && shader != null)
			shader.dispose();
		mesh.dispose();
	}
	
	static private String createVertexShader(boolean hasNormals, boolean hasColors, int numTexCoords) {
		StringBuilder shader = new StringBuilder("attribute vec4 " + Shader.POSITION_ATTRIBUTE + ";\n"
				+ (hasNormals ? "attribute vec3 " + Shader.NORMAL_ATTRIBUTE + ";\n" : "")
				+ (hasColors ? "attribute vec4 " + Shader.COLOR_ATTRIBUTE + ";\n" : ""));
		
		for (int i = 0; i < numTexCoords; i++)
			shader.append("attribute vec2 " + Shader.TEXCOORD_ATTRIBUTE).append(i).append(";\n");
		
		shader.append("uniform mat4 u_projModelView;\n").append(hasColors ? "varying vec4 v_col;\n" : "");
		
		for (int i = 0; i < numTexCoords; i++)
			shader.append("varying vec2 v_tex").append(i).append(";\n");
		
		shader.append("void main() {\n" + "   gl_Position = u_projModelView * " + Shader.POSITION_ATTRIBUTE + ";\n");
		if (hasColors)
			shader.append("   v_col = " + Shader.COLOR_ATTRIBUTE + ";\n" + "   v_col.a *= 255.0 / 254.0;\n");
		
		for (int i = 0; i < numTexCoords; i++)
			shader.append("   v_tex").append(i).append(" = ").append(Shader.TEXCOORD_ATTRIBUTE).append(i).append(";\n");
		
		shader.append("""
				   gl_PointSize = 1.0;
				}
				""");
		return shader.toString();
	}
	
	static private String createFragmentShader(boolean hasNormals, boolean hasColors, int numTexCoords) {
		StringBuilder shader = new StringBuilder("""
				#ifdef GL_ES
				precision mediump float;
				#endif
				""");
		
		if (hasColors)
			shader.append("varying vec4 v_col;\n");
		for (int i = 0; i < numTexCoords; i++) {
			shader.append("varying vec2 v_tex").append(i).append(";\n");
			shader.append("uniform sampler2D u_sampler").append(i).append(";\n");
		}
		
		shader.append("void main() {\n" //
				+ "   gl_FragColor = ").append(hasColors ? "v_col" : "vec4(1, 1, 1, 1)");
		
		if (numTexCoords > 0)
			shader.append(" * ");
		
		for (int i = 0; i < numTexCoords; i++) {
			if (i == numTexCoords - 1)
				shader.append(" texture2D(u_sampler").append(i).append(",  v_tex").append(i).append(")");
			else
				shader.append(" texture2D(u_sampler").append(i).append(",  v_tex").append(i).append(") *");
		}
		
		shader.append(";\n}");
		return shader.toString();
	}
	
	static public Shader createDefaultShader(boolean hasNormals, boolean hasColors, int numTexCoords) {
		String vertexShader = createVertexShader(hasNormals, hasColors, numTexCoords);
		String fragmentShader = createFragmentShader(hasNormals, hasColors, numTexCoords);
		Shader program = new Shader(vertexShader, fragmentShader);
		if (!program.isCompiled())
			throw new MicroRuntimeException("Error compiling shader: " + program.getLog());
		return program;
	}
	
}