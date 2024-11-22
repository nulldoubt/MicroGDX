package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.Micro;
import me.nulldoubt.micro.exceptions.MicroRuntimeException;
import me.nulldoubt.micro.exceptions.SerializationException;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.graphics.Color;
import me.nulldoubt.micro.graphics.Texture;
import me.nulldoubt.micro.graphics.g2d.*;
import me.nulldoubt.micro.graphics.g2d.BitmapFont.BitmapFontData;
import me.nulldoubt.micro.graphics.g2d.TextureAtlas.AtlasRegion;
import me.nulldoubt.micro.graphics.g2d.TextureAtlas.AtlasSprite;
import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.scenes.scene2d.utils.*;
import me.nulldoubt.micro.utils.Disposable;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.ObjectMap;
import me.nulldoubt.micro.utils.json.Json;
import me.nulldoubt.micro.utils.json.Json.ReadOnlySerializer;
import me.nulldoubt.micro.utils.json.JsonValue;

import java.lang.reflect.Method;

public class Skin implements Disposable {
	
	ObjectMap<Class<?>, ObjectMap<String, Object>> resources = new ObjectMap<>();
	TextureAtlas atlas;
	float scale = 1;
	
	private final ObjectMap<String, Class<?>> jsonClassTags = new ObjectMap<>(defaultTagClasses.length);
	
	{
		for (Class<?> c : defaultTagClasses)
			jsonClassTags.put(c.getSimpleName(), c);
	}
	
	public Skin() {}
	
	public Skin(FileHandle skinFile) {
		FileHandle atlasFile = skinFile.sibling(skinFile.nameWithoutExtension() + ".atlas");
		if (atlasFile.exists()) {
			atlas = new TextureAtlas(atlasFile);
			addRegions(atlas);
		}
		
		load(skinFile);
	}
	
	public Skin(FileHandle skinFile, TextureAtlas atlas) {
		this.atlas = atlas;
		addRegions(atlas);
		load(skinFile);
	}
	
	public Skin(TextureAtlas atlas) {
		this.atlas = atlas;
		addRegions(atlas);
	}
	
	public void load(FileHandle skinFile) {
		try {
			getJsonLoader(skinFile).fromJson(Skin.class, skinFile);
		} catch (SerializationException ex) {
			throw new SerializationException("Error reading file: " + skinFile, ex);
		}
	}
	
	/**
	 * Adds all named texture regions from the atlas. The atlas will not be automatically disposed when the skin is disposed.
	 */
	public void addRegions(TextureAtlas atlas) {
		Array<AtlasRegion> regions = atlas.getRegions();
		for (int i = 0, n = regions.size; i < n; i++) {
			AtlasRegion region = regions.get(i);
			String name = region.name;
			if (region.index != -1) {
				name += "_" + region.index;
			}
			add(name, region, TextureRegion.class);
		}
	}
	
	public void add(String name, Object resource) {
		add(name, resource, resource.getClass());
	}
	
	public void add(String name, Object resource, Class type) {
		if (name == null)
			throw new IllegalArgumentException("name cannot be null.");
		if (resource == null)
			throw new IllegalArgumentException("resource cannot be null.");
		ObjectMap<String, Object> typeResources = resources.get(type);
		if (typeResources == null) {
			typeResources = new ObjectMap(type == TextureRegion.class || type == Drawable.class || type == Sprite.class ? 256 : 64);
			resources.put(type, typeResources);
		}
		typeResources.put(name, resource);
	}
	
	public void remove(String name, Class type) {
		if (name == null)
			throw new IllegalArgumentException("name cannot be null.");
		ObjectMap<String, Object> typeResources = resources.get(type);
		typeResources.remove(name);
	}
	
	/**
	 * Returns a resource named "default" for the specified type.
	 *
	 * @throws MicroRuntimeException if the resource was not found.
	 */
	public <T> T get(Class<T> type) {
		return get("default", type);
	}
	
