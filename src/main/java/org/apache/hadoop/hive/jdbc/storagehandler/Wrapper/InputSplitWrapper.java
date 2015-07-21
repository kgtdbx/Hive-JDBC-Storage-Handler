package org.apache.hadoop.hive.jdbc.storagehandler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.util.ReflectionUtils;

import org.apache.hadoop.fs.Path;

import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class InputSplitWrapper extends FileSplit implements InputSplit {

	org.apache.hadoop.mapreduce.InputSplit realSplit;
	private static final String[] EMPTY_ARRAY = new String[] {};
    private static final Log LOG = LogFactory.getLog(InputSplitWrapper.class);

	@SuppressWarnings("unused")
	// MapReduce instantiates this.
	public InputSplitWrapper() {
		super((Path) null, 0, 0, EMPTY_ARRAY);
	}

	public InputSplitWrapper(org.apache.hadoop.mapreduce.InputSplit realSplit,
			Path aPath) {
		super(aPath, 0, 0, EMPTY_ARRAY);
		this.realSplit = realSplit;
	}

	@Override
	public long getLength() {
		try {
			return realSplit.getLength();
		} catch (Exception e) {
			// throw new IOException(e);
		}
		return 0;
	}

	@Override
	public String[] getLocations() {
		try {
			return realSplit.getLocations();
		} catch (Exception e) {
			// throw new IOException(e);
		}
		return new String[] { "localhost" };
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		String className = WritableUtils.readString(in);
		Class<?> splitClass;

		try {
			splitClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}

		realSplit = (org.apache.hadoop.mapreduce.InputSplit) ReflectionUtils
				.newInstance(splitClass, null);
		((Writable) realSplit).readFields(in);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		WritableUtils.writeString(out, realSplit.getClass().getName());
		((Writable) realSplit).write(out);
	}
}