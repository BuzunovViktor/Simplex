import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Main {
    public static void main(String[] args) {
        Model model = new Model("Planning problem");

        P p1 = new P("P1",1,3,5);
        P p2 = new P("P2",2,8,10);
        P p3 = new P("P3",3,4,4);
        P p4 = new P("P4",1,2,3);
        P p5 = new P("P5",4,4,8);
        P p6 = new P("P6",1,1,1);

        p1.day = 55;
        p4.day = 70;
        p5.day = 85;
        p6.day = 100;

        p1.dependent.add(p2);
        p2.dependent.add(p3);
        p2.dependencies.add(p1);
        p3.dependent.addAll(List.of(p4,p5));
        p3.dependencies.add(p2);
        p4.dependent.add(p6);
        p4.dependencies.add(p3);
        p5.dependent.add(p6);
        p5.dependencies.add(p3);
        p6.dependencies.addAll(List.of(p4,p5));

        List<P> processes = new ArrayList<>(List.of(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6
        ));

        int lowerBound = 0;
        int upperBound = 1000;
        Map<String, P> resultMap = new HashMap<>();
        Map<P,IntVar> pTimeMap = new HashMap<>();
        for (P p: processes) {
            IntVar pTime = model.intVar(p.title, p.day, upperBound);
            pTimeMap.put(p, pTime);
            resultMap.put(p.title, p);
        }

        IntVar goal = null;
        for (P p: processes) {
            for (P dp: p.dependent) {
                String dStdPTitle = p.title + "-" + dp.title + "_dStdP";
                String dStdMTitle = p.title + "-" + dp.title + "_dStdM";
                String dMaxTitle = p.title + "-" + dp.title + "_dMax";
                String dMinTitle = p.title + "-" + dp.title + "_dMin";

                IntVar dStdP = model.intVar(dStdPTitle, lowerBound, upperBound);
                IntVar dStdM = model.intVar(dStdMTitle, lowerBound, upperBound);
                IntVar dMax = model.intVar(dMaxTitle, lowerBound, upperBound);
                IntVar dMin = model.intVar(dMinTitle, lowerBound, upperBound);

                model.arithm(model.intMinusView(pTimeMap.get(p)).add(pTimeMap.get(dp))
                        .add(dStdM).sub(dStdP).intVar(), "=", p.std).post();
                model.arithm(model.intMinusView(pTimeMap.get(p))
                        .add(pTimeMap.get(dp)).sub(dMax).intVar(),"<=", p.max).post();
                model.arithm(model.intMinusView(pTimeMap.get(p))
                        .add(pTimeMap.get(dp)).add(dMin).intVar(),">=", p.min).post();
                model.arithm(dStdM,"<=", p.std).post();

                if (goal == null) {
                    goal = model.intVar(0);
                }
                goal = goal.add(dStdP).add(dStdM).add(dMax).add(dMin).intVar();
            }
            if (p.day != 0) {
                model.arithm(pTimeMap.get(p), "=", p.day).post();
            }
        }
        Objects.requireNonNull(goal, "Goal must not be null!");
        Solver solver = model.getSolver();
        solver.showShortStatistics();
        Solution solution = solver.findOptimalSolution(goal,false);
        if (solution != null) {
            System.out.println(solution);
            System.out.println("Congratulations! Solution has been found!");
            solution.retrieveIntVars(false).forEach(val->{
                P p = resultMap.get(val.getName());
                if (p != null) {
                    p.day = solution.getIntVal(val);
                    System.out.println(p);
                }
            });
        } else {
            System.out.println("Solution not found :(");
        }
    }

    static class P {
        public String title;
        public int min;
        public int std;
        public int max;
        public int day;
        public List<P> dependent = new ArrayList<>();
        public List<P> dependencies = new ArrayList<>();

        public P(String title, int min, int std, int max) {
            this.title = title;
            this.min = min;
            this.std = std;
            this.max = max;
        }

        @Override
        public String toString() {
            return "P{" +
                    "title='" + title + '\'' +
                    ", day=" + day +
                    '}';
        }
    }
}
