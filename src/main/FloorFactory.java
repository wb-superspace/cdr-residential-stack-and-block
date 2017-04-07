package main;

import java.util.HashMap;
import java.util.Map;

import cdr.geometry.primitives.Polygon3D;

public class FloorFactory {
	
	private Map<String, Polygon3D> footprints = new HashMap<>(); 
	private Map<String, Float> costs = new HashMap<>();
	private Map<String, Float> premiums = new HashMap<>();

	public void addFootprint(String type, Polygon3D footprint) {
		this.footprints.put(type, footprint);
	}
	
	public void addCost(String type, Float cost) {
		this.costs.put(type, cost);
	}
	
	public void addPremium(String type, Float premium) {
		this.premiums.put(type, premium);
	}
	
	public Floor createFloor(String type) {
		return new Floor(footprints.get(type), costs.get(type), premiums.get(type));
	}
	
	public class Floor {
				
		public final Polygon3D footprint;
		public final float cost;
		public final float premium;
		
		public Floor(Polygon3D footprint, float cost, float premium) {
			this.footprint = footprint;
			this.cost = cost;
			this.premium = premium;
		}
	}
}


