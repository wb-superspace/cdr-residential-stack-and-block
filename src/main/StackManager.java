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
		return footprints.keySet();
	}
	
	public void addFootPrint(String footprintType, List<Polygon3D> footprints) {
		this.footprints.put(footprintType, footprints);
		
		for (Polygon3D footprint : footprints) {
			this.types.put(footprint, footprintType);
			this.stacks.put(footprint, new Stack<>());
		}
	}
	
	public void pushFloor(String footprintType, List<String> floorMix) {
		this.stacks.get(footprintType).push(floorMix);
	}
	
	public List<String> popFloor(String footprintType) {
		return this.stacks.get(footprintType).pop();
	}
		
	public List<Polygon3D> getFootprints (String footprintType) {
		return footprints.get(footprintType);
	}
	
	public Stack<List<String>> getStack(Polygon3D footprint) {
		return this.stacks.get(footprint);
	}
	
	public List<Polygon3D> getBoundaries() {
		return this.boundaries;
	}
}
