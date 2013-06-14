/*
 * BeagleSequenceSimulator.java
 *
 * Copyright (c) 2002-2013 Alexei Drummond, Andrew Rambaut and Marc Suchard
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

package dr.app.bss.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.math.random.MersenneTwister;

import dr.evolution.alignment.Alignment;
import dr.evolution.alignment.SimpleAlignment;
import dr.evolution.datatype.Codons;
import dr.evolution.datatype.DataType;
import dr.evolution.sequence.Sequence;
import dr.evolution.util.Taxon;
import dr.math.MathUtils;

/**
 * @author Filip Bielejec
 * @version $Id$
 * 
 */
public class BeagleSequenceSimulator2 {

	// Constructor fields
	private ArrayList<Partition2> partitions;
	private ConcurrentHashMap<Taxon, int[]> alignmentMap;
	private int siteCount;

	// Alignment fields
	private final int gapFlag = Integer.MAX_VALUE;
	private SimpleAlignment alignment;

	// Random number generation
	private List<MersenneTwister> randomList = new ArrayList<MersenneTwister>();

	public BeagleSequenceSimulator2(ArrayList<Partition2> partitions) {

		this.partitions = partitions;
		this.alignmentMap = new ConcurrentHashMap<Taxon, int[]>();
		this.alignment = new SimpleAlignment();
		alignment.setReportCountStatistics(false);

		int siteCount = 0;
		int to = 0;
		for (Partition2 partition : partitions) {

			to = partition.to;
			if (to > siteCount) {
				siteCount = to;
			}
		}

		this.siteCount = siteCount + 1;
	}// END: Constructor

	private MersenneTwister getNextMersenneTwister(int paritionNumber) {
		return new MersenneTwister(MathUtils.nextLong());
	}

	public Alignment simulate(boolean parallel) {

		try {

			// Executor for threads
			int NTHREDS = 1;
			if (parallel) {
				NTHREDS = Math.min(partitions.size(), Runtime.getRuntime()
						.availableProcessors());
			}

			for (int i = 0; i < partitions.size(); ++i) {
				randomList.add(getNextMersenneTwister(i));
			}

			ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);
			ArrayList<Callable<Void>> simulatePartitionCallers = new ArrayList<Callable<Void>>();

			int partitionCount = 0;
			for (Partition2 partition : partitions) {

				partition.setPartitionNumber(partitionCount);

				simulatePartitionCallers.add(new simulatePartitionCallable(
						partition));
				partitionCount++;

			}// END: partitions loop

			executor.invokeAll(simulatePartitionCallers);

			// Wait until all threads are finished
			executor.shutdown();
			while (!executor.isTerminated()) {
			}

		} catch (Exception e) {
			e.printStackTrace();
		}// END: try-catch block

		alignment = compileAlignment();

		return alignment;
	}// END: simulate

	private class simulatePartitionCallable implements Callable<Void> {

		private Partition2 partition;

		private simulatePartitionCallable(Partition2 partition) {
			this.partition = partition;
		}// END: Constructor

		public Void call() {

			try {

				partition.simulatePartition();

			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}// END: call

	}// END: simulatePartitionCallable class

	private SimpleAlignment compileAlignment() {

		SimpleAlignment simpleAlignment = new SimpleAlignment();
		simpleAlignment.setReportCountStatistics(false);

		// compile the alignment
		for (Partition2 partition : partitions) {

			Map<Taxon, int[]> sequenceMap = partition.getSequencesMap();
			Iterator<Entry<Taxon, int[]>> iterator = sequenceMap.entrySet()
					.iterator();

			while (iterator.hasNext()) {

				Entry<Taxon, int[]> pairs = (Entry<Taxon, int[]>) iterator
						.next();

				Taxon taxon = pairs.getKey();
				int[] partitionSequence = pairs.getValue();

				if (alignmentMap.containsKey(taxon)) {

					int j = 0;
					for (int i = partition.from; i <= partition.to; i += partition.every) {

						alignmentMap.get(taxon)[i] = partitionSequence[j];
						j++;

					}// END: i loop

				} else {

					int[] sequence = new int[siteCount];
					// dirty solution for gaps when taxa between the tree
					// topologies don't match
					Arrays.fill(sequence, gapFlag);

					int j = 0;
					for (int i = partition.from; i <= partition.to; i += partition.every) {

						sequence[i] = partitionSequence[j];
						j++;

					}// END: i loop

					alignmentMap.put(taxon, sequence);
				}// END: key check

			}

		}

		// TODO: fix data type
		DataType datatype = partitions.get(0).getDataType();
		Iterator<Entry<Taxon, int[]>> iterator = alignmentMap.entrySet()
				.iterator();
		while (iterator.hasNext()) {

			Entry<Taxon, int[]> pairs = (Entry<Taxon, int[]>) iterator.next();
			simpleAlignment.addSequence(intArray2Sequence(
					(Taxon) pairs.getKey(), (int[]) pairs.getValue(), gapFlag,
					datatype));

			iterator.remove();

		}// END: while has next

		return simpleAlignment;
	}// END: compileAlignment

	private Sequence intArray2Sequence(Taxon taxon, int[] seq, int gapFlag,
			DataType dataType) {

		StringBuilder sSeq = new StringBuilder();

		if (dataType instanceof Codons) {

			for (int i = 0; i < siteCount; i++) {

				int state = seq[i];

				if (state == gapFlag) {
					sSeq.append(dataType.getTriplet(dataType.getGapState()));
				} else {
					sSeq.append(dataType.getTriplet(seq[i]));
				}// END: gap check

			}// END: replications loop

		} else {

			for (int i = 0; i < siteCount; i++) {

				int state = seq[i];

				if (state == gapFlag) {
					sSeq.append(dataType.getCode(dataType.getGapState()));
				} else {
					sSeq.append(dataType.getCode(seq[i]));
				}// END: gap check

			}// END: replications loop

		}// END: dataType check

		return new Sequence(taxon, sSeq.toString());
	}// END: intArray2Sequence

	// private int[] sequence2intArray(Sequence sequence) {
	//
	// int array[] = new int[siteCount];
	//
	// if (dataType instanceof Codons) {
	//
	// int k = 0;
	// for (int i = 0; i < siteCount; i++) {
	// array[i] = ((Codons) dataType).getState(sequence.getChar(k),
	// sequence.getChar(k + 1), sequence.getChar(k + 2));
	// k += 3;
	// }// END: replications loop
	//
	// } else {
	//
	// for (int i = 0; i < siteCount; i++) {
	// array[i] = dataType.getState(sequence.getChar(i));
	// }// END: replications loop
	//
	// }// END: dataType check
	//
	// return array;
	// }// END: sequence2intArray

} // END: class