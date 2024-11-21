
package me.nulldoubt.micro.input;

import me.nulldoubt.micro.Input;

public class NativeInputConfiguration {
	
	private Input.OnscreenKeyboardType type = Input.OnscreenKeyboardType.Default;
	private boolean preventCorrection = false;
	
	private TextInputWrapper textInputWrapper;
	private boolean isMultiLine = false;
	private Integer maxLength;
	private Input.InputStringValidator validator;
	private String placeholder = "";
	private boolean showPasswordButton = false;
	private String[] autoComplete = null;
	
	public Input.OnscreenKeyboardType getType() {
		return type;
	}
	
	public NativeInputConfiguration setType(Input.OnscreenKeyboardType type) {
		this.type = type;
		return this;
	}
	
	public boolean isPreventCorrection() {
		return preventCorrection;
	}
	
	public NativeInputConfiguration setPreventCorrection(boolean preventCorrection) {
		this.preventCorrection = preventCorrection;
		return this;
	}
	
	public TextInputWrapper getTextInputWrapper() {
		return textInputWrapper;
	}
	
	public NativeInputConfiguration setTextInputWrapper(TextInputWrapper textInputWrapper) {
		this.textInputWrapper = textInputWrapper;
		return this;
	}
	
	public boolean isMultiLine() {
		return isMultiLine;
	}
	
	public NativeInputConfiguration setMultiLine(boolean multiLine) {
		isMultiLine = multiLine;
		return this;
	}
	
	public Integer getMaxLength() {
		return maxLength;
	}
	
	public NativeInputConfiguration setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;
		return this;
	}
	
	public Input.InputStringValidator getValidator() {
		return validator;
	}
	
	public NativeInputConfiguration setValidator(Input.InputStringValidator validator) {
		this.validator = validator;
		return this;
	}
	
	public String getPlaceholder() {
		return placeholder;
	}
	
	public NativeInputConfiguration setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
		return this;
	}
	
	public boolean isShowPasswordButton() {
		return showPasswordButton;
	}
	
	public NativeInputConfiguration setShowPasswordButton(boolean showPasswordButton) {
		this.showPasswordButton = showPasswordButton;
		return this;
	}
	
	public String[] getAutoComplete() {
		return autoComplete;
	}
	
	public NativeInputConfiguration setAutoComplete(String[] autoComplete) {
		this.autoComplete = autoComplete;
		return this;
	}
	
	public void validate() {
		String message = null;
		if (type == null)
			message = "OnscreenKeyboardType needs to be non null";
		if (textInputWrapper == null)
			message = "TextInputWrapper needs to be non null";
		if (showPasswordButton && type != Input.OnscreenKeyboardType.Password)
			message = "ShowPasswordButton only works with OnscreenKeyboardType.Password";
		if (placeholder == null)
			message = "Placeholder needs to be non null";
		if (autoComplete != null && type != Input.OnscreenKeyboardType.Default)
			message = "AutoComplete should only be used with OnscreenKeyboardType.Default";
		if (autoComplete != null && isMultiLine)
			message = "AutoComplete shouldn't be used with multiline";
		
		if (message != null)
			throw new IllegalArgumentException("NativeInputConfiguration validation failed: " + message);
	}
	
}
