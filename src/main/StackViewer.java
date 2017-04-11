package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import cdr.geometry.primitives.ArrayVector3D;
import cdr.geometry.primitives.Polygon3D;

public class StackViewer {

	public List<Polygon3D> getViewableStack(Polygon3D footprint, Stack<List<String>> stack, float floorToCeilingHeight) {
		
		List<Polygon3D> viewableStack = new ArrayList<>();
		
		for (int i=0; i<stack.size(); i++) {		
			viewableStack.add(new Polygon3D(footprint.iterablePoints()));
			viewableStack.get(i).translate(new ArrayVector3D(0, 0, i*floorToCeilingHeight));
		}
		
		return viewableStack;
	}
}
