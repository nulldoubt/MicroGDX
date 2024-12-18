package me.nulldoubt.micro.graphics.glutils;

import me.nulldoubt.micro.Application;
import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.graphics.GL20;
import me.nulldoubt.micro.math.Matrix3;
import me.nulldoubt.micro.math.Matrix4;
import me.nulldoubt.micro.math.Vector2;
import me.nulldoubt.micro.math.Vector3;
import me.nulldoubt.micro.utils.Buffers;
import me.nulldoubt.micro.utils.Disposable;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.ObjectIntMap;
import me.nulldoubt.micro.utils.collections.ObjectMap;
import me.nulldoubt.micro.utils.strings.StringBuilder;

import java.nio.*;

public class Shader implements Disposable {
	
	public static final String POSITION_ATTRIBUTE = "a_position";
	public static final String NORMAL_ATTRIBUTE = "a_normal";
	public static final String COLOR_ATTRIBUTE = "a_color";
	public static final String TEXCOORD_ATTRIBUTE = "a_texCoord";
	public static final String TANGENT_ATTRIBUTE = "a_tangent";
	public static final String BINORMAL_ATTRIBUTE = "a_binormal";
	public static final String BONEWEIGHT_ATTRIBUTE = "a_boneWeight";
	
	public static boolean pedantic = true;
	
	public static String prependVertexCode = "";
	public static String prependFragmentCode = "";
	
	private final static ObjectMap<Application, Array<Shader>> shaders = new ObjectMap<>();
	
	private String log = "";
	private boolean compiled;
	
	private final ObjectIntMap<String> uniforms = new ObjectIntMap<>();
	private final ObjectIntMap<String> uniformTypes = new ObjectIntMap<>();
	private final ObjectIntMap<String> uniformSizes = new ObjectIntMap<>();
	private String[] uniformNames;
	
	private final ObjectIntMap<String> attributes = new ObjectIntMap<>();
	private final ObjectIntMap<String> attributeTypes = new ObjectIntMap<>();
	private final ObjectIntMap<String> attributeSizes = new ObjectIntMap<>();
	private String[] attributeNames;
	
	private int program;
	
	private int vertexShaderHandle;
	
	private int fragmentShaderHandle;
	
	private final String vertexShaderSource;
	
	private final String fragmentShaderSource;
	
	private boolean invalidated;
	
	public Shader(String vertexShader, String fragmentShader) {
		if (vertexShader == null)
			throw new IllegalArgumentException("vertex shader must not be null");
		if (fragmentShader == null)
			throw new IllegalArgumentException("fragment shader must not be null");
		
		if (prependVertexCode != null && prependVertexCode.length() > 0)
			vertexShader = prependVertexCode + vertexShader;
		if (prependFragmentCode != null && prependFragmentCode.length() > 0)
			fragmentShader = prependFragmentCode + fragmentShader;
		
		this.vertexShaderSource = vertexShader;
		this.fragmentShaderSource = fragmentShader;
		
		compileShaders(vertexShader, fragmentShader);
		if (isCompiled()) {
			fetchAttributes();
			fetchUniforms();
			addManagedShader(Micro.app, this);
		}
	}
	
	public Shader(FileHandle vertexShader, FileHandle fragmentShader) {
		this(vertexShader.readString(), fragmentShader.readString());
	}
	
	private void compileShaders(String vertexShader, String fragmentShader) {
		vertexShaderHandle = loadShader(GL20.GL_VERTEX_SHADER, vertexShader);
		fragmentShaderHandle = loadShader(GL20.GL_FRAGMENT_SHADER, fragmentShader);
		
		if (vertexShaderHandle == -1 || fragmentShaderHandle == -1) {
			compiled = false;
			return;
		}
		
		program = linkProgram(createProgram());
		if (program == -1) {
			compiled = false;
			return;
		}
		
		compiled = true;
	}
	
