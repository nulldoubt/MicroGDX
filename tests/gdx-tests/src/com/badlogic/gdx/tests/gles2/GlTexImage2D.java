/*******************************************************************************
 * Copyright 2024 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.tests.gles2;

import java.nio.FloatBuffer;
import java.nio.ByteBuffer;

import com.badlogic.gdx.Micro;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.tests.utils.GdxTest;
import com.badlogic.gdx.utils.BufferUtils;

/** Added during glTexImage2D corrections.
 * @author Ret-Mode */
public class GlTexImage2D extends GdxTest {

	ShaderProgram shader;

	int texture = 0;
	int pixmapTexture = 0;

	int vertsBuffer = 0;
	int pixmapBuffer = 0;
	int uvBuffer = 0;

	Pixmap pixmapCheck;

	String vertexShader = "attribute vec2 vPosition;                    \n" + "attribute vec2 vTexCoords;                   \n"
		+ "varying   vec2 fTexCoords;                   \n" + "void main()                                  \n"
		+ "{                                            \n" + "   gl_Position = vec4(vPosition, 0.0, 1.0);  \n"
		+ "   fTexCoords  = vTexCoords;                 \n" + "}";
	String fragmentShader = "#ifdef GL_ES\n" + "precision mediump float;\n" + "#endif\n"
		+ "varying vec2 fTexCoords;                       \n" + "uniform sampler2D uTex2d;                      \n"
		+ "void main()                                    \n" + "{                                              \n"
		+ "  gl_FragColor = texture2D(uTex2d, fTexCoords);\n" + "}";

	FloatBuffer verticesData = BufferUtils.newFloatBuffer(8);
	FloatBuffer pixmapVerticesData = BufferUtils.newFloatBuffer(8);
	FloatBuffer uvData = BufferUtils.newFloatBuffer(8);
	ByteBuffer textureColorData = BufferUtils.newByteBuffer(12);

	@Override
	public void create () {

		float[] vertices = {-1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, -1.0f, 1.0f};
		float[] pixmapVertices = {1.0f, 0.0f, -1.0f, 0.0f, -1.0f, -1.0f, 1.0f, -1.0f};
		float[] uv = {0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f};
		byte[] data = {0x7F, 0x00, 0x00, /* r */
			0x00, 0x7F, 0x00, /* g */
			0x00, 0x00, 0x7F, /* b */
			0x00, 0x7F, 0x7F}; /* rg */

		verticesData.put(vertices);
		verticesData.rewind();
		pixmapVerticesData.put(pixmapVertices);
		pixmapVerticesData.rewind();
		uvData.put(uv);
		uvData.rewind();
		textureColorData.put(data);
		textureColorData.rewind();
	}

	@Override
	public void render () {
		/*
		 * check if OpenGL context needs to be reloaded; checking only texture should be sufficient; but i guess this check is
		 * redundant
		 */
		if (!Micro.gl20.glIsTexture(texture)) {
			reload();
		}

		/* bump bytes and upload to gpu */
		for (int colorComponent = 0; colorComponent < textureColorData.capacity(); ++colorComponent) {
			textureColorData.put(colorComponent, (byte)(textureColorData.get(colorComponent) + 1));
		}
		textureColorData.rewind();
		Micro.gl20.glActiveTexture(GL20.GL_TEXTURE1);
		Micro.gl20.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGB, 2, 2, 0, GL20.GL_RGB, GL20.GL_UNSIGNED_BYTE, textureColorData);
		Micro.gl20.glActiveTexture(GL20.GL_TEXTURE0);

		/* init drawing */
		Micro.gl20.glViewport(0, 0, Micro.graphics.getBackBufferWidth(), Micro.graphics.getBackBufferHeight());
		Micro.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

