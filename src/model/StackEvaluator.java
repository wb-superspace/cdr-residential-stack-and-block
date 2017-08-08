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
import cdr.mesh.datastructure.Mesh3D;
import cdr.mesh.datastructure.fvMesh.FVMesh;
import cdr.mesh.toolkit.operators.MeshOperators;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import jdk.nashorn.internal.ir.annotations.Ignore;
import model.StackAnalysis.AnalysisFloor;
import model.StackAnalysis.AnalysisFloor.AnalysisUnit;
import sun.reflect.generics.tree.FloatSignature;

public class StackEvaluator {
		
	private StackManager sm;
	
	private long sleep = 0l;
	
	public Set<String> stackHashes;
	
	public Map<String, AnalysisFloor> floorHashes;
	
	public Map<Point3D, Stack<AnalysisFloor>> analysis = new ConcurrentHashMap<>();
	
	public Map<String, Integer> counts = new ConcurrentHashMap<>();
		
	public SimpleFloatProperty value = new SimpleFloatProperty(-Float.MAX_VALUE);
		
	public SimpleBooleanProperty evaluate = new SimpleBooleanProperty(false);
	
	public SimpleIntegerProperty generation = new SimpleIntegerProperty(0);
		
	RayTracer rt = new RayTracer();
	
	public StackEvaluator(StackManager sm) {
		this.sm = sm;
	}
	
	/*
	 * =========================================================
	 * EVALUATIONS
	 * =========================================================
	 */
	
	private float evaluateDelta(Map<Point3D, Stack<AnalysisFloor>> analysisStacks) {
		
		float delta = 0;
		
		for (Stack<AnalysisFloor> analysisStack: analysisStacks.values()) {
			for (AnalysisFloor analysisFloor : analysisStack) {
				delta += StackAnalysis.getAnalysisAttribute(analysisFloor.getAnalysisUnits(), "unitDelta-total").getSum();
			}
		}

		return delta;
	}
	
	private boolean evaluateValidity(Map<Point3D, Stack<AnalysisFloor>> analysisStacks) {
				
		Map<String, Integer> counts = new HashMap<>();
		
		for (Stack<AnalysisFloor> analysisStack: analysisStacks.values()) {
			for (AnalysisFloor analysisFloor : analysisStack) {
				for (AnalysisUnit analysisUnit : analysisFloor.getAnalysisUnits()) {
					
					if (!counts.containsKey(analysisUnit.getUnitType())) {
						counts.put(analysisUnit.getUnitType(), 0);
					}
					
					counts.put(analysisUnit.getUnitType(), counts.get(analysisUnit.getUnitType()) + 1);
				}
			}
		}
		
		for (Map.Entry<String, Integer> count : counts.entrySet()) {
			if (count.getKey() != null && count.getValue() > sm.unitCounts.get(count.getKey())) {
				return false;
			}
		}
		
		return true;
	}
		
	private void evaluateFloor(AnalysisFloor analysisFloor) {
		
		String floorHash = analysisFloor.getHashString();
		for (int p : analysisFloor.getRelativPosition()) {
			floorHash += "," + p;
		}
				
		if (floorHashes.containsKey(floorHash)) {	
			analysisFloor.copy(floorHashes.get(floorHash));
			return;
		}
		
		for (AnalysisUnit analysisUnit : analysisFloor.getAnalysisUnits()) {
			
			float unitArea = analysisUnit.getArea();
			float unitCost = evaluateUnitCost(analysisUnit);
			float unitValue = evaluateUnitValue(analysisUnit);
			float unitDelta = unitValue - unitCost;
			
			analysisUnit.addAttribute("unitDelta-total", unitDelta, "\u00A3");
			analysisUnit.addAttribute("unitDelta-m2", unitDelta / unitArea, "\u00A3");
		}
									
		floorHashes.put(floorHash, analysisFloor.clone());
	}
		
