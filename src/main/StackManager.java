package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import cdr.geometry.primitives.Polygon3D;

public class StackManager {
	
	private List<Polygon3D> boundaries = new ArrayList<>();
	private Map<String, List<Polygon3D>> footprints = new HashMap<>();
	private Map<Polygon3D, String> types = new HashMap<>();
	private Map<Polygon3D, Stack<List<String>>> stacks = new HashMap<>();
			
	public Set<String> getFootprintTypes() {
		return this.footprints.keySet();
	}
	
	public String getFootprintType(Polygon3D footprint) {
		return this.types.get(footprint);
	}
	
	public void addFootPrint(String footprintType, List<Polygon3D> footprints) {
		this.footprints.put(footprintType, footprints);
		
		for (Polygon3D footprint : footprints) {
			this.types.put(footprint, footprintType);
			this.stacks.put(footprint, new Stack<>());
		}
	}
	
	public void pushFloor(Polygon3D footprint, List<String> floor) {
		this.stacks.get(footprint).push(floor);
	}
		
	public List<String> popFloor(Polygon3D footprint) {
		return this.stacks.get(footprint).pop();
	}
	
	public void swapFloor(Polygon3D f1, Integer i1, Polygon3D f2, Integer i2) {
		
		List<String> u1 = new ArrayList<>(stacks.get(f1).get(i1));
		List<String> u2 = new ArrayList<>(stacks.get(f2).get(i2));
		
		stacks.get(f2).set(i2, u1);
		stacks.get(f1).set(i1, u2);
	}
	
//	public void insertFloor(Polygon3D fromFootprint, Integer fromIndex, Polygon3D toFootprint, Integer toIndex) {
//		
//		List<String> floor = stacks.get(fromFootprint).get(fromIndex);
//		stacks.get(fromFootprint).remove(fromIndex);
//		stacks.get(toFootprint).insertElementAt(floor, toIndex);
//	}
			
	public List<Polygon3D> getFootprints (String footprintType) {
		return this.footprints.get(footprintType);
	}
	
	public Stack<List<String>> getStack(Polygon3D footprint) {
		return this.stacks.get(footprint);
	}
	
	public void setStack(Polygon3D footprint, Stack<List<String>> stack) {
		stacks.put(footprint, stack);
	}
	
	public List<Polygon3D> getBoundaries() {
		return this.boundaries;
	}
}