	private int loadShader(int type, String source) {
		GL20 gl = Micro.gl20;
		IntBuffer intbuf = Buffers.newIntBuffer(1);
		
		int shader = gl.glCreateShader(type);
		if (shader == 0)
			return -1;
		
		gl.glShaderSource(shader, source);
		gl.glCompileShader(shader);
		gl.glGetShaderiv(shader, GL20.GL_COMPILE_STATUS, intbuf);
		
		int compiled = intbuf.get(0);
		if (compiled == 0) {
			// gl.glGetShaderiv(shader, GL20.GL_INFO_LOG_LENGTH, intbuf);
			// int infoLogLength = intbuf.get(0);
			// if (infoLogLength > 1) {
			String infoLog = gl.glGetShaderInfoLog(shader);
			log += type == GL20.GL_VERTEX_SHADER ? "Vertex shader\n" : "Fragment shader:\n";
			log += infoLog;
			// }
			return -1;
		}
		
		return shader;
	}
	
	protected int createProgram() {
		GL20 gl = Micro.gl20;
		int program = gl.glCreateProgram();
		return program != 0 ? program : -1;
	}
	
	private int linkProgram(int program) {
		GL20 gl = Micro.gl20;
		if (program == -1)
			return -1;
		
		gl.glAttachShader(program, vertexShaderHandle);
		gl.glAttachShader(program, fragmentShaderHandle);
		gl.glLinkProgram(program);
		
		ByteBuffer tmp = ByteBuffer.allocateDirect(4);
		tmp.order(ByteOrder.nativeOrder());
		IntBuffer intBuffer = tmp.asIntBuffer();
		
		gl.glGetProgramiv(program, GL20.GL_LINK_STATUS, intBuffer);
		int linked = intBuffer.get(0);
		if (linked == 0) {
			log = Micro.gl20.glGetProgramInfoLog(program);
			return -1;
		}
		
		return program;
	}
	
	public String getLog() {
		if (compiled)
			log = Micro.gl20.glGetProgramInfoLog(program);
		return log;
	}
	
	public boolean isCompiled() {
		return compiled;
	}
	
	private int fetchAttributeLocation(String name) {
		GL20 gl = Micro.gl20;
		// -2 == not yet cached
		// -1 == cached but not found
		int location;
		if ((location = attributes.get(name, -2)) == -2) {
			location = gl.glGetAttribLocation(program, name);
			attributes.put(name, location);
		}
		return location;
	}
	
	private int fetchUniformLocation(String name) {
		return fetchUniformLocation(name, pedantic);
	}
	
	public int fetchUniformLocation(String name, boolean pedantic) {
		// -2 == not yet cached
		// -1 == cached but not found
		int location;
		if ((location = uniforms.get(name, -2)) == -2) {
			location = Micro.gl20.glGetUniformLocation(program, name);
			if (location == -1 && pedantic) {
				if (compiled)
					throw new IllegalArgumentException("No uniform with name '" + name + "' in shader");
				throw new IllegalStateException("An attempted fetch uniform from uncompiled shader \n" + getLog());
			}
			uniforms.put(name, location);
		}
		return location;
	}
	
