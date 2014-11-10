/**
 ******************* ISSUE LIST **************************
 *
 * 1. Rename to DynamicVariables
 * 2. We can/should remove all setters from VariableImplementation right?
 * 3. Is there any need for the field atts? It is only used in the constructor.
 * 4. If the fields in VariableImplementation are all objects then the TemporalClone only contains
 *    pointers, which would ensure consistency, although we are not planing to modify these values.
 *
 * ********************************************************
 */



package eu.amidst.core.header.dynamics;

import eu.amidst.core.database.statics.readers.Attribute;
import eu.amidst.core.database.statics.readers.Attributes;
import eu.amidst.core.database.statics.readers.DistType;
import eu.amidst.core.database.statics.readers.StateSpaceType;
import eu.amidst.core.header.Variable;
import eu.amidst.core.header.VariableBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by afa on 02/07/14.
 */
public class DynamicModelHeader {
    private Attributes atts;
    private List<Variable> allVariables;
    private List<Variable> temporalClones;

    public DynamicModelHeader(Attributes atts) {
        this.atts = atts;
        this.allVariables = new ArrayList<>();
        this.temporalClones = new ArrayList<>();

        for (Attribute att : atts.getSet()) {
            VariableBuilder builder = new VariableBuilder();

            VariableBuilder.setName(att.getName());
            VariableBuilder.setIsObservable();

            VariableBuilder.setStateSpaceType(att.getStateSpaceType());
            switch (att.getStateSpaceType()) {
                case REAL:
                    VariableBuilder.setDistributionType(DistType.GAUSSIAN);
                    break;
                case INTEGER:
                    VariableBuilder.setDistributionType(DistType.GAUSSIAN);
                    break;
                default:
                    throw new IllegalArgumentException(" The string \"" + att.getStateSpaceType() + "\" does not map to any Type.");
            }

            VariableBuilder.setNumberOfStates(att.getNumberOfStates());

            VariableImplementation var = new VariableImplementation(builder, att.getIndex());
            allVariables.add(var.getVarID(), var);


            VariableImplementation temporalClone = new VariableImplementation(var);
            temporalClones.add(var.getVarID(), temporalClone);
        }
    }

    public Variable getTemporalCloneFromVariable(Variable var){
        return temporalClones.get(var.getVarID());
    }

    public Variable getVariableFromTemporalClone(Variable var){
        return allVariables.get(var.getVarID());
    }


    public Variable addHiddenVariable(VariableBuilder builder) {

        VariableImplementation var = new VariableImplementation(builder, allVariables.size());
        allVariables.add(var);

        VariableImplementation temporalClone = new VariableImplementation(var);
        temporalClones.add(var.getVarID(),temporalClone);

        return var;
    }

    public List<Variable> getVariables() {
        return this.allVariables;
    }

    public List<Variable> getTemporalClones() {
        return this.temporalClones;
    }

    public Variable getVariableById(int varID) {
        return this.allVariables.get(varID);
    }

    public Variable getTemporalCloneById(int varID) {
        return this.temporalClones.get(varID);
    }

    public int getNumberOfVars() {
        return this.allVariables.size();
    }

    private class VariableImplementation implements Variable {
        private String name;
        private int varID;
        private boolean observable;
        private int numberOfStates;
        private StateSpaceType stateSpaceType;
        private DistType distributionType;
        private final boolean isTemporalClone;

        /*
         * Constructor for a Variable (not a temporal clone)
         */
        public VariableImplementation(VariableBuilder builder, int varID) {
            this.name = builder.getName();
            this.varID = varID;
            this.observable = builder.isObservable();
            this.numberOfStates = builder.getNumberOfStates();
            this.stateSpaceType = builder.getStateSpaceType();
            this.distributionType = builder.getDistributionType();
            this.isTemporalClone = false;
        }

        /*
         * Constructor for a Temporal clone (based on a variable)
         */
        public VariableImplementation(Variable variable) {
            this.name = variable.getName();
            this.varID = variable.getVarID();
            this.observable = variable.isObservable();
            this.numberOfStates = variable.getNumberOfStates();
            this.stateSpaceType = variable.getStateSpaceType();
            this.distributionType = variable.getDistributionType();
            this.isTemporalClone = true;
        }

        public String getName() {
            return this.name;
        }

        public int getVarID() {
            return varID;
        }

        private void setVarID(int id) {
            this.varID = id;
        }

        public boolean isObservable() {
            return observable;
        }

        public int getNumberOfStates() {
            return numberOfStates;
        }

        @Override
        public StateSpaceType getStateSpaceType() {
            return stateSpaceType;
        }

        public DistType getDistributionType() {
            return distributionType;
        }

        public boolean isTemporalClone(){
            return isTemporalClone;
        }


    }
}
