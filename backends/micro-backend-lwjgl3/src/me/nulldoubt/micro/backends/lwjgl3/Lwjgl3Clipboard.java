package me.nulldoubt.micro.backends.lwjgl3;

import me.nulldoubt.micro.Clipboard;
import me.nulldoubt.micro.Micro;
import org.lwjgl.glfw.GLFW;

public class Lwjgl3Clipboard implements Clipboard {
	
	@Override
	public boolean hasContents() {
		String contents = getContents();
		return contents != null && !contents.isEmpty();
	}
	
	@Override
	public String getContents() {
		return GLFW.glfwGetClipboardString(((Lwjgl3Graphics) Micro.graphics).getWindow().getWindowHandle());
	}
	
	@Override
	public void setContents(String content) {
		GLFW.glfwSetClipboardString(((Lwjgl3Graphics) Micro.graphics).getWindow().getWindowHandle(), content);
	}
	
}
