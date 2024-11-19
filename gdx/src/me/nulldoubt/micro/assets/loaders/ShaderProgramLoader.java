package me.nulldoubt.micro.assets.loaders;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.AssetLoaderParameters;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.glutils.Shader;
import me.nulldoubt.micro.utils.Array;

public class ShaderProgramLoader extends AsynchronousAssetLoader<Shader, ShaderProgramLoader.ShaderProgramParameter> {
	
	private String vertexFileSuffix = ".vert";
	private String fragmentFileSuffix = ".frag";
	
	public ShaderProgramLoader(FileHandleResolver resolver) {
		super(resolver);
	}
	
	public ShaderProgramLoader(FileHandleResolver resolver, String vertexFileSuffix, String fragmentFileSuffix) {
		super(resolver);
		this.vertexFileSuffix = vertexFileSuffix;
		this.fragmentFileSuffix = fragmentFileSuffix;
	}
	
	@Override
	public Array<AssetDescriptor<?>> getDependencies(String fileName, FileHandle file, ShaderProgramParameter parameter) {
		return null;
	}
	
	@Override
	public void loadAsync(AssetManager manager, String fileName, FileHandle file, ShaderProgramParameter parameter) {}
	
	@Override
	public Shader loadSync(AssetManager manager, String fileName, FileHandle file, ShaderProgramParameter parameter) {
		String vertFileName = null, fragFileName = null;
		if (parameter != null) {
			if (parameter.vertexFile != null)
				vertFileName = parameter.vertexFile;
			if (parameter.fragmentFile != null)
				fragFileName = parameter.fragmentFile;
		}
		if (vertFileName == null && fileName.endsWith(fragmentFileSuffix)) {
			vertFileName = fileName.substring(0, fileName.length() - fragmentFileSuffix.length()) + vertexFileSuffix;
		}
		if (fragFileName == null && fileName.endsWith(vertexFileSuffix)) {
			fragFileName = fileName.substring(0, fileName.length() - vertexFileSuffix.length()) + fragmentFileSuffix;
		}
		FileHandle vertexFile = vertFileName == null ? file : resolve(vertFileName);
		FileHandle fragmentFile = fragFileName == null ? file : resolve(fragFileName);
		String vertexCode = vertexFile.readString();
		String fragmentCode = vertexFile.equals(fragmentFile) ? vertexCode : fragmentFile.readString();
		if (parameter != null) {
			if (parameter.prependVertexCode != null)
				vertexCode = parameter.prependVertexCode + vertexCode;
			if (parameter.prependFragmentCode != null)
				fragmentCode = parameter.prependFragmentCode + fragmentCode;
		}
		
		final Shader shader = new Shader(vertexCode, fragmentCode);
		if ((parameter == null || parameter.logOnCompileFailure) && !shader.isCompiled())
			Micro.app.error(AssetManager.TAG, "ShaderProgram " + fileName + " failed to compile:\n" + shader.getLog());
		
		return shader;
	}
	
	public static class ShaderProgramParameter extends AssetLoaderParameters<Shader> {
		
		/**
		 * File name to be used for the vertex program instead of the default determined by the file name used to submit this asset
		 * to AssetManager.
		 */
		public String vertexFile;
		/**
		 * File name to be used for the fragment program instead of the default determined by the file name used to submit this
		 * asset to AssetManager.
		 */
		public String fragmentFile;
		/**
		 * Whether to log (at the error level) the shader's log if it fails to compile. Default true.
		 */
		public boolean logOnCompileFailure = true;
		/**
		 * Code that is always added to the vertex shader code. This is added as-is, and you should include a newline (`\n`) if
		 * needed. {@linkplain Shader#prependVertexCode} is placed before this code.
		 */
		public String prependVertexCode;
		/**
		 * Code that is always added to the fragment shader code. This is added as-is, and you should include a newline (`\n`) if
		 * needed. {@linkplain Shader#prependFragmentCode} is placed before this code.
		 */
		public String prependFragmentCode;
		
	}
	
}