	private float evaluateUnitValue(AnalysisUnit analysisUnit) {
		
		float unitValue = 0f;
		float unitArea = analysisUnit.getArea();
		
		if (analysisUnit.getUnitType() != null) {
											
			float unitCap = sm.unitCaps.containsKey(analysisUnit.getUnitType())
					? sm.unitCaps.get(analysisUnit.getUnitType())
					: sm.unitCaps.get(null);
				
			float unitVisibilityPremium = evaluateUnitVisibilityPremium(analysisUnit);
			float unitFloorPremium = evaluateUnitFloorPremium(analysisUnit);
			
			float unitValuePremium = unitFloorPremium + unitVisibilityPremium;	
			float unitValueBase = analysisUnit.getBaseValue();
			float unitValueTotal = unitValueBase + unitValuePremium;
		
			if (unitValueTotal > unitCap) {
				unitValueTotal = unitCap;
			}
											
			analysisUnit.addAttribute("unitValue-base", unitValueBase, "\u00A3");
			analysisUnit.addAttribute("unitValue-premium", unitValuePremium, "\u00A3");
			analysisUnit.addAttribute("unitValue-total", unitValueTotal, "\u00A3");
			analysisUnit.addAttribute("unitValue-m2", unitValueTotal / unitArea, "\u00A3");

			analysisUnit.addAttribute("unitPremium-visibility", unitVisibilityPremium, "\u00A3");
			analysisUnit.addAttribute("unitPremium-floor", unitFloorPremium, "\u00A3");
			
			unitValue = unitValueTotal;
		} 
				
		return unitValue;
	}
	
	private float evaluateUnitCost(AnalysisUnit analysisUnit) {
		
		float unitCost = 0f;
		float unitArea = analysisUnit.getArea();
		
		float unitCostBase = analysisUnit.getBaseCost();
		float unitCostMultiplier = sm.floorplateCostFloorMultiplier * analysisUnit.getFloorIndex();
		float unitCostTotal =  unitCostBase * (unitCostMultiplier + 1); 
		
		analysisUnit.addAttribute("unitCost-base", unitCostBase, "\u00A3");
		analysisUnit.addAttribute("unitCost-total", unitCostTotal, "\u00A3");
		analysisUnit.addAttribute("unitCost-m2", unitCostTotal / unitArea, "\u00A3");
		
		unitCost = unitCostTotal;
				
		return unitCost;
	}
	
	private float evaluateUnitVisibilityPremium(AnalysisUnit analysisUnit) {
		
		float unitVisibilityPremium = 0f;
		
		float totalViewablePoints = 0f;
		float totalViewedPoints = 0f;
		
		Set<Point3D> viewedPoints = new HashSet<>();
		
		for (Point3D viewpoint : sm.getViewPoints()) {
			for (Point3D analysisPoint : analysisUnit.getAnalysisPoints(false)) {
				if(!rt.obstructed(rt.createRay(analysisPoint, viewpoint))) {
					
					viewedPoints.add(viewpoint);
					
					totalViewedPoints++;
				}
				totalViewablePoints++;
			}
		}
		
		if (totalViewablePoints == 0) return 0;
			
		float visibilityRatio = totalViewedPoints / totalViewablePoints;
		float visibilityMultiplier = sm.unitPremiumVisibilityMultiplier * visibilityRatio;
		float visibilityPremium = analysisUnit.getBaseValue() * visibilityMultiplier;
		
		unitVisibilityPremium = visibilityPremium;
		
		analysisUnit.addAttribute("unitVisibility-viewPoints", viewedPoints.size(), "pts");
		analysisUnit.addAttribute("unitVisibility-totalPoints", totalViewedPoints, "pts");
		
		return unitVisibilityPremium;		
	}
	
	private float evaluateUnitFloorPremium(AnalysisUnit analysisUnit) {
		
		float unitFloorPremium = 0f;
		
		float floorMultiplier = sm.unitPremiumFloorMultiplier * analysisUnit.getFloorIndex();
		float floorPremium = analysisUnit.getBaseValue() * floorMultiplier;
		
		unitFloorPremium = floorPremium;
	
		return unitFloorPremium;
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
		float areaTotal = sm.getStackArea() - areaFrom + areaTo;
				
		return (from == null || sm.getStack(from).size() > 0) && (heightTo < heightMax && areaTotal < areaMax);
	}
	
	
	/*
	 * =========================================================
	 * RUN
	 * =========================================================
	 */
	
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
	
	public Map<Point3D, Stack<AnalysisFloor>> getAnalysisStacks() {		
		return new HashMap<>(analysis);
	}
	
	public SimpleIntegerProperty getGeneration() {
		return this.generation;
	}
	
