package com.badlogic.gdx.graphics.profiling;

import com.badlogic.gdx.Micro;
import com.badlogic.gdx.utils.GdxRuntimeException;

import static com.badlogic.gdx.graphics.profiling.GLInterceptor.resolveErrorNumber;

public interface GLErrorListener {
	
	void onError(int error);
	
	GLErrorListener LOGGING_LISTENER = (error) -> {
		String place = null;
		try {
			final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
			for (int i = 0; i < stack.length; i++) {
				if ("check".equals(stack[i].getMethodName())) {
					if (i + 1 < stack.length) {
						final StackTraceElement glMethod = stack[i + 1];
						place = glMethod.getMethodName();
					}
					break;
				}
			}
		} catch (Exception ignored) {
		}
		
		if (place != null)
			Micro.app.error("GLProfiler", "Error " + resolveErrorNumber(error) + " from " + place);
		else
			Micro.app.error("GLProfiler", "Error " + resolveErrorNumber(error) + " at: ", new Exception());
	};
	
	GLErrorListener THROWING_LISTENER = (error) -> {
		throw new GdxRuntimeException("GLProfiler: Got GL error " + resolveErrorNumber(error));
	};
	
}