	/**
	 * Returns a named resource of the specified type.
	 *
	 * @throws MicroRuntimeException if the resource was not found.
	 */
	public <T> T get(String name, Class<T> type) {
		if (name == null)
			throw new IllegalArgumentException("name cannot be null.");
		if (type == null)
			throw new IllegalArgumentException("type cannot be null.");
		
		if (type == Drawable.class)
			return (T) getDrawable(name);
		if (type == TextureRegion.class)
			return (T) getRegion(name);
		if (type == NinePatch.class)
			return (T) getPatch(name);
		if (type == Sprite.class)
			return (T) getSprite(name);
		
		ObjectMap<String, Object> typeResources = resources.get(type);
		if (typeResources == null)
			throw new MicroRuntimeException("No " + type.getName() + " registered with name: " + name);
		Object resource = typeResources.get(name);
		if (resource == null)
			throw new MicroRuntimeException("No " + type.getName() + " registered with name: " + name);
		return (T) resource;
	}
	
	/**
	 * Returns a named resource of the specified type.
	 *
	 * @return null if not found.
	 */
	public <T> T optional(String name, Class<T> type) {
		if (name == null)
			throw new IllegalArgumentException("name cannot be null.");
		if (type == null)
			throw new IllegalArgumentException("type cannot be null.");
		ObjectMap<String, Object> typeResources = resources.get(type);
		if (typeResources == null)
			return null;
		return (T) typeResources.get(name);
	}
	
	public boolean has(String name, Class type) {
		ObjectMap<String, Object> typeResources = resources.get(type);
		if (typeResources == null)
			return false;
		return typeResources.containsKey(name);
	}
	
	/**
	 * Returns the name to resource mapping for the specified type, or null if no resources of that type exist.
	 */
	public <T> ObjectMap<String, T> getAll(Class<T> type) {
		return (ObjectMap<String, T>) resources.get(type);
	}
	
	public Color getColor(String name) {
		return get(name, Color.class);
	}
	
	public BitmapFont getFont(String name) {
		return get(name, BitmapFont.class);
	}
	
	/**
	 * Returns a registered texture region. If no region is found but a texture exists with the name, a region is created from the
	 * texture and stored in the skin.
	 */
	public TextureRegion getRegion(String name) {
		TextureRegion region = optional(name, TextureRegion.class);
		if (region != null)
			return region;
		
		Texture texture = optional(name, Texture.class);
		if (texture == null)
			throw new MicroRuntimeException("No TextureRegion or Texture registered with name: " + name);
		region = new TextureRegion(texture);
		add(name, region, TextureRegion.class);
		return region;
	}
	
	/**
	 * @return an array with the {@link TextureRegion} that have an index != -1, or null if none are found.
	 */
	public Array<TextureRegion> getRegions(String regionName) {
		Array<TextureRegion> regions = null;
		int i = 0;
		TextureRegion region = optional(regionName + "_" + (i++), TextureRegion.class);
		if (region != null) {
			regions = new Array<TextureRegion>();
			while (region != null) {
				regions.add(region);
				region = optional(regionName + "_" + (i++), TextureRegion.class);
			}
		}
		return regions;
	}
	
	/**
	 * Returns a registered tiled drawable. If no tiled drawable is found but a region exists with the name, a tiled drawable is
	 * created from the region and stored in the skin.
	 */
	public TiledDrawable getTiledDrawable(String name) {
		TiledDrawable tiled = optional(name, TiledDrawable.class);
		if (tiled != null)
			return tiled;
		
		tiled = new TiledDrawable(getRegion(name));
		tiled.setName(name);
		if (scale != 1) {
			scale(tiled);
			tiled.setScale(scale);
		}
		add(name, tiled, TiledDrawable.class);
		return tiled;
	}
	
	/**
	 * Returns a registered ninepatch. If no ninepatch is found but a region exists with the name, a ninepatch is created from the
	 * region and stored in the skin. If the region is an {@link AtlasRegion} then its split {@link AtlasRegion#values} are used,
	 * otherwise the ninepatch will have the region as the center patch.
	 */
	public NinePatch getPatch(String name) {
		NinePatch patch = optional(name, NinePatch.class);
		if (patch != null)
			return patch;
		
		try {
			TextureRegion region = getRegion(name);
			if (region instanceof AtlasRegion) {
				int[] splits = ((AtlasRegion) region).findValue("split");
				if (splits != null) {
					patch = new NinePatch(region, splits[0], splits[1], splits[2], splits[3]);
					int[] pads = ((AtlasRegion) region).findValue("pad");
					if (pads != null)
						patch.setPadding(pads[0], pads[1], pads[2], pads[3]);
				}
			}
			if (patch == null)
				patch = new NinePatch(region);
			if (scale != 1)
				patch.scale(scale, scale);
			add(name, patch, NinePatch.class);
			return patch;
		} catch (MicroRuntimeException ex) {
			throw new MicroRuntimeException("No NinePatch, TextureRegion, or Texture registered with name: " + name);
		}
	}
	
