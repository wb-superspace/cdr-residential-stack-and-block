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
import model.StackAnalysis.AnalysisFloor;
import model.StackAnalysis.AnalysisUnit;

public class StackEvaluator {
		
	private StackManager sm;
	
	private long sleep = 0l;
	
	public Set<String> hashes;	
	
	public Map<Point3D, Stack<AnalysisFloor>> analysis = new ConcurrentHashMap<>();
	
	public Map<String, Integer> counts = new ConcurrentHashMap<>();
		
	public SimpleFloatProperty value = new SimpleFloatProperty(-Float.MAX_VALUE);
	
	public SimpleFloatProperty delta = new SimpleFloatProperty(0f);
	
	public SimpleBooleanProperty evaluate = new SimpleBooleanProperty(false);
	
	RayTracer rt = new RayTracer();
		
	/*
	 * =========================================================
	 * EVALUATIONS
	 * =========================================================
	 */
		
	public void evaluateFloor(AnalysisFloor analysisFloor) {
		
		float floorCost = 0f;
		float floorValue = 0f;
		
		for (AnalysisUnit analysisUnit : analysisFloor.getAnalysisUnits()) {
			
			String unitType = analysisUnit.getUnitType();
			
			float unitArea = analysisUnit.getArea(); 
			float baseCost = sm.floorplateCostFloorMultiplier * (analysisUnit.getFloorIndex() + 1) + sm.floorplateCostBase;
			float unitCost = baseCost * unitArea;
									
			if (unitType != null) {
				
				float visibilityMultiplier = 0;
				float visibilityTotal = 0;
							
				for (Point3D viewpoint : sm.getViewPoints()) {
					for (Point3D analysisPoint : analysisUnit.getAnalysisPoints(false)) {
						if(!rt.obstructed(rt.createRay(analysisPoint, viewpoint))) {
							visibilityMultiplier++;
						}
						visibilityTotal++;
					}
				}
									
				float unitValue = sm.unitValues.get(unitType) * unitArea;
				float unitCap = sm.unitCaps.containsKey(unitType) ? sm.unitCaps.get(unitType) : sm.unitCaps.get(null);
					
				float unitFloorPremium = unitValue * sm.unitPremiumFloorMultiplier * (analysisUnit.getFloorIndex()+1);
				float unitVisibilityPremium =  visibilityTotal != 0 ? unitValue * (visibilityMultiplier / visibilityTotal) : 0;
				
				unitValue += unitFloorPremium + unitVisibilityPremium;
			
				if (unitValue > unitCap) {
					unitValue = unitCap;
				}
								
				analysisUnit.addAttribute("unitValue", unitValue);
				analysisUnit.addAttribute("unitVisibility", visibilityMultiplier);
				
				floorCost += unitCost;
				floorValue += unitValue;
			} 
		}
				
		for (AnalysisUnit analysisUnit: analysisFloor.getAnalysisUnits()) {
			
			analysisUnit.addAttribute("floorCost", floorCost);
			analysisUnit.addAttribute("floorValue", floorValue);
			analysisUnit.addAttribute("floorDelta", floorValue - floorCost);
			
			analysisFloor.addAttribute("floorCost", floorCost);
			analysisFloor.addAttribute("floorValue", floorValue);
			analysisFloor.addAttribute("floorDelta", floorValue - floorCost);
		}
	}
			
	public float evaluateStackDelta(Point3D footprint) {
		
		float value = 0;
				
		for (AnalysisFloor analysisFloor: analysis.get(footprint)) {
			value += analysisFloor.getAttribute("floorDelta");
		}
				
		return value;
	}
			
