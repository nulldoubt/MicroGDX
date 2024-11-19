
package com.badlogic.gdx.tests;

import com.badlogic.gdx.Micro;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.tests.utils.GdxTest;
import com.badlogic.gdx.utils.ScreenUtils;

public class GLES30Test extends GdxTest {

	SpriteBatch batch;
	Texture texture;
	ShaderProgram shaderProgram;

	@Override
	public void create () {
		Micro.app.log("GLES30Test", "GL_VERSION = " + Micro.gl.glGetString(GL20.GL_VERSION));
		batch = new SpriteBatch();
		texture = new Texture(Micro.files.internal("data/badlogic.jpg"));
		shaderProgram = new ShaderProgram(Micro.files.internal("data/shaders/gles30sprite.vert"),
			Micro.files.internal("data/shaders/gles30sprite.frag"));
		Micro.app.log("GLES30Test", shaderProgram.getLog());
		if (shaderProgram.isCompiled()) {
			Micro.app.log("GLES30Test", "Shader compiled");
			batch.setShader(shaderProgram);
		}
	}

	@Override
	public void render () {
		ScreenUtils.clear(0, 0, 0, 1);

		batch.begin();
		batch.draw(texture, 0, 0, Micro.graphics.getWidth() / 2f, Micro.graphics.getHeight() / 2f);
		batch.end();
	}

	@Override
	public void dispose () {
		texture.dispose();
		batch.dispose();
		shaderProgram.dispose();
	}
}
