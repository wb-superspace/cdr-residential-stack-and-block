package main;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BlockManager {
	
	private Map<String, Map<String, Map<String, Float>>> blockMatrix = new HashMap<>();
	
	public void addValue(String footprintType, String unitType, String propertyType, Float propertyValue) {
		if (!this.blockMatrix.containsKey(footprintType)) {
			this.blockMatrix.put(footprintType, new HashMap<>());
		}
		if (!this.blockMatrix.get(footprintType).containsKey(unitType)) {
			this.blockMatrix.get(footprintType).put(unitType, new HashMap<>());
		}
		this.blockMatrix.get(footprintType).get(propertyType).put(propertyType, propertyValue);
	}
	
	public Set<String> getFootprintTypes() {
		return this.blockMatrix.keySet();
	}
	
	public Set<String> getUnitTypes(String footprintType) {
		return this.blockMatrix.get(footprintType).keySet();
	}
	
	public Set<String> getPropertyTypes(String footprintType, String unitType) {
		return this.blockMatrix.get(footprintType).get(unitType).keySet();
	}
	
	public Float getPropertyValue(String footprintType, String unitType, String propertyType) {
		return this.blockMatrix.get(footprintType).get(unitType).get(propertyType);
	}
}
