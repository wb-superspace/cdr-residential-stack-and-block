package main;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BlockManager {
	
	private Map<String, Unit> blockMatrix = new HashMap<>();
	
	public void addUnit(String type, Integer count, Float area, Float value, String color) {
		blockMatrix.put(type, new Unit(type, count, area, value, color));
	}
	
	public Set<String> getUnitTypes() {
		return this.blockMatrix.keySet();
	}
	
	public Unit getUnit(String type) {
		return this.blockMatrix.get(type);
	}
	
	public class Unit {
		
		public String type;
		public String color;
		
		public Integer count;
		public Float area;
		public Float value;
		
		public Unit(String type, Integer count, Float area, Float value, String color) {
			this.type = type;
			this.count = count;
			this.area = area;
			this.value = value;
			this.color = color;
		}

		@Override
		public String toString() {
			return this.type;
		}
	}
}
