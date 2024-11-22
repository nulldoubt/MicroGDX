package me.nulldoubt.micro.graphics.profiling;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;

import static me.nulldoubt.micro.graphics.profiling.GLInterceptor.resolveErrorNumber;

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
		} catch (Exception _) {}
		
		if (place != null)
			Micro.app.error("GLProfiler", "Error " + resolveErrorNumber(error) + " from " + place);
		else
			Micro.app.error("GLProfiler", "Error " + resolveErrorNumber(error) + " at: ", new Exception());
	};
	
	GLErrorListener THROWING_LISTENER = (error) -> {
		throw new MicroRuntimeException("GLProfiler: Got GL error " + resolveErrorNumber(error));
	};
	
}