	public void setUniformi(String name, int value) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchUniformLocation(name);
		gl.glUniform1i(location, value);
	}
	
	public void setUniformi(int location, int value) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUniform1i(location, value);
	}
	
	public void setUniformi(String name, int value1, int value2) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchUniformLocation(name);
		gl.glUniform2i(location, value1, value2);
	}
	
	public void setUniformi(int location, int value1, int value2) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUniform2i(location, value1, value2);
	}
	
	public void setUniformi(String name, int value1, int value2, int value3) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchUniformLocation(name);
		gl.glUniform3i(location, value1, value2, value3);
	}
	
	public void setUniformi(int location, int value1, int value2, int value3) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUniform3i(location, value1, value2, value3);
	}
	
	public void setUniformi(String name, int value1, int value2, int value3, int value4) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchUniformLocation(name);
		gl.glUniform4i(location, value1, value2, value3, value4);
	}
	
	public void setUniformi(int location, int value1, int value2, int value3, int value4) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUniform4i(location, value1, value2, value3, value4);
	}
	
	public void setUniformf(String name, float value) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchUniformLocation(name);
		gl.glUniform1f(location, value);
	}
	
	public void setUniformf(int location, float value) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUniform1f(location, value);
	}
	
	public void setUniformf(String name, float value1, float value2) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchUniformLocation(name);
		gl.glUniform2f(location, value1, value2);
	}
	
	public void setUniformf(int location, float value1, float value2) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUniform2f(location, value1, value2);
	}
	
	public void setUniformf(String name, float value1, float value2, float value3) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchUniformLocation(name);
		gl.glUniform3f(location, value1, value2, value3);
	}
	
	public void setUniformf(int location, float value1, float value2, float value3) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUniform3f(location, value1, value2, value3);
	}
	
	public void setUniformf(String name, float value1, float value2, float value3, float value4) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchUniformLocation(name);
		gl.glUniform4f(location, value1, value2, value3, value4);
	}
	
	public void setUniformf(int location, float value1, float value2, float value3, float value4) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUniform4f(location, value1, value2, value3, value4);
	}
	
	public void setUniform1fv(String name, float[] values, int offset, int length) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchUniformLocation(name);
		gl.glUniform1fv(location, length, values, offset);
	}
	
	public void setUniform1fv(int location, float[] values, int offset, int length) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUniform1fv(location, length, values, offset);
	}
	
	public void setUniform2fv(String name, float[] values, int offset, int length) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchUniformLocation(name);
		gl.glUniform2fv(location, length / 2, values, offset);
	}
	
	public void setUniform2fv(int location, float[] values, int offset, int length) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUniform2fv(location, length / 2, values, offset);
	}
	
	public void setUniform3fv(String name, float[] values, int offset, int length) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchUniformLocation(name);
		gl.glUniform3fv(location, length / 3, values, offset);
	}
	
	public void setUniform3fv(int location, float[] values, int offset, int length) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUniform3fv(location, length / 3, values, offset);
	}
	
	public void setUniform4fv(String name, float[] values, int offset, int length) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchUniformLocation(name);
		gl.glUniform4fv(location, length / 4, values, offset);
	}
	
	public void setUniform4fv(int location, float[] values, int offset, int length) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUniform4fv(location, length / 4, values, offset);
	}
	
	public void setUniformMatrix(String name, Matrix4 matrix) {
		setUniformMatrix(name, matrix, false);
	}
	
	public void setUniformMatrix(String name, Matrix4 matrix, boolean transpose) {
		setUniformMatrix(fetchUniformLocation(name), matrix, transpose);
	}
	
	public void setUniformMatrix(int location, Matrix4 matrix) {
		setUniformMatrix(location, matrix, false);
	}
	
	public void setUniformMatrix(int location, Matrix4 matrix, boolean transpose) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUniformMatrix4fv(location, 1, transpose, matrix.val, 0);
	}
	
	public void setUniformMatrix(String name, Matrix3 matrix) {
		setUniformMatrix(name, matrix, false);
	}
	
	public void setUniformMatrix(String name, Matrix3 matrix, boolean transpose) {
		setUniformMatrix(fetchUniformLocation(name), matrix, transpose);
	}
	
	public void setUniformMatrix(int location, Matrix3 matrix) {
		setUniformMatrix(location, matrix, false);
	}
	
	public void setUniformMatrix(int location, Matrix3 matrix, boolean transpose) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUniformMatrix3fv(location, 1, transpose, matrix.val, 0);
	}
	
	public void setUniformMatrix3fv(String name, FloatBuffer buffer, int count, boolean transpose) {
		GL20 gl = Micro.gl20;
		checkManaged();
		((Buffer) buffer).position(0);
		int location = fetchUniformLocation(name);
		gl.glUniformMatrix3fv(location, count, transpose, buffer);
	}
	
	/**
	 * Sets an array of uniform matrices with the given name. The {@link Shader} must be bound for this to work.
	 *
	 * @param name      the name of the uniform
	 * @param buffer    buffer containing the matrix data
	 * @param transpose whether the uniform matrix should be transposed
	 */
	public void setUniformMatrix4fv(String name, FloatBuffer buffer, int count, boolean transpose) {
		GL20 gl = Micro.gl20;
		checkManaged();
		((Buffer) buffer).position(0);
		int location = fetchUniformLocation(name);
		gl.glUniformMatrix4fv(location, count, transpose, buffer);
	}
	
	public void setUniformMatrix4fv(int location, float[] values, int offset, int length) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUniformMatrix4fv(location, length / 16, false, values, offset);
	}
	
	public void setUniformMatrix4fv(String name, float[] values, int offset, int length) {
		setUniformMatrix4fv(fetchUniformLocation(name), values, offset, length);
	}
	
	public void setUniformf(String name, Vector2 values) {
		setUniformf(name, values.x, values.y);
	}
	
	public void setUniformf(int location, Vector2 values) {
		setUniformf(location, values.x, values.y);
	}
	
	public void setUniformf(String name, Vector3 values) {
		setUniformf(name, values.x, values.y, values.z);
	}
	
	public void setUniformf(int location, Vector3 values) {
		setUniformf(location, values.x, values.y, values.z);
	}
	
	public void setUniformf(String name, Color values) {
		setUniformf(name, values.r, values.g, values.b, values.a);
	}
	
	public void setUniformf(int location, Color values) {
		setUniformf(location, values.r, values.g, values.b, values.a);
	}
	
	public void setVertexAttribute(String name, int size, int type, boolean normalize, int stride, Buffer buffer) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchAttributeLocation(name);
		if (location == -1)
			return;
		gl.glVertexAttribPointer(location, size, type, normalize, stride, buffer);
	}
	
	public void setVertexAttribute(int location, int size, int type, boolean normalize, int stride, Buffer buffer) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glVertexAttribPointer(location, size, type, normalize, stride, buffer);
	}
	
	public void setVertexAttribute(String name, int size, int type, boolean normalize, int stride, int offset) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchAttributeLocation(name);
		if (location == -1)
			return;
		gl.glVertexAttribPointer(location, size, type, normalize, stride, offset);
	}
	
	public void setVertexAttribute(int location, int size, int type, boolean normalize, int stride, int offset) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glVertexAttribPointer(location, size, type, normalize, stride, offset);
	}
	
	public void bind() {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glUseProgram(program);
	}
	
	public void dispose() {
		GL20 gl = Micro.gl20;
		gl.glUseProgram(0);
		gl.glDeleteShader(vertexShaderHandle);
		gl.glDeleteShader(fragmentShaderHandle);
		gl.glDeleteProgram(program);
		if (shaders.get(Micro.app) != null)
			shaders.get(Micro.app).removeValue(this, true);
	}
	
	public void disableVertexAttribute(String name) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchAttributeLocation(name);
		if (location == -1)
			return;
		gl.glDisableVertexAttribArray(location);
	}
	
	public void disableVertexAttribute(int location) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glDisableVertexAttribArray(location);
	}
	
	public void enableVertexAttribute(String name) {
		GL20 gl = Micro.gl20;
		checkManaged();
		int location = fetchAttributeLocation(name);
		if (location == -1)
			return;
		gl.glEnableVertexAttribArray(location);
	}
	
	public void enableVertexAttribute(int location) {
		GL20 gl = Micro.gl20;
		checkManaged();
		gl.glEnableVertexAttribArray(location);
	}
	
	private void checkManaged() {
		if (invalidated) {
			compileShaders(vertexShaderSource, fragmentShaderSource);
			invalidated = false;
		}
	}
	
	private void addManagedShader(Application app, Shader shader) {
		Array<Shader> managedResources = shaders.get(app);
		if (managedResources == null)
			managedResources = new Array<Shader>();
		managedResources.add(shader);
		shaders.put(app, managedResources);
	}
	
	public static void invalidateAllShaderPrograms(final Application app) {
		if (Micro.gl20 == null)
			return;
		
		Array<Shader> shaderArray = shaders.get(app);
		if (shaderArray == null)
			return;
		
		for (int i = 0; i < shaderArray.size; i++) {
			shaderArray.get(i).invalidated = true;
			shaderArray.get(i).checkManaged();
		}
	}
	
	public static void clearAllShaderPrograms(Application app) {
		shaders.remove(app);
	}
	
	public static String getManagedStatus() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Managed shaders/app: { ");
		for (final Application application : shaders.keys()) {
			builder.append(shaders.get(application).size);
			builder.append(" ");
		}
		builder.append("}");
		return builder.toString();
	}
	
	public static int getNumManagedShaderPrograms() {
		return shaders.get(Micro.app).size;
	}
	
	public void setAttributef(String name, float value1, float value2, float value3, float value4) {
		GL20 gl = Micro.gl20;
		int location = fetchAttributeLocation(name);
		gl.glVertexAttrib4f(location, value1, value2, value3, value4);
	}
	
	IntBuffer params = Buffers.newIntBuffer(1);
	IntBuffer type = Buffers.newIntBuffer(1);
	
	private void fetchUniforms() {
		((Buffer) params).clear();
		Micro.gl20.glGetProgramiv(program, GL20.GL_ACTIVE_UNIFORMS, params);
		int numUniforms = params.get(0);
		
		uniformNames = new String[numUniforms];
		
		for (int i = 0; i < numUniforms; i++) {
			((Buffer) params).clear();
			params.put(0, 1);
			((Buffer) type).clear();
			String name = Micro.gl20.glGetActiveUniform(program, i, params, type);
			int location = Micro.gl20.glGetUniformLocation(program, name);
			uniforms.put(name, location);
			uniformTypes.put(name, type.get(0));
			uniformSizes.put(name, params.get(0));
			uniformNames[i] = name;
		}
	}
	
	private void fetchAttributes() {
		((Buffer) params).clear();
		Micro.gl20.glGetProgramiv(program, GL20.GL_ACTIVE_ATTRIBUTES, params);
		int numAttributes = params.get(0);
		
		attributeNames = new String[numAttributes];
		
		for (int i = 0; i < numAttributes; i++) {
			((Buffer) params).clear();
			params.put(0, 1);
			((Buffer) type).clear();
			String name = Micro.gl20.glGetActiveAttrib(program, i, params, type);
			int location = Micro.gl20.glGetAttribLocation(program, name);
			attributes.put(name, location);
			attributeTypes.put(name, type.get(0));
			attributeSizes.put(name, params.get(0));
			attributeNames[i] = name;
		}
	}
	
	public boolean hasAttribute(String name) {
		return attributes.containsKey(name);
	}
	
	public int getAttributeType(String name) {
		return attributeTypes.get(name, 0);
	}
	
	public int getAttributeLocation(String name) {
		return attributes.get(name, -1);
	}
	
	public int getAttributeSize(String name) {
		return attributeSizes.get(name, 0);
	}
	
	public boolean hasUniform(String name) {
		return uniforms.containsKey(name);
	}
	
	public int getUniformType(String name) {
		return uniformTypes.get(name, 0);
	}
	
	public int getUniformLocation(String name) {
		return uniforms.get(name, -1);
	}
	
	public int getUniformSize(String name) {
		return uniformSizes.get(name, 0);
	}
	
	public String[] getAttributes() {
		return attributeNames;
	}
	
	public String[] getUniforms() {
		return uniformNames;
	}
	
	public String getVertexShaderSource() {
		return vertexShaderSource;
	}
	
	public String getFragmentShaderSource() {
		return fragmentShaderSource;
	}
	
	public int getHandle() {
		return program;
	}
	
}
