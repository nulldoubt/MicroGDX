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
			typeResources = new ObjectMap<>(type == TextureRegion.class || type == Drawable.class || type == Sprite.class ? 256 : 64);
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
	
	public <T> T get(Class<T> type) {
		return get("default", type);
	}
	
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
	
	public <T> ObjectMap<String, T> getAll(Class<T> type) {
		return (ObjectMap<String, T>) resources.get(type);
	}
	
	public Color getColor(String name) {
		return get(name, Color.class);
	}
	
	public BitmapFont getFont(String name) {
		return get(name, BitmapFont.class);
	}
	
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
	
	public Sprite getSprite(String name) {
		Sprite sprite = optional(name, Sprite.class);
		if (sprite != null)
			return sprite;
		
		try {
			TextureRegion textureRegion = getRegion(name);
			if (textureRegion instanceof AtlasRegion region) {
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
	
	public Drawable getDrawable(String name) {
		Drawable drawable = optional(name, Drawable.class);
		if (drawable != null)
			return drawable;
		
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
		
		if (drawable == null) {
			NinePatch patch = optional(name, NinePatch.class);
			if (patch != null)
				drawable = new NinePatchDrawable(patch);
			else {
				Sprite sprite = optional(name, Sprite.class);
				if (sprite != null)
					drawable = new SpriteDrawable(sprite);
				else
					throw new MicroRuntimeException("No Drawable, NinePatch, TextureRegion, Texture, or Sprite registered with name: " + name);
			}
		}
		
		((BaseDrawable) drawable).setName(name);
		
		add(name, drawable, Drawable.class);
		return drawable;
	}
	
	public String find(Object resource) {
		if (resource == null)
			throw new IllegalArgumentException("style cannot be null.");
		ObjectMap<String, Object> typeResources = resources.get(resource.getClass());
		if (typeResources == null)
			return null;
		return typeResources.findKey(resource, true);
	}
	
	public Drawable newDrawable(String name) {
		return newDrawable(getDrawable(name));
	}
	
	public Drawable newDrawable(String name, float r, float g, float b, float a) {
		return newDrawable(getDrawable(name), new Color(r, g, b, a));
	}
	
	public Drawable newDrawable(String name, Color tint) {
		return newDrawable(getDrawable(name), tint);
	}
	
	public Drawable newDrawable(Drawable drawable) {
		if (drawable instanceof TiledDrawable titledDrawable)
			return new TiledDrawable(titledDrawable);
		if (drawable instanceof TextureRegionDrawable textureRegionDrawable)
			return new TextureRegionDrawable(textureRegionDrawable);
		if (drawable instanceof NinePatchDrawable ninePatchDrawable)
			return new NinePatchDrawable(ninePatchDrawable);
		if (drawable instanceof SpriteDrawable spriteDrawable)
			return new SpriteDrawable(spriteDrawable);
		throw new MicroRuntimeException("Unable to copy, unknown drawable type: " + drawable.getClass());
	}
	
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
	
	public void scale(Drawable drawable) {
		drawable.setLeftWidth(drawable.getLeftWidth() * scale);
		drawable.setRightWidth(drawable.getRightWidth() * scale);
		drawable.setBottomHeight(drawable.getBottomHeight() * scale);
		drawable.setTopHeight(drawable.getTopHeight() * scale);
		drawable.setMinWidth(drawable.getMinWidth() * scale);
		drawable.setMinHeight(drawable.getMinHeight() * scale);
	}
	
	public void setScale(float scale) {
		this.scale = scale;
	}
	
	public void setEnabled(Actor actor, boolean enabled) {
		Method method = findMethod(actor.getClass(), "getStyle");
		if (method == null)
			return;
		Object style;
		try {
			style = method.invoke(actor);
		} catch (Exception _) {
			return;
		}
		String name = find(style);
		if (name == null)
			return;
		name = name.replace("-disabled", "") + (enabled ? "" : "-disabled");
		style = get(name, style.getClass());
		method = findMethod(actor.getClass(), "setStyle");
		if (method == null)
			return;
		try {
			method.invoke(actor, style);
		} catch (Exception _) {}
	}
	
	public TextureAtlas getAtlas() {
		return atlas;
	}
	
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
