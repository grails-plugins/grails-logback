package grails.plugin.logback;

import java.io.PrintStream;

/**
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class LogLog {

	public static final String DEBUG_KEY = "logback.debug";

	protected static final String PREFIX = "logback: ";
	protected static final String ERR_PREFIX = "logback:ERROR ";
	protected static final String WARN_PREFIX = "logback:WARN ";

	protected static boolean debugEnabled = false;
	protected static boolean quietMode = false;

	protected static LogbackStackTraceFilterer logbackStackTraceFilterer = new LogbackStackTraceFilterer();

	static {
		String key = getSystemProperty(DEBUG_KEY, null);
		if (key != null) {
			debugEnabled = !"false".equals(key.trim());
		}
	}

	public static void setInternalDebugging(boolean enabled) {
		debugEnabled = enabled;
	}

	public static void setQuietMode(boolean quiet) {
		quietMode = quiet;
	}

	public static void debug(String message) {
		debug(message, null);
	}

	public static void debug(String message, Throwable t) {
		if (debugEnabled) {
			log(message, t, false, PREFIX);
		}
	}

	public static void error(String message) {
		error(message, null);
	}

	public static void error(String message, Throwable t) {
		log(message, t, true, ERR_PREFIX);
	}

	public static void warn(String message) {
		warn(message, null);
	}

	public static void warn(String message, Throwable t) {
		log(message, t, true, WARN_PREFIX);
	}

	public static void log(String message, Throwable t, boolean error, String prefix) {
		if (quietMode) {
			return;
		}

		PrintStream ps = error ? System.err : System.out;

		ps.println(prefix + message);
		if (t != null) {
			logbackStackTraceFilterer.filter(t).printStackTrace(ps);
		}
	}

	protected static String getSystemProperty(String key, String defaultValue) {
		try {
			return System.getProperty(key, defaultValue);
		}
		catch (Throwable e) { // MS-Java throws com.ms.security.SecurityExceptionEx
			debug("Was not allowed to read system property \"" + key + "\".");
			return defaultValue;
		}
	}
}