	public float evaluateTotalDelta() {
				
		float value = 0;
		
		for (Point3D footprint : sm.getFootprints()) {
			value += evaluateStackDelta(footprint);
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
	
	public int evalutatetotalUnitMix() {
		
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
		
		float heightMax =  sm.maxHeight;
		float heightTo = sm.floorToCeilingHeight * (sm.getStack(to).size() + 1);
		
		float areaMax = sm.maxArea;
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
		
		rt.clearMeshes();
		
		Mesh3D analysisMesh = new FVMesh.D3();
		Map<Point3D, Stack<AnalysisFloor>> analysisStacks = StackAnalysis.getAnalysisStacks(sm);
		
		for (Map.Entry<Point3D, Stack<AnalysisFloor>> analysisStack : analysisStacks.entrySet()) {
											
			for (AnalysisFloor analysisFloor : analysisStack.getValue()) {
				for (AnalysisUnit analysisUnit : analysisFloor.getAnalysisUnits()) {
					if (analysisUnit.getUnitType() != null) {
						new MeshOperators().joinMeshes(analysisMesh, analysisUnit.getAnalysisMesh(false));
					}		
				}
			}
		}
		
		for (Mesh3D contextMesh : sm.getContext()) {
			new MeshOperators().joinMeshes(analysisMesh, contextMesh);
		}
		
		rt.addTriangleMesh(analysisMesh.toString(), analysisMesh);
		
		for (Map.Entry<Point3D, Stack<AnalysisFloor>> analysisEntry : analysisStacks.entrySet()) {
			for (AnalysisFloor analysisFloor : analysisEntry.getValue()) {
				evaluateFloor(analysisFloor);
			}
		}
		
		analysis = analysisStacks;
	}
			
	private String swapUnitType(String footprintType, String floorplateType) {
		
		List<String> selection = new ArrayList<>();
		List<String> floorplateTypes = new ArrayList<>(sm.getFloorplateTypes(footprintType));
		
		loop:
		for (String type : floorplateTypes) {
			
			if (type == floorplateType) {
				continue loop;
			}
			
			for (String unitType : sm.getUnitTypes(footprintType, type)) {
				
				if (sm.unitCounts.get(unitType) - evaluateTotalUnitCount(unitType) <= 0) {
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
						 
						 evaluateFloor(sAnalysisFloorClone);
						 evaluateFloor(tAnalysisFloorClone);
						 
						 float vc = sAnalysisFloor.getAttribute("floorDelta") + tAnalysisFloor.getAttribute("floorDelta");
						 float vt = sAnalysisFloorClone.getAttribute("floorDelta") + tAnalysisFloorClone.getAttribute("floorDelta");
												 
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
		
		if (sm.getPointers().isEmpty()) {
			return;
		}
			
		for (int i = new Random().nextInt(sm.getPointers().size()); i>=0;) {
						
			int[] pointer  = sm.getPointers().get(new Random().nextInt(sm.getPointers().size()));
			
			Point3D footprint = sm.getFootprints().get(pointer[0]);
			
			if (sm.getStack(footprint).get(pointer[1]) != null || value.get() == -Float.MAX_VALUE) {
				
				float option = new Random().nextFloat();
				
				if (option < 0.5) {
					sm.pushFloor(footprint, null);
				} else if (0.5 <= option ) {
					sm.removeFloor(footprint, pointer[1]);
				}
				
				i--;
			}
		}
				
		for (int[] pointer : sm.getPointers()) {
			
			Point3D footprint = sm.getFootprints().get(pointer[0]);
			
			if (sm.getStack(footprint).get(pointer[1]) == null) {
				sm.getStack(footprint).set(pointer[1], 
						swapUnitType(sm.getFootprintType(footprint), null));
			}
		}
		
		for (Point3D footprint : sm.getFootprints()) {
			for (int i=sm.getStack(footprint).size()-1; i>=0; i--) {
				if (sm.getStack(footprint).get(i) == null) {
					sm.removeFloor(footprint, i);
				}
			}
		}
		
		setAnalysisStacks();
		sortStackFloors();
		
		String hashString = sm.getHashString();
		
		if (hashes.contains(hashString)) {
			mutateStackFloors();			
		} else {
			hashes.add(hashString);
		}
	}
			
	public void reset(StackManager sm) {
		
		this.sm = sm;
		this.hashes = new HashSet<>();
		this.counts = new ConcurrentHashMap<>();
		this.value = new SimpleFloatProperty(-Float.MAX_VALUE);
		this.analysis = new ConcurrentHashMap<>();
		
		sm.reset();		
				
		boolean valid = true;
		
		while (valid) {
			
			valid = false;
			
			for (Point3D footprint : sm.getFootprints()) {
				
				String floorplateType = swapUnitType(sm.getFootprintType(footprint), null);
				
				if (floorplateType != null) {
					sm.pushFloor(footprint, floorplateType);
					
					valid = true;
				}
			}
		}
		
		for (Point3D footprint : sm.getFootprints()) {
			for (int i = 0; i<sm.getStack(footprint).size(); i++) {
				sm.getStack(footprint).set(i, null);
			}
		}
		
		for (String unitType : sm.getUnitTypes()) {
			this.counts.put(unitType, 0);
		}
	}
	
	private void update() {

		float value = evaluateTotalDelta();
		float delta = value - this.value.get();
		
		this.value.set(value);
		this.delta.set(delta);
		
		for (String unitType : sm.getUnitTypes()) {
			this.counts.put(unitType, evaluateTotalUnitCount(unitType));
		}
		
		sm.flag();
	}
	
	public void stop() {
		
		evaluate.set(false);
	}
			
	public void start() {
		
		evaluate.set(true);;		
		
		SortedMap<Float, Map<Point3D, Stack<String>>> elite = new TreeMap<>();
		
		int pool = 100;
				
		elite.put(-Float.MAX_VALUE, sm.saveState());
		
		do {
						
			SortedMap<Float, Map<Point3D, Stack<String>>> front = new TreeMap<>(elite);
			
			float mu  = (front.firstKey() + front.lastKey()) / 2;
			
			elite.clear();
									
			for (Map.Entry<Float, Map<Point3D, Stack<String>>> f : front.entrySet()) {
				
				sm.restoreState(f.getValue());

				if (f.getKey() >= mu) elite.put(f.getKey(), f.getValue());
						
				int offspring = (int) pool / front.size();
																				
				for (int p = offspring ; p > 0 ; p--) {
						
					sm.saveStacks();
										
					mutateStackFloors();							
											
					float delta = evaluateTotalDelta();
					
					if (delta >= mu) {											
						elite.put(delta, sm.saveState());						
						if (f.getKey() != -Float.MAX_VALUE) update();
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
		System.out.println("total area: " + evaluateTotalArea() + " / " + sm.maxArea);
		System.out.println("total delta: " + evaluateTotalDelta());
		System.out.println("total mix: " + evalutatetotalUnitMix());
		System.out.println();
		for (String unitType : sm.getUnitTypes()) {
			System.out.println(unitType + " : " + evaluateTotalUnitCount(unitType) + " / " + sm.unitCounts.get(unitType));
		}
		System.out.println();
		for (Point3D footprint : sm.getFootprints()) {
			System.out.println(sm.getFootprintType(footprint) + " height : " + sm.getStack(footprint).size() * sm.floorToCeilingHeight);
		}
		System.out.println();
	}
}
