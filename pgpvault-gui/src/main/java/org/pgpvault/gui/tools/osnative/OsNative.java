package org.pgpvault.gui.tools.osnative;

public interface OsNative {
	/**
	 * Resolve command line args using OS native API.
	 * 
	 * @param fallBackTo
	 *            these will be returned if call is not supported on current OS
	 *            or error occurred
	 * @return command lines args array
	 */
	String[] getCommandLineArguments(String[] fallBackTo);
}
