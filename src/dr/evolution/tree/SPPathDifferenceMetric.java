/*
 * TopologyTracer.java
 *
 * Copyright (c) 2002-2017 Alexei Drummond, Andrew Rambaut and Marc Suchard
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.evolution.tree;

import dr.evolution.io.Importer;
import dr.evolution.io.NewickImporter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Guy Baele
 * Path difference metric according to Steel & Penny (1993)
 */
public class SPPathDifferenceMetric {

    private Tree focalTree;
    private int dim;
    private double[] focalPath;

    public SPPathDifferenceMetric() {

    }

    public SPPathDifferenceMetric(Tree focalTree) {
        this.focalTree = focalTree;
        this.dim = focalTree.getExternalNodeCount() * focalTree.getExternalNodeCount();
        this.focalPath = new double[dim];

        traverse(this.focalTree, this.focalTree.getRoot(), focalPath);
    }

    public double getMetric(Tree tree) {

        checkTreeTaxa(focalTree, tree);

        double[] pathTwo = new double[dim];

        traverse(tree, tree.getRoot(), pathTwo);

        int n = tree.getExternalNodeCount();

        double metric = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int index = (i * n) + j;
                metric += Math.pow(focalPath[index] - pathTwo[index], 2);
            }
        }
        metric = Math.sqrt(metric);

        return metric;
    }

    /**
     * This method bypasses the constructor entirely, computing the metric on the two provided trees
     * and ignoring the internally stored tree.
     * @param tree1 Focal tree that will be used for computing the metric
     * @param tree2 Provided tree that will be compared to the focal tree
     * @return
     */
    public double getMetric(Tree tree1, Tree tree2) {

        checkTreeTaxa(tree1, tree2);

//        int dim = (tree1.getExternalNodeCount() - 2) * (tree1.getExternalNodeCount() - 1) + tree1.getExternalNodeCount();
        int dim = tree1.getExternalNodeCount() * tree1.getExternalNodeCount();

        double[] pathOne = new double[dim];
        double[] pathTwo = new double[dim];

        traverse(tree1, tree1.getRoot(), pathOne);
        traverse(tree2, tree2.getRoot(), pathTwo);

        int n = tree1.getExternalNodeCount();

        double metric = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int index = (i * n) + j;
                metric += Math.pow(pathOne[index] - pathTwo[index], 2);
            }
        }
        metric = Math.sqrt(metric);

        return metric;
    }

    private void checkTreeTaxa(Tree tree1, Tree tree2) {
        //check if taxon lists are in the same order!!
        if (tree1.getExternalNodeCount() != tree2.getExternalNodeCount()) {
            throw new RuntimeException("Different number of taxa in both trees.");
        } else {
            for (int i = 0; i < tree1.getExternalNodeCount(); i++) {
                if (!tree1.getNodeTaxon(tree1.getExternalNode(i)).getId().equals(tree2.getNodeTaxon(tree2.getExternalNode(i)).getId())) {
                    throw new RuntimeException("Mismatch between taxa in both trees: " + tree1.getNodeTaxon(tree1.getExternalNode(i)).getId() + " vs. " + tree2.getNodeTaxon(tree2.getExternalNode(i)).getId());
                }
            }
        }
    }

    private Set<NodeRef> traverse(Tree tree, NodeRef node, double[] lengths) {
        NodeRef left = tree.getChild(node, 0);
        NodeRef right = tree.getChild(node, 1);


        Set<NodeRef> leftSet = null;
        Set<NodeRef> rightSet = null;

        if (!tree.isExternal(left)) {
            leftSet = traverse(tree, left, lengths);
        } else {
            leftSet = Collections.singleton(left);
        }
        if (!tree.isExternal(right)) {
            rightSet = traverse(tree, right, lengths);
        } else {
            rightSet = Collections.singleton(right);
        }

        for (NodeRef tip1 : leftSet) {
            for (NodeRef tip2 : rightSet) {
                int index;
                if (tip1.getNumber() < tip2.getNumber()) {
                    index = (tip1.getNumber() * tree.getExternalNodeCount()) + tip2.getNumber();
                } else {
                    index = (tip2.getNumber() * tree.getExternalNodeCount()) + tip1.getNumber();
                }
                lengths[index] = tree.getNodeHeight(node) * 2
                        - tree.getNodeHeight(tip1)
                        - tree.getNodeHeight(tip2);

            }
        }

        Set<NodeRef> tips = new HashSet<NodeRef>();
        tips.addAll(leftSet);
        tips.addAll(rightSet);

        return tips;
    }

    public static void main(String[] args) {

        try {

            NewickImporter importer = new NewickImporter("(('A':1.2,'B':0.8):0.5,('C':0.8,'D':1.0):1.1)");
            Tree treeOne = importer.importNextTree();
            System.out.println("tree 1: " + treeOne);

            importer = new NewickImporter("((('A':0.8,'B':1.4):0.3,'C':0.7):0.9,'D':1.0)");
            Tree treeTwo = importer.importNextTree();
            System.out.println("tree 2: " + treeTwo + "\n");

            double metric = (new SPPathDifferenceMetric().getMetric(treeOne, treeTwo));

            System.out.println("path difference = " + metric);


            //Additional test for comparing a collection of trees against a (fixed) focal tree
            SPPathDifferenceMetric fixed = new SPPathDifferenceMetric(treeOne);
            metric = fixed.getMetric(treeTwo);

            System.out.println("path difference = " + metric);

        } catch(Importer.ImportException ie) {
            System.err.println(ie);
        } catch(IOException ioe) {
            System.err.println(ioe);
        }

    }

}
