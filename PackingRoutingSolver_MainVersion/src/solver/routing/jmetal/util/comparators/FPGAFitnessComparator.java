//  FPGAFitnessComparator.java
//
//  Author:
//       Antonio J. Nebro <antonio@lcc.uma.es>
//       Juan J. Durillo <durillo@lcc.uma.es>
//
//  Copyright (c) 2011 Antonio J. Nebro, Juan J. Durillo
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package solver.routing.jmetal.util.comparators;

import solver.routing.jmetal.core.Solution;

import java.util.Comparator;

/**
 * This class implements a <code>Comparator</code> (a method for comparing
 * <code>Solution</code> objects) based on the rank used in FPGA.
 */
public class FPGAFitnessComparator implements Comparator {
  
 /**
  * Compares two solutions.
  * @param o1 Object representing the first <code>Solution</code>.
  * @param o2 Object representing the second <code>Solution</code>.
  * @return -1, or 0, or 1 if o1 is less than, equal, or greater than o2,
  * respectively.
  */
  public int compare(Object o1, Object o2) {
    Solution solution1, solution2;
    solution1 = (Solution) o1;
    solution2 = (Solution) o2;
    
    if (solution1.getRank()==0 && solution2.getRank()> 0) {
      return -1;      
    } else if (solution1.getRank() > 0 &&  solution2.getRank() == 0) {
      return 1;
    } else {
      if (solution1.getFitness() > solution2.getFitness()) {
        return -1;
      } else if (solution1.getFitness() < solution2.getFitness()) {
        return 1;      
      } else {
        return 0;
      }
    }
  } // compare
} // FPGAFitnessComparator