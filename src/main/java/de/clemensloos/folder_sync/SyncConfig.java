package de.clemensloos.folder_sync;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class SyncConfig {

	private boolean compareSize = false;
	private boolean compareChecksum = false;

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

	public List<Target> getTargetList() {
		return targetList;
	}

	public void setTargetList(List<Target> targetList) {
		this.targetList = targetList;
	}

	public void addTarget(Target target) {
		this.targetList.add(target);
	}

	public void setFilter(String[] filenameFilterString, String[] pathFilterString) {
		this.filenameFilter = new MyFilenameFilter(filenameFilterString, pathFilterString);
	}
	
	public FilenameFilter getFilenameFilter() {
		return this.filenameFilter;
	}

	private class MyFilenameFilter implements FilenameFilter {

		private String[] filenameFilterString;
		private String[] pathFilterString;
		
		public MyFilenameFilter(String[] filenameFilterString, String[] pathFilterString) {
			this.filenameFilterString = filenameFilterString;
			this.pathFilterString = pathFilterString;
		}
		
		@Override
		public boolean accept(File dir, String name) {
			for (String s : filenameFilterString) {
				if (name.equals(s)) {
					return false;
				}
			}
			for (String s : pathFilterString) {
				if ((dir.getAbsolutePath() + Sync.SLASH + name).contains(s)) {
					return false;
				}
			}
			return true;
		}

	}

}
