package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import cdr.geometry.primitives.ArrayVector3D;
import cdr.geometry.primitives.Polygon3D;
import cdr.geometry.primitives.Rectangle2D;
import cdr.mesh.datastructure.Face;
import cdr.mesh.datastructure.Mesh3D;
import lucy.MoreMeshPrimitives;

public class StackViewer {
	
	BlockManager bm;
	StackManager sm;
	
	public void setBlockManager(BlockManager bm) {
		this.bm = bm;
	}
	
	public void setStackManager(StackManager sm) {
		this.sm = sm;
	}

	public List<Polygon3D> getViewableStack(Polygon3D footprint, Stack<List<String>> stack, float floorToCeilingHeight) {
				
		List<Polygon3D> viewableStack = new ArrayList<>();
		
		if (sm == null) {
			return viewableStack;
		}
		
		for (int i=0; i<stack.size(); i++) {		
			viewableStack.add(new Polygon3D(footprint.iterablePoints()));
			viewableStack.get(i).translate(new ArrayVector3D(0, 0, i*floorToCeilingHeight));
		}
		
		return viewableStack;
	}
	
	public Map<String, List<Polygon3D>> getViewableStackUnits(Polygon3D footprint, Stack<List<String>> stack, float floorToCeilingHeight) {
				
		Map<String, List<Polygon3D>> viewableStackUnits = new HashMap<>();
		
		if (bm == null) {
			return viewableStackUnits;
		}
		
		for (String unitType : bm.getUnitTypes()) {
			viewableStackUnits.put(bm.getUnit(unitType).color, new ArrayList<>());
		}
		
		for (int i =0; i<stack.size(); i++) {
			
			float offset = 0f;
			
			for (int j=0; j<stack.get(i).size(); j++) {
				
				float width = bm.getUnit(stack.get(i).get(j)).area;
				float height = bm.getUnit(stack.get(i).get(j)).area;
				
				width = (float) Math.sqrt(width);
				height = (float) Math.sqrt(height);
				
				String color = bm.getUnit(stack.get(i).get(j)).color;
				
				Rectangle2D unit = new Rectangle2D(footprint.getAnchor().x(), footprint.getAnchor().y() + offset, width, height);
			
				Polygon3D boundary = unit.getPolygon2D().getPolygon3D(i*floorToCeilingHeight);
				
				Mesh3D mesh = MoreMeshPrimitives.makeExtrudedMeshFromPolygon3D(boundary, floorToCeilingHeight);
				
				for(Face face : mesh.iterableFaces()) {
				
					viewableStackUnits.get(color).add(mesh.getPolygon(face)); 					
				}

				offset+= height;
			}
		}
		
		return viewableStackUnits;
	}
}
