/*
 * Copyright 2010 Mario Zechner (contact@badlogicgames.com), Nathan Sweet (admin@esotericsoftware.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
#ifndef __MICRO2D__
#define __MICRO2D__

#include <stdint.h>

#ifndef NOJNI
#include <jni.h>
#else
#define JNIEXPORT
#endif

#ifdef __cplusplus
extern "C" {
#endif

/**
 * pixmap formats, components are laid out in memory
 * in the order they appear in the constant name. E.g.
 * MICRO_FORMAT_RGB => pixmap[0] = r, pixmap[1] = g, pixmap[2] = b.
 * Components are 8-bit each except for RGB565 and RGBA4444 which
 * take up two bytes each. The order of bytes is machine dependent
 * within a short the high order byte holds r and the first half of g
 * the low order byte holds the lower half of g and b as well as a
 * if the format is RGBA4444
 */
#define MICRO2D_FORMAT_ALPHA 				1
#define MICRO2D_FORMAT_LUMINANCE_ALPHA 	2
#define MICRO2D_FORMAT_RGB888 			3
#define MICRO2D_FORMAT_RGBA8888			4
#define MICRO2D_FORMAT_RGB565				5
#define MICRO2D_FORMAT_RGBA4444			6

/**
 * blending modes, to be extended
 */
#define MICRO2D_BLEND_NONE 		0
#define MICRO2D_BLEND_SRC_OVER 	1

/**
 * scaling modes, to be extended
 */
#define MICRO2D_SCALE_NEAREST		0
#define MICRO2D_SCALE_BILINEAR	1

/**
 * simple pixmap struct holding the pixel data,
 * the dimensions and the format of the pixmap.
 * the format is one of the MICRO2D_FORMAT_XXX constants.
 */
typedef struct {
	uint32_t width;
	uint32_t height;
	uint32_t format;
	uint32_t blend;
	uint32_t scale;
	const unsigned char* pixels;
} micro2d_pixmap;

JNIEXPORT micro2d_pixmap* micro2d_load (const unsigned char *buffer, uint32_t len);
JNIEXPORT micro2d_pixmap* micro2d_new  (uint32_t width, uint32_t height, uint32_t format);
JNIEXPORT void 		 micro2d_free (const micro2d_pixmap* pixmap);

JNIEXPORT void micro2d_set_blend	  (micro2d_pixmap* pixmap, uint32_t blend);
JNIEXPORT void micro2d_set_scale	  (micro2d_pixmap* pixmap, uint32_t scale);

JNIEXPORT const char*   micro2d_get_failure_reason(void);
JNIEXPORT void		micro2d_clear	   	  (const micro2d_pixmap* pixmap, uint32_t col);
JNIEXPORT void		micro2d_set_pixel   (const micro2d_pixmap* pixmap, int32_t x, int32_t y, uint32_t col);
JNIEXPORT uint32_t micro2d_get_pixel	  (const micro2d_pixmap* pixmap, int32_t x, int32_t y);
JNIEXPORT void		micro2d_draw_line   (const micro2d_pixmap* pixmap, int32_t x, int32_t y, int32_t x2, int32_t y2, uint32_t col);
JNIEXPORT void		micro2d_draw_rect   (const micro2d_pixmap* pixmap, int32_t x, int32_t y, uint32_t width, uint32_t height, uint32_t col);
JNIEXPORT void		micro2d_draw_circle (const micro2d_pixmap* pixmap, int32_t x, int32_t y, uint32_t radius, uint32_t col);
JNIEXPORT void		micro2d_fill_rect   (const micro2d_pixmap* pixmap, int32_t x, int32_t y, uint32_t width, uint32_t height, uint32_t col);
JNIEXPORT void		micro2d_fill_circle (const micro2d_pixmap* pixmap, int32_t x, int32_t y, uint32_t radius, uint32_t col);
JNIEXPORT void		micro2d_fill_triangle(const micro2d_pixmap* pixmap,int32_t x1, int32_t y1, int32_t x2, int32_t y2, int32_t x3, int32_t y3, uint32_t col);
JNIEXPORT void		micro2d_draw_pixmap (const micro2d_pixmap* src_pixmap,
								   const micro2d_pixmap* dst_pixmap,
								   int32_t src_x, int32_t src_y, uint32_t src_width, uint32_t src_height,
								   int32_t dst_x, int32_t dst_y, uint32_t dst_width, uint32_t dst_height);

JNIEXPORT uint32_t micro2d_bytes_per_pixel(uint32_t format);

#ifdef __cplusplus
}
#endif

#endif