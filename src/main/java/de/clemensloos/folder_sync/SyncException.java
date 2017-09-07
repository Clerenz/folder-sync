package de.clemensloos.folder_sync;

/**
 * My own exception. Great.
 */
public class SyncException extends Exception {

	private static final long serialVersionUID = -4224258588015236273L;

	public SyncException(String msg) {
		super(msg);
	}

	public SyncException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
