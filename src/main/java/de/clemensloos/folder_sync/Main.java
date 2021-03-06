package de.clemensloos.folder_sync;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The front end somewhat. Read the properties files, create the sync instances
 * and run. Simply replace this class with a UI and get a great application.
 */
public class Main {

	private static final Logger logger = LogManager.getLogger(Main.class);

	private static final String pathSep = ";";

	public static void main(String args[]) throws FileNotFoundException, IOException {

		if (args.length == 0) {
			readAndRun("folder-sync.properties");
		} else {
			for (String s : args) {
				readAndRun(s);
			}
		}
	}

	private static void readAndRun(String propertiesFile) {

		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(propertiesFile));
		} catch (IOException e) {
			logger.error("Input configuration '" + propertiesFile + "' not found or not readable.", e);
			return;
		}

		SyncConfig config = new SyncConfig();
		boolean compareSize = Boolean.valueOf(prop.getProperty("compareSize", "false"));
		boolean compareChecksum = Boolean.valueOf(prop.getProperty("compareChecksum", "false"));
		boolean random = Boolean.valueOf(prop.getProperty("random", "false"));
		int keepHistory = Integer.valueOf(prop.getProperty("keepHistory", "0"));
		boolean interactive = Boolean.valueOf(prop.getProperty("interactive", "true"));
		config.setCompareChecksum(compareChecksum);
		config.setCompareSize(compareSize);
		config.setRandom(random);
		config.setKeepHistory(keepHistory);
		config.setInteractive(interactive);
		String filenameFilter = prop.getProperty("filenameFilter", "");
		String pathFilter = prop.getProperty("pathFilter", "");
		String extensionFilter = prop.getProperty("extensionFilter", "");
		config.setFilter(filenameFilter.split(pathSep), pathFilter.split(pathSep), extensionFilter.split(pathSep));

		for (int i = 1; i < 100; i++) {
			String target = prop.getProperty("target." + i);
			String sourceString = prop.getProperty("source." + i);
			if (sourceString == null || target == null) {
				logger.debug("Input configuration, 'source." + i + "' or 'target." + i + "' not set in '"
						+ propertiesFile + "'. Skip further reading.");
				break;
			}

			if (sourceString.contains(pathSep)) {
				for (String source : sourceString.split(pathSep)) {
					try {
						Target t = new Target(source, target, true);
						config.addTarget(t);
					} catch (SyncException e) {
						logger.error(e.getMessage(), e);
						continue;
					}
				}
			} else {
				try {
					Target t = new Target(sourceString, target, false);
					config.addTarget(t);
				} catch (SyncException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}

		Sync sync = new Sync(config);
		sync.sync();
	}

}