	/**
	 * Returns a registered sprite. If no sprite is found but a region exists with the name, a sprite is created from the region
	 * and stored in the skin. If the region is an {@link AtlasRegion} then an {@link AtlasSprite} is used if the region has been
	 * whitespace stripped or packed rotated 90 degrees.
	 */
	public Sprite getSprite(String name) {
		Sprite sprite = optional(name, Sprite.class);
		if (sprite != null)
			return sprite;
		
		try {
			TextureRegion textureRegion = getRegion(name);
			if (textureRegion instanceof AtlasRegion) {
				AtlasRegion region = (AtlasRegion) textureRegion;
				if (region.rotate || region.packedWidth != region.originalWidth || region.packedHeight != region.originalHeight)
					sprite = new AtlasSprite(region);
			}
			if (sprite == null)
				sprite = new Sprite(textureRegion);
			if (scale != 1)
				sprite.setSize(sprite.getWidth() * scale, sprite.getHeight() * scale);
			add(name, sprite, Sprite.class);
			return sprite;
		} catch (MicroRuntimeException ex) {
			throw new MicroRuntimeException("No NinePatch, TextureRegion, or Texture registered with name: " + name);
		}
	}
	
	/**
	 * Returns a registered drawable. If no drawable is found but a region, ninepatch, or sprite exists with the name, then the
	 * appropriate drawable is created and stored in the skin.
	 */
	public Drawable getDrawable(String name) {
		Drawable drawable = optional(name, Drawable.class);
		if (drawable != null)
			return drawable;
		
		// Use texture or texture region. If it has splits, use ninepatch. If it has rotation or whitespace stripping, use sprite.
		try {
			TextureRegion textureRegion = getRegion(name);
			if (textureRegion instanceof AtlasRegion region) {
				if (region.findValue("split") != null)
					drawable = new NinePatchDrawable(getPatch(name));
				else if (region.rotate || region.packedWidth != region.originalWidth || region.packedHeight != region.originalHeight)
					drawable = new SpriteDrawable(getSprite(name));
			}
			if (drawable == null) {
				drawable = new TextureRegionDrawable(textureRegion);
				if (scale != 1)
					scale(drawable);
			}
		} catch (MicroRuntimeException _) {}
		
		// Check for explicit registration of ninepatch, sprite, or tiled drawable.
		if (drawable == null) {
			NinePatch patch = optional(name, NinePatch.class);
			if (patch != null)
				drawable = new NinePatchDrawable(patch);
			else {
				Sprite sprite = optional(name, Sprite.class);
				if (sprite != null)
					drawable = new SpriteDrawable(sprite);
				else
					throw new MicroRuntimeException(
							"No Drawable, NinePatch, TextureRegion, Texture, or Sprite registered with name: " + name);
			}
		}
		
		((BaseDrawable) drawable).setName(name);
		
		add(name, drawable, Drawable.class);
		return drawable;
	}
	
	/**
	 * Returns the name of the specified style object, or null if it is not in the skin. This compares potentially every style
	 * object in the skin of the same type as the specified style, which may be a somewhat expensive operation.
	 */
	public String find(Object resource) {
		if (resource == null)
			throw new IllegalArgumentException("style cannot be null.");
		ObjectMap<String, Object> typeResources = resources.get(resource.getClass());
		if (typeResources == null)
			return null;
		return typeResources.findKey(resource, true);
	}
	
	/**
	 * Returns a copy of a drawable found in the skin via {@link #getDrawable(String)}.
	 */
	public Drawable newDrawable(String name) {
		return newDrawable(getDrawable(name));
	}
	
