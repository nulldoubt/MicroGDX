/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package me.nulldoubt.micro.scenes.scene2d.ui;

import me.nulldoubt.micro.scenes.scene2d.Actor;
import me.nulldoubt.micro.scenes.scene2d.utils.Layout;
import com.nulldoubt.micro.utils.Null;

/** Value placeholder, allowing the value to be computed on request. Values can be provided an actor for context to reduce the
 * number of value instances that need to be created and reduce verbosity in code that specifies values.
 * @author Nathan Sweet */
public abstract class Value {
	/** Calls {@link #get(Actor)} with null. */
	public float get () {
		return get(null);
	}

	/** @param context May be null. */
	public abstract float get (Actor context);

	/** A value that is always zero. */
	public static final Fixed zero = new Fixed(0);

	/** A fixed value that is not computed each time it is used.
	 * @author Nathan Sweet */
	public static class Fixed extends Value {
		static final Fixed[] cache = new Fixed[111];

		private final float value;

		public Fixed (float value) {
			this.value = value;
		}

		public float get (Actor context) {
			return value;
		}

		public String toString () {
			return Float.toString(value);
		}

		public static Fixed valueOf (float value) {
			if (value == 0) return zero;
			if (value >= -10 && value <= 100 && value == (int)value) {
				Fixed fixed = cache[(int)value + 10];
				if (fixed == null) cache[(int)value + 10] = fixed = new Fixed(value);
				return fixed;
			}
			return new Fixed(value);
		}
	}

	/** Value that is the minWidth of the actor in the cell. */
	public static Value minWidth = new Value() {
		public float get (Actor context) {
			if (context instanceof Layout) return ((Layout)context).getMinWidth();
			return context == null ? 0 : context.getWidth();
		}
	};

	/** Value that is the minHeight of the actor in the cell. */
	public static Value minHeight = new Value() {
		public float get (Actor context) {
			if (context instanceof Layout) return ((Layout)context).getMinHeight();
			return context == null ? 0 : context.getHeight();
		}
	};

	/** Value that is the prefWidth of the actor in the cell. */
	public static Value prefWidth = new Value() {
		public float get (Actor context) {
			if (context instanceof Layout) return ((Layout)context).getPrefWidth();
			return context == null ? 0 : context.getWidth();

		}
	};

	/** Value that is the prefHeight of the actor in the cell. */
	public static Value prefHeight = new Value() {
		public float get (Actor context) {
			if (context instanceof Layout) return ((Layout)context).getPrefHeight();
			return context == null ? 0 : context.getHeight();
		}
	};

	/** Value that is the maxWidth of the actor in the cell. */
	public static Value maxWidth = new Value() {
		public float get (Actor context) {
			if (context instanceof Layout) return ((Layout)context).getMaxWidth();
			return context == null ? 0 : context.getWidth();
		}
	};

	/** Value that is the maxHeight of the actor in the cell. */
	public static Value maxHeight = new Value() {
		public float get (Actor context) {
			if (context instanceof Layout) return ((Layout)context).getMaxHeight();
			return context == null ? 0 : context.getHeight();
		}
	};

	/** Returns a value that is a percentage of the actor's width. */
	public static Value percentWidth (final float percent) {
		return new Value() {
			public float get (Actor actor) {
				return actor.getWidth() * percent;
			}
		};
	}

	/** Returns a value that is a percentage of the actor's height. */
	public static Value percentHeight (final float percent) {
		return new Value() {
			public float get (Actor actor) {
				return actor.getHeight() * percent;
			}
		};
	}

	/** Returns a value that is a percentage of the specified actor's width. The context actor is ignored. */
	public static Value percentWidth (final float percent, final Actor actor) {
		if (actor == null) throw new IllegalArgumentException("actor cannot be null.");
		return new Value() {
			public float get (Actor context) {
				return actor.getWidth() * percent;
			}
		};
	}

	/** Returns a value that is a percentage of the specified actor's height. The context actor is ignored. */
	public static Value percentHeight (final float percent, final Actor actor) {
		if (actor == null) throw new IllegalArgumentException("actor cannot be null.");
		return new Value() {
			public float get (Actor context) {
				return actor.getHeight() * percent;
			}
		};
	}
}
