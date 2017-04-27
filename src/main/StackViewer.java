package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

import cdr.geometry.primitives.ArrayPoint3D;
import cdr.geometry.primitives.ArrayVector3D;
import cdr.geometry.primitives.Point3D;
import cdr.geometry.primitives.Polygon3D;
import cdr.geometry.primitives.Rectangle2D;
import cdr.geometry.primitives.Vector3D;
import cdr.mesh.datastructure.Face;
import cdr.mesh.datastructure.Mesh3D;
import lucy.MoreMeshPrimitives;

public class StackViewer {

	public static List<Polygon3D> getViewableStackPolygons(StackManager sm, Point3D footprint, float floorToCeilingHeight) {
				
		List<Polygon3D> viewableStack = new ArrayList<>();
		
		if (sm == null) {
			return viewableStack;
		}
		
		CopyOnWriteArrayList<String> stack = new CopyOnWriteArrayList<>(sm.getStack(footprint));
		
		try {
			if (stack != null) {
				for (int i=0; i<stack.size(); i++) {		
					if (stack.get(i) != null) {
						Point3D floorplate = sm.getFloorplate(sm.getFootprintType(footprint), stack.get(i));
						List<Polygon3D> units = sm.getUnits(floorplate);	
						Vector3D translate = new ArrayVector3D(footprint.x()-floorplate.x(), footprint.y()-floorplate.y(), i*floorToCeilingHeight);
						
						for (Polygon3D unit : units) {
							Polygon3D translated = new Polygon3D(unit.iterablePoints());
							translated.translate(translate);
							viewableStack.add(translated);
						}
					}
				}
			}
		} catch (NullPointerException e) {
			// TODO: handle exception
		}

	
		return viewableStack;
	}
		
	public static Map<String, List<Mesh3D>> getViewableStackMeshes(StackManager sm, Point3D footprint, float floorToCeilingHeight) {
		
		Map<String, List<Mesh3D>> viewableStackUnits = new HashMap<>();
				
		if (sm == null) {
			return viewableStackUnits;
		}
		
		for (String type : sm.getUnitTypes()) {
			viewableStackUnits.put(type, new ArrayList<>());
		}
		
		CopyOnWriteArrayList<String> stack = new CopyOnWriteArrayList<>(sm.getStack(footprint));
		
		try {
			if (stack != null) {
				for (int i=0; i<stack.size(); i++) {		
					if (stack.get(i) != null) {
						Point3D floorplate = sm.getFloorplate(sm.getFootprintType(footprint), stack.get(i));
						List<Polygon3D> units = sm.getUnits(floorplate);	
						Vector3D translate = new ArrayVector3D(footprint.x()-floorplate.x(), footprint.y()-floorplate.y(), i*floorToCeilingHeight);
						
						for (Polygon3D unit : units) {
							Polygon3D translated = new Polygon3D(unit.iterablePoints());
							String unitType = sm.getUnitType(unit);				
							translated.translate(translate);
							
							if (unitType != null) {
								viewableStackUnits.get(unitType).add(MoreMeshPrimitives.makeExtrudedMeshFromPolygon3D(translated, floorToCeilingHeight));		
							}
						}
					}
				}
			}
		} catch (NullPointerException e) {
			// TODO: handle exception
		}

		return viewableStackUnits;
	}

	public static Map<Point3D, Float> getViewableStackValues(StackManager sm, StackEvaluator se, Point3D footprint) {
		
		Map<Point3D, Float> viewableStackValues = new HashMap<>();
		
		if (sm == null || se == null) {
			return viewableStackValues;
		}
		
		CopyOnWriteArrayList<String> stack = new CopyOnWriteArrayList<>(sm.getStack(footprint));
		
		try {
			if (stack != null) {
				for (int i=0; i<stack.size(); i++) {		
					if (stack.get(i) != null) {
						Point3D floorplate = sm.getFloorplate(sm.getFootprintType(footprint), stack.get(i));
						List<Polygon3D> units = sm.getUnits(floorplate);	
						Vector3D translate = new ArrayVector3D(footprint.x()-floorplate.x(), footprint.y()-floorplate.y(), i*se.floorToCeilingHeight);
						
						Point3D location = new ArrayPoint3D(floorplate);
						location.translate(translate);
												
						float value = se.evaluateFloorValue(units, i+1);
						
						viewableStackValues.put(location, value);
					}
				}
			}
		} catch (NullPointerException e) {
			// TODO: handle exception
		}
		
		return viewableStackValues;
	}
}