	/**
	 * Returns a tinted copy of a drawable found in the skin via {@link #getDrawable(String)}.
	 */
	public Drawable newDrawable(String name, float r, float g, float b, float a) {
		return newDrawable(getDrawable(name), new Color(r, g, b, a));
	}
	
	/**
	 * Returns a tinted copy of a drawable found in the skin via {@link #getDrawable(String)}.
	 */
	public Drawable newDrawable(String name, Color tint) {
		return newDrawable(getDrawable(name), tint);
	}
	
	/**
	 * Returns a copy of the specified drawable.
	 */
	public Drawable newDrawable(Drawable drawable) {
		if (drawable instanceof TiledDrawable)
			return new TiledDrawable((TiledDrawable) drawable);
		if (drawable instanceof TextureRegionDrawable)
			return new TextureRegionDrawable((TextureRegionDrawable) drawable);
		if (drawable instanceof NinePatchDrawable)
			return new NinePatchDrawable((NinePatchDrawable) drawable);
		if (drawable instanceof SpriteDrawable)
			return new SpriteDrawable((SpriteDrawable) drawable);
		throw new MicroRuntimeException("Unable to copy, unknown drawable type: " + drawable.getClass());
	}
	
	/**
	 * Returns a tinted copy of a drawable found in the skin via {@link #getDrawable(String)}.
	 */
	public Drawable newDrawable(Drawable drawable, float r, float g, float b, float a) {
		return newDrawable(drawable, new Color(r, g, b, a));
	}
	
	public Drawable newDrawable(Drawable drawable, Color tint) {
		Drawable newDrawable = switch (drawable) {
			case TextureRegionDrawable textureRegionDrawable -> textureRegionDrawable.tint(tint);
			case NinePatchDrawable ninePatchDrawable -> ninePatchDrawable.tint(tint);
			case SpriteDrawable spriteDrawable -> spriteDrawable.tint(tint);
			case null, default ->
					throw new MicroRuntimeException("Unable to copy, unknown drawable type: " + drawable.getClass());
		};
		
		if (newDrawable instanceof BaseDrawable named)
			named.setName(((BaseDrawable) drawable).getName() + " (" + tint + ")");
		
		return newDrawable;
	}
	
	public void scale(Drawable drawble) {
		drawble.setLeftWidth(drawble.getLeftWidth() * scale);
		drawble.setRightWidth(drawble.getRightWidth() * scale);
		drawble.setBottomHeight(drawble.getBottomHeight() * scale);
		drawble.setTopHeight(drawble.getTopHeight() * scale);
		drawble.setMinWidth(drawble.getMinWidth() * scale);
		drawble.setMinHeight(drawble.getMinHeight() * scale);
	}
	
	/**
	 * The scale used to size drawables created by this skin.
	 * <p>
	 * This can be useful when scaling an entire UI (eg with a stage's viewport) then using an atlas with images whose resolution
	 * matches the UI scale. The skin can then be scaled the opposite amount so that the larger or smaller images are drawn at the
	 * original size. For example, if the UI is scaled 2x, the atlas would have images that are twice the size, then the skin's
	 * scale would be set to 0.5.
	 */
	public void setScale(float scale) {
		this.scale = scale;
	}
	
	/**
	 * Sets the style on the actor to disabled or enabled. This is done by appending "-disabled" to the style name when enabled is
	 * false, and removing "-disabled" from the style name when enabled is true. A method named "getStyle" is called the actor via
	 * reflection and the name of that style is found in the skin. If the actor doesn't have a "getStyle" method or the style was
	 * not found in the skin, no exception is thrown and the actor is left unchanged.
	 */
	public void setEnabled(Actor actor, boolean enabled) {
		// Get current style.
		Method method = findMethod(actor.getClass(), "getStyle");
		if (method == null)
			return;
		Object style;
		try {
			style = method.invoke(actor);
		} catch (Exception _) {
			return;
		}
		// Determine new style.
		String name = find(style);
		if (name == null)
			return;
		name = name.replace("-disabled", "") + (enabled ? "" : "-disabled");
		style = get(name, style.getClass());
		// Set new style.
		method = findMethod(actor.getClass(), "setStyle");
		if (method == null)
			return;
		try {
			method.invoke(actor, style);
		} catch (Exception _) {}
	}
	
