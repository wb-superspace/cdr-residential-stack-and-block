package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.sunflow.core.RayTracer;

import cdr.geometry.primitives.Point3D;
import cdr.geometry.primitives.Polygon3D;
import cdr.mesh.datastructure.Mesh3D;
import cdr.mesh.datastructure.fvMesh.FVMesh;
import cdr.mesh.toolkit.operators.MeshOperators;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import model.StackAnalysis.AnalysisFloor;
import model.StackAnalysis.AnalysisUnit;


public class StackEvaluator {
	
	public float floorplateCostBase = 125f; 							// AECOM
	public float floorplateCostFloorMultiplier = 3f; 					// AECOM
	public float unitPremiumFloorMultiplier = 0.015f; 					// AECOM (1.5% -> 2.2% (PH))
	
	float maxArea = 81747f;
	float maxHeight = 170f;
	
	private StackManager sm;
	
	private long sleep = 0l;
	
	public Set<Integer> hashes;	
	
	public Map<Point3D, Stack<AnalysisFloor>> analysis = new ConcurrentHashMap<>();
	
	public Map<String, Integer> counts = new ConcurrentHashMap<>();
	
	public SimpleFloatProperty value = new SimpleFloatProperty(0);
	
	public SimpleBooleanProperty evaluate = new SimpleBooleanProperty(false);
	
	RayTracer rt = new RayTracer();
	
	/*
	 * =========================================================
	 * BOUNDS
	 * =========================================================
	 */
		
	public float[] getBounds( Map<Point3D, Stack<AnalysisFloor>> analysisStacks, String valueType) {
		
		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;
		
		for (Stack<AnalysisFloor> analysisStack : analysisStacks.values()) {
		
			for (AnalysisFloor analysisFloor: analysisStack) {
				for (AnalysisUnit analysisUnit: analysisFloor.getAnalysisUnits()) {
					
					if (analysisUnit.getType() != null) {
						
						switch (valueType) {
						case "value":
							
							if (analysisUnit.value > max) max = analysisUnit.value;
							if (analysisUnit.value < min) min = analysisUnit.value;
							
							break;
						case "visibility":
							
							if (analysisUnit.visibility > max) max = analysisUnit.visibility;
							if (analysisUnit.visibility < min) min = analysisUnit.visibility;
							
							break;
						default:
							
							min = 0;
							max = 1;
							
							break;
						}
					}	
				}
			}
		}
		
		return new float[] {min, max};
	}
	
	/*
	 * =========================================================
	 * EVALUATIONS
	 * =========================================================
	 */
		
	public void evaluateFloorValue(AnalysisFloor analysisFloor) {
		
		for (AnalysisUnit analysisUnit : analysisFloor.getAnalysisUnits()) {
			
			String unitType = analysisUnit.getType();
						
			if (unitType != null) {
				
				int visibilityMultiplier = 1;
				
				float baseCost = floorplateCostFloorMultiplier * (analysisUnit.getFloorIndex() + 1) + floorplateCostBase;
					
				loop:
				for (Point3D viewpoint : sm.getViewPoints()) {
					for (Point3D analysisPoint : analysisUnit.getAnalysisPoints(false)) {
						if(!rt.obstructed(rt.createRay(analysisPoint, viewpoint))) {
							visibilityMultiplier++;
							continue loop;
						}
					}
				}
				
				float unitArea = sm.unitAreas.get(unitType);  
				float unitValue = sm.unitValues.get(unitType) * unitArea;
				float unitCost = baseCost * unitArea;
				float unitCap = sm.unitCaps.containsKey(unitType) ? sm.unitCaps.get(unitType) : sm.unitCaps.get(null);
								
				unitValue += unitValue * unitPremiumFloorMultiplier * (analysisUnit.getFloorIndex()+1) * visibilityMultiplier;
				
				if (unitValue > unitCap) {
					unitValue = unitCap;
				}
				
				analysisUnit.cost = unitCost;
				analysisUnit.value = unitValue;
				analysisUnit.visibility = visibilityMultiplier;
			}
		}
	}
			
	public float evaluateStackValue(Point3D footprint) {
		
		float value = 0;
				
		for (AnalysisFloor analysisFloor: analysis.get(footprint)) {
			value += analysisFloor.getValue();
		}
				
		return value;
	}
			
