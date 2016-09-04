import ilog.concert.*;
import ilog.cplex.*;

public class TryCPLEX {
	//IloCplex.Algorithm.Dual
	/*Indices:*/
	//private static final int employee_i = 5;			//5 employees
	//private static final int shift_j    = 2;			//2 shifts; shift_j = 0 -> morning shift; shift_j = 1 -> night shift;
	//private static final int day_k      = 2;			//2 days; day_k = 0 -> Monday; day_k = 1 -> Tuesday;

	public static void main(String[] args){
		try{
			
			
			IloCplex         cplex = new IloCplex();
			IloNumVar[][]    var   = new IloNumVar[1][];
			IloRange[][]     rng   = new IloRange[1][];
			
			populateByIDK(cplex, var, rng);				//To find feasible solution.
			
			cplex.exportModel("TryCplex.lp");			//Export a file.
			
			if(cplex.solve()){							//If it could be found the solution(s), then print the output.
				
				double[]     Y = cplex.getValues(var[0]);
				double[] slack = cplex.getSlacks(rng[0]);
				
				double[]    dj = cplex.getReducedCosts(var[0]);
	            double[]    pi = cplex.getDuals(rng[0]);
				
				cplex.output().println("Solution Status = " + cplex.getStatus());
				cplex.output().println("Solution Values = " + cplex.getObjValue());
				cplex.output().println("CPLEX Status    = " + cplex.getCplexStatus());
				
				int nvar = Y.length;
				for (int j = 0; j < nvar; ++j ){
					cplex.output().println("Variable " + j + 
							               ": Value = " + Y[j] +
							               "Reduced Cost = " + dj[j]);
				}
				
				int cons = slack.length;
				for (int i = 0; i < cons; ++i){
					cplex.output().println("Constraint " + i + 
							               ": Slack = " + slack[i] +
							               "Pi = " + pi[i]);
				}
			}  
			cplex.end();
			
		}catch (IloException e){
			System.err.println("Concert Exception '" + e + "' caught.");
		}
		
	}
	
	static void populateByIDK(IloModeler model, IloNumVar[][] var, IloRange[][] rng) throws IloException{

		/*Decision Variables & Coefficients: */
		String[] varName = {"Y111", "Y121", "Y112", "Y122"
						   ,"Y211", "Y221", "Y212", "Y222"
				           ,"Y311", "Y321", "Y312", "Y322"
				           ,"Y411", "Y421", "Y412", "Y422"
				           ,"Y511", "Y521", "Y512", "Y522"};
		double[] objVals = {  10.0,   -1.0,    8.0,  -1.0
				            , 16.0,   18.0,   20.0,  14.0
				            , -3.0,   30.0,   -3.0,  -3.0
				            , 40.0,   -4.0,   -4.0,  40.0
				            , -5.0,   45.0,   10.0,  -5.0}; //Calculated by "Seniority", "availability", "preference".
															//Recall: from the online paper.
		
		String[] varName2 = {"X11", "X12"
							,"X21", "X22"
							,"X31", "X32"
							,"X41", "X42"
							,"X51", "X52"};
		
		
		double lowerBound = 0.0;							//Since for all Yijk are whether 1 or 0.
		double upperBound = 1.0;							//Recall: from the online paper.
	
		IloNumVar[] x = model.numVarArray(20, lowerBound, upperBound, varName);
		var[0] = x;										//Insert the solution into array_var's 1st-row and 1st-column
		
		IloNumVar[] z = model.numVarArray(varName2.length, lowerBound, upperBound, varName2);
		
		/*Generate the Objective Function*/
		model.addMaximize(model.scalProd(x, objVals));
		
		/*										Constraints											*/
		rng[0] = new IloRange[39];
		
		
		//Constraint 1-1
		IloLinearNumExpr expr1 = model.linearNumExpr();
		for(int i = 0; i < x.length; i += 4){
			expr1.addTerm(1.0, x[i]);
		}
		rng[0][0] = model.addEq(expr1, 2);
		//Constraint 1-2
		IloLinearNumExpr expr2 = model.linearNumExpr();
		for(int i = 1; i < x.length; i += 4){
			expr2.addTerm(1.0, x[i]);
		}
		rng[0][1] = model.addEq(expr2, 1);
		//Constraint 1-3
		IloLinearNumExpr expr3 = model.linearNumExpr();
		for(int i = 2; i < x.length; i += 4){
			expr3.addTerm(1.0, x[i]);
		}
		rng[0][2] = model.addEq(expr3, 1);
		//Constraint 1-4
		IloLinearNumExpr expr4 = model.linearNumExpr();
		for(int i = 3; i < x.length; i += 4){
			expr4.addTerm(1.0, x[i]);
		}
		rng[0][3] = model.addEq(expr4, 2);
		
		//Constraint 2
		for(int i = 0; i < x.length; i += 2){
			rng[0][4 + (i/2)] = model.addRange(0, model.sum(model.prod(7.0, x[i]), model.prod(8.0, x[i+1])), 10);
		}
		
		//Constraint 3
		for(int i = 0; i < x.length; i += 4){
			rng[0][15 + (i/4)] = model.addRange(10, model.sum(model.prod(7.0, x[i]), model.prod(8.0, x[i+1]), 
					           model.prod(7.0, x[i+2]), model.prod(8.0, x[i+3]))
					      ,25);
		}
		
		//Constraint 4
	/*	for(int i = 0; i < x.length; i += 4){
			rng[0][20 + ((5*i)/4)	] = (IloRange) model.addLe(x[i],   z[i/2]);
			rng[0][20 + ((5*i)/4)+1 ] = (IloRange) model.addLe(x[i+1], z[i/2]);
			rng[0][20 + ((5*i)/4)+2 ] = (IloRange) model.addLe(x[i+2], z[(i/2)+1]);
			rng[0][20 + ((5*i)/4)+3 ] = (IloRange) model.addLe(x[i+3], z[(i/2)+1]);
		}*/
		//System.out.println("/////////MODEL/////////");
		System.out.println(model);
		
		
	}
}
