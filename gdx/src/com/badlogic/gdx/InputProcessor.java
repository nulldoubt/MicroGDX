package com.badlogic.gdx;

public interface InputProcessor {
	
	default boolean keyDown(final int keycode) {
		return false;
	}
	
	default boolean keyUp(final int keycode) {
		return false;
	}
	
	default boolean keyTyped(final char character) {
		return false;
	}
	
	default boolean touchDown(final int screenX, final int screenY, final int pointer, final int button) {
		return false;
	}
	
	default boolean touchUp(final int screenX, final int screenY, final int pointer, final int button) {
		return false;
	}
	
	default boolean touchCancelled(final int screenX, final int screenY, final int pointer, final int button) {
		return false;
	}
	
	default boolean touchDragged(final int screenX, final int screenY, final int pointer) {
		return false;
	}
	
	default boolean mouseMoved(final int screenX, final int screenY) {
		return false;
	}
	
	default boolean scrolled(final float amountX, final float amountY) {
		return false;
	}
	
}
