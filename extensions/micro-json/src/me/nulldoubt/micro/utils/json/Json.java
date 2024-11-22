package me.nulldoubt.micro.utils.json;

import me.nulldoubt.micro.exceptions.SerializationException;
import me.nulldoubt.micro.files.FileHandle;
import me.nulldoubt.micro.utils.Streams;
import me.nulldoubt.micro.utils.collections.*;
import me.nulldoubt.micro.utils.collections.Array;
import me.nulldoubt.micro.utils.collections.Queue;
import me.nulldoubt.micro.utils.json.JsonValue.PrettyPrintSettings;
import me.nulldoubt.micro.utils.json.JsonWriter.OutputType;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class Json {
	
	static private final boolean debug = false;
	
	private JsonWriter writer;
	private String typeName = "class";
	private boolean usePrototypes = true;
	private OutputType outputType;
	private boolean quoteLongValues;
	private boolean ignoreUnknownFields;
	private boolean ignoreDeprecated;
	private boolean readDeprecated;
	private boolean enumNames = true;
	private boolean sortFields;
	private Serializer defaultSerializer;
	private final ObjectMap<Class, OrderedMap<String, FieldMetadata>> typeToFields = new ObjectMap<>();
	private final ObjectMap<String, Class> tagToClass = new ObjectMap<>();
	private final ObjectMap<Class, String> classToTag = new ObjectMap<>();
	private final ObjectMap<Class, Serializer> classToSerializer = new ObjectMap<>();
	private final ObjectMap<Class, Object[]> classToDefaultValues = new ObjectMap<>();
	private final Object[] equals1 = {null}, equals2 = {null};
	
	public Json() {
		outputType = OutputType.minimal;
	}
	
	public Json(OutputType outputType) {
		this.outputType = outputType;
	}
	
	public void setIgnoreUnknownFields(boolean ignoreUnknownFields) {
		this.ignoreUnknownFields = ignoreUnknownFields;
	}
	
	public boolean getIgnoreUnknownFields() {
		return ignoreUnknownFields;
	}
	
	public void setIgnoreDeprecated(boolean ignoreDeprecated) {
		this.ignoreDeprecated = ignoreDeprecated;
	}
	
	public void setReadDeprecated(boolean readDeprecated) {
		this.readDeprecated = readDeprecated;
	}
	
	public void setOutputType(OutputType outputType) {
		this.outputType = outputType;
	}
	
	public void setQuoteLongValues(boolean quoteLongValues) {
		this.quoteLongValues = quoteLongValues;
	}
	
	public void setEnumNames(boolean enumNames) {
		this.enumNames = enumNames;
	}
	
	public void addClassTag(String tag, Class type) {
		tagToClass.put(tag, type);
		classToTag.put(type, tag);
	}
	
	public Class getClass(String tag) {
		return tagToClass.get(tag);
	}
	
	public String getTag(Class type) {
		return classToTag.get(type);
	}
	
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
	
	public void setDefaultSerializer(Serializer defaultSerializer) {
		this.defaultSerializer = defaultSerializer;
	}
	
	public <T> void setSerializer(Class<T> type, Serializer<T> serializer) {
		classToSerializer.put(type, serializer);
	}
	
	public <T> Serializer<T> getSerializer(Class<T> type) {
		return classToSerializer.get(type);
	}
	
	public void setUsePrototypes(boolean usePrototypes) {
		this.usePrototypes = usePrototypes;
	}
	
	public void setElementType(Class type, String fieldName, Class elementType) {
		FieldMetadata metadata = getFields(type).get(fieldName);
		if (metadata == null)
			throw new SerializationException("Field not found: " + fieldName + " (" + type.getName() + ")");
		metadata.elementType = elementType;
	}
	
	public void setDeprecated(Class type, String fieldName, boolean deprecated) {
		FieldMetadata metadata = getFields(type).get(fieldName);
		if (metadata == null)
			throw new SerializationException("Field not found: " + fieldName + " (" + type.getName() + ")");
		metadata.deprecated = deprecated;
	}
	
	public void setSortFields(boolean sortFields) {
		this.sortFields = sortFields;
	}
	
	protected void sortFields(Class type, Array<String> fieldNames) {
		if (sortFields)
			fieldNames.sort();
	}
	
	private OrderedMap<String, FieldMetadata> getFields(Class type) {
		OrderedMap<String, FieldMetadata> fields = typeToFields.get(type);
		if (fields != null)
			return fields;
		
		Array<Class> classHierarchy = new Array<>();
		Class nextClass = type;
		while (nextClass != Object.class) {
			classHierarchy.add(nextClass);
			nextClass = nextClass.getSuperclass();
		}
		ArrayList<Field> allFields = new ArrayList<>();
		for (int i = classHierarchy.size - 1; i >= 0; i--)
			Collections.addAll(allFields, classHierarchy.get(i).getDeclaredFields());
		
		OrderedMap<String, FieldMetadata> nameToField = new OrderedMap<>(allFields.size());
		
		for (Field field : allFields) {
			if (Modifier.isTransient(field.getModifiers()))
				continue;
			if (Modifier.isStatic(field.getModifiers()))
				continue;
			if (field.isSynthetic())
				continue;
			
			if (!field.isAccessible()) {
				try {
					field.setAccessible(true);
				} catch (RuntimeException ex) {
					continue;
				}
			}
			
			nameToField.put(field.getName(), new FieldMetadata(field));
		}
		sortFields(type, nameToField.keys);
		typeToFields.put(type, nameToField);
		return nameToField;
	}
	
	public String toJson(Object object) {
		return toJson(object, object == null ? null : object.getClass(), (Class) null);
	}
	
	public String toJson(Object object, Class knownType) {
		return toJson(object, knownType, (Class) null);
	}
	
	public String toJson(Object object, Class knownType, Class elementType) {
		StringWriter buffer = new StringWriter();
		toJson(object, knownType, elementType, buffer);
		return buffer.toString();
	}
	
	public void toJson(Object object, FileHandle file) {
		toJson(object, object == null ? null : object.getClass(), null, file);
	}
	
	public void toJson(Object object, Class knownType, FileHandle file) {
		toJson(object, knownType, null, file);
	}
	
	public void toJson(Object object, Class knownType, Class elementType, FileHandle file) {
		Writer writer = null;
		try {
			writer = file.writer(false, "UTF-8");
			toJson(object, knownType, elementType, writer);
		} catch (Exception ex) {
			throw new SerializationException("Error writing file: " + file, ex);
		} finally {
			Streams.closeQuietly(writer);
		}
	}
	
	public void toJson(Object object, Writer writer) {
		toJson(object, object == null ? null : object.getClass(), null, writer);
	}
	
	public void toJson(Object object, Class knownType, Writer writer) {
		toJson(object, knownType, null, writer);
	}
	
	public void toJson(Object object, Class knownType, Class elementType, Writer writer) {
		setWriter(writer);
		try {
			writeValue(object, knownType, elementType);
		} finally {
			Streams.closeQuietly(this.writer);
			this.writer = null;
		}
	}
	
	public void setWriter(Writer writer) {
		if (!(writer instanceof JsonWriter))
			writer = new JsonWriter(writer);
		this.writer = (JsonWriter) writer;
		this.writer.setOutputType(outputType);
		this.writer.setQuoteLongValues(quoteLongValues);
	}
	
	public JsonWriter getWriter() {
		return writer;
	}
	
	public void writeFields(Object object) {
		Class type = object.getClass();
		
		Object[] defaultValues = getDefaultValues(type);
		
		OrderedMap<String, FieldMetadata> fields = getFields(type);
		int defaultIndex = 0;
		Array<String> fieldNames = fields.orderedKeys();
		for (int i = 0, n = fieldNames.size; i < n; i++) {
			FieldMetadata metadata = fields.get(fieldNames.get(i));
			if (ignoreDeprecated && metadata.deprecated)
				continue;
			Field field = metadata.field;
			try {
				Object value = field.get(object);
				if (defaultValues != null) {
					Object defaultValue = defaultValues[defaultIndex++];
					if (value == null && defaultValue == null)
						continue;
					if (value != null && defaultValue != null) {
						if (value.equals(defaultValue))
							continue;
						if (value.getClass().isArray() && defaultValue.getClass().isArray()) {
							equals1[0] = value;
							equals2[0] = defaultValue;
							if (Arrays.deepEquals(equals1, equals2))
								continue;
						}
					}
				}
				
				if (debug)
					System.out.println("Writing field: " + field.getName() + " (" + type.getName() + ")");
				writer.name(field.getName());
				writeValue(value, field.getType(), metadata.elementType);
			} catch (SecurityException e) {
				throw new SerializationException("Error accessing field: " + field.getName() + " (" + type.getName() + ")", e);
			} catch (SerializationException e) {
				e.addTrace(field + " (" + type.getName() + ")");
				throw e;
			} catch (Exception runtimeEx) {
				SerializationException ex = new SerializationException(runtimeEx);
				ex.addTrace(field + " (" + type.getName() + ")");
				throw ex;
			}
		}
	}
	
	private Object[] getDefaultValues(Class type) {
		if (!usePrototypes)
			return null;
		if (classToDefaultValues.containsKey(type))
			return classToDefaultValues.get(type);
		Object object;
		try {
			object = newInstance(type);
		} catch (Exception ex) {
			classToDefaultValues.put(type, null);
			return null;
		}
		
		OrderedMap<String, FieldMetadata> fields = getFields(type);
		Object[] values = new Object[fields.size];
		classToDefaultValues.put(type, values);
		
		int defaultIndex = 0;
		Array<String> fieldNames = fields.orderedKeys();
		for (int i = 0, n = fieldNames.size; i < n; i++) {
			FieldMetadata metadata = fields.get(fieldNames.get(i));
			if (ignoreDeprecated && metadata.deprecated)
				continue;
			Field field = metadata.field;
			try {
				values[defaultIndex++] = field.get(object);
			} catch (SecurityException e) {
				throw new SerializationException("Error accessing field: " + field.getName() + " (" + type.getName() + ")", e);
			} catch (SerializationException e) {
				e.addTrace(field + " (" + type.getName() + ")");
				throw e;
			} catch (RuntimeException e) {
				SerializationException e1 = new SerializationException(e);
				e1.addTrace(field + " (" + type.getName() + ")");
				throw e1;
			}
		}
		return values;
	}
	
	public void writeField(Object object, String name) {
		writeField(object, name, name, null);
	}
	
	public void writeField(Object object, String name, Class elementType) {
		writeField(object, name, name, elementType);
	}
	
	public void writeField(Object object, String fieldName, String jsonName) {
		writeField(object, fieldName, jsonName, null);
	}
	
	public void writeField(Object object, String fieldName, String jsonName, Class elementType) {
		Class type = object.getClass();
		FieldMetadata metadata = getFields(type).get(fieldName);
		if (metadata == null)
			throw new SerializationException("Field not found: " + fieldName + " (" + type.getName() + ")");
		Field field = metadata.field;
		if (elementType == null)
			elementType = metadata.elementType;
		try {
			if (debug)
				System.out.println("Writing field: " + field.getName() + " (" + type.getName() + ")");
			writer.name(jsonName);
			writeValue(field.get(object), field.getType(), elementType);
		} catch (SecurityException ex) {
			throw new SerializationException("Error accessing field: " + field.getName() + " (" + type.getName() + ")", ex);
		} catch (SerializationException ex) {
			ex.addTrace(field + " (" + type.getName() + ")");
			throw ex;
		} catch (Exception runtimeEx) {
			SerializationException ex = new SerializationException(runtimeEx);
			ex.addTrace(field + " (" + type.getName() + ")");
			throw ex;
		}
	}
	
	public void writeValue(String name, Object value) {
		try {
			writer.name(name);
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
		if (value == null)
			writeValue((Object) null, null, null);
		else
			writeValue(value, value.getClass(), null);
	}
	
	public void writeValue(String name, Object value, Class knownType) {
		try {
			writer.name(name);
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
		writeValue(value, knownType, null);
	}
	
	public void writeValue(String name, Object value, Class knownType, Class elementType) {
		try {
			writer.name(name);
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
		writeValue(value, knownType, elementType);
	}
	
	public void writeValue(Object value) {
		if (value == null)
			writeValue((Object) null, null, null);
		else
			writeValue(value, value.getClass(), null);
	}
	
	public void writeValue(Object value, Class knownType) {
		writeValue(value, knownType, null);
	}
	
	public void writeValue(Object value, Class knownType, Class elementType) {
		try {
			if (value == null) {
				writer.value(null);
				return;
			}
			
			if ((knownType != null && knownType.isPrimitive()) || knownType == String.class || knownType == Integer.class
					|| knownType == Boolean.class || knownType == Float.class || knownType == Long.class || knownType == Double.class
					|| knownType == Short.class || knownType == Byte.class || knownType == Character.class) {
				writer.value(value);
				return;
			}
			
			Class actualType = value.getClass();
			
			if (actualType.isPrimitive() || actualType == String.class || actualType == Integer.class || actualType == Boolean.class
					|| actualType == Float.class || actualType == Long.class || actualType == Double.class || actualType == Short.class
					|| actualType == Byte.class || actualType == Character.class) {
				writeObjectStart(actualType, null);
				writeValue("value", value);
				writeObjectEnd();
				return;
			}
			
			if (value instanceof Serializable) {
				writeObjectStart(actualType, knownType);
				((Serializable) value).write(this);
				writeObjectEnd();
				return;
			}
			
			Serializer serializer = classToSerializer.get(actualType);
			if (serializer != null) {
				serializer.write(this, value, knownType);
				return;
			}
			
			// JSON array special cases.
			switch (value) {
				case Array array -> {
					if (knownType != null && actualType != knownType && actualType != Array.class)
						throw new SerializationException("Serialization of an Array other than the known type is not supported.\n"
								+ "Known type: " + knownType + "\nActual type: " + actualType);
					writeArrayStart();
					for (int i = 0, n = array.size; i < n; i++)
						writeValue(array.get(i), elementType, null);
					writeArrayEnd();
					return;
				}
				case Queue queue -> {
					if (knownType != null && actualType != knownType && actualType != Queue.class)
						throw new SerializationException("Serialization of a Queue other than the known type is not supported.\n" + "Known type: " + knownType + "\nActual type: " + actualType);
					writeArrayStart();
					for (int i = 0, n = queue.size; i < n; i++)
						writeValue(queue.get(i), elementType, null);
					writeArrayEnd();
					return;
				}
				case Collection collection -> {
					if (typeName != null && actualType != ArrayList.class && (knownType == null || knownType != actualType)) {
						writeObjectStart(actualType, knownType);
						writeArrayStart("items");
						for (Object item : (Collection) value)
							writeValue(item, elementType, null);
						writeArrayEnd();
						writeObjectEnd();
					} else {
						writeArrayStart();
						for (Object item : (Collection) value)
							writeValue(item, elementType, null);
						writeArrayEnd();
					}
					return;
				}
				default -> {
				}
			}
			if (actualType.isArray()) {
				if (elementType == null)
					elementType = actualType.getComponentType();
				int length = java.lang.reflect.Array.getLength(value);
				writeArrayStart();
				for (int i = 0; i < length; i++)
					writeValue(java.lang.reflect.Array.get(value, i), elementType, null);
				writeArrayEnd();
				return;
			}
			
			// JSON object special cases.
			if (value instanceof ObjectMap) {
				if (knownType == null)
					knownType = ObjectMap.class;
				writeObjectStart(actualType, knownType);
				for (ObjectMap.Entry<?, ?> entry : ((ObjectMap<?, ?>) value).entries()) {
					writer.name(convertToString(entry.key));
					writeValue(entry.value, elementType, null);
				}
				writeObjectEnd();
				return;
			}
			if (value instanceof ObjectIntMap) {
				if (knownType == null)
					knownType = ObjectIntMap.class;
				writeObjectStart(actualType, knownType);
				for (ObjectIntMap.Entry entry : ((ObjectIntMap<?>) value).entries()) {
					writer.name(convertToString(entry.key));
					writeValue(entry.value, Integer.class);
				}
				writeObjectEnd();
				return;
			}
			if (value instanceof ObjectFloatMap) {
				if (knownType == null)
					knownType = ObjectFloatMap.class;
				writeObjectStart(actualType, knownType);
				for (ObjectFloatMap.Entry entry : ((ObjectFloatMap<?>) value).entries()) {
					writer.name(convertToString(entry.key));
					writeValue(entry.value, Float.class);
				}
				writeObjectEnd();
				return;
			}
			if (value instanceof ObjectSet) {
				if (knownType == null)
					knownType = ObjectSet.class;
				writeObjectStart(actualType, knownType);
				writer.name("values");
				writeArrayStart();
				for (Object entry : (ObjectSet) value)
					writeValue(entry, elementType, null);
				writeArrayEnd();
				writeObjectEnd();
				return;
			}
			if (value instanceof IntMap) {
				if (knownType == null)
					knownType = IntMap.class;
				writeObjectStart(actualType, knownType);
				for (IntMap.Entry entry : ((IntMap<?>) value).entries()) {
					writer.name(String.valueOf(entry.key));
					writeValue(entry.value, elementType, null);
				}
				writeObjectEnd();
				return;
			}
			if (value instanceof LongMap) {
				if (knownType == null)
					knownType = LongMap.class;
				writeObjectStart(actualType, knownType);
				for (LongMap.Entry entry : ((LongMap<?>) value).entries()) {
					writer.name(String.valueOf(entry.key));
					writeValue(entry.value, elementType, null);
				}
				writeObjectEnd();
				return;
			}
			if (value instanceof IntSet) {
				if (knownType == null)
					knownType = IntSet.class;
				writeObjectStart(actualType, knownType);
				writer.name("values");
				writeArrayStart();
				for (IntSetIterator iter = ((IntSet) value).iterator(); iter.hasNext; )
					writeValue(iter.next(), Integer.class, null);
				writeArrayEnd();
				writeObjectEnd();
				return;
			}
			if (value instanceof ArrayMap) {
				if (knownType == null)
					knownType = ArrayMap.class;
				writeObjectStart(actualType, knownType);
				ArrayMap map = (ArrayMap) value;
				for (int i = 0, n = map.size; i < n; i++) {
					writer.name(convertToString(map.keys[i]));
					writeValue(map.values[i], elementType, null);
				}
				writeObjectEnd();
				return;
			}
			if (value instanceof Map) {
				if (knownType == null)
					knownType = HashMap.class;
				writeObjectStart(actualType, knownType);
				for (Map.Entry entry : ((Map<?, ?>) value).entrySet()) {
					writer.name(convertToString(entry.getKey()));
					writeValue(entry.getValue(), elementType, null);
				}
				writeObjectEnd();
				return;
			}
			
			if (Enum.class.isAssignableFrom(actualType)) {
				if (actualType.getEnumConstants() == null) // Get the enum type when an enum value is an inner class (enum A {b{}}).
					actualType = actualType.getSuperclass();
				if (typeName != null && (knownType == null || knownType != actualType)) {
					writeObjectStart(actualType, null);
					writer.name("value");
					writer.value(convertToString((Enum) value));
					writeObjectEnd();
				} else {
					writer.value(convertToString((Enum) value));
				}
				return;
			}
			
			writeObjectStart(actualType, knownType);
			writeFields(value);
			writeObjectEnd();
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
	}
	
	public void writeObjectStart(String name) {
		try {
			writer.name(name);
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
		writeObjectStart();
	}
	
	public void writeObjectStart(String name, Class actualType, Class knownType) {
		try {
			writer.name(name);
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
		writeObjectStart(actualType, knownType);
	}
	
	public void writeObjectStart() {
		try {
			writer.object();
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
	}
	
	public void writeObjectStart(Class actualType, Class knownType) {
		try {
			writer.object();
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
		if (knownType == null || knownType != actualType)
			writeType(actualType);
	}
	
	public void writeObjectEnd() {
		try {
			writer.pop();
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
	}
	
	public void writeArrayStart(String name) {
		try {
			writer.name(name);
			writer.array();
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
	}
	
	public void writeArrayStart() {
		try {
			writer.array();
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
	}
	
	public void writeArrayEnd() {
		try {
			writer.pop();
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
	}
	
	public void writeType(Class type) {
		if (typeName == null)
			return;
		String className = getTag(type);
		if (className == null)
			className = type.getName();
		try {
			writer.set(typeName, className);
		} catch (IOException ex) {
			throw new SerializationException(ex);
		}
		if (debug)
			System.out.println("Writing type: " + type.getName());
	}
	
	public <T> T fromJson(Class<T> type, Reader reader) {
		return readValue(type, null, new JsonReader().parse(reader));
	}
	
	public <T> T fromJson(Class<T> type, Class elementType, Reader reader) {
		return readValue(type, elementType, new JsonReader().parse(reader));
	}
	
	public <T> T fromJson(Class<T> type, InputStream input) {
		return readValue(type, null, new JsonReader().parse(input));
	}
	
	public <T> T fromJson(Class<T> type, Class elementType, InputStream input) {
		return readValue(type, elementType, new JsonReader().parse(input));
	}
	
	public <T> T fromJson(Class<T> type, FileHandle file) {
		try {
			return readValue(type, null, new JsonReader().parse(file));
		} catch (Exception ex) {
			throw new SerializationException("Error reading file: " + file, ex);
		}
	}
	
	public <T> T fromJson(Class<T> type, Class elementType, FileHandle file) {
		try {
			return readValue(type, elementType, new JsonReader().parse(file));
		} catch (Exception ex) {
			throw new SerializationException("Error reading file: " + file, ex);
		}
	}
	
	public <T> T fromJson(Class<T> type, char[] data, int offset, int length) {
		return readValue(type, null, new JsonReader().parse(data, offset, length));
	}
	
	public <T> T fromJson(Class<T> type, Class elementType, char[] data, int offset, int length) {
		return readValue(type, elementType, new JsonReader().parse(data, offset, length));
	}
	
	public <T> T fromJson(Class<T> type, String json) {
		return readValue(type, null, new JsonReader().parse(json));
	}
	
	public <T> T fromJson(Class<T> type, Class elementType, String json) {
		return readValue(type, elementType, new JsonReader().parse(json));
	}
	
	public void readField(Object object, String name, JsonValue jsonData) {
		readField(object, name, name, null, jsonData);
	}
	
	public void readField(Object object, String name, Class elementType, JsonValue jsonData) {
		readField(object, name, name, elementType, jsonData);
	}
	
	public void readField(Object object, String fieldName, String jsonName, JsonValue jsonData) {
		readField(object, fieldName, jsonName, null, jsonData);
	}
	
	public void readField(Object object, String fieldName, String jsonName, Class elementType, JsonValue jsonMap) {
		Class type = object.getClass();
		FieldMetadata metadata = getFields(type).get(fieldName);
		if (metadata == null)
			throw new SerializationException("Field not found: " + fieldName + " (" + type.getName() + ")");
		Field field = metadata.field;
		if (elementType == null)
			elementType = metadata.elementType;
		readField(object, field, jsonName, elementType, jsonMap);
	}
	
	public void readField(Object object, Field field, String jsonName, Class elementType, JsonValue jsonMap) {
		JsonValue jsonValue = jsonMap.get(jsonName);
		if (jsonValue == null)
			return;
		try {
			field.set(object, readValue(field.getType(), elementType, jsonValue));
		} catch (SecurityException | IllegalAccessException e) {
			throw new SerializationException("Error accessing field: " + field.getName() + " (" + field.getDeclaringClass().getName() + ")", e);
		} catch (SerializationException e) {
			e.addTrace(field.getName() + " (" + field.getDeclaringClass().getName() + ")");
			throw e;
		} catch (RuntimeException e) {
			SerializationException e1 = new SerializationException(e);
			e1.addTrace(jsonValue.trace());
			e1.addTrace(field.getName() + " (" + field.getDeclaringClass().getName() + ")");
			throw e1;
		}
	}
	
	public void readFields(Object object, JsonValue jsonMap) {
		Class type = object.getClass();
		OrderedMap<String, FieldMetadata> fields = getFields(type);
		for (JsonValue child = jsonMap.child; child != null; child = child.next) {
			FieldMetadata metadata = fields.get(child.name().replace(" ", "_"));
			if (metadata == null) {
				if (child.name.equals(typeName))
					continue;
				if (ignoreUnknownFields || ignoreUnknownField(type, child.name)) {
					if (debug)
						System.out.println("Ignoring unknown field: " + child.name + " (" + type.getName() + ")");
					continue;
				} else {
					SerializationException ex = new SerializationException(
							"Field not found: " + child.name + " (" + type.getName() + ")");
					ex.addTrace(child.trace());
					throw ex;
				}
			} else {
				if (ignoreDeprecated && !readDeprecated && metadata.deprecated)
					continue;
			}
			Field field = metadata.field;
			try {
				field.set(object, readValue(field.getType(), metadata.elementType, child));
			} catch (SecurityException | IllegalAccessException e) {
				throw new SerializationException("Error accessing field: " + field.getName() + " (" + type.getName() + ")", e);
			} catch (SerializationException e) {
				e.addTrace(field.getName() + " (" + type.getName() + ")");
				throw e;
			} catch (RuntimeException e) {
				SerializationException e1 = new SerializationException(e);
				e1.addTrace(child.trace());
				e1.addTrace(field.getName() + " (" + type.getName() + ")");
				throw e1;
			}
		}
	}
	
	protected boolean ignoreUnknownField(Class type, String fieldName) {
		return false;
	}
	
	public <T> T readValue(String name, Class<T> type, JsonValue jsonMap) {
		return readValue(type, null, jsonMap.get(name));
	}
	
	public <T> T readValue(String name, Class<T> type, T defaultValue, JsonValue jsonMap) {
		JsonValue jsonValue = jsonMap.get(name);
		if (jsonValue == null)
			return defaultValue;
		return readValue(type, null, jsonValue);
	}
	
	public <T> T readValue(String name, Class<T> type, Class elementType, JsonValue jsonMap) {
		return readValue(type, elementType, jsonMap.get(name));
	}
	
	public <T> T readValue(String name, Class<T> type, Class elementType, T defaultValue, JsonValue jsonMap) {
		JsonValue jsonValue = jsonMap.get(name);
		return readValue(type, elementType, defaultValue, jsonValue);
	}
	
	public <T> T readValue(Class<T> type, Class elementType, T defaultValue, JsonValue jsonData) {
		if (jsonData == null)
			return defaultValue;
		return readValue(type, elementType, jsonData);
	}
	
	public <T> T readValue(Class<T> type, JsonValue jsonData) {
		return readValue(type, null, jsonData);
	}
	
	public <T> T readValue(Class<T> type, Class elementType, JsonValue jsonData) {
		if (jsonData == null)
			return null;
		
		if (jsonData.isObject()) {
			String className = typeName == null ? null : jsonData.getString(typeName, null);
			if (className != null) {
				type = getClass(className);
				if (type == null) {
					try {
						type = (Class<T>) Class.forName(className);
					} catch (SecurityException | ClassNotFoundException ex) {
						throw new SerializationException(ex);
					}
				}
			}
			
			if (type == null) {
				if (defaultSerializer != null)
					return (T) defaultSerializer.read(this, jsonData, type);
				return (T) jsonData;
			}
			
			if (typeName != null && Collection.class.isAssignableFrom(type)) {
				jsonData = jsonData.get("items");
				if (jsonData == null)
					throw new SerializationException("Unable to convert object to collection: " + jsonData + " (" + type.getName() + ")");
			} else {
				Serializer serializer = classToSerializer.get(type);
				if (serializer != null)
					return (T) serializer.read(this, jsonData, type);
				
				if (type == String.class || type == Integer.class || type == Boolean.class || type == Float.class
						|| type == Long.class || type == Double.class || type == Short.class || type == Byte.class
						|| type == Character.class || Enum.class.isAssignableFrom(type)) {
					return readValue("value", type, jsonData);
				}
				
				Object object = newInstance(type);
				
				if (object instanceof Serializable serializable) {
					serializable.read(this, jsonData);
					return (T) object;
				}
				
				if (object instanceof ObjectMap) {
					ObjectMap result = (ObjectMap) object;
					for (JsonValue child = jsonData.child; child != null; child = child.next)
						result.put(child.name, readValue(elementType, null, child));
					return (T) result;
				}
				if (object instanceof ObjectIntMap result) {
					for (JsonValue child = jsonData.child; child != null; child = child.next)
						result.put(child.name, readValue(Integer.class, null, child));
					return (T) result;
				}
				if (object instanceof ObjectFloatMap result) {
					for (JsonValue child = jsonData.child; child != null; child = child.next)
						result.put(child.name, readValue(Float.class, null, child));
					return (T) result;
				}
				if (object instanceof ObjectSet result) {
					for (JsonValue child = jsonData.getChild("values"); child != null; child = child.next)
						result.add(readValue(elementType, null, child));
					return (T) result;
				}
				if (object instanceof IntMap result) {
					for (JsonValue child = jsonData.child; child != null; child = child.next)
						result.put(Integer.parseInt(child.name), readValue(elementType, null, child));
					return (T) result;
				}
				if (object instanceof LongMap result) {
					for (JsonValue child = jsonData.child; child != null; child = child.next)
						result.put(Long.parseLong(child.name), readValue(elementType, null, child));
					return (T) result;
				}
				if (object instanceof IntSet result) {
					for (JsonValue child = jsonData.getChild("values"); child != null; child = child.next)
						result.add(child.asInt());
					return (T) result;
				}
				if (object instanceof ArrayMap result) {
					for (JsonValue child = jsonData.child; child != null; child = child.next)
						result.put(child.name, readValue(elementType, null, child));
					return (T) result;
				}
				if (object instanceof Map result) {
					for (JsonValue child = jsonData.child; child != null; child = child.next) {
						if (child.name.equals(typeName))
							continue;
						result.put(child.name, readValue(elementType, null, child));
					}
					return (T) result;
				}
				
				readFields(object, jsonData);
				return (T) object;
			}
		}
		
		if (type != null) {
			Serializer serializer = classToSerializer.get(type);
			if (serializer != null)
				return (T) serializer.read(this, jsonData, type);
			
			if (Serializable.class.isAssignableFrom(type)) {
				Object object = newInstance(type);
				((Serializable) object).read(this, jsonData);
				return (T) object;
			}
		}
		
		if (jsonData.isArray()) {
			if (type == null || type == Object.class)
				type = (Class<T>) Array.class;
			if (Array.class.isAssignableFrom(type)) {
				Array result = type == Array.class ? new Array() : (Array) newInstance(type);
				for (JsonValue child = jsonData.child; child != null; child = child.next)
					result.add(readValue(elementType, null, child));
				return (T) result;
			}
			if (Queue.class.isAssignableFrom(type)) {
				Queue result = type == Queue.class ? new Queue() : (Queue) newInstance(type);
				for (JsonValue child = jsonData.child; child != null; child = child.next)
					result.addLast(readValue(elementType, null, child));
				return (T) result;
			}
			if (Collection.class.isAssignableFrom(type)) {
				Collection result = type.isInterface() ? new ArrayList() : (Collection) newInstance(type);
				for (JsonValue child = jsonData.child; child != null; child = child.next)
					result.add(readValue(elementType, null, child));
				return (T) result;
			}
			if (type.isArray()) {
				Class componentType = type.getComponentType();
				if (elementType == null)
					elementType = componentType;
				Object result = java.lang.reflect.Array..newInstance(componentType, jsonData.size);
				int i = 0;
				for (JsonValue child = jsonData.child; child != null; child = child.next)
					java.lang.reflect.Array.set(result, i++, readValue(elementType, null, child));
				return (T) result;
			}
			throw new SerializationException("Unable to convert value to required type: " + jsonData + " (" + type.getName() + ")");
		}
		
		if (jsonData.isNumber()) {
			try {
				if (type == null || type == float.class || type == Float.class)
					return (T) (Float) jsonData.asFloat();
				if (type == int.class || type == Integer.class)
					return (T) (Integer) jsonData.asInt();
				if (type == long.class || type == Long.class)
					return (T) (Long) jsonData.asLong();
				if (type == double.class || type == Double.class)
					return (T) (Double) jsonData.asDouble();
				if (type == String.class)
					return (T) jsonData.asString();
				if (type == short.class || type == Short.class)
					return (T) (Short) jsonData.asShort();
				if (type == byte.class || type == Byte.class)
					return (T) (Byte) jsonData.asByte();
			} catch (NumberFormatException ignored) {
			}
			jsonData = new JsonValue(jsonData.asString());
		}
		
		if (jsonData.isBoolean()) {
			try {
				if (type == null || type == boolean.class || type == Boolean.class)
					return (T) (Boolean) jsonData.asBoolean();
			} catch (NumberFormatException ignored) {
			}
			jsonData = new JsonValue(jsonData.asString());
		}
		
		if (jsonData.isString()) {
			String string = jsonData.asString();
			if (type == null || type == String.class)
				return (T) string;
			try {
				if (type == int.class || type == Integer.class)
					return (T) Integer.valueOf(string);
				if (type == float.class || type == Float.class)
					return (T) Float.valueOf(string);
				if (type == long.class || type == Long.class)
					return (T) Long.valueOf(string);
				if (type == double.class || type == Double.class)
					return (T) Double.valueOf(string);
				if (type == short.class || type == Short.class)
					return (T) Short.valueOf(string);
				if (type == byte.class || type == Byte.class)
					return (T) Byte.valueOf(string);
			} catch (NumberFormatException ignored) {
			}
			if (type == boolean.class || type == Boolean.class)
				return (T) Boolean.valueOf(string);
			if (type == char.class || type == Character.class)
				return (T) (Character) string.charAt(0);
			if (Enum.class.isAssignableFrom(type)) {
				Enum[] constants = (Enum[]) type.getEnumConstants();
				for (Enum e : constants) {
					if (string.equals(convertToString(e)))
						return (T) e;
				}
			}
			if (type == CharSequence.class)
				return (T) string;
			throw new SerializationException("Unable to convert value to required type: " + jsonData + " (" + type.getName() + ")");
		}
		
		return null;
	}
	
	public void copyFields(Object from, Object to) {
		OrderedMap<String, FieldMetadata> toFields = getFields(to.getClass());
		for (ObjectMap.Entry<String, FieldMetadata> entry : getFields(from.getClass())) {
			FieldMetadata toField = toFields.get(entry.key);
			Field fromField = entry.value.field;
			if (toField == null)
				throw new SerializationException("To object is missing field: " + entry.key);
			try {
				toField.field.set(to, fromField.get(from));
			} catch (SecurityException ex) {
				throw new SerializationException("Error copying field: " + fromField.getName(), ex);
			}
		}
	}
	
	private String convertToString(Enum e) {
		return enumNames ? e.name() : e.toString();
	}
	
	private String convertToString(Object object) {
		if (object instanceof Enum)
			return convertToString((Enum) object);
		if (object instanceof Class)
			return ((Class) object).getName();
		return String.valueOf(object);
	}
	
	protected Object newInstance(Class type) {
		try {
			return type.getConstructor().newInstance();
		} catch (Exception e) {
			Exception exception = e;
			try {
				Constructor<?> constructor = type.getDeclaredConstructor();
				constructor.setAccessible(true);
				return constructor.newInstance();
			} catch (SecurityException _) {
				if (Enum.class.isAssignableFrom(type)) {
					if (type.getEnumConstants() == null)
						type = type.getSuperclass();
					return type.getEnumConstants()[0];
				}
				if (type.isArray())
					throw new SerializationException("Encountered JSON object when expected array of type: " + type.getName(), exception);
				else if (type.isMemberClass() && !Modifier.isStatic(type.getModifiers()))
					throw new SerializationException("Class cannot be created (non-static member class): " + type.getName(), exception);
				else
					throw new SerializationException("Class cannot be created (missing no-arg constructor): " + type.getName(), exception);
			} catch (Exception e1) {
				exception = e1;
			}
			throw new SerializationException("Error constructing instance of class: " + type.getName(), exception);
		}
	}
	
	public String prettyPrint(Object object) {
		return prettyPrint(object, 0);
	}
	
	public String prettyPrint(String json) {
		return prettyPrint(json, 0);
	}
	
	public String prettyPrint(Object object, int singleLineColumns) {
		return prettyPrint(toJson(object), singleLineColumns);
	}
	
	public String prettyPrint(String json, int singleLineColumns) {
		return new JsonReader().parse(json).prettyPrint(outputType, singleLineColumns);
	}
	
	public String prettyPrint(Object object, PrettyPrintSettings settings) {
		return prettyPrint(toJson(object), settings);
	}
	
	public String prettyPrint(String json, PrettyPrintSettings settings) {
		return new JsonReader().parse(json).prettyPrint(settings);
	}
	
	static private class FieldMetadata {
		
		final Field field;
		Class elementType;
		boolean deprecated;
		
		public FieldMetadata(Field field) {
			this.field = field;
			this.elementType = getElementType((ObjectMap.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType())) ? 1 : 0);
			deprecated = field.isAnnotationPresent(Deprecated.class);
		}
		
		private Class<?> getElementType(final int index) {
			Type genericType = field.getGenericType();
			if (genericType instanceof ParameterizedType parameterizedType) {
				final Type[] actualTypes = parameterizedType.getActualTypeArguments();
				if (actualTypes.length - 1 >= index) {
					final Type actualType = actualTypes[index];
					if (actualType instanceof Class<?> clazz)
						return clazz;
					else if (actualType instanceof ParameterizedType parameterizedType1)
						return (Class<?>) parameterizedType1.getRawType();
					else if (actualType instanceof GenericArrayType genericArrayType) {
						final Type componentType = genericArrayType.getGenericComponentType();
						if (componentType instanceof Class<?> componentTypeClazz)
							return java.lang.reflect.Array.newInstance(componentTypeClazz, 0).getClass();
					}
				}
			}
			return null;
		}
		
	}
	
	public interface Serializer<T> {
		
		void write(Json json, T object, Class knownType);
		
		T read(Json json, JsonValue jsonData, Class type);
		
	}
	
	public static abstract class ReadOnlySerializer<T> implements Serializer<T> {
		
		public void write(Json json, T object, Class knownType) {}
		
		public abstract T read(Json json, JsonValue jsonData, Class type);
		
	}
	
	public interface Serializable {
		
		void write(Json json);
		
		void read(Json json, JsonValue jsonData);
		
	}
	
}