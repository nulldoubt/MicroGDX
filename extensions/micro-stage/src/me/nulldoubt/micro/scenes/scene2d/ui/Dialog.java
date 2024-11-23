package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.math.Interpolation;
import me.nulldoubt.micro.scenes.scene2d.*;
import me.nulldoubt.micro.scenes.scene2d.actions.Actions;
import me.nulldoubt.micro.scenes.scene2d.ui.Label.LabelStyle;
import me.nulldoubt.micro.scenes.scene2d.ui.TextButton.TextButtonStyle;
import me.nulldoubt.micro.scenes.scene2d.utils.ChangeListener;
import me.nulldoubt.micro.scenes.scene2d.utils.FocusListener;
import me.nulldoubt.micro.utils.collections.ObjectMap;

import static me.nulldoubt.micro.scenes.scene2d.actions.Actions.fadeOut;
import static me.nulldoubt.micro.scenes.scene2d.actions.Actions.sequence;

public class Dialog extends Window {
	
	Table contentTable, buttonTable;
	private Skin skin;
	ObjectMap<Actor, Object> values = new ObjectMap<>();
	boolean cancelHide;
	Actor previousKeyboardFocus, previousScrollFocus;
	FocusListener focusListener;
	
	protected InputListener ignoreTouchDown = new InputListener() {
		public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
			event.cancel();
			return false;
		}
	};
	
	public Dialog(String title, Skin skin) {
		super(title, skin.get(WindowStyle.class));
		setSkin(skin);
		this.skin = skin;
		initialize();
	}
	
	public Dialog(String title, Skin skin, String windowStyleName) {
		super(title, skin.get(windowStyleName, WindowStyle.class));
		setSkin(skin);
		this.skin = skin;
		initialize();
	}
	
	public Dialog(String title, WindowStyle windowStyle) {
		super(title, windowStyle);
		initialize();
	}
	
	private void initialize() {
		setModal(true);
		
		defaults().space(6);
		add(contentTable = new Table(skin)).expand().fill();
		row();
		add(buttonTable = new Table(skin)).fillX();
		
		contentTable.defaults().space(6);
		buttonTable.defaults().space(6);
		
		buttonTable.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				if (!values.containsKey(actor))
					return;
				while (actor.getParent() != buttonTable)
					actor = actor.getParent();
				result(values.get(actor));
				if (!cancelHide)
					hide();
				cancelHide = false;
			}
		});
		
		focusListener = new FocusListener() {
			public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
				if (!focused)
					focusChanged(event);
			}
			
			public void scrollFocusChanged(FocusEvent event, Actor actor, boolean focused) {
				if (!focused)
					focusChanged(event);
			}
			
			private void focusChanged(FocusEvent event) {
				Stage stage = getStage();
				if (isModal && stage != null && stage.getRoot().getChildren().size > 0
						&& stage.getRoot().getChildren().peek() == Dialog.this) { // Dialog is top most actor.
					Actor newFocusedActor = event.getRelatedActor();
					if (newFocusedActor != null && !newFocusedActor.isDescendantOf(Dialog.this)
							&& !(newFocusedActor.equals(previousKeyboardFocus) || newFocusedActor.equals(previousScrollFocus)))
						event.cancel();
				}
			}
		};
	}
	
	protected void setStage(Stage stage) {
		if (stage == null)
			addListener(focusListener);
		else
			removeListener(focusListener);
		super.setStage(stage);
	}
	
	public Table getContentTable() {
		return contentTable;
	}
	
	public Table getButtonTable() {
		return buttonTable;
	}
	
	public Dialog text(String text) {
		if (skin == null)
			throw new IllegalStateException("This method may only be used if the dialog was constructed with a Skin.");
		return text(text, skin.get(LabelStyle.class));
	}
	
	public Dialog text(String text, LabelStyle labelStyle) {
		return text(new Label(text, labelStyle));
	}
	
	public Dialog text(Label label) {
		contentTable.add(label);
		return this;
	}
	
	public Dialog button(String text) {
		return button(text, null);
	}
	
	public Dialog button(String text, Object object) {
		if (skin == null)
			throw new IllegalStateException("This method may only be used if the dialog was constructed with a Skin.");
		return button(text, object, skin.get(TextButtonStyle.class));
	}
	
	public Dialog button(String text, Object object, TextButtonStyle buttonStyle) {
		return button(new TextButton(text, buttonStyle), object);
	}
	
	public Dialog button(Button button) {
		return button(button, null);
	}
	
	public Dialog button(Button button, Object object) {
		buttonTable.add(button);
		setObject(button, object);
		return this;
	}
	
	public Dialog show(Stage stage, Action action) {
		clearActions();
		removeCaptureListener(ignoreTouchDown);
		
		previousKeyboardFocus = null;
		Actor actor = stage.getKeyboardFocus();
		if (actor != null && !actor.isDescendantOf(this))
			previousKeyboardFocus = actor;
		
		previousScrollFocus = null;
		actor = stage.getScrollFocus();
		if (actor != null && !actor.isDescendantOf(this))
			previousScrollFocus = actor;
		
		stage.addActor(this);
		pack();
		stage.cancelTouchFocus();
		stage.setKeyboardFocus(this);
		stage.setScrollFocus(this);
		if (action != null)
			addAction(action);
		
		return this;
	}
	
	public Dialog show(Stage stage) {
		show(stage, sequence(Actions.alpha(0), Actions.fadeIn(0.4f, Interpolation.fade)));
		setPosition(Math.round((stage.getWidth() - getWidth()) / 2), Math.round((stage.getHeight() - getHeight()) / 2));
		return this;
	}
	
	public void hide(Action action) {
		Stage stage = getStage();
		if (stage != null) {
			removeListener(focusListener);
			if (previousKeyboardFocus != null && previousKeyboardFocus.getStage() == null)
				previousKeyboardFocus = null;
			Actor actor = stage.getKeyboardFocus();
			if (actor == null || actor.isDescendantOf(this))
				stage.setKeyboardFocus(previousKeyboardFocus);
			
			if (previousScrollFocus != null && previousScrollFocus.getStage() == null)
				previousScrollFocus = null;
			actor = stage.getScrollFocus();
			if (actor == null || actor.isDescendantOf(this))
				stage.setScrollFocus(previousScrollFocus);
		}
		if (action != null) {
			addCaptureListener(ignoreTouchDown);
			addAction(sequence(action, Actions.removeListener(ignoreTouchDown, true), Actions.removeActor()));
		} else
			remove();
	}
	
	public void hide() {
		hide(fadeOut(0.4f, Interpolation.fade));
	}
	
	public void setObject(Actor actor, Object object) {
		values.put(actor, object);
	}
	
	public Dialog key(final int keycode, final Object object) {
		addListener(new InputListener() {
			public boolean keyDown(InputEvent event, int keycode2) {
				if (keycode == keycode2) {
					Micro.app.post(() -> {
						result(object);
						if (!cancelHide)
							hide();
						cancelHide = false;
					});
				}
				return false;
			}
		});
		return this;
	}
	
	protected void result(Object object) {}
	
	public void cancel() {
		cancelHide = true;
	}
	
}
