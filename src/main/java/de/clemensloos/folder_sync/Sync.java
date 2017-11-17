package de.clemensloos.folder_sync;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The sync engine.
 */
public class Sync {

	public static final String SLASH = "/";
	public static Logger log = LogManager.getLogger(Sync.class);
	private static volatile boolean keepGoing = true;
	
	private SyncConfig config;

	public Sync(SyncConfig config) {
		this.config = config;
	}

	public void sync() {
		
		Thread.currentThread().setName("MAIN");
		
		ShutdownHook shutdownHook = new ShutdownHook();
		Runtime.getRuntime().addShutdownHook(shutdownHook);

		for (Target t : config.getTargetList()) {

			Thread thread = new SyncThread(t);
			shutdownHook.setCurrentThread(thread);
			thread.start();
			try {
				thread.join();
			} catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
			shutdownHook.setCurrentThread(null);
		}
	}
	
	
	private class ShutdownHook extends Thread {
		
		private Thread currentThread = null;
		
		void setCurrentThread(Thread thread) {
			this.currentThread = thread;
		}
		
		@Override
		public void run() {
			Thread.currentThread().setName("HOOK");
			keepGoing = false;
			if (currentThread != null) {
				log.info("Safely shutting down ...");
				try {
					currentThread.join();
				} catch (InterruptedException e) {
					log.error(e.getMessage(), e);
				}
			}
		}
		
	}
	
	
	private class SyncThread extends Thread {

		private Target t;
		
		SyncThread(Target t) {
			this.t = t;
		}
		
		@Override
		public void run() {
			
			try {
				t.logPreStart();
				t.setFilesTotal(countFiles(t.getSourceFile()));
				t.setSizeTotal(sizeOf(t.getSourceFile()));
				t.logStart();
				resursiveSync(t, "");
				t.logEnd();

			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	private void resursiveSync(Target t, String file) throws IOException {
		
		if (!keepGoing) {
			return;
		}

		File source = new File(t.getSource() + (file.equals("") ? "" : SLASH + file));
		File target = new File(t.getTarget() + (file.equals("") ? "" : SLASH + file));

		// Handle directory
		if (source.isDirectory()) {
			if (target.exists() && target.isDirectory()) {
				String[] sourceChildArray = source.list(config.getFilenameFilter());
				if (sourceChildArray != null) {
					List<String> sourceChildList = Arrays.asList(sourceChildArray);
					if (config.isRandom()) {
						Collections.shuffle(sourceChildList);
					}
					for (String child : sourceChildList) {
						resursiveSync(t, file + SLASH + child);
					}
				}
				String[] targetChildArray = target.list();
				if (targetChildArray == null) {
					return;
				}
				List<String> targetChildList = Arrays.asList(targetChildArray);
				if (config.isRandom()) {
					Collections.shuffle(targetChildList);
				}
				for (String child : targetChildList) {
					if (!new File(source, child).exists() || !config.getFilenameFilter().accept(target, child)) {
						log.debug("Delete obsolete file " + file + SLASH + child);
						if (new File(target, child).isDirectory()) {
							t.filesDeleted(countFiles(new File(target, child)));
							FileUtils.deleteDirectory(new File(target, child));
						} else {
							t.fileDeleted();
							new File(target, child).delete();
						}
					}
				}
				return;
			}
			if (target.exists() && !target.isDirectory()) {
				log.debug("Delete wrong file " + file);
				t.fileDeleted();
				target.delete();
			}
			log.debug("Copy new directory " + file);
			copyDirectory(source, target, t);
			return;
		}

		// Handle file
		if (source.isFile()) {
			if (target.exists() && target.isFile()) {
				if (needsUpdate(source, target)) {
					if(source.canRead()) {
						log.debug("Replace newer file " + file);
						target.delete();
						try {
							FileUtils.copyFile(source, target);
							t.fileUpdated();
						} catch (FileNotFoundException e) {
							log.info("Skip replacing locked file " + source.getAbsolutePath());
						}
					}
					else {
						log.info("Skip replacing locked file " + source.getAbsolutePath());
					}
					return;
				}
				log.trace("Skipping file " + file);
				t.fileOkay();
				return;
			}
			if (target.exists() && !target.isFile()) {
				log.debug("Delete wrong folder " + file);
				t.filesDeleted(countFiles(target));
				FileUtils.deleteDirectory(target);
			}
			if (source.canRead()) {
				log.debug("Copy new file " + file);
				try {
					FileUtils.copyFile(source, target);
					t.fileAdded();
				} catch (FileNotFoundException e) {
					log.info("Skip locked file " + source.getAbsolutePath());
				}
			} else {
				log.info("Skip locked file " + source.getAbsolutePath());
			}
		}
	}

	/**
	 * Check whether a file needs to be updated.
	 * 
	 * @param source
	 * @param target
	 * @return true if the file requires an update.
	 * @throws IOException
	 */
	private boolean needsUpdate(File source, File target) throws IOException {
		return (FileUtils.isFileNewer(source, target)
				|| (config.isCompareSize() && FileUtils.sizeOf(source) != FileUtils.sizeOf(target))
				|| (config.isCompareChecksum() && FileUtils.checksumCRC32(source) != FileUtils.checksumCRC32(target)));
	}

	/**
	 * Get number of files in subdirectories
	 * 
	 * @param f
	 *            File or folder
	 * @return number of files
	 */
	private int countFiles(File f) {
		if (f.isDirectory()) {
			int c = 0;
			if (f.listFiles() == null) {
				return 0;
			}
			for (File child : f.listFiles()) {
				c += countFiles(child);
			}
			return c;
		} else {
			return 1;
		}
	}

	/**
	 * Get file / folder size human readable
	 * 
	 * @param f
	 *            File or folder
	 * @return e. g. 120 KB or 7 MB
	 */
	private String sizeOf(File f) {
		return FileUtils.byteCountToDisplaySize(FileUtils.sizeOfAsBigInteger(f));
	}

	/**
	 * Recursively copy a directory.
	 * 
	 * @param source
	 * @param target
	 * @param t
	 * @throws IOException
	 */
	public static void copyDirectory(File source, File target, Target t) throws IOException {
		if (source.isDirectory()) {
			if (source.list() != null) {
				target.mkdir();
				for (File sourceChild : source.listFiles()) {
					copyDirectory(sourceChild, new File(target, sourceChild.getName()), t);
				}
				target.setLastModified(source.lastModified());
			}
		} else {
			if (source.canRead()) {
				try {
					FileUtils.copyFile(source, target);
					t.fileAdded();
				} catch (FileNotFoundException e) {
					log.info("Skip locked file " + source.getAbsolutePath());
				}
			}
			else {
				log.info("Skip locked file " + source.getAbsolutePath());
			}
		}
	}

}
