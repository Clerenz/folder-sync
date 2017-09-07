package de.clemensloos.folder_sync;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The sync engine.
 */
public class Sync {

	public static final String SLASH = "/";

	private SyncConfig config;
	public static Logger log = LogManager.getLogger(Sync.class);

	public Sync(SyncConfig config) {
		this.config = config;
	}

	public void sync() {

		for (Target t : config.getTargetList()) {

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

		File source = new File(t.getSource() + (file.equals("") ? "" : SLASH + file));
		File target = new File(t.getTarget() + (file.equals("") ? "" : SLASH + file));

		// Handle directory
		if (source.isDirectory()) {
			if (target.exists() && target.isDirectory()) {
				for (String child : source.list(config.getFilenameFilter())) {
					resursiveSync(t, file + SLASH + child);
				}
				for (String child : target.list()) {
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
					log.debug("Replace newer file " + file);
					t.fileUpdated();
					target.delete();
					FileUtils.copyFile(source, target);
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
			log.debug("Copy new file " + file);
			t.fileAdded();
			FileUtils.copyFile(source, target);
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
			target.mkdir();
			for (File sourceChild : source.listFiles()) {
				copyDirectory(sourceChild, new File(target, sourceChild.getName()), t);
			}
			target.setLastModified(source.lastModified());
		} else {
			t.fileAdded();
			FileUtils.copyFile(source, target);
		}
	}

}
