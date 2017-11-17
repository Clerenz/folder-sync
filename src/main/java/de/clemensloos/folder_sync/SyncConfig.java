package de.clemensloos.folder_sync;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * The global SyncConfig contains one or more targets. One instance is usually
 * represented by one config file.
 */
public class SyncConfig {

	private boolean compareSize = false;
	private boolean compareChecksum = false;
	private boolean random = true;
	private int keepHistory = 0;

	private FilenameFilter filenameFilter;

	private List<Target> targetList = new ArrayList<>();

	public boolean isCompareSize() {
		return compareSize;
	}

	public void setCompareSize(boolean compareSize) {
		this.compareSize = compareSize;
	}

	public boolean isCompareChecksum() {
		return compareChecksum;
	}

	public void setCompareChecksum(boolean compareChecksum) {
		this.compareChecksum = compareChecksum;
	}
	
	public boolean isRandom() {
		return random;
	}
	
	public void setRandom(boolean random) {
		this.random = random;
	}
	
	public int getKeepHistory() {
		return keepHistory;
	}

	public void setKeepHistory(int keepHistory) {
		this.keepHistory = keepHistory;
	}
	
	public List<Target> getTargetList() {
		return targetList;
	}

	public void setTargetList(List<Target> targetList) {
		this.targetList = targetList;
	}

	public void addTarget(Target target) {
		this.targetList.add(target);
	}

	public void setFilter(String[] filenameFilterString, String[] pathFilterString, String[] extensionFilterString) {
		this.filenameFilter = new MyFilenameFilter(filenameFilterString, pathFilterString, extensionFilterString);
	}

	public FilenameFilter getFilenameFilter() {
		return this.filenameFilter;
	}

	private class MyFilenameFilter implements FilenameFilter {

		private String[] filenameFilterString;
		private String[] pathFilterString;
		private String[] extensionFilterString;

		public MyFilenameFilter(String[] filenameFilterString, String[] pathFilterString, String[] extensionFilterString) {
			this.filenameFilterString = filenameFilterString;
			this.pathFilterString = pathFilterString;
			this.extensionFilterString = extensionFilterString;
		}

		@Override
		public boolean accept(File dir, String name) {
			for (String s : filenameFilterString) {
				if (!s.isEmpty() && name.equals(s)) {
					return false;
				}
			}
			for (String s : pathFilterString) {
				if (!s.isEmpty() && (dir.getAbsolutePath() + Sync.SLASH + name).contains(s)) {
					return false;
				}
			}
			for (String s : extensionFilterString) {
				if (!s.isEmpty() &&name.contains(".") && name.substring(name.lastIndexOf(".") + 1).equalsIgnoreCase(s)) {
					return false;
				}
			}
			return true;
		}

	}

}
