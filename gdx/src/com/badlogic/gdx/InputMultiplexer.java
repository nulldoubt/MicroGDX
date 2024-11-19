package com.badlogic.gdx;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.SnapshotArray;

public class InputMultiplexer implements InputProcessor {
	
	private final SnapshotArray<InputProcessor> processors = new SnapshotArray<>(4);
	
	public InputMultiplexer() {}
	
	public InputMultiplexer(final InputProcessor... processors) {
		this.processors.addAll(processors);
	}
	
	public void add(final int index, final InputProcessor processor) {
		if (processor == null)
			throw new NullPointerException("processor cannot be null");
		processors.insert(index, processor);
	}
	
	public void remove(final int index) {
		processors.removeIndex(index);
	}
	
	public void add(final InputProcessor processor) {
		if (processor == null)
			throw new NullPointerException("processor cannot be null");
		processors.add(processor);
	}
	
	public void remove(final InputProcessor processor) {
		processors.removeValue(processor, true);
	}
	
	public int size() {
		return processors.size;
	}
	
	public void clear() {
		processors.clear();
	}
	
	public void set(final InputProcessor... processors) {
		this.processors.clear();
		this.processors.addAll(processors);
	}
	
	public void set(final Array<InputProcessor> processors) {
		this.processors.clear();
		this.processors.addAll(processors);
	}
	
	public SnapshotArray<InputProcessor> get() {
		return processors;
	}
	
	public boolean keyDown(final int keycode) {
		final Object[] items = processors.begin();
		try {
			for (int i = 0, n = processors.size; i < n; i++)
				if (((InputProcessor) items[i]).keyDown(keycode))
					return true;
		} finally {
			processors.end();
		}
		return false;
	}
	
	public boolean keyUp(final int keycode) {
		final Object[] items = processors.begin();
		try {
			for (int i = 0, n = processors.size; i < n; i++)
				if (((InputProcessor) items[i]).keyUp(keycode))
					return true;
		} finally {
			processors.end();
		}
		return false;
	}
	
	public boolean keyTyped(final char character) {
		final Object[] items = processors.begin();
		try {
			for (int i = 0, n = processors.size; i < n; i++)
				if (((InputProcessor) items[i]).keyTyped(character))
					return true;
		} finally {
			processors.end();
		}
		return false;
	}
	
	public boolean touchDown(final int screenX, final int screenY, final int pointer, final int button) {
		final Object[] items = processors.begin();
		try {
			for (int i = 0, n = processors.size; i < n; i++)
				if (((InputProcessor) items[i]).touchDown(screenX, screenY, pointer, button))
					return true;
		} finally {
			processors.end();
		}
		return false;
	}
	
	public boolean touchUp(final int screenX, final int screenY, final int pointer, final int button) {
		final Object[] items = processors.begin();
		try {
			for (int i = 0, n = processors.size; i < n; i++)
				if (((InputProcessor) items[i]).touchUp(screenX, screenY, pointer, button))
					return true;
		} finally {
			processors.end();
		}
		return false;
	}
	
	public boolean touchCancelled(final int screenX, final int screenY, final int pointer, final int button) {
		final Object[] items = processors.begin();
		try {
			for (int i = 0, n = processors.size; i < n; i++)
				if (((InputProcessor) items[i]).touchCancelled(screenX, screenY, pointer, button))
					return true;
		} finally {
			processors.end();
		}
		return false;
	}
	
	public boolean touchDragged(final int screenX, final int screenY, final int pointer) {
		final Object[] items = processors.begin();
		try {
			for (int i = 0, n = processors.size; i < n; i++)
				if (((InputProcessor) items[i]).touchDragged(screenX, screenY, pointer))
					return true;
		} finally {
			processors.end();
		}
		return false;
	}
	
	public boolean mouseMoved(final int screenX, final int screenY) {
		final Object[] items = processors.begin();
		try {
			for (int i = 0, n = processors.size; i < n; i++)
				if (((InputProcessor) items[i]).mouseMoved(screenX, screenY))
					return true;
		} finally {
			processors.end();
		}
		return false;
	}
	
	public boolean scrolled(final float amountX, final float amountY) {
		final Object[] items = processors.begin();
		try {
			for (int i = 0, n = processors.size; i < n; i++)
				if (((InputProcessor) items[i]).scrolled(amountX, amountY))
					return true;
		} finally {
			processors.end();
		}
		return false;
	}
	
}