	public float evaluateTotalValue() {
				
		float value = 0;
		
		for (Point3D footprint : sm.getFootprints()) {
			value += evaluateStackValue(footprint);
		}
		
		return value;
	}
		
	private float evaluateTotalArea() {
		
		float area = 0f;
		
		for (Point3D footprint : sm.getFootprints()) {
			area += sm.getFootprintArea(footprint) * sm.getStack(footprint).size();
		}
						
		return area;
	}
	
	public int evaluateTotalDelta() {
		
		int delta = 0;
		
		for (String unitType : sm.getUnitTypes()) {
			
			int curr = evaluateTotalUnitCount(unitType);
			int count = sm.unitCounts.get(unitType);
			
			delta += curr - count;
		}
		
		return delta;
	}
	
	public int evaluateTotalUnitCount(String unitType) {
		
		int count = 0;
		
		if (sm != null) {
			
			for (Point3D footprint : sm.getFootprints()) {
				
				String footprintType = sm.getFootprintType(footprint);
				
				for (String floorplateType : sm.getStack(footprint)) {	
					
					Point3D floorplate = sm.getFloorplate(footprintType, floorplateType);
					List<Polygon3D> units = sm.getUnits(floorplate);
					
					if (units != null) {
						for (String type : sm.getUnitTypes(footprintType, floorplateType)) {
							if (type.equals(unitType)) {
								count++;
							}
						}
					}
				}
			}
		}
						
		return count;
	}
	
	/*
	 * =========================================================
	 * TESTS
	 * =========================================================
	 */
	
	private boolean isPushFloorValid(Point3D from , Point3D to) {
		
		float heightMax =  maxHeight;
		float heightTo = sm.floorToCeilingHeight * (sm.getStack(to).size() + 1);
		
		float areaMax = maxArea;
		float areaFrom = from == null ? 0 : sm.getFootprintArea(from);
		float areaTo = to == null ? 0 : sm.getFootprintArea(to);	
		float areaTotal = evaluateTotalArea() - areaFrom + areaTo;
				
		return (from == null || sm.getStack(from).size() > 0) && (heightTo < heightMax && areaTotal < areaMax);
	}
	
	
	/*
	 * =========================================================
	 * RUN
	 * =========================================================
	 */
	
	public Map<Point3D, Stack<AnalysisFloor>> getAnalysisStacks() {		
		return new HashMap<>(analysis);
	}
	
