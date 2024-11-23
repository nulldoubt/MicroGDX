package me.nulldoubt.micro.physics.box2d;

import me.nulldoubt.micro.math.Vector2;

public class PolygonShape extends Shape {
	/*JNI
     #include <Box2D/Box2D.h>
	 */
	
	public PolygonShape() {
		addr = newPolygonShape();
	}
	
	protected PolygonShape(long addr) {
		this.addr = addr;
	}
	
	private native long newPolygonShape(); /*
		b2PolygonShape* poly = new b2PolygonShape();
		return (jlong)poly;
	*/
	
	@Override
	public Type getType() {
		return Type.Polygon;
	}
	
	public void set(Vector2[] vertices) {
		final float[] floatVertices = new float[vertices.length * 2];
		for (int i = 0, j = 0; i < vertices.length * 2; i += 2, j++) {
			floatVertices[i] = vertices[j].x;
			floatVertices[i + 1] = vertices[j].y;
		}
		jniSet(addr, floatVertices, 0, floatVertices.length);
	}
	
	public void set(float[] vertices) {
		jniSet(addr, vertices, 0, vertices.length);
	}
	
	public void set(float[] vertices, int offset, int len) {
		jniSet(addr, vertices, offset, len);
	}
	
	private native void jniSet(long addr, float[] verts, int offset, int len); /*
		b2PolygonShape* poly = (b2PolygonShape*)addr;
		int numVertices = len / 2;
		b2Vec2* verticesOut = new b2Vec2[numVertices];
		for(int i = 0; i < numVertices; i++) { 
			verticesOut[i] = b2Vec2(verts[(i<<1) + offset], verts[(i<<1) + offset + 1]);
		}
		poly->Set(verticesOut, numVertices);
		delete[] verticesOut;
	 */
	
	public void setAsBox(float hx, float hy) {
		jniSetAsBox(addr, hx, hy);
	}
	
	private native void jniSetAsBox(long addr, float hx, float hy); /*
		b2PolygonShape* poly = (b2PolygonShape*)addr;
		poly->SetAsBox(hx, hy);
	*/
	
	public void setAsBox(float hx, float hy, Vector2 center, float angle) {
		jniSetAsBox(addr, hx, hy, center.x, center.y, angle);
	}
	
	private native void jniSetAsBox(long addr, float hx, float hy, float centerX, float centerY, float angle); /*
		b2PolygonShape* poly = (b2PolygonShape*)addr;
		poly->SetAsBox( hx, hy, b2Vec2( centerX, centerY ), angle );
	*/
	
	public int getVertexCount() {
		return jniGetVertexCount(addr);
	}
	
	private native int jniGetVertexCount(long addr); /*
		b2PolygonShape* poly = (b2PolygonShape*)addr;
		return poly->GetVertexCount();
	*/
	
	private static final float[] vertices = new float[2];
	
	public void getVertex(int index, Vector2 vertex) {
		jniGetVertex(addr, index, vertices);
		vertex.x = vertices[0];
		vertex.y = vertices[1];
	}
	
	private native void jniGetVertex(long addr, int index, float[] vertices); /*
		b2PolygonShape* poly = (b2PolygonShape*)addr;
		const b2Vec2 v = poly->GetVertex( index );
		vertices[0] = v.x;
		vertices[1] = v.y;
	*/
	
}
