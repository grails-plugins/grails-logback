package grails.plugin.logback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.groovy.grails.exceptions.StackTraceFilterer;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Based on DefaultStackTraceFilterer but doesn't use a static logger.
 *
 * @author Graeme Rocher
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
public class LogbackStackTraceFilterer implements StackTraceFilterer {

	protected List<String> packagesToFilter = new ArrayList<String>(Arrays.asList(new String[] {
			"com.opensymphony.",
			"com.springsource.loaded.",
			"gant.",
			"groovy.",
			"java.lang.reflect.",
			"javax.servlet.",
			"net.sf.cglib.proxy.",
			"org.apache.catalina.",
			"org.apache.coyote.",
			"org.apache.tomcat.",
			"org.codehaus.gant.",
			"org.codehaus.groovy.ast.",
			"org.codehaus.groovy.grails.",
			"org.codehaus.groovy.reflection.",
			"org.codehaus.groovy.runtime.",
			"org.grails.plugin.resource.DevMode",
			"org.hibernate.",
			"org.mortbay.",
			"org.springframework.",
			"org.springsource.loaded.",
			"sun."
		}));
	protected boolean shouldFilter = !Boolean.getBoolean(SYS_PROP_DISPLAY_FULL_STACKTRACE);
	protected String cutOffPackage;

	public void addInternalPackage(String name) {
		Assert.notNull(name, "Package name cannot be null");
		packagesToFilter.add(name);
	}

	public void setCutOffPackage(String name) {
		cutOffPackage = name;
	}

	public Throwable filter(Throwable source, boolean recursive) {
		if (recursive) {
			Throwable current = source;
			while (current != null) {
				current = filter(current);
				current = current.getCause();
			}
		}
		return filter(source);
	}

	public Throwable filter(Throwable source) {
		if (shouldFilter) {
			StackTraceElement[] trace = source.getStackTrace();
			List<StackTraceElement> newTrace = filterTraceWithCutOff(trace, cutOffPackage);

			if (newTrace.isEmpty()) {
				// filter with no cut-off so at least there is some trace
				newTrace = filterTraceWithCutOff(trace, null);
			}

			// Only trim the trace if there was some application trace on the stack
			// if not we will just skip sanitizing and leave it as is
			if (!newTrace.isEmpty()) {
				// We don't want to lose anything, so log it
				LoggerFactory.getLogger("StackTrace").error(FULL_STACK_TRACE_MESSAGE, source);
				StackTraceElement[] clean = new StackTraceElement[newTrace.size()];
				newTrace.toArray(clean);
				source.setStackTrace(clean);
			}
		}
		return source;
	}

	protected List<StackTraceElement> filterTraceWithCutOff(StackTraceElement[] trace, String endPackage) {
		List<StackTraceElement> newTrace = new ArrayList<StackTraceElement>();
		boolean foundGroovy = false;
		for (StackTraceElement stackTraceElement : trace) {
			String className = stackTraceElement.getClassName();
			String fileName = stackTraceElement.getFileName();
			if (!foundGroovy && fileName != null && fileName.endsWith(".groovy")) {
				foundGroovy = true;
			}
			if (endPackage != null && className.startsWith(endPackage) && foundGroovy) break;
			if (isApplicationClass(className)) {
				if (stackTraceElement.getLineNumber() > -1) {
					newTrace.add(stackTraceElement);
				}
			}
		}
		return newTrace;
	}

	/**
	 * Whether the given class name is an internal class and should be filtered
	 * @param className The class name
	 * @return true if is internal
	 */
	protected boolean isApplicationClass(String className) {
		for (String packageName : packagesToFilter) {
			if (className.startsWith(packageName)) return false;
		}
		return true;
	}

	public void setShouldFilter(boolean filter) {
		shouldFilter = filter;
	}
}
