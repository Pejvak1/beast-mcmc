package dr.app.beauti.options;

import java.util.ArrayList;
import java.util.List;

import dr.app.beauti.priorsPanel.PriorType;
import dr.evomodel.tree.RateStatistic;

/**
 * @author Andrew Rambaut
 * @author Walter Xie
 * @version $Id$
 */
public class PartitionClockModelTreeModelLink extends ModelOptions {
	
	// Instance variables

    private final BeautiOptions options;
    private final PartitionClockModel model;
    private final PartitionTreeModel tree; 
    
    
	public PartitionClockModelTreeModelLink(BeautiOptions options, PartitionClockModel model, PartitionTreeModel tree) {
		this.options = options;
		this.model = model;
		this.tree = tree;
				
		initClockModelParaAndOpers();
    }

    

    public void initClockModelParaAndOpers() {
    	double rateWeights = 3.0; 
          	
        createOperator("upDownRateHeights", "Substitution rate and heights",
                "Scales substitution rates inversely to node heights of the tree", model.getParameter("clock.rate"),
                tree.getParameter("treeModel.allInternalNodeHeights"), OperatorType.UP_DOWN, 0.75, rateWeights);
        createOperator("upDownUCEDMeanHeights", "UCED mean and heights",
                "Scales UCED mean inversely to node heights of the tree", model.getParameter(ClockType.UCED_MEAN),
                tree.getParameter("treeModel.allInternalNodeHeights"), OperatorType.UP_DOWN, 0.75, rateWeights);
        createOperator("upDownUCLDMeanHeights", "UCLD mean and heights",
                "Scales UCLD mean inversely to node heights of the tree", model.getParameter(ClockType.UCLD_MEAN),
                tree.getParameter("treeModel.allInternalNodeHeights"), OperatorType.UP_DOWN, 0.75, rateWeights);
        
        // #meanRate = #Relaxed Clock Model * #Tree Model
        createStatistic("meanRate", "The mean rate of evolution over the whole tree", 0.0, Double.POSITIVE_INFINITY);
        // #covariance = #Relaxed Clock Model * #Tree Model
        createStatistic("covariance", "The covariance in rates of evolution on each lineage with their ancestral lineages",
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            
    }
    
	/**
     * return a list of parameters that are required
     *
     * @param params the parameter list
     */
    public void selectStatistics(List<Parameter> params) {
        // Statistics
		if (model.getClockType() != ClockType.STRICT_CLOCK) {
			params.add(getParameter("meanRate"));
			params.add(getParameter("covariance"));
		}
	}

    /**
     * return a list of operators that are required
     *
     * @param ops the operator list
     */
    public void selectOperators(List<Operator> ops) {
    	if (options.hasData()) {

            if (!options.isFixedSubstitutionRate()) {
                switch (model.getClockType()) {
                    case STRICT_CLOCK:
                        ops.add(getOperator("upDownRateHeights"));
                        break;

                    case UNCORRELATED_EXPONENTIAL:
                        ops.add(getOperator("upDownUCEDMeanHeights"));                        
                        break;

                    case UNCORRELATED_LOGNORMAL:
                        ops.add(getOperator("upDownUCLDMeanHeights"));                        
                        break;

                    case AUTOCORRELATED_LOGNORMAL:                    	
                        break;

                    case RANDOM_LOCAL_CLOCK:
                        ops.add(getOperator("upDownRateHeights"));                        
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown clock model");
                }
            } 
        }
    }
    
    /////////////////////////////////////////////////////////////
 
    public Parameter getParameter(String name) {

    	Parameter parameter = parameters.get(name);

        if (parameter == null) {
            throw new IllegalArgumentException("Parameter with name, " + name + ", is unknown");
        }

        parameter.setPrefix(getPrefix());

        return parameter;
    }

    public Operator getOperator(String name) {

        Operator operator = operators.get(name);

        if (operator == null) throw new IllegalArgumentException("Operator with name, " + name + ", is unknown");

        operator.setPrefix(getPrefix());

        return operator;
    }

    public String getPrefix() {        
        return model.getPrefix() + tree.getPrefix();
    }

}
