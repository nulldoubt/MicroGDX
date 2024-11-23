package me.nulldoubt.micro.physics.box2d;

import me.nulldoubt.micro.math.Vector2;

public class ChainShape extends Shape {
	
	/*JNI
#include <Box2D/Box2D.h>
	 */
	boolean isLooped = false;
	
	public ChainShape() {
		addr = newChainShape();
	}
	
	private native long newChainShape(); /*
		return (jlong)(new b2ChainShape());
	*/
	
	ChainShape(long addr) {
		this.addr = addr;
	}
	
	@Override
	public Type getType() {
		return Type.Chain;
	}
	
	public void createLoop(float[] vertices) {
		jniCreateLoop(addr, vertices, 0, vertices.length / 2);
		isLooped = true;
	}
	
	public void createLoop(float[] vertices, int offset, int length) {
		jniCreateLoop(addr, vertices, offset, length / 2);
		isLooped = true;
	}
	
	public void createLoop(Vector2[] vertices) {
		float[] verts = new float[vertices.length * 2];
		for (int i = 0, j = 0; i < vertices.length * 2; i += 2, j++) {
			verts[i] = vertices[j].x;
			verts[i + 1] = vertices[j].y;
		}
		jniCreateLoop(addr, verts, 0, verts.length / 2);
		isLooped = true;
	}
	
	private native void jniCreateLoop(long addr, float[] verts, int offset, int numVertices); /*
		b2ChainShape* chain = (b2ChainShape*)addr;
		b2Vec2* verticesOut = new b2Vec2[numVertices];
		for( int i = 0; i < numVertices; i++ )
			verticesOut[i] = b2Vec2(verts[offset+(i<<1)], verts[offset+(i<<1)+1]);
		chain->CreateLoop( verticesOut, numVertices );
		delete[] verticesOut;
	*/
	
	public void createChain(float[] vertices) {
		jniCreateChain(addr, vertices, 0, vertices.length / 2);
		isLooped = false;
	}
	
	public void createChain(float[] vertices, int offset, int length) {
		jniCreateChain(addr, vertices, offset, length / 2);
		isLooped = false;
	}
	
	public void createChain(Vector2[] vertices) {
		float[] verts = new float[vertices.length * 2];
		for (int i = 0, j = 0; i < vertices.length * 2; i += 2, j++) {
			verts[i] = vertices[j].x;
			verts[i + 1] = vertices[j].y;
		}
		jniCreateChain(addr, verts, 0, vertices.length);
		isLooped = false;
	}
	
	private native void jniCreateChain(long addr, float[] verts, int offset, int numVertices); /*
		b2ChainShape* chain = (b2ChainShape*)addr;
		b2Vec2* verticesOut = new b2Vec2[numVertices];
		for( int i = 0; i < numVertices; i++ )
			verticesOut[i] = b2Vec2(verts[offset+(i<<1)], verts[offset+(i<<1)+1]);
		chain->CreateChain( verticesOut, numVertices );
		delete[] verticesOut;
	*/
	
	public void setPrevVertex(Vector2 prevVertex) {
		setPrevVertex(prevVertex.x, prevVertex.y);
	}
	
	public void setPrevVertex(float prevVertexX, float prevVertexY) {
		jniSetPrevVertex(addr, prevVertexX, prevVertexY);
	}
	
	private native void jniSetPrevVertex(long addr, float x, float y); /*
		b2ChainShape* chain = (b2ChainShape*)addr;
		chain->SetPrevVertex(b2Vec2(x, y));
	*/
	
	public void setNextVertex(Vector2 nextVertex) {
		setNextVertex(nextVertex.x, nextVertex.y);
	}
	
	public void setNextVertex(float nextVertexX, float nextVertexY) {
		jniSetNextVertex(addr, nextVertexX, nextVertexY);
	}
	
	private native void jniSetNextVertex(long addr, float x, float y); /*
		b2ChainShape* chain = (b2ChainShape*)addr;
		chain->SetNextVertex(b2Vec2(x, y));
	*/
	
	public int getVertexCount() {
		return jniGetVertexCount(addr);
	}
	
	private native int jniGetVertexCount(long addr); /*
		b2ChainShape* chain = (b2ChainShape*)addr;
		return chain->GetVertexCount();
	*/
	
	private static final float[] vertices = new float[2];
	
	public void getVertex(int index, Vector2 vertex) {
		jniGetVertex(addr, index, vertices);
		vertex.x = vertices[0];
		vertex.y = vertices[1];
	}
	
	private native void jniGetVertex(long addr, int index, float[] vertices); /*
		b2ChainShape* chain = (b2ChainShape*)addr;
		const b2Vec2 v = chain->GetVertex( index );
		vertices[0] = v.x;
		vertices[1] = v.y;
	*/
	
	public boolean isLooped() {
		return isLooped;
	}
	
	public void clear() {
		jniClear(addr);
	}
	
	private native void jniClear(long addr); /*
		b2ChainShape* chain = (b2ChainShape*)addr;
		chain->Clear();
	*/
	
}
