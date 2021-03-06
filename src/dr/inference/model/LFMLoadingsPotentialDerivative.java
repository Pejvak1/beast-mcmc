package dr.inference.model;

/**
 * @author Max Tolkoff
 */
public class LFMLoadingsPotentialDerivative implements PotentialDerivativeInterface {
    LatentFactorModel lfm;

    public LFMLoadingsPotentialDerivative(LatentFactorModel lfm){
        this.lfm = lfm;
    }


    @Override
    public double[] getDerivative() {
        double[] derivative = new double[lfm.getLoadings().getDimension()];
        Parameter missingIndicator = lfm.getMissingIndicator();
        int ntaxa = lfm.getFactors().getColumnDimension();
        int ntraits = lfm.getLoadings().getRowDimension();
        int nfac = lfm.getLoadings().getColumnDimension();
        double[] residual = lfm.getResidual();

        for (int i = 0; i < nfac; i++) {
            for (int j = 0; j < ntraits; j++) {
                for (int k = 0; k < ntaxa; k++) {
                    if(missingIndicator == null || missingIndicator.getParameterValue(k * ntraits + j) != 1){
                        derivative[i * ntraits + j] -= lfm.getFactors().getParameterValue(i, k) * lfm.getColumnPrecision().getParameterValue(j, j) *
                                residual[k * ntraits + j];
                    }
                }
            }
        }

        return derivative;
    }
}