	/**
	 * Returns the {@link TextureAtlas} passed to this skin constructor, or null.
	 */
	public TextureAtlas getAtlas() {
		return atlas;
	}
	
	/**
	 * Disposes the {@link TextureAtlas} and all {@link Disposable} resources in the skin.
	 */
	public void dispose() {
		if (atlas != null)
			atlas.dispose();
		for (ObjectMap<String, Object> entry : resources.values()) {
			for (Object resource : entry.values())
				if (resource instanceof Disposable)
					((Disposable) resource).dispose();
		}
	}
	
	protected Json getJsonLoader(final FileHandle skinFile) {
		final Skin skin = this;
		
		final Json json = new Json() {
			private static final String parentFieldName = "parent";
			
			public <T> T readValue(Class<T> type, Class elementType, JsonValue jsonData) {
				// If the JSON is a string but the type is not, look up the actual value by name.
				if (jsonData != null && jsonData.isString() && !CharSequence.class.isAssignableFrom(type))
					return get(jsonData.asString(), type);
				return super.readValue(type, elementType, jsonData);
			}
			
			protected boolean ignoreUnknownField(Class type, String fieldName) {
				return fieldName.equals(parentFieldName);
			}
			
			public void readFields(Object object, JsonValue jsonMap) {
				if (jsonMap.has(parentFieldName)) {
					String parentName = readValue(parentFieldName, String.class, jsonMap);
					Class parentType = object.getClass();
					while (true) {
						try {
							copyFields(get(parentName, parentType), object);
							break;
						} catch (MicroRuntimeException ex) { // Parent resource doesn't exist.
							parentType = parentType.getSuperclass(); // Try resource for super class.
							if (parentType == Object.class) {
								SerializationException se = new SerializationException("Unable to find parent resource with name: " + parentName);
								se.addTrace(jsonMap.child.trace());
								throw se;
							}
						}
					}
				}
				super.readFields(object, jsonMap);
			}
		};
		json.setTypeName(null);
		json.setUsePrototypes(false);
		
		json.setSerializer(Skin.class, new ReadOnlySerializer<>() {
			public Skin read(Json json, JsonValue typeToValueMap, Class clazz) {
				for (JsonValue valueMap = typeToValueMap.child; valueMap != null; valueMap = valueMap.next) {
					try {
						Class type = json.getClass(valueMap.name());
						if (type == null)
							type = Class.forName(valueMap.name());
						readNamedObjects(json, type, valueMap);
					} catch (SecurityException | ClassNotFoundException e) {
						throw new SerializationException(e);
					}
				}
				return skin;
			}
			
			private void readNamedObjects(Json json, Class type, JsonValue valueMap) {
				Class addType = type == TintedDrawable.class ? Drawable.class : type;
				for (JsonValue valueEntry = valueMap.child; valueEntry != null; valueEntry = valueEntry.next) {
					Object object = json.readValue(type, valueEntry);
					if (object == null)
						continue;
					try {
						add(valueEntry.name, object, addType);
						if (addType != Drawable.class && Drawable.class.isAssignableFrom(addType))
							add(valueEntry.name, object, Drawable.class);
					} catch (Exception e) {
						throw new SerializationException("Error reading " + type.getSimpleName() + ": " + valueEntry.name, e);
					}
				}
			}
		});
		
		json.setSerializer(BitmapFont.class, new ReadOnlySerializer<>() {
			public BitmapFont read(Json json, JsonValue jsonData, Class type) {
				String path = json.readValue("file", String.class, jsonData);
				float scaledSize = json.readValue("scaledSize", float.class, -1f, jsonData);
				Boolean flip = json.readValue("flip", Boolean.class, false, jsonData);
				Boolean markupEnabled = json.readValue("markupEnabled", Boolean.class, false, jsonData);
				Boolean useIntegerPositions = json.readValue("useIntegerPositions", Boolean.class, true, jsonData);
				
				FileHandle fontFile = skinFile.parent().child(path);
				if (!fontFile.exists())
					fontFile = Micro.files.internal(path);
				if (!fontFile.exists())
					throw new SerializationException("Font file not found: " + fontFile);
				
				// Use a region with the same name as the font, else use a PNG file in the same directory as the FNT file.
				String regionName = fontFile.nameWithoutExtension();
				try {
					BitmapFont font;
					Array<TextureRegion> regions = skin.getRegions(regionName);
					if (regions != null)
						font = new BitmapFont(new BitmapFontData(fontFile, flip), regions, true);
					else {
						TextureRegion region = skin.optional(regionName, TextureRegion.class);
						if (region != null)
							font = new BitmapFont(fontFile, region, flip);
						else {
							FileHandle imageFile = fontFile.parent().child(regionName + ".png");
							if (imageFile.exists())
								font = new BitmapFont(fontFile, imageFile, flip);
							else
								font = new BitmapFont(fontFile, flip);
						}
					}
					font.getData().markupEnabled = markupEnabled;
					font.setUseIntegerPositions(useIntegerPositions);
					// Scaled size is the desired cap height to scale the font to.
					if (scaledSize != -1)
						font.getData().setScale(scaledSize / font.getCapHeight());
					return font;
				} catch (RuntimeException ex) {
					throw new SerializationException("Error loading bitmap font: " + fontFile, ex);
				}
			}
		});
		
		json.setSerializer(Color.class, new ReadOnlySerializer<>() {
			public Color read(Json json, JsonValue jsonData, Class type) {
				if (jsonData.isString())
					return get(jsonData.asString(), Color.class);
				String hex = json.readValue("hex", String.class, (String) null, jsonData);
				if (hex != null)
					return Color.valueOf(hex);
				float r = json.readValue("r", float.class, 0f, jsonData);
				float g = json.readValue("g", float.class, 0f, jsonData);
				float b = json.readValue("b", float.class, 0f, jsonData);
				float a = json.readValue("a", float.class, 1f, jsonData);
				return new Color(r, g, b, a);
			}
		});
		
		json.setSerializer(TintedDrawable.class, new ReadOnlySerializer<>() {
			public TintedDrawable read(Json json, JsonValue jsonData, Class type) {
				String name = json.readValue("name", String.class, jsonData);
				Color color = json.readValue("color", Color.class, jsonData);
				if (color == null)
					throw new SerializationException("TintedDrawable missing color: " + jsonData);
				Drawable drawable = newDrawable(name, color);
				if (drawable instanceof BaseDrawable named)
					named.setName(jsonData.name + " (" + name + ", " + color + ")");
				return (TintedDrawable) drawable;
			}
		});
		
		for (ObjectMap.Entry<String, Class<?>> entry : jsonClassTags)
			json.addClassTag(entry.key, entry.value);
		
		return json;
	}
	
