/*
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.badlogic.gdx.tests.g3d;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Micro;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureArray;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.glutils.Shader;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.tests.utils.GdxTest;
import com.badlogic.gdx.tests.utils.GdxTestConfig;

/** @author Tomski **/
@GdxTestConfig(requireGL30 = true)
public class TextureArrayTest extends GdxTest {

	TextureArray textureArray;
	Mesh terrain;

	Shader shader;

	PerspectiveCamera camera;
	FirstPersonCameraController cameraController;

	Matrix4 modelView = new Matrix4();

	GLProfiler glProfiler;

	@Override
	public void create () {
		glProfiler = new GLProfiler(Micro.graphics);
		glProfiler.enable();

		Shader.prependVertexCode = Micro.app.getType().equals(Application.ApplicationType.Desktop)
			? "#version 140\n #extension GL_EXT_texture_array : enable\n"
			: "#version 300 es\n";
		Shader.prependFragmentCode = Micro.app.getType().equals(Application.ApplicationType.Desktop)
			? "#version 140\n #extension GL_EXT_texture_array : enable\n"
			: "#version 300 es\n";

		String[] texPaths = new String[] {"data/g3d/materials/Searing Gorge.jpg", "data/g3d/materials/Lava Cracks.jpg",
			"data/g3d/materials/Deep Fire.jpg"};

		camera = new PerspectiveCamera(67, Micro.graphics.getWidth(), Micro.graphics.getHeight());
		camera.position.set(8, 10f, 20f);
		camera.lookAt(10, 0, 10);
		camera.up.set(0, 1, 0);
		camera.update();
		cameraController = new FirstPersonCameraController(camera);
		Micro.input.setInputProcessor(cameraController);

		FileHandle[] texFiles = new FileHandle[texPaths.length];
		for (int i = 0; i < texPaths.length; i++) {
			texFiles[i] = Micro.files.internal(texPaths[i]);
		}

		textureArray = new TextureArray(true, texFiles);
		textureArray.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
		textureArray.setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear);
		shader = new Shader(Micro.files.internal("data/shaders/texturearray.vert"),
			Micro.files.internal("data/shaders/texturearray.frag"));
		System.out.println(shader.getLog());

		int vertexStride = 6;
		int vertexCount = 100 * 100;
		terrain = new Mesh(false, vertexCount * 6, 0, new VertexAttributes(VertexAttribute.Position(),
			new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 3, Shader.TEXCOORD_ATTRIBUTE + 0)));

		Pixmap data = new Pixmap(Micro.files.internal("data/g3d/heightmap.png"));
		float[] vertices = new float[vertexCount * vertexStride * 6];
		int idx = 0;
		for (int i = 0; i < 100 - 1; i++) {
			for (int j = 0; j < 100 - 1; j++) {
				idx = addVertex(i, j, vertices, data, idx);
				idx = addVertex(i, j + 1, vertices, data, idx);
				idx = addVertex(i + 1, j, vertices, data, idx);

				idx = addVertex(i, j + 1, vertices, data, idx);
				idx = addVertex(i + 1, j + 1, vertices, data, idx);
				idx = addVertex(i + 1, j, vertices, data, idx);
			}
		}
		terrain.setVertices(vertices);

		data.dispose();
	}

	Color tmpColor = new Color();

	private int addVertex (int i, int j, float[] vertsOut, Pixmap heightmap, int idx) {
		int pixel = heightmap.getPixel((int)(i / 100f * heightmap.getWidth()), (int)(j / 100f * heightmap.getHeight()));
		tmpColor.set(pixel);
		vertsOut[idx++] = i / 5f;
		vertsOut[idx++] = tmpColor.r * 25f / 5f;
		vertsOut[idx++] = j / 5f;
		vertsOut[idx++] = i / 20f;
		vertsOut[idx++] = j / 20f;
		vertsOut[idx++] = (tmpColor.r * 3f) - 0.5f;
		return idx;
	}

	@Override
	public void render () {
		Micro.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		Micro.gl.glEnable(GL20.GL_DEPTH_TEST);
		Micro.gl.glDepthFunc(GL20.GL_LEQUAL);
		Micro.gl.glCullFace(GL20.GL_BACK);

		modelView.translate(10f, 0, 10f).rotate(0, 1f, 0, 2f * Micro.graphics.getDeltaTime()).translate(-10f, 0, -10f);

		cameraController.update();

		textureArray.bind();

		shader.bind();
		shader.setUniformi("u_textureArray", 0);
		shader.setUniformMatrix("u_projViewTrans", camera.combined);
		shader.setUniformMatrix("u_modelView", modelView);
		terrain.render(shader, GL20.GL_TRIANGLES);
	}

	@Override
	public void dispose () {
		terrain.dispose();
		shader.dispose();
	}
}