	private void setAnalysisStacks() {
		
		Mesh3D analysisMesh = new FVMesh.D3();
		
		analysis.clear();
		rt.clearMeshes();
		
		for (Point3D footprint : sm.getFootprints()) {
			
			Stack<AnalysisFloor> analysisStack = StackAnalysis.getAnalysisStack(sm, footprint, false);
								
			for (AnalysisFloor analysisFloor : analysisStack) {
				for (AnalysisUnit analysisUnit : analysisFloor.getAnalysisUnits()) {
					if (analysisUnit.getType() != null) {
						new MeshOperators().joinMeshes(analysisMesh, analysisUnit.getAnalysisMesh(false));
					}		
				}
			}
			
			analysis.put(footprint, analysisStack);
			
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		for (Mesh3D contextMesh : sm.getContext()) {
			new MeshOperators().joinMeshes(analysisMesh, contextMesh);
		}
		
		rt.addTriangleMesh(analysisMesh.toString(), analysisMesh);
		
		for (Map.Entry<Point3D, Stack<AnalysisFloor>> analysisEntry : analysis.entrySet()) {
			for (AnalysisFloor analysisFloor : analysisEntry.getValue()) {
				evaluateFloorValue(analysisFloor);
			}
		}
	}
			
	private String swapUnitType(String footprintType, String floorplateType) {
		
		List<String> selection = new ArrayList<>();
		List<String> floorplateTypes = new ArrayList<>(sm.getFloorplateTypes(footprintType));
		
		loop:
		for (String type : floorplateTypes) {
						
			for (String unitType : sm.getUnitTypes(footprintType, type)) {
				
				if (sm.unitCounts.get(unitType) - evaluateTotalUnitCount(unitType) < 0) {
					continue loop;
				}
			}
			
			selection.add(type);
		}
		
		String mutation = selection.isEmpty() 
			? null
			: selection.get(new Random().nextInt(selection.size()));
		
		return mutation;
	}
	
	private void sortStackFloors() {
		
		setAnalysisStacks();
				
		for (Point3D footprint : sm.getFootprints()) {
		
			for (int s=0; s<sm.getStack(footprint).size(); s++) {
				for (int t=s+1; t<sm.getStack(footprint).size(); t++) {				
					if (sm.getStack(footprint).get(s) != sm.getStack(footprint).get(t)) {
						
						 AnalysisFloor sAnalysisFloor = analysis.get(footprint).get(s);
						 AnalysisFloor tAnalysisFloor = analysis.get(footprint).get(t);
		
						 AnalysisFloor sAnalysisFloorClone = sAnalysisFloor.clone();
						 AnalysisFloor tAnalysisFloorClone = tAnalysisFloor.clone();
						 						 
						 sAnalysisFloorClone.setFloorIndex(t);
						 tAnalysisFloorClone.setFloorIndex(s);
						 
						 evaluateFloorValue(sAnalysisFloorClone);
						 evaluateFloorValue(tAnalysisFloorClone);
						 
						 float vc = sAnalysisFloor.getDelta() + tAnalysisFloor.getDelta();
						 float vt = sAnalysisFloorClone.getDelta() + tAnalysisFloorClone.getDelta();
						
						if (vc < vt) {
							sm.swapFloors(footprint, s, t);
														
							analysis.get(footprint).set(s, tAnalysisFloorClone);
							analysis.get(footprint).set(t, sAnalysisFloorClone);
							
							try {
								Thread.sleep(sleep);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							
						}
					}
				}
			}
		}
	}
		
	private void mutateStackFloors() {
		
		List<int[]> indexes = new ArrayList<>();
				
		for (int i = new Random().nextInt(sm.getPointers().size()); i>=0 ; i--) {
			
			List<int[]> pointers = sm.getPointers();
			Collections.shuffle(pointers, new Random());
			
			int[] pull = pointers.get(new Random().nextInt(pointers.size()));
			int[] push = pointers.get(new Random().nextInt(pointers.size()));
			
			if (!indexes.contains(push) && !indexes.contains(pull) && push[0] != pull[0]) {
				
				if (isPushFloorValid(sm.getFootprints().get(push[0]), sm.getFootprints().get(pull[0]))) {

					sm.removeFloor(sm.getFootprints().get(push[0]), push[1]);
					sm.pushFloor(sm.getFootprints().get(pull[0]), null);
					
					try {
						Thread.sleep(sleep);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					indexes.add(push);
					indexes.add(pull);
					
					i--;
				}
			}
		}
		
		for (int[] pointer : sm.getPointers()) {
			if (sm.getStack(sm.getFootprints().get(pointer[0])).get(pointer[1]) == null) {
				sm.getStack(sm.getFootprints().get(pointer[0])).set(pointer[1], 
						swapUnitType(sm.getFootprintType(sm.getFootprints().get(pointer[0])), null));
				
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		for (Point3D footprint : sm.getFootprints()) {
			for (int i=sm.getStack(footprint).size()-1; i>=0; i--) {
				if (sm.getStack(footprint).get(i) == null) {
					sm.removeFloor(footprint, i);
					
					try {
						Thread.sleep(sleep);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
			
		sortStackFloors();
		
		Integer hash = sm.getStackHash();
		
		if (hashes.contains(hash)) {
			mutateStackFloors();			
		} else {
			hashes.add(hash);
		}
	}
			
	public void reset(StackManager sm) {
		
		this.sm = sm;
		this.hashes = new HashSet<>();
		this.counts = new ConcurrentHashMap<>();
		this.value = new SimpleFloatProperty(0);
		this.analysis = new ConcurrentHashMap<>();
		
		sm.reset();		
				
		boolean valid = true;
		
		while (valid) {
			
			valid = false;
			
			for (Point3D footprint :  sm.getFootprints()) {	
				if (isPushFloorValid(null, footprint)) {
					sm.pushFloor(footprint, null);
					
					valid = true;
				}			
			}
		}
		
		for (String unitType : sm.getUnitTypes()) {
			this.counts.put(unitType, 0);
		}
	}
	
	private void update() {

		this.value.set(evaluateTotalValue());
		
		for (String unitType : sm.getUnitTypes()) {
			this.counts.put(unitType, evaluateTotalUnitCount(unitType));
		}
		
		sm.flag();
	}
	
	public void stop() {
		
		evaluate.set(false);
	}
	
//	public void _start() {
//		
//		evaluate.set(true);;		
//		
//		SortedMap<Float, Map<Point3D, Stack<String>>> elite = new TreeMap<>();
//		
//		int pool = 50;
//		int cross = 10;
//				
//		elite.put(evaluateTotalValue(), sm.saveState());
//		
//		do {
//						
//			SortedMap<Float, Map<Point3D, Stack<String>>> front = new TreeMap<>(elite);
//			
//			elite.clear();
//									
//			for (Map.Entry<Float, Map<Point3D, Stack<String>>> f : front.entrySet()) {
//				
//				sm.restoreState(f.getValue());
//												
//				boolean flag = false;
//								
//				if (f.getKey() != 0) {			
//					update();
//				}
//																
//				for (int p = pool ; p > 0 ; p--) {
//						
//					sm.saveStacks();
//					
//					mutateStackFloors();							
//											
//					float value = evaluateTotalValue();
//										
//					if (value > f.getKey()) {											
//						elite.put(value, sm.saveState());
//						flag = true;	
//					}				
//
//					try {
//						Thread.sleep(sleep);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//					
//					sm.restoreStacks();	
//				}
//								
//				if (!flag) {					
//					elite.put(f.getKey(), f.getValue());
//				}
//			}
//			
//			List<Float> values = new ArrayList<>(elite.keySet());
//			Collections.reverse(values);
//			
//			for (int i=values.size()-1; i>=cross; i--) {
//				elite.remove(values.get(i));
//			}
//					
//		} while(evaluate.get() == true);
//		
//		sm.restoreState(elite.get(elite.lastKey()));
//		
//		update();
//										
//		print();
//	}
		
	public void start() {
		
		evaluate.set(true);;		
		
		SortedMap<Float, Map<Point3D, Stack<String>>> elite = new TreeMap<>();
		
		int pool = 100;
				
		elite.put(0f, sm.saveState());
		
		do {
						
			SortedMap<Float, Map<Point3D, Stack<String>>> front = new TreeMap<>(elite);
			
			float mu  = front.firstKey(); //(front.firstKey() + front.lastKey()) / 2;

			elite.clear();
									
			for (Map.Entry<Float, Map<Point3D, Stack<String>>> f : front.entrySet()) {
				
				sm.restoreState(f.getValue());

				if (f.getKey() > mu) elite.put(f.getKey(), f.getValue());
						
				int offspring = (int) pool / front.size();
																				
				for (int p = offspring ; p > 0 ; p--) {
						
					sm.saveStacks();
										
					mutateStackFloors();							
											
					float value = evaluateTotalValue();
					
					if (value > mu) {											
						elite.put(value, sm.saveState());	
						
						update();
					}				

					try {
						Thread.sleep(sleep);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
										
					sm.restoreStacks();				
				}
			}
			
			if (elite.keySet().size() == 0) {			
				elite = front;			
			} 
			
		} while(evaluate.get() == true);
		
		sm.restoreState(elite.get(elite.lastKey()));
		
		setAnalysisStacks();
		
		update();
										
		print();
	}
													
	private void print() {
		
		System.out.println();
		System.out.println();
		System.out.println("total area: " + evaluateTotalArea() + " / " + maxArea);
		System.out.println("total value: " + evaluateTotalValue());
		System.out.println("total delta: " + evaluateTotalDelta());
		System.out.println();
		for (String unitType : sm.getUnitTypes()) {
			System.out.println(unitType + " : " + evaluateTotalUnitCount(unitType) + " / " + sm.unitCounts.get(unitType));
		}
		for (Point3D footprint : sm.getFootprints()) {
			System.out.println(sm.getFootprintType(footprint) + " height : " + sm.getStack(footprint).size() * sm.floorToCeilingHeight);
		}
		System.out.println();
	}
}
