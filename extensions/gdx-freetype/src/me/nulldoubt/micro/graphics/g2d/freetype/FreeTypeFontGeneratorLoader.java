/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package me.nulldoubt.micro.graphics.g2d.freetype;

import me.nulldoubt.micro.assets.AssetDescriptor;
import me.nulldoubt.micro.assets.AssetLoaderParameters;
import me.nulldoubt.micro.assets.AssetManager;
import me.nulldoubt.micro.assets.loaders.FileHandleResolver;
import me.nulldoubt.micro.assets.loaders.SynchronousAssetLoader;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.utils.collections.Array;

/** Makes {@link FreeTypeFontGenerator} managable via {@link AssetManager}.
 * <p>
 * Do
 * {@code assetManager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(new InternalFileHandleResolver()))}
 * to register it.
 * </p>
 * @author Daniel Holderbaum */
public class FreeTypeFontGeneratorLoader
	extends SynchronousAssetLoader<FreeTypeFontGenerator, FreeTypeFontGeneratorLoader.FreeTypeFontGeneratorParameters> {

	public FreeTypeFontGeneratorLoader (FileHandleResolver resolver) {
		super(resolver);
	}

	@Override
	public FreeTypeFontGenerator load (AssetManager assetManager, String fileName, FileHandle file,
		FreeTypeFontGeneratorParameters parameter) {
		FreeTypeFontGenerator generator = null;
		if (file.extension().equals("gen")) {
			generator = new FreeTypeFontGenerator(file.sibling(file.nameWithoutExtension()));
		} else {
			generator = new FreeTypeFontGenerator(file);
		}
		return generator;
	}

	@Override
	public Array<AssetDescriptor> getDependencies (String fileName, FileHandle file, FreeTypeFontGeneratorParameters parameter) {
		return null;
	}

	public static class FreeTypeFontGeneratorParameters extends AssetLoaderParameters<FreeTypeFontGenerator> {
	}
}