		/* draw texture built from bytes */
		Micro.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vertsBuffer);
		Micro.gl20.glVertexAttribPointer(shader.getAttributeLocation("vPosition"), 2, GL20.GL_FLOAT, false, 0, 0);
		shader.setUniformi("uTex2d", 1);
		Micro.gl20.glDrawArrays(GL20.GL_TRIANGLE_FAN, 0, 4);

		/* draw pixmap */
		Micro.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, pixmapBuffer);
		Micro.gl20.glVertexAttribPointer(shader.getAttributeLocation("vPosition"), 2, GL20.GL_FLOAT, false, 0, 0);
		shader.setUniformi("uTex2d", 2);
		Micro.gl20.glDrawArrays(GL20.GL_TRIANGLE_FAN, 0, 4);

	}

	private void reload () {

		/* common */
		Micro.gl20.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);

		/* generate texture */
		texture = Micro.gl20.glGenTexture();
		Micro.gl20.glActiveTexture(GL20.GL_TEXTURE1);
		Micro.gl20.glBindTexture(GL20.GL_TEXTURE_2D, texture);
		Micro.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
		Micro.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
		Micro.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);
		Micro.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
		Micro.gl20.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGB, 2, 2, 0, GL20.GL_RGB, GL20.GL_UNSIGNED_BYTE, textureColorData);

		/* load pixmap to verify that pixmap was not broken */
		pixmapCheck = new Pixmap(Micro.files.internal("data/walkanim.png"));

		/* generate texture for pixmap */
		pixmapTexture = Micro.gl20.glGenTexture();
		Micro.gl20.glActiveTexture(GL20.GL_TEXTURE2);
		Micro.gl20.glBindTexture(GL20.GL_TEXTURE_2D, pixmapTexture);
		Micro.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
		Micro.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
		Micro.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);
		Micro.gl20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
		Micro.gl20.glTexImage2D(GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA, pixmapCheck.getWidth(), pixmapCheck.getHeight(), 0, GL20.GL_RGBA,
			GL20.GL_UNSIGNED_BYTE, pixmapCheck.getPixels());

		/* set shader */
		shader = new ShaderProgram(vertexShader, fragmentShader);
		shader.bind();

		/* set vertices */
		vertsBuffer = Micro.gl20.glGenBuffer();
		Micro.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vertsBuffer);
		Micro.gl20.glBufferData(GL20.GL_ARRAY_BUFFER, 8 * 4, verticesData, GL20.GL_STATIC_DRAW);

		/* set pixmap verts */
		pixmapBuffer = Micro.gl20.glGenBuffer();
		Micro.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, pixmapBuffer);
		Micro.gl20.glBufferData(GL20.GL_ARRAY_BUFFER, 8 * 4, pixmapVerticesData, GL20.GL_STATIC_DRAW);

		/* set uvs */
		uvBuffer = Micro.gl20.glGenBuffer();
		Micro.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, uvBuffer);
		Micro.gl20.glBufferData(GL20.GL_ARRAY_BUFFER, 8 * 4, uvData, GL20.GL_STATIC_DRAW);
		Micro.gl20.glVertexAttribPointer(shader.getAttributeLocation("vTexCoords"), 2, GL20.GL_FLOAT, false, 0, 0);

		/* finalize setup */
		Micro.gl20.glEnableVertexAttribArray(shader.getAttributeLocation("vPosition"));
		Micro.gl20.glEnableVertexAttribArray(shader.getAttributeLocation("vTexCoords"));

		Micro.gl20.glActiveTexture(GL20.GL_TEXTURE0);
		Micro.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
	}

	@Override
	public void pause () {
		dispose();
	}

	@Override
	public void resume () {
		reload();
	}

	@Override
	public void dispose () {
		Micro.gl20.glDisableVertexAttribArray(shader.getAttributeLocation("vPosition"));
		Micro.gl20.glDisableVertexAttribArray(shader.getAttributeLocation("vTexCoords"));

		Micro.gl20.glDeleteBuffer(vertsBuffer);
		Micro.gl20.glDeleteBuffer(pixmapBuffer);
		Micro.gl20.glDeleteBuffer(uvBuffer);

		Micro.gl20.glDeleteTexture(texture);
		Micro.gl20.glDeleteTexture(pixmapTexture);

		shader.dispose();
		pixmapCheck.dispose();
	}
}