	public ObjectMap<String, Class<?>> getJsonClassTags() {
		return jsonClassTags;
	}
	
	private static final Class<?>[] defaultTagClasses = {BitmapFont.class, Color.class, TintedDrawable.class, NinePatchDrawable.class,
			SpriteDrawable.class, TextureRegionDrawable.class, TiledDrawable.class, Button.ButtonStyle.class,
			CheckBox.CheckBoxStyle.class, ImageButton.ImageButtonStyle.class, ImageTextButton.ImageTextButtonStyle.class,
			Label.LabelStyle.class, List.ListStyle.class, ProgressBar.ProgressBarStyle.class, ScrollPane.ScrollPaneStyle.class,
			SelectBox.SelectBoxStyle.class, Slider.SliderStyle.class, SplitPane.SplitPaneStyle.class, TextButton.TextButtonStyle.class,
			TextField.TextFieldStyle.class, TextTooltip.TextTooltipStyle.class, Touchpad.TouchpadStyle.class, Tree.TreeStyle.class,
			Window.WindowStyle.class};
	
	private static Method findMethod(Class type, String name) {
		Method[] methods = type.getMethods();
		for (Method method : methods) {
			if (method.getName().equals(name))
				return method;
		}
		return null;
	}
	
	public static class TintedDrawable {
		
		public String name;
		public Color color;
		
	}
	
}
