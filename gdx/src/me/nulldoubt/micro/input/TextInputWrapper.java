
package me.nulldoubt.micro.input;

public interface TextInputWrapper {

	String getText ();

	int getSelectionStart ();

	int getSelectionEnd ();

	void setText (String text);

	void setPosition (int position);

	boolean shouldClose ();
}
