package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

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

	public static List<Polygon3D> getViewableStack(StackManager sm, Polygon3D footprint, float floorToCeilingHeight) {
				
		List<Polygon3D> viewableStack = new ArrayList<>();
		
		if (sm == null) {
			return viewableStack;
		}
		
		for (int i=0; i<sm.getStack(footprint).size(); i++) {		
			viewableStack.add(new Polygon3D(footprint.iterablePoints()));
			viewableStack.get(i).translate(new ArrayVector3D(0, 0, i*floorToCeilingHeight));
		}
		
		return viewableStack;
	}
		
	public static Map<String, List<Mesh3D>> getViewableStackUnits(StackManager sm, BlockManager bm, Polygon3D footprint, float floorToCeilingHeight) {
		
		Map<String, List<Mesh3D>> viewableStackUnits = new HashMap<>();
		
		float e1 = footprint.getLineSegment(0).getLength();
		float e2 =  footprint.getLineSegment(1).getLength();
		
		float minEdge = Math.min(e1,e2);
		float maxEdge = Math.max(e1, e2);
				
		for (String unitType : bm.getUnitTypes()) {
			viewableStackUnits.put(unitType, new ArrayList<>());
		}
		
		CopyOnWriteArrayList<List<String>> list = new CopyOnWriteArrayList<>(sm.getStack(footprint));
		
		for (int i =0; i<list.size(); i++) {
			
			float offset = 0f;
			
			for (int j=0; j<list.get(i).size(); j++) {
							
				float height = bm.getUnit(list.get(i).get(j)).area / minEdge;
				float width = bm.getUnit(list.get(i).get(j)).area / height;
							
				Rectangle2D unit = new Rectangle2D(footprint.getAnchor().x(), footprint.getAnchor().y() + offset, width, height);
				Polygon3D boundary = unit.getPolygon2D().getPolygon3D(i*floorToCeilingHeight);
				
				viewableStackUnits.get(list.get(i).get(j)).add(MoreMeshPrimitives.makeExtrudedMeshFromPolygon3D(boundary, floorToCeilingHeight));
				
				offset+= height;
			}
		}
		
		return viewableStackUnits;
	}
}