	private String swapFloorplateType(String footprintType, String floorplateType) {
		
		List<String> selection = new ArrayList<>();
		List<String> floorplateTypes = new ArrayList<>(sm.getFloorplateTypes(footprintType));
		
		loop:
		for (String type : floorplateTypes) {
			
			if (type == floorplateType) {
				continue loop;
			}
			
			for (String unitType : sm.getUnitTypes(footprintType, type)) {
				
				if (sm.unitCounts.get(unitType) - sm.getUnitCount(unitType) <= 0) {
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
		
			boolean swapped = true;
			
			int max = 0; // TODO - temporary fix
			
			while (swapped && max++ < 4) {
				
				swapped = false;
								
				for (int s=0; s<sm.getStack(footprint).size(); s++) {
					
					// TODO - make iterable types a part of stack manager for speed
					
					Set<String> iterableTypes = sm.getIterableFloorplateTypes(
							sm.getFootprintType(footprint),
							sm.getStack(footprint).get(s));
					
					for (int t=s+1; t<sm.getStack(footprint).size(); t++) {				
						if (sm.getStack(footprint).get(s) != sm.getStack(footprint).get(t) && 
							!iterableTypes.contains(sm.getStack(footprint).get(t))) {
							
							AnalysisFloor sAnalysisFloor = analysis.get(footprint).get(s);
							AnalysisFloor tAnalysisFloor = analysis.get(footprint).get(t);
			
							AnalysisFloor sAnalysisFloorClone = sAnalysisFloor.clone();
							AnalysisFloor tAnalysisFloorClone = tAnalysisFloor.clone();
							 						 
							sAnalysisFloorClone.setFloorIndex(t);
							tAnalysisFloorClone.setFloorIndex(s);
							 
							evaluateFloor(sAnalysisFloorClone);
							evaluateFloor(tAnalysisFloorClone);
							
							List<AnalysisUnit> cAnalysisUnits = new ArrayList<>(sAnalysisFloor.getAnalysisUnits());
							cAnalysisUnits.addAll(tAnalysisFloor.getAnalysisUnits());
							
							List<AnalysisUnit> tAnalysisUnits = new ArrayList<>(sAnalysisFloorClone.getAnalysisUnits());
							tAnalysisUnits.addAll(tAnalysisFloorClone.getAnalysisUnits());
							 
							float vc = StackAnalysis.getAnalysisAttribute(cAnalysisUnits, "unitDelta-total").getSum();
							float vt =  StackAnalysis.getAnalysisAttribute(tAnalysisUnits, "unitDelta-total").getSum();
														
							if (vc < vt) {
								 
								sm.swapFloors(footprint, s, t);												
								analysis.get(footprint).set(s, tAnalysisFloorClone);
								analysis.get(footprint).set(t, sAnalysisFloorClone);
								
								swapped = true;
																
								try {
									 Thread.sleep(sleep);
								} catch (InterruptedException e) {
									 e.printStackTrace();
								}	
							}
						}
					}
				}
				
				for (int s=0; s<sm.getStack(footprint).size(); s++) {
					
					Set<String> iterableTypes = sm.getIterableFloorplateTypes(
							sm.getFootprintType(footprint),
							sm.getStack(footprint).get(s));
							
					for (String iterableType : iterableTypes) {
						
						if (iterableType != sm.getStack(footprint).get(s)) {
							
							AnalysisFloor sAnalysisFloor = analysis.get(footprint).get(s);
							AnalysisFloor sAnalysisFloorClone = sAnalysisFloor.clone();
							
							sAnalysisFloorClone.setFloorPlate(sm.getFloorplate(sm.getFootprintType(footprint), iterableType));
							
							evaluateFloor(sAnalysisFloorClone);
							
							float vc = StackAnalysis.getAnalysisAttribute(
									sAnalysisFloor.getAnalysisUnits(), "unitDelta-total").getSum();
							float vt = StackAnalysis.getAnalysisAttribute(
									sAnalysisFloorClone.getAnalysisUnits(), "unitDelta-total").getSum();
							
							if (vc < vt) {
								
								sm.getStack(footprint).set(s, iterableType);							
								analysis.get(footprint).set(s, sAnalysisFloorClone);
												
								swapped = true;
								 
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
	}
			
	private void mutateStackFloors() {
		
		if (sm.getPointers().isEmpty()) {
			return;
		}
			
		for (int i = new Random().nextInt(sm.getPointers().size()); i>=0;) {
						
			int[] pointer  = sm.getPointers().get(new Random().nextInt(sm.getPointers().size()));
						
			Point3D footprint = sm.getFootprints().get(pointer[0]);
			
			if (sm.getStack(footprint).get(pointer[1]) != null || this.generation.get() == 0) {
				
				float option = new Random().nextFloat();
								
				if (option < 0.3 && isPushFloorValid(null, footprint)) {
					sm.pushFloor(footprint, null);
				} else if (option < 0.6) {
					sm.getStack(footprint).set(pointer[1], null);
				} else {
					sm.removeFloor(footprint, pointer[1]);
				}
				
				i--;
			}
		}
		
		List<int[]> pointers = sm.getPointers();
		Collections.shuffle(pointers);
				
		for (int[] pointer : pointers) {
			
			Point3D footprint = sm.getFootprints().get(pointer[0]);
			
			if (sm.getStack(footprint).get(pointer[1]) == null) {
				sm.getStack(footprint).set(pointer[1], 
						swapFloorplateType(sm.getFootprintType(footprint), null));
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
		
		String hashString = StackAnalysis.hashAnalysisStack(this.analysis);
		
		if (stackHashes.contains(hashString)) { // TODO - this slows down the simulation sometimes
			mutateStackFloors();			
		} else {
			stackHashes.add(hashString);
		}
	}
				
	public void clearEvaluations() {
		this.stackHashes = new HashSet<>();
		this.floorHashes = new HashMap<>();
		this.counts = new ConcurrentHashMap<>();
		this.value = new SimpleFloatProperty(-Float.MAX_VALUE);
		this.analysis = new ConcurrentHashMap<>();
	}
	
	public void initialize(Map<Point3D, Stack<String>> state) {
		
		System.out.println("init : " + state);
		
		if (state == null) {
			
			boolean valid = true;
			
			while (valid) {
				
				valid = false;
				
				for (Point3D footprint : sm.getFootprints()) {
					
					String floorplateType = swapFloorplateType(sm.getFootprintType(footprint), null);
					
					if (floorplateType != null && isPushFloorValid(null, footprint)) {
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
			
		} else {
			
			sm.restoreState(state);
		}
		
		setAnalysisStacks();		
		
		this.value.set(evaluateDelta(this.analysis));
		
		for (String unitType : sm.getUnitTypes()) {
			this.counts.put(unitType, sm.getUnitCount(unitType));
		}
		
		print();
	}
	
	public void evaluate(int generation) {
				
		if (generation != 0) {
			
			this.value.set(evaluateDelta(this.analysis));
			
			for (String unitType : sm.getUnitTypes()) {
				this.counts.put(unitType, sm.getUnitCount(unitType));
			}	
		} 
		
		this.generation.set(generation);
	}
	
	public void stop() {
		
		this.evaluate.set(false);
	}
	
	public void start() {
		
		this.evaluate.set(true);
		
		SortedMap<Float, Map<Point3D, Stack<String>>> elite = new TreeMap<>();
		
		int pool = 100;
		int generation = 0;
				
		elite.put(-Float.MAX_VALUE, sm.saveState());
		
		loop : do {
						
			SortedMap<Float, Map<Point3D, Stack<String>>> front = new TreeMap<>(elite);
									
			elite.clear();
			
 			System.out.println(" ---> new front : " + front.size());	
 			 			
			for (Map.Entry<Float, Map<Point3D, Stack<String>>> f : front.entrySet()) {
								
				sm.restoreState(f.getValue());
				
				setAnalysisStacks();
										
				evaluate(generation);
				
				boolean flag = false;
				
				for (int p = (int) pool / front.size(); p > 0 ; p--) {
										
					sm.saveStacks();
					
					if (evaluate.get() == false) {	
						
						elite.putAll(front);
						
						break loop;
					}
										
					mutateStackFloors();							
											
					float delta = evaluateDelta(this.analysis);
					
					if (!evaluateValidity(this.analysis)) {
						delta = -Float.MAX_VALUE;
					}
					
					if (delta > f.getKey() || generation == 0) {					
						elite.put(delta, sm.saveState());	
						flag = true;
					}				
										
					sm.restoreStacks();				
				}
												
				if (!flag) {
					
					elite.put(f.getKey(), f.getValue());
				}
				
				generation++;
			}
			
			if (elite.keySet().size() == 0) {	
				
				elite = front;	
				
			} else { 
				
				float mu = 0;
				
				List<Float> values = new ArrayList<>(elite.keySet());
				
				for (Float value : values) {
					mu += value / elite.size();
				}
				
				for (Float value : values) {
					if (value < mu) elite.remove(value);
				}
			}		
			
		} while(evaluate.get() == true);
		
		sm.restoreState(elite.get(elite.lastKey()));
		
		setAnalysisStacks();
		
		evaluate(++generation);
										
		print();
	}
														
	private void print() {
		
		System.out.println();
		System.out.println();
		System.out.println("total area: " +sm.getStackArea() + " / " + sm.maxArea);
		System.out.println("total delta: " + evaluateDelta(this.analysis));
		System.out.println();
		for (String unitType : sm.getUnitTypes()) {
			System.out.println(unitType + " : " + sm.getUnitCount(unitType) + " / " + sm.unitCounts.get(unitType));
		}
		System.out.println();
		for (Point3D footprint : sm.getFootprints()) {
			System.out.println(sm.getFootprintType(footprint) + " height : " + sm.getStack(footprint).size() * sm.floorToCeilingHeight);
		}
		System.out.println();
	}
}
