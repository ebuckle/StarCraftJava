package atlantis.workers;

import atlantis.AGame;
import atlantis.buildings.managers.AGasManager;
import atlantis.units.AUnit;
import atlantis.units.Select;
import atlantis.units.Units;
import atlantis.units.actions.UnitActions;
import atlantis.util.CodeProfiler;
import atlantis.util.PositionUtil;
import java.util.Collection;

/**
 * Manages all worker (SCV, Probe, Drone) actions.
 */
public class AWorkerCommander {

    /**
     * Executed only once per frame.
     */
    public static void update() {
        CodeProfiler.startMeasuring(CodeProfiler.ASPECT_WORKERS);

        // === Handle assigning workers to gas / bases ============================
        
        AGasManager.handleGasBuildings();

        // === Act individually with every worker =================================

        for (AUnit worker : Select.ourWorkers().listUnits()) {
            AWorkerManager.update(worker);
        }
        
        // =========================================================
        
        // Updated logic in unused function to better support base expansion for Timmy and Johnny.
        // transferWorkersBetweenBasesIfNeeded();
        
        CodeProfiler.endMeasuring(CodeProfiler.ASPECT_WORKERS);
    }

    // =========================================================
    
    /**
     * Every base should have similar number of workers, more or less.
     */
    private static void transferWorkersBetweenBasesIfNeeded() {

        // Don't run every frame
    	// This function rarely needs to be checked, as it is affecting a slow, macro level decision.
    	// Running every 300 ticks should be sufficient.
        if (AGame.getTimeFrames() % 300 == 0) {
            return;
        }

        // Select all of the bases we currently have.
        Collection<AUnit> ourBases = Select.ourBases().listUnits();
        if (ourBases.size() <= 1) {
            return;
        }

        // Count ratios of workers / minerals for every base
        Units baseWorkersRatios = new Units();
        
        for (AUnit base : ourBases) {
        	// Mineral patches can only sustain a certain number of workers, so calculating the ratio of workers at different bases
        	// means we can balance out worker production.
            int numOfWorkersNearBase = AWorkerManager.getHowManyWorkersGatheringAt(base);
            int numOfMineralsNearBase = Select.minerals().inRadius(10, base).count() + 1;
            double workersToMineralsRatio = (double) numOfWorkersNearBase / numOfMineralsNearBase;
            baseWorkersRatios.setValueFor(base, workersToMineralsRatio);
        }

        // Take the base with lowest and highest worker ratio
        AUnit baseWithFewestWorkers = baseWorkersRatios.getUnitWithLowestValue();
        AUnit baseWithMostWorkers = baseWorkersRatios.getUnitWithHighestValue();

        if (baseWithFewestWorkers == null || baseWithMostWorkers == null) {
            return;
        }

        double fewestWorkers = baseWorkersRatios.getValueFor(baseWithFewestWorkers);
        double mostWorkers = baseWorkersRatios.getValueFor(baseWithMostWorkers);
        
        // The ideal ratio for workers is 2. As at most 2 workers can mine one mineral patch at the same time.
        // Anything over this is wasteful.
        
        if ((mostWorkers >= 2 && Math.abs(mostWorkers - fewestWorkers) < 0.17) || PositionUtil.distanceTo(baseWithMostWorkers, baseWithFewestWorkers) < 6) {
            performWorkerTransfer(baseWithMostWorkers, baseWithFewestWorkers);
            return;
        }

        // If there's only 117% as many workers as minerals OR bases are too close, don't transfer
        if (Math.abs(mostWorkers - fewestWorkers) < 0.17
                || PositionUtil.distanceTo(baseWithMostWorkers, baseWithFewestWorkers) < 6) {
            return;
        }
        
        performWorkerTransfer(baseWithMostWorkers, baseWithFewestWorkers);
    }
    
    private static void performWorkerTransfer(AUnit baseWithMostWorkers, AUnit baseWithFewestWorkers) {
        AUnit worker = (AUnit) Select.ourWorkersThatGatherMinerals(true)
                .inRadius(12, baseWithMostWorkers)
                .nearestTo(baseWithFewestWorkers);
        if (worker != null) {
            worker.move(baseWithFewestWorkers.getPosition(), UnitActions.MOVE);
        }
    }

}
