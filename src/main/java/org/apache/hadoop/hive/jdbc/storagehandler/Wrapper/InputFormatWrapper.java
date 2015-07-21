package org.apache.hadoop.hive.jdbc.storagehandler;

import java.io.IOException;

import java.util.List;

import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.hive.shims.ShimLoader;
import org.apache.hadoop.conf.Configuration;
public class InputFormatWrapper<K, V> implements
		org.apache.hadoop.mapred.InputFormat {

	private static final Log LOG = LogFactory.getLog(InputFormatWrapper.class);

	protected InputFormat<K, V> realInputFormat;

	public InputFormatWrapper() {
		// real inputFormat is initialized based on conf.
	}

	public InputFormatWrapper(InputFormat<K, V> realInputFormat) {
		this.realInputFormat = realInputFormat;

	}

	@Override
	public RecordReader<K, V> getRecordReader(InputSplit split, JobConf job,
			Reporter reporter) throws IOException {		 
		if (this.realInputFormat != null) {
			return new RecordReaderWrapper<K, V>(realInputFormat, split, job,
					reporter);
		} else {
			return null;
		}
	}

	@Override
	public InputSplit[] getSplits(JobConf job, int numSplits)
			throws IOException {
		if (this.realInputFormat != null) {
			try {
				// create a MapContext to pass reporter to record reader (for
				// counters)
				TaskAttemptContext taskContext = ShimLoader.getHadoopShims()
						.newTaskAttemptContext(job, null);

				List<org.apache.hadoop.mapreduce.InputSplit> splits = realInputFormat
						.getSplits(taskContext);

				if (splits == null) {
					return null;
				}

				InputSplit[] resultSplits = new InputSplit[splits.size()];
				int i = 0;
				for (org.apache.hadoop.mapreduce.InputSplit split : splits) {
					if (split.getClass() == org.apache.hadoop.mapreduce.lib.input.FileSplit.class) {
						org.apache.hadoop.mapreduce.lib.input.FileSplit mapreduceFileSplit = ((org.apache.hadoop.mapreduce.lib.input.FileSplit) split);
						resultSplits[i++] = new FileSplit(
								mapreduceFileSplit.getPath(),
								mapreduceFileSplit.getStart(),
								mapreduceFileSplit.getLength(),
								mapreduceFileSplit.getLocations());
					} else {
						final Path[] paths = FileInputFormat.getInputPaths(job);
						resultSplits[i++] = new InputSplitWrapper(split, paths[0]);
					}
				}

				return resultSplits;

			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		} else {
			return null;
		}
	}
}

