package de.clemensloos.folder_sync;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Target is the combination of one source and one target folder.
 */
public class Target {

	private String source;
	private String target;

	private String sizeTotal;
	private int filesTotal;

	private AtomicInteger filesAdded = new AtomicInteger(0);
	private AtomicInteger filesDeleted = new AtomicInteger(0);
	private AtomicInteger filesUpdated = new AtomicInteger(0);
	private AtomicInteger filesOkay = new AtomicInteger(0);

	public Target(String source, String target, boolean isMultiSource) throws SyncException {
		if (!source.contains(Sync.SLASH)) {
			throw new SyncException(
					"Bad configuration: source '" + source + "' does not contain separator '" + Sync.SLASH + "'");
		}
		this.source = source;
		if (isMultiSource) {
			this.target = target + source.substring(source.lastIndexOf(Sync.SLASH));
		} else {
			this.target = target;
		}
		verifyTarget();
	}

	public void fileAdded(boolean interactive) {
		filesAdded.incrementAndGet();
		log(interactive);
	}

	public void fileDeleted() {
		filesDeleted.incrementAndGet();
	}

	public void filesDeleted(int numberOfFiles, boolean interactive) {
		filesDeleted.addAndGet(numberOfFiles);
		log(interactive);
	}

	public void fileUpdated(boolean interactive) {
		filesUpdated.incrementAndGet();
		log(interactive);
	}

	public void fileOkay(boolean interactive) {
		filesOkay.incrementAndGet();
		log(interactive);
	}

	private void verifyTarget() throws SyncException {
		if (!getTargetFile().getParentFile().exists()) {
			throw new SyncException("Target folder '" + getTarget() + "' does not exist.");
		}
		if (!getTargetFile().getParentFile().isDirectory()) {
			throw new SyncException("Target '" + getTarget() + "' is not a folder.");
		}
		if (!getTargetFile().getParentFile().canWrite()) {
			throw new SyncException("Target folder '" + getTarget() + "' cannot be written.");
		}
		if (!getSourceFile().exists()) {
			throw new SyncException("Source '" + getSource() + "' does not exist.");
		}
		if (!getSourceFile().canRead()) {
			throw new SyncException("Source '" + getSource() + "' cannot be read.");
		}
	}

	private File getTargetFile() {
		return new File(this.target);
	}

	public String getTarget() {
		return this.target;
	}

	public File getSourceFile() {
		return new File(this.source);
	}

	public String getSource() {
		return this.source;
	}

	public void setSizeTotal(String sizeTotal) {
		this.sizeTotal = sizeTotal;
	}

	public void setFilesTotal(int filesTotal) {
		this.filesTotal = filesTotal;
	}

	public void logPreStart(boolean interactive) {
		Sync.log.info("########################################");
		Sync.log.info("Processing " + source + " > " + target);
		if (interactive) {
			Sync.log.info("Counting ...");
		}
	}

	public void logStart() {
		Sync.log.info("Files: " + filesTotal);
		Sync.log.info("Size: " + sizeTotal);
	}

	public void logEnd() {
		Sync.log.info("Files added: " + filesAdded.get());
		Sync.log.info("Files deleted: " + filesDeleted.get());
		Sync.log.info("Files updated: " + filesUpdated.get());
		Sync.log.info("########################################");
	}

	public void log(boolean interactive) {
		if (!interactive) {
			return;
		}
		System.out.print("  > " + (filesAdded.get() + filesUpdated.get() + filesOkay.get()) + "/" + filesTotal + "\r");
	}

}
